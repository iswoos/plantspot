package com.studio.plantspot.presentation.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
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
                loadUserPlants() // Refresh list
            } catch (e: Exception) {
                // Handle error (e.g., show snackbar)
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

        // Water Gauge Calculation
        val daysSinceWatered = ChronoUnit.DAYS.between(lastWateredAt, now)
        val waterPercentage = if (waterPeriod > 0) {
            (1.0 - (daysSinceWatered.toDouble() / waterPeriod.toDouble()))
                .coerceIn(0.0, 1.0)
                .toFloat()
        } else 1f

        val dDay = waterPeriod - daysSinceWatered
        
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
            waterPercentage = waterPercentage,
            dDay = dDay.toInt(),
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
    val waterPercentage: Float,
    val dDay: Int,
    val expression: CharacterExpression,
    val isNight: Boolean
)

enum class CharacterExpression {
    HAPPY, NORMAL, SAD, SLEEPING
}
