package com.studio.plantspot.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.PlantRepository
import com.studio.plantspot.ui.model.PlantUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 오늘의 케어(물주기) ViewModel
 * QuickCareRow에서 물주기 완료 버튼 클릭 시 Supabase care_logs에 기록
 */
class CareViewModel : ViewModel() {

    // 오늘 케어가 필요한 식물 목록
    private val _carePlants = MutableStateFlow<List<PlantUiModel>>(emptyList())
    val carePlants: StateFlow<List<PlantUiModel>> = _carePlants.asStateFlow()

    // 오늘 이미 물을 준 식물 ID 집합
    private val _wateredTodayIds = MutableStateFlow<Set<String>>(emptySet())
    val wateredTodayIds: StateFlow<Set<String>> = _wateredTodayIds.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 오늘의 케어 식물 로드 (D-Day ≤ 1)
     */
    fun loadTodayCare() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = UserPreferences.getUserId()
            _carePlants.value = PlantRepository.getTodayCare(userId)
            _isLoading.value = false
        }
    }

    /**
     * 물주기 완료 처리 → care_logs INSERT
     */
    fun markAsWatered(plantId: String) {
        if (_wateredTodayIds.value.contains(plantId)) return
        val userId = UserPreferences.getUserId()
        val today = LocalDate.now().toString()

        viewModelScope.launch {
            val success = PlantRepository.logWatering(plantId, userId, today)
            if (success) {
                _wateredTodayIds.value = _wateredTodayIds.value + plantId
            }
        }
    }
}
