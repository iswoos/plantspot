package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.PlantDiagnosisHistory
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.PlantRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

@Serializable
private data class UserPlantDto(
    val id: String,
    @SerialName("user_id") val userId: String,
    val nickname: String,
    @SerialName("official_name") val officialName: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("match_score") val matchScore: Int = 0,
    @SerialName("water_period") val waterPeriod: Int = 7,
    @SerialName("last_watered_at") val lastWateredAt: String? = null,
    @SerialName("last_measured_at") val lastMeasuredAt: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class PlantInsertDto(
    @SerialName("user_id") val userId: String,
    val nickname: String,
    @SerialName("official_name") val officialName: String,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("match_score") val matchScore: Int,
    @SerialName("water_period") val waterPeriod: Int,
    @SerialName("last_watered_at") val lastWateredAt: String?,
    @SerialName("last_measured_at") val lastMeasuredAt: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
private data class DiagnosisUpdateDto(
    @SerialName("match_score") val matchScore: Int,
    @SerialName("last_measured_at") val lastMeasuredAt: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
private data class WaterHistoryInsertDto(
    val id: String? = null,
    @SerialName("plant_id") val plantId: String,
    @SerialName("watered_at") val wateredAt: String
)

@Serializable
private data class PlantIdResponseDto(
    val id: String
)

@Serializable
private data class PlantMemoDto(
    val id: String,
    @SerialName("plant_id") val plantId: String,
    val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class PlantMemoInsertDto(
    @SerialName("plant_id") val plantId: String,
    val content: String,
    @SerialName("image_url") val imageUrl: String? = null
)

@Serializable
private data class PlantMemoUpdateDto(
    val content: String,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
private data class WaterPeriodUpdateDto(
    @SerialName("water_period") val waterPeriod: Int
)

@Serializable
private data class DiagnosisHistoryInsertDto(
    @SerialName("plant_id") val plantId: String,
    @SerialName("health_status") val healthStatus: String,
    @SerialName("health_score") val healthScore: Int,
    val analysis: String?,
    val solution: String?,
    @SerialName("image_url") val imageUrl: String?,
    @SerialName("created_at") val createdAt: String
)

class PlantRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : PlantRepository {

    override fun getUserPlants(): Flow<List<UserPlant>> = flow {
        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId != null) {
            // 1. Fetch plants
            val plantResponse = supabase.postgrest.from("plantspot_user_plants")
                .select {
                    filter { eq("user_id", userId) }
                    order("created_at", Order.DESCENDING)
                }
            val plantsDto = plantResponse.decodeList<UserPlantDto>()
            val plantIds = plantsDto.map { it.id }

            // 2. Fetch latest water history for each plant (optimized search)
            val historyRecords = if (plantIds.isNotEmpty()) {
                supabase.postgrest.from("plantspot_user_plants_water_history")
                    .select {
                        filter { isIn("plant_id", plantIds) }
                        order("watered_at", Order.DESCENDING)
                    }.decodeList<WaterHistoryInsertDto>()
            } else {
                emptyList()
            }

            // 3. Map history to plants (taking the first/latest one for each plantId)
            val latestHistoryMap = historyRecords.groupBy { it.plantId }
                .mapValues { (_, records) -> records.first().wateredAt }

            val plants = plantsDto.map { dto ->
                dto.toDomain().copy(
                    lastWateredAt = latestHistoryMap[dto.id]?.let { OffsetDateTime.parse(it) }
                )
            }
            emit(plants)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun updateWateringDate(plantId: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        
        // Only insert into history. Do NOT update plantspot_user_plants.last_watered_at.
        val historyDto = WaterHistoryInsertDto(
            plantId = plantId,
            wateredAt = now
        )
        supabase.postgrest.from("plantspot_user_plants_water_history").insert(historyDto)
    }

    override suspend fun updateNickname(plantId: String, newNickname: String) {
        supabase.postgrest.from("plantspot_user_plants")
            .update({
                set("nickname", newNickname)
            }) {
                filter { eq("id", plantId) }
            }
    }

    override suspend fun updateDiagnosisResult(plantId: String, score: Int, imageUrl: String?) {
        val updateDto = DiagnosisUpdateDto(
            matchScore = score,
            lastMeasuredAt = OffsetDateTime.now(ZoneOffset.UTC).toString(),
            imageUrl = imageUrl
        )
        
        supabase.postgrest.from("plantspot_user_plants")
            .update(updateDto) {
                filter { eq("id", plantId) }
            }
    }

    override suspend fun addPlant(nickname: String, officialName: String, imageUrl: String?, score: Int, waterPeriod: Int): String {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("User not logged in")
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        
        val insertDto = PlantInsertDto(
            userId = userId,
            nickname = nickname,
            officialName = officialName,
            imageUrl = imageUrl,
            matchScore = score,
            waterPeriod = waterPeriod,
            lastWateredAt = null,
            lastMeasuredAt = now,
            createdAt = now
        )

        val response = supabase.postgrest.from("plantspot_user_plants").insert(insertDto) {
            select()
        }.decodeSingle<PlantIdResponseDto>()
        
        return response.id
    }

    override suspend fun cancelWateringDate(plantId: String) {
        val historyResponse = supabase.postgrest.from("plantspot_user_plants_water_history")
            .select {
                filter { eq("plant_id", plantId) }
                order("watered_at", Order.DESCENDING)
            }
        
        val historyList = historyResponse.decodeList<WaterHistoryInsertDto>()
        val latestRecord = historyList.firstOrNull()
        
        if (latestRecord == null || latestRecord.id == null) {
            throw Exception("취소할 수 있는 최근 물을 준 기록이 없습니다.")
        }
        
        // Accurate Timezone Comparison: Convert UTC from DB to System Local Time
        val systemZone = java.time.ZoneId.systemDefault()
        val recordLocalDate = java.time.OffsetDateTime.parse(latestRecord.wateredAt)
            .atZoneSameInstant(systemZone)
            .toLocalDate()
        val todayLocalDate = java.time.LocalDate.now(systemZone)
        
        if (recordLocalDate != todayLocalDate) {
            // Cancellation is restricted to items performed "today" in the user's local time
            throw Exception("취소는 물을 준 당일에만 가능합니다.")
        }

        // Delete using unique ID (UUID string)
        supabase.postgrest.from("plantspot_user_plants_water_history").delete {
            filter { 
                eq("id", latestRecord.id)
            }
        }
    }

    override suspend fun addDiagnosisHistory(history: PlantDiagnosisHistory) {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        val insertDto = DiagnosisHistoryInsertDto(
            plantId = history.plantId,
            healthStatus = history.healthStatus,
            healthScore = history.healthScore,
            analysis = history.analysis,
            solution = history.solution,
            imageUrl = history.imageUrl,
            createdAt = now
        )
        
        supabase.postgrest.from("plantspot_user_plants_diagnosis_history").insert(insertDto)
    }

    override suspend fun deletePlant(plantId: String) {
        supabase.postgrest.from("plantspot_user_plants")
            .delete {
                filter { eq("id", plantId) }
            }
    }

    // ─────────────────────────────────────────────────────────
    // 식물 상세 페이지용 메서드 구현
    // ─────────────────────────────────────────────────────────

    override suspend fun getPlantById(plantId: String): UserPlant? {
        // plantspot_user_plants 단건 조회
        val response = supabase.postgrest.from("plantspot_user_plants")
            .select {
                filter { eq("id", plantId) }
            }
        val dtoList = response.decodeList<UserPlantDto>()
        val dto = dtoList.firstOrNull() ?: return null

        // 최신 급수일 조회
        val historyResponse = supabase.postgrest.from("plantspot_user_plants_water_history")
            .select {
                filter { eq("plant_id", plantId) }
                order("watered_at", Order.DESCENDING)
            }
        val latestWateredAt = historyResponse.decodeList<WaterHistoryInsertDto>()
            .firstOrNull()?.wateredAt

        return dto.toDomain().copy(
            lastWateredAt = latestWateredAt?.let { OffsetDateTime.parse(it) }
        )
    }

    override suspend fun updateWaterPeriod(plantId: String, waterPeriod: Int) {
        supabase.postgrest.from("plantspot_user_plants")
            .update(WaterPeriodUpdateDto(waterPeriod = waterPeriod)) {
                filter { eq("id", plantId) }
            }
    }

    override fun getPlantMemos(plantId: String): Flow<List<PlantMemo>> = flow {
        val response = supabase.postgrest.from("plantspot_user_plant_memos")
            .select {
                filter { eq("plant_id", plantId) }
                order("created_at", Order.DESCENDING)
            }
        val memos = response.decodeList<PlantMemoDto>().map { dto ->
            // imageUrl이 DB에 path 형태로 저장되어 있다면 (http로 시작 안함) 서명된 URL로 변환
            val finalUrl = if (!dto.imageUrl.isNullOrBlank() && !dto.imageUrl.startsWith("http")) {
                try {
                    createSignedUrl("plant-user-memo-images", dto.imageUrl)
                } catch (e: Exception) {
                    null // 에러 시 null 처리
                }
            } else {
                dto.imageUrl
            }
            dto.copy(imageUrl = finalUrl).toDomain()
        }
        emit(memos)
    }

    override suspend fun addPlantMemo(plantId: String, content: String, imageUrl: String?): String {
        val insertDto = PlantMemoInsertDto(
            plantId = plantId,
            content = content,
            imageUrl = imageUrl
        )
        val response = supabase.postgrest.from("plantspot_user_plant_memos")
            .insert(insertDto) { select() }
        return response.decodeSingle<PlantMemoDto>().id
    }

    override suspend fun updatePlantMemo(memoId: String, content: String, imageUrl: String?) {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        supabase.postgrest.from("plantspot_user_plant_memos")
            .update(PlantMemoUpdateDto(content = content, imageUrl = imageUrl, updatedAt = now)) {
                filter { eq("id", memoId) }
            }
    }

    override suspend fun deletePlantMemo(memoId: String) {
        // 1. 기존 메모 엔티티 조회 (이미지 경로 확보 용도, imageUrl은 path 형태)
        val memoDto = supabase.postgrest.from("plantspot_user_plant_memos")
            .select { filter { eq("id", memoId) } }
            .decodeSingleOrNull<PlantMemoDto>()

        // 2. 만약 이미지 URL(Path)이 있다면 Storage 버킷에서 명시적으로 삭제
        val imagePath = memoDto?.imageUrl
        if (!imagePath.isNullOrBlank() && !imagePath.startsWith("http")) {
            try {
                // supabase-kt SDK에서는 파일(혹은 여러 파일) 삭제 시 delete() 확장 함수를 사용하거나 delete(listOf) 사용
                supabase.storage.from("plant-user-memo-images").delete(imagePath)
                android.util.Log.d("PlantRepository", "Storage 이미지 삭제 완료: $imagePath")
            } catch (e: Exception) {
                android.util.Log.e("PlantRepository", "Storage 이미지 삭제 실패: 권한(RLS) 부족이거나 파일 없음 - $imagePath", e)
            }
        }

        // 3. DB에서 최종 삭제
        supabase.postgrest.from("plantspot_user_plant_memos")
            .delete {
                filter { eq("id", memoId) }
            }
    }

    override fun getWateringHistory(plantId: String): Flow<List<OffsetDateTime>> = flow {
        val response = supabase.postgrest.from("plantspot_user_plants_water_history")
            .select {
                filter { eq("plant_id", plantId) }
                order("watered_at", Order.DESCENDING)
            }
        val dates = response.decodeList<WaterHistoryInsertDto>()
            .map { OffsetDateTime.parse(it.wateredAt) }
        emit(dates)
    }

    override fun getAllWateringHistory(): Flow<List<Pair<String, OffsetDateTime>>> = flow {
        val response = supabase.postgrest.from("plantspot_user_plants_water_history")
            .select { order("watered_at", Order.DESCENDING) }
        val histories = response.decodeList<WaterHistoryInsertDto>()
            .map { it.plantId to OffsetDateTime.parse(it.wateredAt) }
        emit(histories)
    }

    override fun getAllPlantMemos(): Flow<List<PlantMemo>> = flow {
        val response = supabase.postgrest.from("plantspot_user_plant_memos")
            .select { order("created_at", Order.DESCENDING) }
        val memos = response.decodeList<PlantMemoDto>().map { dto ->
            val finalUrl = if (!dto.imageUrl.isNullOrBlank() && !dto.imageUrl.startsWith("http")) {
                try {
                    createSignedUrl("plant-user-memo-images", dto.imageUrl)
                } catch (e: Exception) {
                    null
                }
            } else {
                dto.imageUrl
            }
            dto.copy(imageUrl = finalUrl).toDomain()
        }
        emit(memos)
    }

    /**
     * Private 버킷용: 서명된 URL(1시간 유효)을 생성해 반환합니다.
     * 메모 이미지 표시 시에는 이 URL을 사용합니다.
     */
    suspend fun createSignedUrl(bucket: String, path: String, expiresInSeconds: Long = 3600): String {
        return supabase.storage.from(bucket).createSignedUrl(path, expiresInSeconds.seconds)
    }

    private fun PlantMemoDto.toDomain(): PlantMemo = PlantMemo(
        id = id,
        plantId = plantId,
        content = content,
        imageUrl = imageUrl,
        createdAt = OffsetDateTime.parse(createdAt),
        updatedAt = OffsetDateTime.parse(updatedAt)
    )

    private fun UserPlantDto.toDomain(): UserPlant {
        return UserPlant(
            id = id,
            userId = userId,
            nickname = nickname,
            officialName = officialName,
            imageUrl = imageUrl,
            matchScore = matchScore,
            waterPeriod = waterPeriod,
            lastWateredAt = lastWateredAt?.let { OffsetDateTime.parse(it) },
            lastMeasuredAt = OffsetDateTime.parse(lastMeasuredAt),
            createdAt = OffsetDateTime.parse(createdAt)
        )
    }
}
