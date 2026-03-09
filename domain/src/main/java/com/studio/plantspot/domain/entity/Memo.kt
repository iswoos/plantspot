package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

data class Memo(
    val id: String,
    val userId: String,
    val title: String,
    val content: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)
