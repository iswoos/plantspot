package com.studio.plantspot.domain.entity

import kotlinx.serialization.Serializable

@Serializable
data class DiagnosisResult(
    val spaceAnalysis: String? = null,
    val recommendedPlants: List<RecommendedPlant>? = null,
    val plantName: String? = null,
    val healthStatus: String? = null,
    val analysis: String? = null,
    val measuredLux: Float? = null,
    val matchScore: Int? = null,
    val solution: String? = null,
    val careTips: CareTips? = null
)

@Serializable
data class RecommendedPlant(
    val name: String,
    val reason: String,
    val suitabilityScore: Int
)

@Serializable
data class CareTips(
    val humidity: String,
    val soil: String
)
