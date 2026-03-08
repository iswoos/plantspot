package com.studio.plantspot.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val plantRepository: PlantRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadUserPlants()
    }

    fun loadUserPlants() {
        viewModelScope.launch {
            plantRepository.getUserPlants()
                .onStart { _uiState.value = HomeUiState.Loading }
                .catch { e -> _uiState.value = HomeUiState.Error(e.message ?: "Unknown Error") }
                .collect { plants ->
                    _uiState.value = if (plants.isEmpty()) {
                        HomeUiState.Empty
                    } else {
                        HomeUiState.Success(plants.map { it.toUiModel() })
                    }
                }
        }
    }

    fun waterPlant(plantId: String) {
        viewModelScope.launch {
            try {
                plantRepository.updateWateringDate(plantId)
                // Partial update instead of full reload
                val currentState = _uiState.value
                if (currentState is HomeUiState.Success) {
                    val updatedPlants = currentState.plants.map { plant ->
                        if (plant.id == plantId) {
                            val now = OffsetDateTime.now()
                            val localNow = now.atZoneSameInstant(ZoneId.systemDefault())
                            val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)
                            plant.copy(lastWateredDate = localNow.format(dateFormatter))
                        } else {
                            plant
                        }
                    }
                    _uiState.value = HomeUiState.Success(updatedPlants)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun UserPlant.toUiModel(): UserPlantUiModel {
        val now = OffsetDateTime.now()
        
        // Match Score Decay Logic
        val daysSinceMeasure = ChronoUnit.DAYS.between(lastMeasuredAt, now)
        val decayedScore = if (daysSinceMeasure > 7) {
            val deduction = ((daysSinceMeasure - 7) * 10).toInt()
            (matchScore - deduction).coerceAtLeast(0)
        } else {
            matchScore
        }

        // Sunshine Label based on score
        val sunshineLabel = when {
            decayedScore >= 80 -> "매우 좋음"
            decayedScore >= 60 -> "좋음"
            decayedScore >= 40 -> "보통"
            else -> "주의 필요"
        }

        // Format Last Watered Date (Convert UTC to Local) - AM/PM Format
        val dateFormatter = DateTimeFormatter.ofPattern("M월 d일 a h:mm", Locale.KOREAN)
        val localLastWateredAt = lastWateredAt?.atZoneSameInstant(ZoneId.systemDefault())
        val formattedDate = localLastWateredAt?.format(dateFormatter) ?: "아직 급수 전 💧"
        
        // Character State (Facial Expression)
        val characterState = when {
            decayedScore >= 80 -> CharacterExpression.HAPPY
            decayedScore >= 40 -> CharacterExpression.NORMAL
            else -> CharacterExpression.SAD
        }

        return UserPlantUiModel(
            id = id,
            nickname = nickname,
            officialName = officialName,
            imageUrl = imageUrl,
            matchScore = decayedScore,
            sunshineLabel = sunshineLabel,
            lastWateredDate = formattedDate,
            expression = characterState,
            isNight = now.hour >= 22 || now.hour < 6
        )
    }
}

sealed class HomeUiState {
    object Loading : HomeUiState()
    object Empty : HomeUiState()
    data class Success(val plants: List<UserPlantUiModel>) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}

data class UserPlantUiModel(
    val id: String,
    val nickname: String,
    val officialName: String,
    val imageUrl: String?,
    val matchScore: Int,
    val sunshineLabel: String,
    val lastWateredDate: String,
    val expression: CharacterExpression,
    val isNight: Boolean
)

enum class CharacterExpression {
    HAPPY, NORMAL, SAD, SLEEPING
}
