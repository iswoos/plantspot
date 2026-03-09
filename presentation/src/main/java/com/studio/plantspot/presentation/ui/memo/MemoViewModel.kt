package com.studio.plantspot.presentation.ui.memo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.Memo
import com.studio.plantspot.domain.repository.MemoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class MemoUiState {
    object Loading : MemoUiState()
    object Empty : MemoUiState()
    data class Success(val memos: List<Memo>) : MemoUiState()
    data class Error(val message: String) : MemoUiState()
}

@HiltViewModel
class MemoViewModel @Inject constructor(
    private val memoRepository: MemoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<MemoUiState>(MemoUiState.Loading)
    val uiState: StateFlow<MemoUiState> = _uiState.asStateFlow()

    init {
        loadMemos()
    }

    fun loadMemos() {
        viewModelScope.launch {
            memoRepository.getMemos()
                .onStart { _uiState.value = MemoUiState.Loading }
                .catch { e -> _uiState.value = MemoUiState.Error(e.message ?: "오류가 발생했습니다.") }
                .collect { memos ->
                    _uiState.value = if (memos.isEmpty()) MemoUiState.Empty
                    else MemoUiState.Success(memos)
                }
        }
    }

    fun createMemo(title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                memoRepository.createMemo(title, content)
                loadMemos()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = MemoUiState.Error(e.message ?: "메모 저장에 실패했습니다.")
            }
        }
    }

    fun updateMemo(id: String, title: String, content: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                memoRepository.updateMemo(id, title, content)
                loadMemos()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = MemoUiState.Error(e.message ?: "메모 수정에 실패했습니다.")
            }
        }
    }

    fun deleteMemo(id: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                memoRepository.deleteMemo(id)
                loadMemos()
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = MemoUiState.Error(e.message ?: "메모 삭제에 실패했습니다.")
            }
        }
    }
}
