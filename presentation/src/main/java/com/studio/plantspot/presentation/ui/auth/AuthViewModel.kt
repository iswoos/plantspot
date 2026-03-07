package com.studio.plantspot.presentation.ui.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.studio.plantspot.domain.entity.UserProfile
import com.studio.plantspot.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @Named("googleWebClientId") private val googleWebClientId: String
) : ViewModel() {

    sealed class UiState {
        object CheckingSession : UiState()
        object Idle : UiState()
        object Loading : UiState()
        data class RequireNickname(val profile: UserProfile) : UiState()
        data class Success(val profile: UserProfile) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.CheckingSession)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome: SharedFlow<Unit> = _navigateToHome.asSharedFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user != null) {
                if (user.nickname.isNullOrBlank()) {
                    _uiState.value = UiState.RequireNickname(user)
                } else {
                    _uiState.value = UiState.Success(user)
                    _navigateToHome.emit(Unit)
                }
            } else {
                _uiState.value = UiState.Idle
            }
        }
    }

    fun signInWithGoogle(activityContext: Context) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val rawNonce = UUID.randomUUID().toString()
                val hashedNonce = sha256(rawNonce)
                
                val option = GetGoogleIdOption.Builder()
                    .setServerClientId(googleWebClientId)
                    .setFilterByAuthorizedAccounts(false)
                    .setNonce(hashedNonce)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(option)
                    .build()

                val credResult = CredentialManager.create(activityContext)
                    .getCredential(activityContext, request)

                val idToken = GoogleIdTokenCredential
                    .createFrom(credResult.credential.data).idToken

                val profile = authRepository.signInWithGoogleIdToken(idToken, rawNonce)
                
                if (profile.nickname.isNullOrBlank()) {
                    _uiState.value = UiState.RequireNickname(profile)
                } else {
                    _uiState.value = UiState.Success(profile)
                    _navigateToHome.emit(Unit)
                }
            } catch (e: GetCredentialCancellationException) {
                _uiState.value = UiState.Idle
            } catch (e: Exception) {
                _uiState.value = UiState.Error("Google 로그인 실패: ${e.message}")
            }
        }
    }

    fun registerNickname(nickname: String) {
        val currentProfile = (uiState.value as? UiState.RequireNickname)?.profile ?: return
        
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                // 1. 유효성 검사 (서버 중복 체크 포함)
                if (!isNicknameFormatValid(nickname)) {
                    _uiState.value = UiState.RequireNickname(currentProfile)
                    // UI에서 에러 메시지 처리를 위해 별도의 SharedFlow나 State가 필요할 수 있으나 
                    // 일단은 Error 상태로 보내거나 RequireNickname 유지하며 스낵바 처리 가능
                    throw IllegalArgumentException("닉네임 형식이 올바르지 않습니다. (2~10자)")
                }

                val isAvailable = authRepository.checkNicknameAvailable(nickname)
                if (!isAvailable) {
                    _uiState.value = UiState.RequireNickname(currentProfile)
                    throw IllegalArgumentException("이미 사용 중인 닉네임입니다.")
                }

                // 2. 업데이트
                val updatedProfile = authRepository.updateNickname(nickname)
                _uiState.value = UiState.Success(updatedProfile)
                _navigateToHome.emit(Unit)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "닉네임 등록 실패")
                // 에러 후 다시 입력할 수 있게 복구
                kotlinx.coroutines.delay(2000)
                _uiState.value = UiState.RequireNickname(currentProfile)
            }
        }
    }

    private fun isNicknameFormatValid(nickname: String): Boolean {
        return nickname.length in 2..10 && nickname.all { it.isLetterOrDigit() }
    }

    fun signOut(onLoggedOut: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                authRepository.signOut()
            } finally {
                _uiState.value = UiState.Idle
                onLoggedOut()
            }
        }
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
