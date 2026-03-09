package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.PlantDiagnosisHistory
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.domain.entity.UserPlant
import kotlinx.coroutines.flow.Flow
import java.time.OffsetDateTime

interface PlantRepository {
    fun getUserPlants(): Flow<List<UserPlant>>
    suspend fun updateWateringDate(plantId: String)
    suspend fun updateNickname(plantId: String, newNickname: String)
    suspend fun updateDiagnosisResult(plantId: String, score: Int, imageUrl: String? = null)
    suspend fun addPlant(nickname: String, officialName: String, imageUrl: String?, score: Int, waterPeriod: Int): String
    suspend fun cancelWateringDate(plantId: String)
    suspend fun addDiagnosisHistory(history: PlantDiagnosisHistory)
    suspend fun deletePlant(plantId: String)

    // 식물 상세 페이지용 메서드
    fun getPlantById(plantId: String): Flow<UserPlant?>
    suspend fun updateWaterPeriod(plantId: String, waterPeriod: Int)
    fun getPlantMemos(plantId: String): Flow<List<PlantMemo>>
    suspend fun addPlantMemo(plantId: String, content: String, imageUrl: String?): String
    suspend fun updatePlantMemo(memoId: String, content: String, imageUrl: String?)
    suspend fun deletePlantMemo(memoId: String)
    fun getWateringHistory(plantId: String): Flow<List<OffsetDateTime>>
    
    // 진단 이력 관련
    fun getPlantDiagnosisHistory(plantId: String): Flow<List<PlantDiagnosisHistory>>
    fun getAllDiagnosisHistory(): Flow<List<PlantDiagnosisHistory>>
    
    // 통합 캘린더 페이지용 전역 데이터 메서드
    fun getAllWateringHistory(): Flow<List<Pair<String, OffsetDateTime>>> // plantId to Date
    fun getAllPlantMemos(): Flow<List<PlantMemo>>
}
