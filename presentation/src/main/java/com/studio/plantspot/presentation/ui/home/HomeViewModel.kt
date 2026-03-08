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

    fun loadUserPlants(showLoading: Boolean = true) {
        viewModelScope.launch {
            plantRepository.getUserPlants()
                .onStart { 
                    if (showLoading) {
                        _uiState.value = HomeUiState.Loading 
                    }
                }
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
                loadUserPlants(showLoading = false) // DB 반영 후 조용히 다시 로드
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun cancelWatering(plantId: String) {
        viewModelScope.launch {
            try {
                // Await for DB deletion to finish
                plantRepository.cancelWateringDate(plantId)
                // Await for data reload to ensure the UI reflects the DB change
                loadUserPlants(showLoading = false)
            } catch (e: Exception) {
                // Return Error state to trigger Snackbar/Toast in the UI
                _uiState.value = HomeUiState.Error(e.message ?: "급수 취소에 실패했습니다. 다시 시도해 주세요.")
            }
        }
    }

    private fun UserPlant.toUiModel(): UserPlantUiModel {
        val now = OffsetDateTime.now()
        
        // Match Score Decay Logic
        val daysSinceMeasure = ChronoUnit.DAYS.between(lastMeasuredAt, now).toInt()
        val decayedScore = if (daysSinceMeasure > 7) {
            val deduction = ((daysSinceMeasure - 7) * 10).toInt()
            (matchScore - deduction).coerceAtLeast(0)
        } else {
            matchScore
        }
        
        val diagnosisDDayText = if (daysSinceMeasure == 0) "오늘 진단" else "진단한 지 +${daysSinceMeasure}일"

        // Sunshine Label based on score
        val sunshineLabel = when {
            decayedScore >= 80 -> "매우 좋음"
            decayedScore >= 60 -> "좋음"
            decayedScore >= 40 -> "보통"
            else -> "주의 필요"
        }

        // Format Last Watered Date (Convert UTC to Local) - AM/PM Format
        val dateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd", Locale.KOREAN)
        val localLastWateredAt = lastWateredAt?.atZoneSameInstant(ZoneId.systemDefault())
        val formattedDate = localLastWateredAt?.format(dateFormatter) ?: "기록 없음"
        
        // Calculate Water D-Day
        val isWateringPeriodSet = waterPeriod > 0
        
        var waterDDayText = ""
        var isWateredToday = false
        var isUrgent = false

        if (lastWateredAt != null) {
            val todayDate = now.toLocalDate()
            val lastWateredDate = localLastWateredAt?.toLocalDate()
            
            if (lastWateredDate == todayDate) {
                isWateredToday = true
            }
        }

        if (isWateringPeriodSet) {
            val todayDate = now.toLocalDate()
            val lastWateredDate = localLastWateredAt?.toLocalDate()
            
            if (lastWateredDate == todayDate) {
                isWateredToday = true
                waterDDayText = "Today"
            } else if (lastWateredDate != null) {
                val daysPassed = ChronoUnit.DAYS.between(lastWateredDate, todayDate).toInt()
                val remainingDays = waterPeriod - daysPassed
                
                waterDDayText = when {
                    remainingDays > 0 -> "D-$remainingDays"
                    remainingDays == 0 -> "D-Day"
                    else -> "D+${-remainingDays}"
                }
                isUrgent = remainingDays <= 0
            } else {
                waterDDayText = "첫 급수 대기" 
                isUrgent = false // D-Day 시작 전이므로 긴급 표시는 제외
            }
        }
        
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
            isNight = now.hour >= 22 || now.hour < 6,
            waterDDayText = waterDDayText,
            diagnosisDDayText = diagnosisDDayText,
            isWateredToday = isWateredToday,
            isWaterUrgent = isUrgent,
            isWateringPeriodSet = isWateringPeriodSet
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
    val isNight: Boolean,
    val waterDDayText: String,
    val diagnosisDDayText: String,
    val isWateredToday: Boolean,
    val isWaterUrgent: Boolean,
    val isWateringPeriodSet: Boolean
)

enum class CharacterExpression {
    HAPPY, NORMAL, SAD, SLEEPING
}
