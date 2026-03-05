package com.studio.plantspot.data.repository

import android.util.Log
import com.studio.plantspot.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

@Serializable
data class AppConfigDto(
    val key: String,
    val value: String
)

class ConfigRepository {

    private var cachedGeminiKey: String? = null
    private val mutex = Mutex()

    /**
     * Supabase DB에서 Gemini API 키를 동적으로 상시 조회 (메모리 캐시 적용)
     */
    suspend fun getGeminiApiKey(): String? = mutex.withLock {
        cachedGeminiKey?.let { return it }

        return withContext(Dispatchers.IO) {
            try {
                Log.d("ConfigRepository", "Fetching Gemini API Key from Supabase...")
                val config = SupabaseClient.client.postgrest["app_config"]
                    .select {
                        filter {
                            eq("key", "gemini_api_key")
                        }
                    }
                    .decodeSingleOrNull<AppConfigDto>()
                
                if (config == null) {
                    Log.e("ConfigRepository", "Gemini API Key not found in 'app_config' table. Please check if SQL was executed.")
                }

                config?.value?.also { 
                    Log.d("ConfigRepository", "Gemini API Key successfully fetched and cached.")
                    cachedGeminiKey = it 
                }
            } catch (e: Exception) {
                Log.e("ConfigRepository", "Error fetching Gemini API Key: ${e.message}")
                e.printStackTrace()
                null
            }
        }
    }
}
