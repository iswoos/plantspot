package com.studio.plantspot.presentation.ui.lounge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.LoungeComment
import com.studio.plantspot.domain.entity.LoungePost
import com.studio.plantspot.domain.repository.LoungeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

// ─── UI State ─────────────────────────────────────────────────────────────────

sealed class LoungeListUiState {
    object Loading : LoungeListUiState()
    data class Success(val posts: List<LoungePost>) : LoungeListUiState()
    data class Error(val message: String) : LoungeListUiState()
}

sealed class LoungeDetailUiState {
    object Loading : LoungeDetailUiState()
    data class Success(
        val post: LoungePost,
        val comments: List<LoungeComment>
    ) : LoungeDetailUiState()
    data class Error(val message: String) : LoungeDetailUiState()
}

// ─── ViewModel ────────────────────────────────────────────────────────────────

@HiltViewModel
class LoungeViewModel @Inject constructor(
    private val loungeRepository: LoungeRepository
) : ViewModel() {

    private val _listUiState = MutableStateFlow<LoungeListUiState>(LoungeListUiState.Loading)
    val listUiState: StateFlow<LoungeListUiState> = _listUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow<LoungeDetailUiState>(LoungeDetailUiState.Loading)
    val detailUiState: StateFlow<LoungeDetailUiState> = _detailUiState.asStateFlow()

    /** 스낵바 메시지 이벤트 */
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // 현재 보고 있는 게시글 ID (상세 화면)
    private var currentPostId: String? = null

    // 좋아요 디바운싱용 Job 관리
    private val likeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()
    private val commentLikeJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // 원본 상태 저장 (롤백용)
    private val originalLikeStates = mutableMapOf<String, Boolean>()
    private val originalCommentLikeStates = mutableMapOf<String, Boolean>()

    // ─── 게시글 목록 ───────────────────────────────────────────────────────────

    /**
     * 게시글 목록 로드 (page 0 = 처음 20개)
     */
    fun loadPosts() {
        viewModelScope.launch {
            _listUiState.value = LoungeListUiState.Loading
            loungeRepository.getPosts(page = 0)
                .catch { e ->
                    _listUiState.value = LoungeListUiState.Error(
                        e.message ?: "게시글을 불러오는 중 오류가 발생했습니다."
                    )
                }
                .collect { posts ->
                    _listUiState.value = LoungeListUiState.Success(posts)
                }
        }
    }

    // ─── 게시글 상세 ───────────────────────────────────────────────────────────

    /**
     * 게시글 상세 + 댓글 로드
     */
    fun loadPostDetail(postId: String) {
        currentPostId = postId
        _detailUiState.value = LoungeDetailUiState.Loading

        // 게시글과 댓글을 동시에 수집
        viewModelScope.launch {
            runCatching {
                loungeRepository.getPostById(postId)
            }.onSuccess { post ->
                if (post == null) {
                    _detailUiState.value = LoungeDetailUiState.Error("게시글을 찾을 수 없습니다.")
                    return@launch
                }
                // 댓글 Flow 수집
                loungeRepository.getComments(postId)
                    .catch { e ->
                        _detailUiState.value = LoungeDetailUiState.Error(
                            e.message ?: "댓글을 불러오는 중 오류가 발생했습니다."
                        )
                    }
                    .collect { comments ->
                        // 게시글은 최신으로 다시 로드
                        val latestPost = runCatching {
                            loungeRepository.getPostById(postId)
                        }.getOrNull() ?: post
                        _detailUiState.value = LoungeDetailUiState.Success(latestPost, comments)
                    }
            }.onFailure { e ->
                _detailUiState.value = LoungeDetailUiState.Error(
                    e.message ?: "게시글을 불러오는 중 오류가 발생했습니다."
                )
            }
        }
    }

    // ─── 게시글 CRUD ───────────────────────────────────────────────────────────

    /**
     * 게시글 작성
     * @return 생성된 게시글 ID (라우팅용), 실패 시 null
     */
    suspend fun createPost(title: String, content: String): String? {
        return runCatching {
            loungeRepository.createPost(title, content)
        }.onFailure {
            _snackbarMessage.emit("게시글 작성 중 오류가 발생했습니다.")
        }.getOrNull()
    }

    /**
     * 게시글 수정
     */
    fun updatePost(postId: String, title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                loungeRepository.updatePost(postId, title, content)
            }.onSuccess {
                _snackbarMessage.emit("게시글이 수정되었습니다.")
                onSuccess()
            }.onFailure {
                _snackbarMessage.emit("게시글 수정 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 게시글 삭제
     */
    fun deletePost(postId: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                loungeRepository.deletePost(postId)
            }.onSuccess {
                _snackbarMessage.emit("게시글이 삭제되었습니다.")
                onSuccess()
            }.onFailure {
                _snackbarMessage.emit("게시글 삭제 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 게시글 좋아요 토글 (디바운싱 + 롤백 적용)
     */
    fun togglePostLike(postId: String) {
        // 1. 원본 상태 기록 (처음 한 번만)
        if (!originalLikeStates.containsKey(postId)) {
            val currentState = getCurrentPostLikeState(postId)
            if (currentState != null) {
                originalLikeStates[postId] = currentState
            }
        }

        // 2. UI 즉시 업데이트 (Optimistic Update)
        updatePostLikeUi(postId)

        // 3. 디바운싱 처리
        likeJobs[postId]?.cancel()
        likeJobs[postId] = viewModelScope.launch {
            kotlinx.coroutines.delay(500L) // 500ms 대기

            val originalState = originalLikeStates[postId]
            val latestState = getCurrentPostLikeState(postId)

            // 상태가 원래대로 돌아왔거나 정보가 유실되었다면 서버 요청 안 함
            if (originalState == null || originalState == latestState || latestState == null) {
                originalLikeStates.remove(postId)
                return@launch
            }

            runCatching {
                loungeRepository.togglePostLike(postId)
            }.onSuccess {
                // 성공 시 원본 상태 기록 삭제
                originalLikeStates.remove(postId)
            }.onFailure {
                // 실패 시 롤백 (null 체크 추가)
                _snackbarMessage.emit("좋아요 처리 중 오류가 발생했습니다.")
                originalState?.let { rollbackPostLike(postId, it) }
                originalLikeStates.remove(postId)
            }
        }
    }

    private fun getCurrentPostLikeState(postId: String): Boolean? {
        val listState = _listUiState.value
        if (listState is LoungeListUiState.Success) {
            return listState.posts.find { it.id == postId }?.isLikedByMe
        }
        val detailState = _detailUiState.value
        if (detailState is LoungeDetailUiState.Success && detailState.post.id == postId) {
            return detailState.post.isLikedByMe
        }
        return null
    }

    private fun updatePostLikeUi(postId: String) {
        // 목록 업데이트
        val listState = _listUiState.value
        if (listState is LoungeListUiState.Success) {
            val idx = listState.posts.indexOfFirst { it.id == postId }
            if (idx != -1) {
                val post = listState.posts[idx]
                val newPosts = listState.posts.toMutableList()
                newPosts[idx] = post.copy(
                    isLikedByMe = !post.isLikedByMe,
                    likeCount = post.likeCount + if (post.isLikedByMe) -1 else 1
                )
                _listUiState.value = LoungeListUiState.Success(newPosts)
            }
        }

        // 상세 업데이트
        val detailState = _detailUiState.value
        if (detailState is LoungeDetailUiState.Success && detailState.post.id == postId) {
            val post = detailState.post
            _detailUiState.value = detailState.copy(
                post = post.copy(
                    isLikedByMe = !post.isLikedByMe,
                    likeCount = post.likeCount + if (post.isLikedByMe) -1 else 1
                )
            )
        }
    }

    private fun rollbackPostLike(postId: String, originalState: Boolean) {
        // 현재 상태가 원본과 다를 때만 롤백 수행
        val currentState = getCurrentPostLikeState(postId)
        if (currentState != originalState) {
            updatePostLikeUi(postId)
        }
    }

    // ─── 댓글 CRUD ─────────────────────────────────────────────────────────────

    /**
     * 댓글 또는 대댓글 작성
     */
    fun createComment(postId: String, content: String, parentId: String? = null) {
        viewModelScope.launch {
            runCatching {
                loungeRepository.createComment(postId, content, parentId)
            }.onFailure {
                _snackbarMessage.emit("댓글 작성 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 댓글 수정
     */
    fun updateComment(commentId: String, content: String) {
        viewModelScope.launch {
            runCatching {
                loungeRepository.updateComment(commentId, content)
            }.onSuccess {
                _snackbarMessage.emit("댓글이 수정되었습니다.")
            }.onFailure {
                _snackbarMessage.emit("댓글 수정 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 댓글 삭제
     */
    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            runCatching {
                loungeRepository.deleteComment(commentId)
            }.onSuccess {
                _snackbarMessage.emit("댓글이 삭제되었습니다.")
            }.onFailure {
                _snackbarMessage.emit("댓글 삭제 중 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 댓글 좋아요 토글 (디바운싱 + 롤백 적용)
     */
    fun toggleCommentLike(commentId: String) {
        // 1. 원본 상태 기록
        if (!originalCommentLikeStates.containsKey(commentId)) {
            val currentState = getCurrentCommentLikeState(commentId)
            if (currentState != null) {
                originalCommentLikeStates[commentId] = currentState
            }
        }

        // 2. UI 즉시 업데이트
        updateCommentLikeUi(commentId)

        // 3. 디바운싱 처리
        commentLikeJobs[commentId]?.cancel()
        commentLikeJobs[commentId] = viewModelScope.launch {
            kotlinx.coroutines.delay(500L)

            val originalState = originalCommentLikeStates[commentId]
            val latestState = getCurrentCommentLikeState(commentId)

            if (originalState == null || originalState == latestState || latestState == null) {
                originalCommentLikeStates.remove(commentId)
                return@launch
            }

            runCatching {
                loungeRepository.toggleCommentLike(commentId)
            }.onSuccess {
                originalCommentLikeStates.remove(commentId)
            }.onFailure {
                _snackbarMessage.emit("좋아요 처리 중 오류가 발생했습니다.")
                originalState?.let { rollbackCommentLike(commentId, it) }
                originalCommentLikeStates.remove(commentId)
            }
        }
    }

    private fun getCurrentCommentLikeState(commentId: String): Boolean? {
        val detailState = _detailUiState.value
        if (detailState is LoungeDetailUiState.Success) {
            // 1. 게시글의 최상위 댓글에서 찾기
            val comment = detailState.comments.find { it.id == commentId }
            if (comment != null) return comment.isLikedByMe
            
            // 2. 대댓글(답글)에서 찾기
            detailState.comments.forEach { c ->
                val reply = c.replies.find { it.id == commentId }
                if (reply != null) return reply.isLikedByMe
            }
        }
        return null
    }

    private fun updateCommentLikeUi(commentId: String) {
        val detailState = _detailUiState.value
        if (detailState is LoungeDetailUiState.Success) {
            val newComments = detailState.comments.map { c ->
                if (c.id == commentId) {
                    // 최상위 댓글 업데이트
                    c.copy(
                        isLikedByMe = !c.isLikedByMe,
                        likeCount = c.likeCount + if (c.isLikedByMe) -1 else 1
                    )
                } else if (c.replies.any { it.id == commentId }) {
                    // 대댓글 업데이트
                    val newReplies = c.replies.map { r ->
                        if (r.id == commentId) {
                            r.copy(
                                isLikedByMe = !r.isLikedByMe,
                                likeCount = r.likeCount + if (r.isLikedByMe) -1 else 1
                            )
                        } else r
                    }
                    c.copy(replies = newReplies)
                } else c
            }
            _detailUiState.value = detailState.copy(comments = newComments)
        }
    }

    private fun rollbackCommentLike(commentId: String, originalState: Boolean) {
        val currentState = getCurrentCommentLikeState(commentId)
        if (currentState != originalState) {
            updateCommentLikeUi(commentId)
        }
    }
}
