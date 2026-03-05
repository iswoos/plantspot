package com.studio.plantspot.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.MemoRepository
import com.studio.plantspot.data.repository.MemoUiModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 관리 메모 CRUD ViewModel
 * plantspot_memos 테이블과 Supabase 연동
 */
class MemoViewModel : ViewModel() {

    private val _memos = MutableStateFlow<List<MemoUiModel>>(emptyList())
    val memos: StateFlow<List<MemoUiModel>> = _memos.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * 특정 식물의 메모 목록 로드
     */
    fun loadMemos(plantId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _memos.value = MemoRepository.getMemos(plantId)
            _isLoading.value = false
        }
    }

    /**
     * 메모 저장 후 목록 갱신
     */
    fun saveMemo(plantId: String, content: String) {
        if (content.isBlank()) return
        val userId = UserPreferences.getUserId()
        viewModelScope.launch {
            val success = MemoRepository.saveMemo(plantId, userId, content)
            if (success) loadMemos(plantId)
        }
    }

    /**
     * 메모 삭제 후 목록 갱신
     */
    fun deleteMemo(plantId: String, memoId: String) {
        viewModelScope.launch {
            val success = MemoRepository.deleteMemo(memoId)
            if (success) loadMemos(plantId)
        }
    }
}
