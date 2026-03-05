package com.studio.plantspot.data.repository

import com.studio.plantspot.data.remote.SupabaseClient
import com.studio.plantspot.ui.model.PlantUiModel
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Supabase plantspot_plants 테이블과 매핑되는 DTO
 */
@Serializable
data class PlantDto(
    val id: String,
    @SerialName("alias_name") val aliasName: String,
    val species: String,
    @SerialName("character_index") val characterIndex: Int = 0,
    @SerialName("match_score") val matchScore: Int = 0,
    @SerialName("water_gauge_percent") val waterGaugePercent: Float = 0.5f,
    @SerialName("next_water_d_day") val nextWaterDDay: Int = 7,
    @SerialName("last_watered_date") val lastWateredDate: String = "",
    val memo: String = ""
)

/**
 * Supabase plantspot_care_logs 테이블과 매핑되는 DTO
 */
@Serializable
data class CareLogDto(
    val id: String? = null,
    @SerialName("plant_id") val plantId: String,
    @SerialName("user_id") val userId: String,
    @SerialName("care_date") val careDate: String,
    @SerialName("care_type") val careType: String  // "water", "fertilize", "repot"
)

object PlantRepository {

    private val db get() = SupabaseClient.client.postgrest

    /**
     * 특정 사용자의 모든 식물 조회
     */
    suspend fun getPlants(userId: String): List<PlantUiModel> {
        return try {
            val response = db["plantspot_plants"]
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<PlantDto>()

            response.map { dto ->
                PlantUiModel(
                    id = dto.id,
                    aliasName = dto.aliasName,
                    species = dto.species,
                    iconIndex = dto.characterIndex,
                    matchScore = dto.matchScore,
                    waterGaugePercent = dto.waterGaugePercent,
                    nextWaterDDay = dto.nextWaterDDay,
                    lastWateredDate = dto.lastWateredDate,
                    memo = dto.memo
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 식물 애칭 및 캐릭터 업데이트
     */
    suspend fun updatePlantProfile(plantId: String, newAlias: String, characterIndex: Int): Boolean {
        return try {
            db["plantspot_plants"]
                .update(
                    mapOf(
                        "alias_name" to newAlias,
                        "character_index" to characterIndex
                    )
                ) {
                    filter { eq("id", plantId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 오늘 물주기 일정이 있는 식물 조회 (D-Day = 0 또는 음수)
     */
    suspend fun getTodayCare(userId: String): List<PlantUiModel> {
        return try {
            val response = db["plantspot_plants"]
                .select(Columns.ALL) {
                    filter {
                        eq("user_id", userId)
                        lte("next_water_d_day", 1)
                    }
                }
                .decodeList<PlantDto>()

            response.map { dto ->
                PlantUiModel(
                    id = dto.id,
                    aliasName = dto.aliasName,
                    species = dto.species,
                    iconIndex = dto.characterIndex,
                    matchScore = dto.matchScore,
                    waterGaugePercent = dto.waterGaugePercent,
                    nextWaterDDay = dto.nextWaterDDay,
                    lastWateredDate = dto.lastWateredDate,
                    memo = dto.memo
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 물주기 완료 기록 추가
     */
    suspend fun logWatering(plantId: String, userId: String, date: String): Boolean {
        return try {
            db["plantspot_care_logs"]
                .insert(
                    CareLogDto(
                        plantId = plantId,
                        userId = userId,
                        careDate = date,
                        careType = "water"
                    )
                )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 특정 월의 물주기 기록 날짜 목록 조회 (달력 표시용)
     */
    suspend fun getCareLogDaysForMonth(userId: String, yearMonth: String): List<Int> {
        return try {
            val response = db["plantspot_care_logs"]
                .select(Columns.list("care_date")) {
                    filter {
                        eq("user_id", userId)
                        like("care_date", "$yearMonth%")
                    }
                }
                .decodeList<Map<String, String>>()

            response.mapNotNull { it["care_date"]?.split("-")?.lastOrNull()?.toIntOrNull() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 식물 삭제 (Supabase DELETE)
     */
    suspend fun deletePlant(plantId: String): Boolean {
        return try {
            db["plantspot_plants"]
                .delete {
                    filter { eq("id", plantId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 특정 식물 상세 정보 조회
     */
    suspend fun getPlant(plantId: String): PlantUiModel? {
        return try {
            val dto = db["plantspot_plants"]
                .select(Columns.ALL) {
                    filter { eq("id", plantId) }
                }
                .decodeSingle<PlantDto>()

            PlantUiModel(
                id = dto.id,
                aliasName = dto.aliasName,
                species = dto.species,
                iconIndex = dto.characterIndex,
                matchScore = dto.matchScore,
                waterGaugePercent = dto.waterGaugePercent,
                nextWaterDDay = dto.nextWaterDDay,
                lastWateredDate = dto.lastWateredDate,
                memo = dto.memo
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

// ──────── 메모 관련 DTO ────────

/**
 * Supabase plantspot_memos 테이블과 매핑되는 DTO
 */
@Serializable
data class MemoDto(
    val id: String? = null,
    @SerialName("plant_id") val plantId: String,
    @SerialName("user_id") val userId: String,
    val content: String,
    @SerialName("created_at") val createdAt: String? = null
)

/**
 * 메모 UI 모델
 */
data class MemoUiModel(
    val id: String,
    val content: String,
    val createdAt: String
)

/**
 * 메모 저장소 (plantspot_memos 테이블)
 */
object MemoRepository {

    private val db get() = SupabaseClient.client.postgrest

    /**
     * 특정 식물의 메모 목록 조회
     */
    suspend fun getMemos(plantId: String): List<MemoUiModel> {
        return try {
            val response = db["plantspot_memos"]
                .select(Columns.ALL) {
                    filter { eq("plant_id", plantId) }
                }
                .decodeList<MemoDto>()

            response.map { dto ->
                MemoUiModel(
                    id = dto.id ?: "",
                    content = dto.content,
                    createdAt = dto.createdAt ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 메모 저장 (INSERT)
     */
    suspend fun saveMemo(plantId: String, userId: String, content: String): Boolean {
        return try {
            db["plantspot_memos"]
                .insert(
                    MemoDto(
                        plantId = plantId,
                        userId = userId,
                        content = content
                    )
                )
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 메모 삭제 (DELETE)
     */
    suspend fun deleteMemo(memoId: String): Boolean {
        return try {
            db["plantspot_memos"]
                .delete {
                    filter { eq("id", memoId) }
                }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

