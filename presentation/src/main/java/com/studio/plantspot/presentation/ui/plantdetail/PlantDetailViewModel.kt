package com.studio.plantspot.presentation.ui.plantdetail

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.FileRepository
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class PlantDetailViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<PlantDetailUiState>(PlantDetailUiState.Loading)
    val uiState: StateFlow<PlantDetailUiState> = _uiState.asStateFlow()

    private val _memos = MutableStateFlow<List<PlantMemo>>(emptyList())
    val memos: StateFlow<List<PlantMemo>> = _memos.asStateFlow()

    private val _wateringHistory = MutableStateFlow<List<OffsetDateTime>>(emptyList())
    val wateringHistory: StateFlow<List<OffsetDateTime>> = _wateringHistory.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // 현재 플랜트 ID 보관
    private var currentPlantId: String = ""

    fun loadAll(plantId: String) {
        currentPlantId = plantId
        viewModelScope.launch {
            _uiState.value = PlantDetailUiState.Loading
            try {
                // 1. 식물 기본 정보 조회
                val plant = plantRepository.getPlantById(plantId)
                if (plant == null) {
                    _uiState.value = PlantDetailUiState.Error("식물을 찾을 수 없습니다.")
                    return@launch
                }
                _uiState.value = PlantDetailUiState.Success(plant)

                // 2. 메모 스트림 수집
                plantRepository.getPlantMemos(plantId).collect { memoList ->
                    _memos.value = memoList
                }
            } catch (e: Exception) {
                _uiState.value = PlantDetailUiState.Error(e.message ?: "데이터를 불러오는 중 오류가 발생했습니다.")
            }
        }

        // 3. 급수 이력 스트림 수집 (별도 코루틴)
        viewModelScope.launch {
            try {
                plantRepository.getWateringHistory(plantId).collect { history ->
                    _wateringHistory.value = history
                }
            } catch (e: Exception) {
                // 급수 이력 오류는 non-fatal
            }
        }
    }

    fun refreshPlant() {
        viewModelScope.launch {
            try {
                val plant = plantRepository.getPlantById(currentPlantId) ?: return@launch
                _uiState.value = PlantDetailUiState.Success(plant)
            } catch (e: Exception) {
                // 조용히 무시
            }
        }
    }

    fun updateNickname(newNickname: String) {
        viewModelScope.launch {
            try {
                plantRepository.updateNickname(currentPlantId, newNickname)
                refreshPlant()
                _snackbarMessage.value = "닉네임이 변경되었습니다."
            } catch (e: Exception) {
                _snackbarMessage.value = "닉네임 변경에 실패했습니다."
            }
        }
    }

    fun updateWaterPeriod(days: Int) {
        viewModelScope.launch {
            try {
                plantRepository.updateWaterPeriod(currentPlantId, days)
                refreshPlant()
                _snackbarMessage.value = "물 주기가 ${days}일로 변경되었습니다."
            } catch (e: Exception) {
                _snackbarMessage.value = "물 주기 변경에 실패했습니다."
            }
        }
    }

    fun addMemo(content: String, imageUri: Uri?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val uploadedImageUrl = imageUri?.let { uploadMemoImage(it) }
                plantRepository.addPlantMemo(currentPlantId, content, uploadedImageUrl)
                refreshMemos()
                _snackbarMessage.value = "메모가 추가되었습니다."
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "추가 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateMemo(memoId: String, content: String, imageUri: Uri?, existingImageUrl: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // 새 이미지가 선택된 경우에만 업로드
                val finalImageUrl = if (imageUri != null) {
                    uploadMemoImage(imageUri)
                } else {
                    existingImageUrl
                }
                plantRepository.updatePlantMemo(memoId, content, finalImageUrl)
                refreshMemos()
                _snackbarMessage.value = "메모가 수정되었습니다."
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarMessage.value = "수정 실패: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteMemo(memoId: String) {
        viewModelScope.launch {
            try {
                plantRepository.deletePlantMemo(memoId)
                refreshMemos()
                _snackbarMessage.value = "메모가 삭제되었습니다."
            } catch (e: Exception) {
                _snackbarMessage.value = "메모 삭제에 실패했습니다."
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    private fun refreshMemos() {
        viewModelScope.launch {
            plantRepository.getPlantMemos(currentPlantId).collect { memoList ->
                _memos.value = memoList
                return@collect
            }
        }
    }

    private suspend fun uploadMemoImage(uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: throw Exception("이미지를 읽을 수 없습니다.")
        inputStream.close()

        val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val fileName = "memo_${System.currentTimeMillis()}.$ext"
        val path = "$currentPlantId/$fileName"

        // uploadFile은 publicUrl을 리턴하지만, Private 버킷에서는 path만 DB에 저장해야 나중에 signed url 생성 가능
        fileRepository.uploadFile("plant-user-memo-images", path, bytes)
        return path
    }
}

sealed class PlantDetailUiState {
    object Loading : PlantDetailUiState()
    data class Success(val plant: UserPlant) : PlantDetailUiState()
    data class Error(val message: String) : PlantDetailUiState()
}
