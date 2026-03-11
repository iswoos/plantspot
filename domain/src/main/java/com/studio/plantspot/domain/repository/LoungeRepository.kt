package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.LoungeComment
import com.studio.plantspot.domain.entity.LoungePost
import kotlinx.coroutines.flow.Flow

/**
 * 플랜트 라운지 커뮤니티 레포지토리 인터페이스
 */
interface LoungeRepository {

    // ─── 게시글 ───────────────────────────────────────────────

    /**
     * 게시글 목록 조회 (최신순 페이징)
     */
    fun getPosts(page: Int, pageSize: Int = 20): Flow<List<LoungePost>>

    /**
     * 게시글 단건 조회
     */
    suspend fun getPostById(postId: String): LoungePost?

    /**
     * 게시글 작성 → 생성된 게시글 ID 반환
     */
    suspend fun createPost(title: String, content: String): String

    /**
     * 게시글 수정
     */
    suspend fun updatePost(postId: String, title: String, content: String)

    /**
     * 게시글 삭제
     */
    suspend fun deletePost(postId: String)

    /**
     * 게시글 좋아요 토글
     * @return true = 좋아요 추가됨 / false = 좋아요 취소됨
     */
    suspend fun togglePostLike(postId: String): Boolean

    // ─── 댓글 ─────────────────────────────────────────────────

    /**
     * 댓글 목록 조회 (최상위 댓글 + 대댓글 포함)
     */
    fun getComments(postId: String): Flow<List<LoungeComment>>

    /**
     * 댓글 또는 대댓글 작성 → 생성된 댓글 ID 반환
     * @param parentId null이면 최상위 댓글, non-null이면 대댓글
     */
    suspend fun createComment(postId: String, content: String, parentId: String?): String

    /**
     * 댓글 수정
     */
    suspend fun updateComment(commentId: String, content: String)

    /**
     * 댓글 삭제
     */
    suspend fun deleteComment(commentId: String)

    /**
     * 댓글 좋아요 토글
     * @return true = 좋아요 추가됨 / false = 좋아요 취소됨
     */
    suspend fun toggleCommentLike(commentId: String): Boolean
}
