package com.studio.plantspot.presentation.ui.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.DiagnosisResult
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.repository.DiagnosisRepository
import com.studio.plantspot.domain.repository.FileRepository
import com.studio.plantspot.domain.repository.PlantRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

sealed class DiagnosisUiState {
    object Idle : DiagnosisUiState()
    object Loading : DiagnosisUiState()
    data class Success(val result: DiagnosisResult) : DiagnosisUiState()
    data class Error(val message: String) : DiagnosisUiState()
}

sealed class DiagnosisSaveEvent {
    object Loading : DiagnosisSaveEvent()
    object Success : DiagnosisSaveEvent()
    data class Error(val message: String) : DiagnosisSaveEvent()
}

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val repository: DiagnosisRepository,
    private val plantRepository: PlantRepository,
    private val fileRepository: FileRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<DiagnosisUiState>(DiagnosisUiState.Idle)
    val uiState: StateFlow<DiagnosisUiState> = _uiState.asStateFlow()

    private val _adoptionState = MutableStateFlow<DiagnosisSaveEvent?>(null)
    val adoptionState: StateFlow<DiagnosisSaveEvent?> = _adoptionState.asStateFlow()

    private val _saveEvent = MutableStateFlow<DiagnosisSaveEvent?>(null)
    val saveEvent: StateFlow<DiagnosisSaveEvent?> = _saveEvent.asStateFlow()

    private val _envImageUri = MutableStateFlow<Uri?>(null)
    val envImageUri: StateFlow<Uri?> = _envImageUri.asStateFlow()

    private val _closeUpImageUri = MutableStateFlow<Uri?>(null)
    val closeUpImageUri: StateFlow<Uri?> = _closeUpImageUri.asStateFlow()

    // Backward compatibility or for spot selection (usually on env image)
    val capturedImageUri: StateFlow<Uri?> = _envImageUri.asStateFlow()

    private val _selectedSpot = MutableStateFlow<Pair<Float, Float>?>(null)
    val selectedSpot: StateFlow<Pair<Float, Float>?> = _selectedSpot.asStateFlow()

    private val _luxValue = MutableStateFlow<Float?>(null)
    val luxValue: StateFlow<Float?> = _luxValue.asStateFlow()

    private val _userPlants = MutableStateFlow<List<UserPlant>>(emptyList())
    val userPlants: StateFlow<List<UserPlant>> = _userPlants.asStateFlow()

    private val _selectedPlantId = MutableStateFlow<String?>(null)
    val selectedPlantId: StateFlow<String?> = _selectedPlantId.asStateFlow()

    private var lastMode: String = "DIAGNOSE"

    init {
        loadUserPlants()
    }

    private fun loadUserPlants() {
        viewModelScope.launch {
            plantRepository.getUserPlants().collect { plants ->
                _userPlants.value = plants
            }
        }
    }

    fun setSelectedPlantId(id: String?) {
        _selectedPlantId.value = id
    }

    fun adoptPlant(nickname: String) {
        val currentState = _uiState.value
        if (currentState !is DiagnosisUiState.Success) return
        
        val result = currentState.result
        val envUri = _envImageUri.value
        val closeUpUri = _closeUpImageUri.value
        
        viewModelScope.launch {
            _adoptionState.value = DiagnosisSaveEvent.Loading
            try {
                // 1. 이미지 업로드 (근접 사진 우선, 없으면 환경 사진)
                val targetUri = closeUpUri ?: envUri
                var uploadedUrl: String? = null
                
                targetUri?.let { uri ->
                    val imageBytes = processImage(uri, null)
                    val fileName = "plant_${System.currentTimeMillis()}.jpg"
                    uploadedUrl = fileRepository.uploadFile("plant-images", fileName, imageBytes)
                }
                
                // 2. DB 등록
                plantRepository.addPlant(
                    nickname = nickname,
                    officialName = result.plantName ?: "알 수 없는 식물",
                    imageUrl = uploadedUrl,
                    score = result.matchScore ?: 0
                )
                
                // 새로운 식물이 추가되었으므로 리스트를 다시 불러옴
                loadUserPlants()
                
                _adoptionState.value = DiagnosisSaveEvent.Success
            } catch (e: Exception) {
                _adoptionState.value = DiagnosisSaveEvent.Error(e.message ?: "입양 중 오류가 발생했습니다.")
            }
        }
    }

    fun resetAdoptionState() {
        _adoptionState.value = null
    }

    fun setEnvImageUri(uri: Uri) {
        _envImageUri.value = uri
    }

    fun setCloseUpImageUri(uri: Uri) {
        _closeUpImageUri.value = uri
    }

    @Deprecated("Use setEnvImageUri or setCloseUpImageUri", replaceWith = ReplaceWith("setEnvImageUri(uri)"))
    fun setCapturedImageUri(uri: Uri) {
        _envImageUri.value = uri
    }

    fun setSelectedSpot(x: Float, y: Float) {
        _selectedSpot.value = x to y
    }

    fun setLuxValue(lux: Float) {
        _luxValue.value = lux
    }

    fun startDiagnosis(mode: String) {
        lastMode = mode
        val envUri = _envImageUri.value ?: return
        val spot = _selectedSpot.value ?: (0.5f to 0.5f)
        val lux = (_luxValue.value ?: 0f).toInt().toFloat()

        viewModelScope.launch {
            _uiState.value = DiagnosisUiState.Loading
            try {
                val images = mutableListOf<ByteArray>()
                
                // Process environment image (DIAGNOSE 모드일 경우 마커 생략)
                val measurementSpot = if (mode == "DIAGNOSE") null else spot
                val envBytes = processImage(envUri, measurementSpot)
                images.add(envBytes)
                
                // Process close up image if exists (DIAGNOSE mode)
                var closeUpBytes: ByteArray? = null
                _closeUpImageUri.value?.let { closeUpUri ->
                    val bytes = processImage(closeUpUri, null)
                    closeUpBytes = bytes
                    images.add(bytes)
                }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                val result = repository.getDiagnosis(
                    images = images,
                    lux = lux,
                    hour = hour,
                    date = date,
                    mode = mode
                )

                // 기존 식물 진단인 경우 자동 업데이트 수행 (이미지 포함)
                _selectedPlantId.value?.let { plantId ->
                    val score = if (mode == "DIAGNOSE") result.matchScore else result.suitabilityScore
                    
                    // 최신 사진으로 Storage 업데이트
                    val targetBytes = closeUpBytes ?: envBytes
                    val fileName = "plant_update_${System.currentTimeMillis()}.jpg"
                    val uploadedUrl = fileRepository.uploadFile("plant-images", fileName, targetBytes)
                    
                    score?.let { s ->
                        plantRepository.updateDiagnosisResult(plantId, s, uploadedUrl)
                    }
                }

                _uiState.value = DiagnosisUiState.Success(result)
            } catch (e: Exception) {
                val userFriendlyMessage = when {
                    e.message?.contains("500") == true -> "AI 서버가 잠시 혼잡합니다. 1~2분 후 다시 시도해 주세요. 🌿"
                    e.message?.contains("timeout") == true -> "진단 시간이 너무 오래 걸리고 있습니다. 네트워크 상태를 확인해 주세요."
                    else -> "식물 진단 중 예기치 못한 문제가 발생했습니다. 다시 시도해 주세요."
                }
                _uiState.value = DiagnosisUiState.Error(userFriendlyMessage)
            }
        }
    }

    private suspend fun processImage(uri: Uri, spot: Pair<Float, Float>?): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close() ?: return@withContext byteArrayOf()

        val rotatedBitmap = rotateImageIfRequired(originalBitmap, uri)

        val scale = 1024f / Math.max(rotatedBitmap.width, rotatedBitmap.height).coerceAtLeast(1)
        val scaledWidth = (rotatedBitmap.width * scale).toInt()
        val scaledHeight = (rotatedBitmap.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, scaledWidth, scaledHeight, true)

        val mutableBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        spot?.let {
            val canvas = Canvas(mutableBitmap)
            val paint = Paint().apply {
                color = Color.RED
                style = Paint.Style.FILL
                isAntiAlias = true
            }
            val markerX = it.first * scaledWidth
            val markerY = it.second * scaledHeight
            canvas.drawCircle(markerX, markerY, 15f, paint)
        }

        val outputStream = ByteArrayOutputStream()
        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    }
    
    fun saveResultToGallery(uri: Uri, spot: Pair<Float, Float>, result: DiagnosisResult) {
        viewModelScope.launch {
            try {
                _saveEvent.value = DiagnosisSaveEvent.Loading
                
                // 식물 진단일 경우 근접 사진(closeUpImageUri)이 있으면 그것을 사용, 없으면 전달받은 uri(첫번째 사진) 사용
                val targetUri = if (result.plantName != null) {
                    _closeUpImageUri.value ?: uri
                } else {
                    uri
                }

                val processedImage = processResultImage(targetUri, spot, result)
                val saveResult = fileRepository.saveImageToGallery(processedImage)
                _saveEvent.value = if (saveResult.isSuccess) DiagnosisSaveEvent.Success else DiagnosisSaveEvent.Error(saveResult.exceptionOrNull()?.message ?: "저장 실패")
            } catch (e: Exception) {
                _saveEvent.value = DiagnosisSaveEvent.Error(e.message ?: "저장 중 오류가 발생했습니다.")
            }
        }
    }

    private suspend fun processResultImage(uri: Uri, spot: Pair<Float, Float>, result: DiagnosisResult): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext byteArrayOf()
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()
        
        val rotatedBitmap = rotateImageIfRequired(originalBitmap, uri)

        // 1. Resize Photo (Width fixed to 1080 for quality)
        val targetWidth = 1080
        val scale = targetWidth.toFloat() / rotatedBitmap.width.coerceAtLeast(1)
        val scaledHeight = (rotatedBitmap.height * scale).toInt()
        val photoBitmap = Bitmap.createScaledBitmap(rotatedBitmap, targetWidth, scaledHeight, true)
        
        // 2. Prepare Paints
        val margin = 60f
        val layoutWidth = targetWidth - (margin * 2).toInt()

        val titlePaint = TextPaint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 54f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        
        val sectionTitlePaint = TextPaint().apply {
            color = Color.parseColor("#2E7D32")
            textSize = 38f
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = TextPaint().apply {
            color = Color.parseColor("#333333")
            textSize = 36f
            isAntiAlias = true
            typeface = Typeface.DEFAULT
        }

        val cardBgPaint = Paint().apply {
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 3. Prepare Content Sections
        val isPlant = result.plantName != null
        val sections = mutableListOf<Pair<String, StaticLayout>>()

        // -- Status Section --
        val statusLabel: String
        val statusColor: Int
        if (isPlant) {
            statusLabel = when(result.healthStatus) {
                "danger" -> "도움이 필요해요"
                "warning" -> "관심이 필요해요"
                "fair" -> "안정적이에요"
                else -> "매우 건강함"
            }
            statusColor = when(result.healthStatus) {
                "danger" -> Color.parseColor("#E57373")
                "warning" -> Color.parseColor("#FFFFB74D")
                "fair" -> Color.parseColor("#AED581")
                else -> Color.parseColor("#81C784")
            }
        } else {
            statusLabel = when(result.healthStatus) {
                "danger" -> "아쉬운 자리"
                "warning" -> "보통의 공간"
                else -> "완벽한 명당"
            }
            statusColor = when(result.healthStatus) {
                "danger" -> Color.parseColor("#E57373")
                "warning" -> Color.parseColor("#FFFFB74D")
                else -> Color.parseColor("#81C784")
            }
        }

        // -- Diagnosis Section --
        val diagnosisText = (if (isPlant) result.analysis else result.spaceAnalysis) ?: ""
        if (diagnosisText.isNotEmpty()) {
            sections.add((if (isPlant) "정밀 진단" else "공간 분석") to createLayout(diagnosisText, bodyPaint, layoutWidth))
        }

        // -- Extra Section (Solution or Recommendations) --
        val solution = result.solution
        val recommendedPlants = result.recommendedPlants
        if (isPlant && !solution.isNullOrEmpty()) {
            sections.add("해결 처방" to createLayout(solution, bodyPaint, layoutWidth))
        } else if (!isPlant && !recommendedPlants.isNullOrEmpty()) {
            val recText = StringBuilder()
            recommendedPlants.sortedByDescending { it.suitabilityScore }.take(3).forEach {
                recText.append("🌿 ${it.name}\n${it.reason}\n\n")
            }
            sections.add("추천 식물" to createLayout(recText.toString().trim(), bodyPaint, layoutWidth))
        }

        // -- Tips Section --
        val careTips = result.careTips
        if (isPlant && careTips != null) {
            val tipText = "💧 습도: ${careTips.humidity}\n🪴 토양: ${careTips.soil}"
            sections.add("Tip! 맞춤 관리 정보" to createLayout(tipText, bodyPaint, layoutWidth))
        }

        // 4. Calculate Total Height
        var totalHeight = scaledHeight + 100f // Photo + spacing
        totalHeight += 120f // Main Title (Plant Name)
        totalHeight += 160f // Status Card + spacing
        
        sections.forEach { (title, layout) ->
            totalHeight += 80f // Section Title
            totalHeight += layout.height + 100f // Content + spacing
        }
        totalHeight += 150f // Footer

        // 5. Build Result Bitmap
        val resultBitmap = Bitmap.createBitmap(targetWidth, totalHeight.toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(photoBitmap, 0f, 0f, null)

        var currentY = scaledHeight + 80f
        
        // Draw Main Title
        val mainTitle = result.plantName ?: "PlantSpot 공간 진단 결과"
        canvas.drawText(mainTitle, margin, currentY, titlePaint)
        currentY += 80f

        // Draw Status Card
        val cardHeight = 120f
        val statusRect = android.graphics.RectF(margin, currentY, targetWidth - margin, currentY + cardHeight)
        cardBgPaint.color = statusColor
        cardBgPaint.alpha = 30
        canvas.drawRoundRect(statusRect, 24f, 24f, cardBgPaint)
        
        val statusTextPaint = TextPaint(bodyPaint).apply {
            color = statusColor
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 40f
        }
        canvas.drawText(statusLabel, margin + 40f, currentY + (cardHeight / 2) + 14f, statusTextPaint)
        currentY += cardHeight + 80f

        // Draw Sections
        sections.forEach { (title, layout) ->
            canvas.drawText(title, margin, currentY, sectionTitlePaint)
            currentY += 40f
            canvas.save()
            canvas.translate(margin, currentY)
            layout.draw(canvas)
            canvas.restore()
            currentY += layout.height + 100f
        }

        // Footer
        val footerPaint = TextPaint().apply {
            color = Color.parseColor("#BBBBBB")
            textSize = 30f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("Analyzed by PlantSpot AI", targetWidth / 2f, totalHeight - 60f, footerPaint)

        val outputStream = ByteArrayOutputStream()
        resultBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        outputStream.toByteArray()
    }

    private fun createLayout(text: String, paint: TextPaint, width: Int): StaticLayout {
        return StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.3f)
            .build()
    }
    
    suspend fun generateResultImage(uri: Uri, result: DiagnosisResult): Uri? = withContext(Dispatchers.IO) {
        try {
            // 식물 진단일 경우 근접 사진(closeUpImageUri)이 있으면 그것을 우선 사용
            val targetUri = if (result.plantName != null) {
                _closeUpImageUri.value ?: uri
            } else {
                uri
            }
            
            val processedImage = processResultImage(targetUri, 0.5f to 0.5f, result)
            val cacheFile = File(context.cacheDir, "shared_diagnosis_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(cacheFile)
            outputStream.write(processedImage)
            outputStream.close()
            return@withContext Uri.fromFile(cacheFile)
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateImageIfRequired(img: Bitmap, uri: Uri): Bitmap {
        val input = context.contentResolver.openInputStream(uri) ?: return img
        val ei = ExifInterface(input)
        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        input.close()

        return when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(img, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(img, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(img, 270f)
            else -> img
        }
    }

    private fun rotateImage(img: Bitmap, degree: Float): Bitmap {
        val matrix = android.graphics.Matrix()
        matrix.postRotate(degree)
        val rotatedImg = Bitmap.createBitmap(img, 0, 0, img.width, img.height, matrix, true)
        img.recycle()
        return rotatedImg
    }

    fun retryDiagnosis() {
        startDiagnosis(lastMode)
    }

    fun clearSaveEvent() {
        _saveEvent.value = null
    }

    fun reset() {
        _uiState.value = DiagnosisUiState.Idle
        _envImageUri.value = null
        _closeUpImageUri.value = null
        _selectedSpot.value = null
        _luxValue.value = null
    }
}
