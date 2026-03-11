package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

/**
 * 플랜트 라운지 커뮤니티 게시글 엔티티
 */
data class LoungePost(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    /** 추후 사진 첨부 기능 대비 (현재 UI 미구현) */
    val imageUrls: List<String>,
    val likeCount: Int,
    val commentCount: Int,
    /** plantspot_users JOIN으로 가져온 작성자 닉네임 */
    val authorNickname: String?,
    /** 현재 로그인한 유저의 좋아요 여부 */
    val isLikedByMe: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
