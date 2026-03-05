package com.studio.plantspot.data.remote.model

import kotlinx.serialization.Serializable

/**
 * [모드 1] 공간진단 응답 모델
 */
@Serializable
data class SpaceAnalysisResponse(
    val space_analysis: String,
    val recommended_plants: List<RecommendedPlant>
)

@Serializable
data class RecommendedPlant(
    val name: String,
    val reason: String,
    val suitability_score: Int
)

/**
 * [모드 2] 식물진단 응답 모델
 */
@Serializable
data class PlantDiagnosisResponse(
    val plant_name: String,
    val health_status: String, // normal, warning, danger
    val analysis: String,
    val measured_lux: Int,
    val match_score: Int,
    val solution: String,
    val care_tips: CareTips
)

@Serializable
data class CareTips(
    val humidity: String,
    val soil: String
)
