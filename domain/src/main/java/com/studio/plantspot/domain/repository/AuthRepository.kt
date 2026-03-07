package com.studio.plantspot.domain.repository

import com.studio.plantspot.domain.entity.UserProfile

interface AuthRepository {
    suspend fun getCurrentUser(): UserProfile?
    suspend fun signInWithGoogleIdToken(idToken: String, rawNonce: String): UserProfile
    suspend fun signOut()
    suspend fun checkNicknameAvailable(nickname: String): Boolean
    suspend fun updateNickname(nickname: String): UserProfile
}
