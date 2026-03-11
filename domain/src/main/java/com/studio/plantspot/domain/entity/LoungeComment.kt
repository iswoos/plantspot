package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

/**
 * 플랜트 라운지 커뮤니티 댓글 엔티티
 * - parentId가 null이면 최상위 댓글
 * - parentId가 있으면 대댓글 (depth 2)
 */
data class LoungeComment(
    val id: String,
    val postId: String,
    val userId: String,
    /** null = 최상위 댓글 / non-null = 대댓글 */
    val parentId: String?,
    val content: String,
    val likeCount: Int,
    /** plantspot_users JOIN으로 가져온 작성자 닉네임 */
    val authorNickname: String?,
    /** 현재 로그인한 유저의 좋아요 여부 */
    val isLikedByMe: Boolean,
    /** 대댓글 목록 (최상위 댓글인 경우에만 채워짐) */
    val replies: List<LoungeComment>,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
