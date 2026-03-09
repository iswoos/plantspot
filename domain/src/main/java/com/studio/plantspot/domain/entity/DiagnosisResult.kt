package com.studio.plantspot.domain.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DiagnosisResult(
    @SerialName("space_analysis")
    val spaceAnalysis: String? = null,
    @SerialName("recommended_plants")
    val recommendedPlants: List<RecommendedPlant>? = null,
    val plantName: String? = null,
    val healthStatus: String? = null,
    val analysis: String? = null,
    val measuredLux: Float? = null,
    val matchScore: Int? = null,
    @SerialName("suitability_score")
    val suitabilityScore: Int? = null,
    val solution: String? = null,
    val careTips: CareTips? = null
)

@Serializable
data class RecommendedPlant(
    val name: String,
    val reason: String,
    @SerialName("suitability_score")
    val suitabilityScore: Int
)

@Serializable
data class CareTips(
    val humidity: String,
    val soil: String
)
