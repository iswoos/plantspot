package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

data class UserPlant(
    val id: String,
    val userId: String,
    val nickname: String,
    val officialName: String,
    val imageUrl: String?,
    val matchScore: Int,
    val waterPeriod: Int,
    val lastWateredAt: OffsetDateTime,
    val lastMeasuredAt: OffsetDateTime,
    val createdAt: OffsetDateTime
)
