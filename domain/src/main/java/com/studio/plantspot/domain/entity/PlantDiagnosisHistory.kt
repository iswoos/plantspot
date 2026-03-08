package com.studio.plantspot.domain.entity

import java.time.OffsetDateTime

data class PlantDiagnosisHistory(
    val id: String? = null,
    val plantId: String,
    val healthStatus: String,
    val healthScore: Int,
    val analysis: String?,
    val solution: String?,
    val imageUrl: String?,
    val createdAt: OffsetDateTime? = null
)
