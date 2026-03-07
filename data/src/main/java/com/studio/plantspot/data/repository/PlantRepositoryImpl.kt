package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.UserPlant
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
    @SerialName("last_watered_at") val lastWateredAt: String,
    @SerialName("last_measured_at") val lastMeasuredAt: String,
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
        supabase.postgrest.from("plantspot_user_plants")
            .update({
                set("last_watered_at", OffsetDateTime.now().toString())
            }) {
                filter { eq("id", plantId) }
            }
    }

    override suspend fun updateNickname(plantId: String, newNickname: String) {
        supabase.postgrest.from("plantspot_user_plants")
            .update({
                set("nickname", newNickname)
            }) {
                filter { eq("id", plantId) }
            }
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
            lastWateredAt = OffsetDateTime.parse(lastWateredAt),
            lastMeasuredAt = OffsetDateTime.parse(lastMeasuredAt),
            createdAt = OffsetDateTime.parse(createdAt)
        )
    }
}
