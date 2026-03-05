package com.studio.plantspot.ui.screens.lounge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.LoungeRepository
import com.studio.plantspot.data.repository.PostUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 커뮤니티(라운지) ViewModel
 * 포스트 목록 조회, 좋아요 토글, 삭제 처리
 */
class LoungeViewModel : ViewModel() {

    private val _posts = MutableStateFlow<List<PostUiModel>>(emptyList())
    val posts: StateFlow<List<PostUiModel>> = _posts.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val currentUserId get() = UserPreferences.getUserId()

    /**
     * 포스트 목록 로드
     */
    fun loadPosts() {
        viewModelScope.launch {
            _isLoading.value = true
            val dtoList = LoungeRepository.getPosts()
            val likedIds = LoungeRepository.getLikedPostIds(currentUserId)
            _posts.value = dtoList.map { dto ->
                PostUiModel(
                    id = dto.id ?: "",
                    userId = dto.userId,
                    authorName = dto.authorName,
                    plantAlias = dto.plantAlias,
                    content = dto.content,
                    imageUrl = dto.imageUrl,
                    likeCount = dto.likeCount,
                    createdAt = dto.createdAt ?: "",
                    isLiked = likedIds.contains(dto.id)
                )
            }
            _isLoading.value = false
        }
    }

    /**
     * 좋아요 토글 (즉각적 UI 반영 후 Supabase 동기화)
     */
    fun toggleLike(postId: String) {
        val post = _posts.value.find { it.id == postId } ?: return
        // 즉각적 UI 반영 (Optimistic Update)
        _posts.value = _posts.value.map {
            if (it.id == postId) it.copy(
                isLiked = !it.isLiked,
                likeCount = if (it.isLiked) it.likeCount - 1 else it.likeCount + 1
            ) else it
        }
        viewModelScope.launch {
            LoungeRepository.toggleLike(postId, currentUserId, post.isLiked)
        }
    }

    /**
     * 포스트 삭제
     */
    fun deletePost(postId: String) {
        viewModelScope.launch {
            val success = LoungeRepository.deletePost(postId)
            if (success) {
                _posts.value = _posts.value.filter { it.id != postId }
            }
        }
    }
}
