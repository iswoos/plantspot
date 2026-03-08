package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.UserPlant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun getUserPlants(): Flow<List<UserPlant>>
    suspend fun updateWateringDate(plantId: String)
    suspend fun updateNickname(plantId: String, newNickname: String)
    suspend fun updateDiagnosisResult(plantId: String, score: Int, imageUrl: String? = null)
    suspend fun addPlant(nickname: String, officialName: String, imageUrl: String?, score: Int)
    suspend fun deletePlant(plantId: String)
}
