package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.entity.PlantDiagnosisHistory
import com.studio.plantspot.domain.repository.PlantRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.inject.Inject

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
    @SerialName("last_watered_at") val lastWateredAt: String? = null,
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
    @SerialName("plant_id") val plantId: String,
    @SerialName("watered_at") val wateredAt: String
)

@Serializable
private data class PlantIdResponseDto(
    val id: String
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
            val response = supabase.postgrest.from("plantspot_user_plants")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                    order("created_at", Order.DESCENDING)
                }
            
            val plants = response.decodeList<UserPlantDto>().map { it.toDomain() }
            emit(plants)
        } else {
            emit(emptyList())
        }
    }

    override suspend fun updateWateringDate(plantId: String) {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        
        // 1. Update the plant's last_watered_at
        supabase.postgrest.from("plantspot_user_plants")
            .update({
                set("last_watered_at", now)
            }) {
                filter { eq("id", plantId) }
            }
            
        // 2. Insert into the water history table
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

    override suspend fun addPlant(nickname: String, officialName: String, imageUrl: String?, score: Int): String {
        val userId = supabase.auth.currentUserOrNull()?.id ?: throw Exception("User not logged in")
        val now = OffsetDateTime.now(ZoneOffset.UTC).toString()
        
        val insertDto = PlantInsertDto(
            userId = userId,
            nickname = nickname,
            officialName = officialName,
            imageUrl = imageUrl,
            matchScore = score,
            waterPeriod = 7,
            lastWateredAt = null,
            lastMeasuredAt = now,
            createdAt = now
        )

        val response = supabase.postgrest.from("plantspot_user_plants").insert(insertDto) {
            select()
        }.decodeSingle<PlantIdResponseDto>()
        
        return response.id
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
