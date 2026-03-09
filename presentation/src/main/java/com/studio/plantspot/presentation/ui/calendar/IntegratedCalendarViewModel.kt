package com.studio.plantspot.presentation.ui.calendar

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.Memo
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.FileRepository
import com.studio.plantspot.domain.repository.MemoRepository
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

sealed class IntegratedCalendarUiState {
    object Loading : IntegratedCalendarUiState()
    data class Success(
        val plants: List<UserPlant>, // 필터용 식물 목록
        val events: List<IntegratedEvent> // 모든 이벤트
    ) : IntegratedCalendarUiState()
    data class Error(val message: String) : IntegratedCalendarUiState()
}

@HiltViewModel
class IntegratedCalendarViewModel @Inject constructor(
    private val plantRepository: PlantRepository,
    private val memoRepository: MemoRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<IntegratedCalendarUiState>(IntegratedCalendarUiState.Loading)
    val uiState: StateFlow<IntegratedCalendarUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val systemZone = ZoneId.systemDefault()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                combine(
                    plantRepository.getUserPlants(),
                    plantRepository.getAllWateringHistory(),
                    plantRepository.getAllPlantMemos(),
                    plantRepository.getAllDiagnosisHistory(),
                    memoRepository.getMemos()
                ) { plants, allWateringMap, allPlantMemos, allDiagnosis, generalMemos ->
                    
                    val combinedEvents = mutableListOf<IntegratedEvent>()
                    val plantMap = plants.associateBy { it.id }

                    // 1. 물주기 기록 맵핑
                    allWateringMap.forEach { (plantId, offsetDateTime) ->
                        val plant = plantMap[plantId]
                        if (plant != null) {
                            combinedEvents.add(
                                IntegratedEvent.Watering(
                                    date = offsetDateTime.atZoneSameInstant(systemZone).toLocalDate(),
                                    plantId = plantId,
                                    plantNickname = plant.nickname
                                )
                            )
                        }
                    }

                    // 2. 식물 메모 기록 맵핑
                    allPlantMemos.forEach { memo ->
                        val plant = plantMap[memo.plantId]
                        if (plant != null) {
                            combinedEvents.add(
                                IntegratedEvent.PlantSpecificMemo(
                                    date = memo.createdAt.atZoneSameInstant(systemZone).toLocalDate(),
                                    plantId = memo.plantId,
                                    plantNickname = plant.nickname,
                                    memo = memo
                                )
                            )
                        }
                    }

                    // 3. 진단 이력 맵핑
                    allDiagnosis.forEach { hist ->
                        val plant = plantMap[hist.plantId]
                        val createdAt = hist.createdAt
                        if (plant != null && createdAt != null) {
                            combinedEvents.add(
                                IntegratedEvent.PlantDiagnosis(
                                    date = createdAt.atZoneSameInstant(systemZone).toLocalDate(),
                                    plantId = hist.plantId,
                                    plantNickname = plant.nickname,
                                    history = hist
                                )
                            )
                        }
                    }

                    // 4. 일반 메모 맵핑
                    generalMemos.forEach { memo ->
                        combinedEvents.add(
                            IntegratedEvent.GeneralMemo(
                                date = memo.createdAt.atZoneSameInstant(systemZone).toLocalDate(),
                                memo = memo
                            )
                        )
                    }

                    // 날짜 최신순 정렬 등
                    combinedEvents.sortByDescending { it.date }
                    
                    IntegratedCalendarUiState.Success(
                        plants = plants,
                        events = combinedEvents
                    )
                }.catch { e ->
                    _uiState.value = IntegratedCalendarUiState.Error(e.message ?: "데이터 로드 실패")
                }.collect { state ->
                    _uiState.value = state
                }

            } catch (e: Exception) {
                _uiState.value = IntegratedCalendarUiState.Error(e.message ?: "에러 발생")
            }
        }
    }

    // --- 액션 메서드들 ---

    // 일반 메모 삭제
    fun deleteGeneralMemo(memoId: String) {
        viewModelScope.launch {
            try {
                memoRepository.deleteMemo(memoId)
                _snackbarMessage.emit("메모가 삭제되었습니다.")
            } catch (e: Exception) {
                _snackbarMessage.emit("삭제 실패: ${e.message}")
            }
        }
    }

    // 일반 메모 수정
    fun updateGeneralMemo(memoId: String, title: String, content: String) {
        viewModelScope.launch {
            try {
                memoRepository.updateMemo(memoId, title, content)
                _snackbarMessage.emit("메모가 수정되었습니다.")
            } catch (e: Exception) {
                _snackbarMessage.emit("수정 실패: ${e.message}")
            }
        }
    }

    // 식물 메모 삭제
    fun deletePlantMemo(memoId: String) {
        viewModelScope.launch {
            try {
                plantRepository.deletePlantMemo(memoId)
                _snackbarMessage.emit("메모가 삭제되었습니다.")
            } catch (e: Exception) {
                _snackbarMessage.emit("삭제 실패: ${e.message}")
            }
        }
    }

    // 식물 메모 수정
    fun updatePlantMemo(memoId: String, plantId: String, content: String, imageUri: Uri?, existingImageUrl: String?) {
        viewModelScope.launch {
            try {
                val finalImageUrl = if (imageUri != null) {
                    uploadMemoImage(plantId, imageUri)
                } else {
                    existingImageUrl
                }
                plantRepository.updatePlantMemo(memoId, content, finalImageUrl)
                _snackbarMessage.emit("식물 메모가 수정되었습니다.")
            } catch (e: Exception) {
                _snackbarMessage.emit("식물 메모 수정 실패: ${e.message}")
            }
        }
    }

    private suspend fun uploadMemoImage(plantId: String, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
        val bytes = inputStream?.readBytes() ?: throw Exception("이미지를 읽을 수 없습니다.")
        inputStream.close()

        val ext = context.contentResolver.getType(uri)?.substringAfter("/") ?: "jpg"
        val fileName = "memo_${System.currentTimeMillis()}.$ext"
        val path = "$plantId/$fileName"

        fileRepository.uploadFile("plant-user-memo-images", path, bytes)
        return path
    }
}
