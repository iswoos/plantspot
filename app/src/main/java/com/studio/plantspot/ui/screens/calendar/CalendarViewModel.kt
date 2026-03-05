package com.studio.plantspot.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.PlantRepository
import com.studio.plantspot.ui.model.PlantUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.YearMonth

/**
 * 달력 화면 ViewModel
 * 월별 물주기 기록 조회 + 식물별 필터링 지원
 */
class CalendarViewModel : ViewModel() {

    private val _careEventDays = MutableStateFlow<List<Int>>(emptyList())
    val careEventDays: StateFlow<List<Int>> = _careEventDays.asStateFlow()

    private val _plants = MutableStateFlow<List<PlantUiModel>>(emptyList())
    val plants: StateFlow<List<PlantUiModel>> = _plants.asStateFlow()

    private val _selectedPlantId = MutableStateFlow<String?>(null)
    val selectedPlantId: StateFlow<String?> = _selectedPlantId.asStateFlow()

    /**
     * 식물 목록 로드 (필터칩 표시용)
     */
    fun loadPlants() {
        viewModelScope.launch {
            val userId = UserPreferences.getUserId()
            _plants.value = PlantRepository.getPlants(userId)
        }
    }

    /**
     * 특정 월의 물주기 기록 로드
     * @param yearMonth 예: "2025-03"
     */
    fun loadCareEvents(yearMonth: String) {
        viewModelScope.launch {
            val userId = UserPreferences.getUserId()
            _careEventDays.value = PlantRepository.getCareLogDaysForMonth(userId, yearMonth)
        }
    }

    /**
     * 식물 필터 선택 변경
     * null이면 전체, 특정 id면 해당 식물만 표시
     */
    fun selectPlant(plantId: String?) {
        _selectedPlantId.value = plantId
    }
}
