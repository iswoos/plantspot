package com.studio.plantspot.data.repository

import com.studio.plantspot.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── DTO 정의 ──

@Serializable
data class PostDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("author_name") val authorName: String,
    @SerialName("plant_alias") val plantAlias: String,
    val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class LikeDto(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String
)

@Serializable
data class CommentDto(
    val id: String? = null,
    @SerialName("post_id") val postId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("author_name") val authorName: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

// ── UI 모델 ──

data class PostUiModel(
    val id: String,
    val userId: String,
    val authorName: String,
    val plantAlias: String,
    val content: String,
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val createdAt: String = "",
    val isLiked: Boolean = false
)

data class CommentUiModel(
    val id: String,
    val authorName: String,
    val content: String,
    val createdAt: String
)

// ── 저장소 ──

object LoungeRepository {

    private val db get() = SupabaseClient.client.postgrest

    /**
     * 포스트 목록 최신순 조회
     */
    suspend fun getPosts(): List<PostDto> {
        return try {
            db["plantspot_posts"]
                .select(Columns.ALL)
                .decodeList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 포스트 작성 (INSERT)
     */
    suspend fun createPost(
        userId: String,
        authorName: String,
        plantAlias: String,
        content: String,
        imageUrl: String? = null
    ): Boolean {
        return try {
            db["plantspot_posts"].insert(
                PostDto(
                    userId = userId,
                    authorName = authorName,
                    plantAlias = plantAlias,
                    content = content,
                    imageUrl = imageUrl
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 포스트 수정 (UPDATE)
     */
    suspend fun updatePost(postId: String, content: String): Boolean {
        return try {
            db["plantspot_posts"].update(mapOf("content" to content)) {
                filter { eq("id", postId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 포스트 삭제 (DELETE)
     */
    suspend fun deletePost(postId: String): Boolean {
        return try {
            db["plantspot_posts"].delete {
                filter { eq("id", postId) }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 좋아요 토글 (UPSERT / DELETE)
     * Supabase unique(post_id, user_id) 제약 활용
     */
    suspend fun toggleLike(postId: String, userId: String, isCurrentlyLiked: Boolean): Boolean {
        return try {
            if (isCurrentlyLiked) {
                db["plantspot_likes"].delete {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                // like_count 감소
                db["plantspot_posts"].update(mapOf("like_count" to "like_count - 1")) {
                    filter { eq("id", postId) }
                }
            } else {
                db["plantspot_likes"].insert(LikeDto(postId = postId, userId = userId))
                // like_count 증가
                db["plantspot_posts"].update(mapOf("like_count" to "like_count + 1")) {
                    filter { eq("id", postId) }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 댓글 목록 조회
     */
    suspend fun getComments(postId: String): List<CommentDto> {
        return try {
            db["plantspot_comments"]
                .select(Columns.ALL) {
                    filter { eq("post_id", postId) }
                }
                .decodeList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 댓글 작성 (INSERT)
     */
    suspend fun addComment(
        postId: String,
        userId: String,
        authorName: String,
        content: String
    ): Boolean {
        return try {
            db["plantspot_comments"].insert(
                CommentDto(
                    postId = postId,
                    userId = userId,
                    authorName = authorName,
                    content = content
                )
            )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 내가 좋아요한 postId 집합 조회
     */
    suspend fun getLikedPostIds(userId: String): Set<String> {
        return try {
            db["plantspot_likes"]
                .select(Columns.list("post_id")) {
                    filter { eq("user_id", userId) }
                }
                .decodeList<Map<String, String>>()
                .mapNotNull { it["post_id"] }
                .toSet()
        } catch (e: Exception) {
            e.printStackTrace()
            emptySet()
        }
    }
}
