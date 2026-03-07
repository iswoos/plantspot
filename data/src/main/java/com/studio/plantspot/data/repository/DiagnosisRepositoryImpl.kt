package com.studio.plantspot.data.repository

import com.studio.plantspot.domain.entity.DiagnosisResult
import com.studio.plantspot.domain.repository.DiagnosisRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions
import io.ktor.client.call.body
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

class DiagnosisRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : DiagnosisRepository {

    override suspend fun getDiagnosis(
        image: ByteArray,
        lux: Float,
        hour: Int,
        date: String,
        mode: String
    ): DiagnosisResult {
        val base64Image = android.util.Base64.encodeToString(image, android.util.Base64.NO_WRAP)
        
        val payload = buildJsonObject {
            put("image", base64Image)
            put("lux", lux)
            put("hour", hour)
            put("date", date)
            put("mode", mode)
        }

        return supabase.functions.invoke("plantspotDiagnosis", payload).body<DiagnosisResult>()
    }
}
