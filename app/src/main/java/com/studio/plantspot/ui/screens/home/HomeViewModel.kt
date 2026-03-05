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

/**
 * 홈 화면 식물 목록 관리 ViewModel
 */
class HomeViewModel : ViewModel() {

    private val _plants = MutableStateFlow<List<PlantUiModel>>(emptyList())
    val plants: StateFlow<List<PlantUiModel>> = _plants.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 사용자의 모든 식물 로드
     */
    fun loadPlants() {
        viewModelScope.launch {
            _isLoading.value = true
            val userId = UserPreferences.getUserId()
            _plants.value = PlantRepository.getPlants(userId)
            _isLoading.value = false
        }
    }
}
