package com.studio.plantspot.data.repository

import android.graphics.Bitmap
import com.studio.plantspot.data.remote.GeminiDataSource
import com.studio.plantspot.data.remote.model.PlantDiagnosisResponse
import com.studio.plantspot.data.remote.model.SpaceAnalysisResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

class GeminiRepository(
    private val geminiDataSource: GeminiDataSource
) {
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
    }

    suspend fun analyzeSpace(
        image: Bitmap,
        lux: Int,
        currentTime: String
    ): Result<SpaceAnalysisResponse> = runCatching {
        val jsonString = geminiDataSource.getSpaceAnalysis(image, lux, currentTime)
        
        // 에러 응답인지 먼저 확인
        if (jsonString.contains("\"error\"")) {
            val errorObj = json.parseToJsonElement(jsonString).jsonObject
            val errorMessage = errorObj["error"]?.toString() ?: "알 수 없는 오류"
            throw Exception(errorMessage)
        }
        
        json.decodeFromString<SpaceAnalysisResponse>(jsonString)
    }

    suspend fun diagnosePlant(
        image: Bitmap,
        lux: Int,
        currentTime: String
    ): Result<PlantDiagnosisResponse> = runCatching {
        val jsonString = geminiDataSource.getPlantDiagnosis(image, lux, currentTime)
        
        // 에러 응답인지 먼저 확인
        if (jsonString.contains("\"error\"")) {
            val errorObj = json.parseToJsonElement(jsonString).jsonObject
            val errorMessage = errorObj["error"]?.toString() ?: "알 수 없는 오류"
            throw Exception(errorMessage)
        }
        
        json.decodeFromString<PlantDiagnosisResponse>(jsonString)
    }
}
