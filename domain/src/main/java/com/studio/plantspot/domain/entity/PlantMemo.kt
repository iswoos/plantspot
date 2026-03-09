package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

data class PlantMemo(
    val id: String,
    val plantId: String,
    val content: String,
    val imageUrl: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
