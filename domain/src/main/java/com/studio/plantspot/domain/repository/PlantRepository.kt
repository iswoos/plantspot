package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.UserPlant
import kotlinx.coroutines.flow.Flow

interface PlantRepository {
    fun getUserPlants(): Flow<List<UserPlant>>
    suspend fun updateWateringDate(plantId: String)
    suspend fun updateNickname(plantId: String, newNickname: String)
    suspend fun deletePlant(plantId: String)
}
