package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.LoungeComment
import com.studio.plantspot.domain.entity.LoungePost
import com.studio.plantspot.domain.repository.LoungeRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onStart
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.inject.Inject

// ─────────────────────────────────────────────────────────────────────────────
// DTO 정의 (내부 전용)
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
private data class PostDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val title: String,
    val content: String,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList(),
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class PostInsertDto(
    @SerialName("user_id") val userId: String,
    val title: String,
    val content: String,
    @SerialName("image_urls") val imageUrls: List<String> = emptyList()
)

@Serializable
private data class PostUpdateDto(
    val title: String,
    val content: String
)

@Serializable
private data class PostIdDto(val id: String)

@Serializable
private data class CommentDto(
    val id: String,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_id") val parentId: String? = null,
    val content: String,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class CommentInsertDto(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("parent_id") val parentId: String? = null,
    val content: String
)

@Serializable
private data class CommentUpdateDto(
    val content: String
)

@Serializable
private data class PostLikeDto(
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class CommentLikeDto(
    @SerialName("comment_id") val commentId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
private data class ProfileNicknameDto(
    val id: String,
    val nickname: String? = null,
    @SerialName("display_name") val displayName: String? = null
)

// ─────────────────────────────────────────────────────────────────────────────
// Repository 구현체
// ─────────────────────────────────────────────────────────────────────────────

class LoungeRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : LoungeRepository {

    /**
     * 데이터 갱신 신호 (추가/수정/삭제 후 Flow 재조회 트리거)
     */
    private val refreshSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private suspend fun triggerRefresh() {
        refreshSignal.emit(Unit)
    }

    // ─── 닉네임 캐시 헬퍼 ──────────────────────────────────────────────────

    /**
     * 유저 ID 목록 → 닉네임 맵 조회
     */
    private suspend fun fetchNicknameMap(userIds: List<String>): Map<String, String?> {
        if (userIds.isEmpty()) return emptyMap()
        return try {
            supabase.postgrest
                .from("plantspot_users")
                .select {
                    filter { isIn("id", userIds) }
                }
                .decodeList<ProfileNicknameDto>()
                .associate { it.id to (it.nickname ?: it.displayName) }
        } catch (e: Exception) {
            android.util.Log.e("LoungeRepository", "닉네임 조회 실패", e)
            emptyMap()
        }
    }

    // ─── 게시글 ────────────────────────────────────────────────────────────

    override fun getPosts(page: Int, pageSize: Int): Flow<List<LoungePost>> =
        refreshSignal
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    val currentUserId = supabase.auth.currentUserOrNull()?.id

                    // 1. 게시글 목록 조회 (최신순)
                    val posts = supabase.postgrest
                        .from("plantspot_user_lounge_posts")
                        .select {
                            order("created_at", Order.DESCENDING)
                            range(
                                from = (page * pageSize).toLong(),
                                to = (page * pageSize + pageSize - 1).toLong()
                            )
                        }
                        .decodeList<PostDto>()

                    // 2. 작성자 닉네임 조회
                    val userIds = posts.map { it.userId }.distinct()
                    val nicknameMap = fetchNicknameMap(userIds)

                    // 3. 현재 유저의 좋아요 여부 조회
                    val likedPostIds = if (currentUserId != null && posts.isNotEmpty()) {
                        fetchLikedPostIds(currentUserId, posts.map { it.id })
                    } else emptySet()

                    // 4. 도메인 엔티티로 변환
                    val result = posts.map { dto ->
                        dto.toDomain(
                            authorNickname = nicknameMap[dto.userId],
                            isLikedByMe = dto.id in likedPostIds
                        )
                    }
                    emit(result)
                }
            }

    override suspend fun getPostById(postId: String): LoungePost? {
        val currentUserId = supabase.auth.currentUserOrNull()?.id
        val dto = supabase.postgrest
            .from("plantspot_user_lounge_posts")
            .select { filter { eq("id", postId) } }
            .decodeSingleOrNull<PostDto>() ?: return null

        val nicknameMap = fetchNicknameMap(listOf(dto.userId))
        val likedPostIds = if (currentUserId != null) {
            fetchLikedPostIds(currentUserId, listOf(postId))
        } else emptySet()

        return dto.toDomain(
            authorNickname = nicknameMap[dto.userId],
            isLikedByMe = postId in likedPostIds
        )
    }

    override suspend fun createPost(title: String, content: String): String {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val response = supabase.postgrest
            .from("plantspot_user_lounge_posts")
            .insert(PostInsertDto(userId = userId, title = title, content = content)) {
                select()
            }
            .decodeSingle<PostIdDto>()

        triggerRefresh()
        return response.id
    }

    override suspend fun updatePost(postId: String, title: String, content: String) {
        supabase.postgrest
            .from("plantspot_user_lounge_posts")
            .update(PostUpdateDto(title = title, content = content)) {
                filter { eq("id", postId) }
            }
        triggerRefresh()
    }

    override suspend fun deletePost(postId: String) {
        supabase.postgrest
            .from("plantspot_user_lounge_posts")
            .delete { filter { eq("id", postId) } }
        triggerRefresh()
    }

    override suspend fun togglePostLike(postId: String): Boolean {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val alreadyLiked = fetchLikedPostIds(userId, listOf(postId)).isNotEmpty()

        if (alreadyLiked) {
            // 좋아요 취소
            supabase.postgrest
                .from("plantspot_user_lounge_post_likes")
                .delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
            triggerRefresh()
            return false
        } else {
            // 좋아요 추가
            supabase.postgrest
                .from("plantspot_user_lounge_post_likes")
                .insert(PostLikeDto(postId = postId, userId = userId))
            triggerRefresh()
            return true
        }
    }

    private suspend fun fetchLikedPostIds(userId: String, postIds: List<String>): Set<String> {
        if (postIds.isEmpty()) return emptySet()
        return try {
            supabase.postgrest
                .from("plantspot_user_lounge_post_likes")
                .select {
                    filter {
                        eq("user_id", userId)
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<PostLikeDto>()
                .map { it.postId }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ─── 댓글 ──────────────────────────────────────────────────────────────

    override fun getComments(postId: String): Flow<List<LoungeComment>> =
        refreshSignal
            .onStart { emit(Unit) }
            .flatMapLatest {
                flow {
                    val currentUserId = supabase.auth.currentUserOrNull()?.id

                    // 1. 해당 게시글의 모든 댓글 조회 (flat list)
                    val allComments = supabase.postgrest
                        .from("plantspot_user_lounge_comments")
                        .select {
                            filter { eq("post_id", postId) }
                            order("created_at", Order.ASCENDING)
                        }
                        .decodeList<CommentDto>()

                    // 2. 작성자 닉네임 조회
                    val userIds = allComments.map { it.userId }.distinct()
                    val nicknameMap = fetchNicknameMap(userIds)

                    // 3. 현재 유저의 댓글 좋아요 여부 조회
                    val likedCommentIds = if (currentUserId != null && allComments.isNotEmpty()) {
                        fetchLikedCommentIds(currentUserId, allComments.map { it.id })
                    } else emptySet()

                    // 4. flat list → 트리 구조 변환 (parent/replies)
                    val commentMap = allComments.associate { dto ->
                        dto.id to dto.toDomain(
                            authorNickname = nicknameMap[dto.userId],
                            isLikedByMe = dto.id in likedCommentIds,
                            replies = emptyList()
                        )
                    }

                    // 최상위 댓글 + 대댓글 그룹핑
                    val repliesMap = allComments
                        .filter { it.parentId != null }
                        .groupBy { it.parentId!! }
                        .mapValues { (_, replyDtos) ->
                            replyDtos.map { dto ->
                                dto.toDomain(
                                    authorNickname = nicknameMap[dto.userId],
                                    isLikedByMe = dto.id in likedCommentIds,
                                    replies = emptyList()
                                )
                            }
                        }

                    val topLevelComments = allComments
                        .filter { it.parentId == null }
                        .map { dto ->
                            commentMap[dto.id]!!.copy(replies = repliesMap[dto.id] ?: emptyList())
                        }

                    emit(topLevelComments)
                }
            }

    override suspend fun createComment(
        postId: String,
        content: String,
        parentId: String?
    ): String {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val response = supabase.postgrest
            .from("plantspot_user_lounge_comments")
            .insert(
                CommentInsertDto(
                    postId = postId,
                    userId = userId,
                    parentId = parentId,
                    content = content
                )
            ) { select() }
            .decodeSingle<PostIdDto>()

        triggerRefresh()
        return response.id
    }

    override suspend fun updateComment(commentId: String, content: String) {
        supabase.postgrest
            .from("plantspot_user_lounge_comments")
            .update(CommentUpdateDto(content = content)) {
                filter { eq("id", commentId) }
            }
        triggerRefresh()
    }

    override suspend fun deleteComment(commentId: String) {
        supabase.postgrest
            .from("plantspot_user_lounge_comments")
            .delete { filter { eq("id", commentId) } }
        triggerRefresh()
    }

    override suspend fun toggleCommentLike(commentId: String): Boolean {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("로그인이 필요합니다.")

        val alreadyLiked = fetchLikedCommentIds(userId, listOf(commentId)).isNotEmpty()

        if (alreadyLiked) {
            supabase.postgrest
                .from("plantspot_user_lounge_comment_likes")
                .delete {
                    filter {
                        eq("comment_id", commentId)
                        eq("user_id", userId)
                    }
                }
            triggerRefresh()
            return false
        } else {
            supabase.postgrest
                .from("plantspot_user_lounge_comment_likes")
                .insert(CommentLikeDto(commentId = commentId, userId = userId))
            triggerRefresh()
            return true
        }
    }

    private suspend fun fetchLikedCommentIds(
        userId: String,
        commentIds: List<String>
    ): Set<String> {
        if (commentIds.isEmpty()) return emptySet()
        return try {
            supabase.postgrest
                .from("plantspot_user_lounge_comment_likes")
                .select {
                    filter {
                        eq("user_id", userId)
                        isIn("comment_id", commentIds)
                    }
                }
                .decodeList<CommentLikeDto>()
                .map { it.commentId }
                .toSet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // ─── DTO → 도메인 변환 ─────────────────────────────────────────────────

    private fun PostDto.toDomain(
        authorNickname: String?,
        isLikedByMe: Boolean
    ) = LoungePost(
        id = id,
        userId = userId,
        title = title,
        content = content,
        imageUrls = imageUrls,
        likeCount = likeCount,
        commentCount = commentCount,
        authorNickname = authorNickname,
        isLikedByMe = isLikedByMe,
        createdAt = OffsetDateTime.parse(createdAt),
        updatedAt = OffsetDateTime.parse(updatedAt)
    )

    private fun CommentDto.toDomain(
        authorNickname: String?,
        isLikedByMe: Boolean,
        replies: List<LoungeComment>
    ) = LoungeComment(
        id = id,
        postId = postId,
        userId = userId,
        parentId = parentId,
        content = content,
        likeCount = likeCount,
        authorNickname = authorNickname,
        isLikedByMe = isLikedByMe,
        replies = replies,
        createdAt = OffsetDateTime.parse(createdAt),
        updatedAt = OffsetDateTime.parse(updatedAt)
    )
}
