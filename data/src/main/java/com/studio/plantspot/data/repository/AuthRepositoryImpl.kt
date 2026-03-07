package com.studio.plantspot.data.repository

import android.content.Context
import android.os.Build
import com.studio.plantspot.domain.entity.UserProfile
import com.studio.plantspot.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.first
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import javax.inject.Inject

@Serializable
private data class ProfileDto(
    val id: String,
    val email: String? = null,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("photo_url") val photoUrl: String? = null,
    val provider: String? = null,
    val locale: String? = null,
    val nickname: String? = null
)

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient,
    @ApplicationContext private val context: Context
) : AuthRepository {

    override suspend fun getCurrentUser(): UserProfile? {
        return try {
            supabase.auth.sessionStatus.first { it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated }
            
            val session = supabase.auth.currentSessionOrNull() ?: return null
            val userId = session.user?.id ?: return null

            val profile = fetchProfile(userId)
            profile ?: UserProfile(
                id = userId,
                email = session.user?.email,
                displayName = session.user?.userMetadata?.get("name")?.toString()?.trim('"'),
                photoUrl = session.user?.userMetadata?.get("picture")?.toString()?.trim('"'),
                provider = "unknown",
                locale = currentLocale(),
                nickname = null
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): UserProfile {
        supabase.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            nonce = rawNonce
        }

        val user = supabase.auth.currentUserOrNull()
            ?: throw IllegalStateException("Google 로그인 후 계정 정보를 가져올 수 없습니다.")

        return upsertAndReturnProfile(
            userId = user.id,
            email = user.email,
            displayName = user.userMetadata?.get("full_name")?.toString()?.trim('"') 
                ?: user.userMetadata?.get("name")?.toString()?.trim('"'),
            photoUrl = user.userMetadata?.get("avatar_url")?.toString()?.trim('"') 
                ?: user.userMetadata?.get("picture")?.toString()?.trim('"'),
            provider = "google",
            locale = currentLocale()
        )
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun checkNicknameAvailable(nickname: String): Boolean {
        return try {
            val exists = supabase.postgrest
                .rpc("check_nickname_exists", buildJsonObject { put("n", nickname) })
                .decodeAs<Boolean>()
            
            !exists
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to check nickname existence via RPC", e)
            false
        }
    }

    override suspend fun updateNickname(nickname: String): UserProfile {
        val user = supabase.auth.currentUserOrNull() 
            ?: throw IllegalStateException("인증된 사용자가 아닙니다.")
        
        val profileData = buildJsonObject {
            put("nickname", nickname)
        }

        supabase.postgrest
            .from("plantspot_users")
            .update(profileData) {
                filter { eq("id", user.id) }
            }

        return fetchProfile(user.id) ?: throw IllegalStateException("프로필을 업데이트했지만 가져올 수 없습니다.")
    }

    private suspend fun upsertAndReturnProfile(
        userId: String,
        email: String?,
        displayName: String?,
        photoUrl: String?,
        provider: String,
        locale: String?
    ): UserProfile {
        val existing = fetchProfile(userId)
        
        val profileData = buildJsonObject {
            put("id", userId)
            put("email", email)
            put("display_name", displayName)
            put("photo_url", photoUrl)
            put("provider", provider)
            put("locale", locale)
            // 닉네임이 이미 있으면 유지, 없으면 추가하지 않음 (null 방지)
            existing?.nickname?.let { put("nickname", it) }
        }

        try {
            supabase.postgrest
                .from("plantspot_users")
                .upsert(profileData) {
                    onConflict = "id"
                }
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Failed to upsert profile to plantspot_users", e)
        }

        return UserProfile(
            id = userId,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl,
            provider = provider,
            locale = locale,
            nickname = existing?.nickname
        )
    }

    private suspend fun fetchProfile(userId: String): UserProfile? {
        return try {
            val dto = supabase.postgrest
                .from("plantspot_users")
                .select { filter { eq("id", userId) } }
                .decodeSingleOrNull<ProfileDto>()

            dto?.let {
                UserProfile(
                    id = it.id,
                    email = it.email,
                    displayName = it.displayName,
                    photoUrl = it.photoUrl,
                    provider = it.provider ?: "unknown",
                    locale = it.locale,
                    nickname = it.nickname
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun currentLocale(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.resources.configuration.locales[0].language
        } else {
            @Suppress("DEPRECATION")
            context.resources.configuration.locale.language
        }
    }
}
