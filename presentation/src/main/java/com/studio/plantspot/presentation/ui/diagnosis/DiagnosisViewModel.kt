package com.studio.plantspot.presentation.ui.diagnosis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studio.plantspot.domain.entity.DiagnosisResult
import com.studio.plantspot.domain.repository.DiagnosisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    private val repository: DiagnosisRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed class UiState {
        object Idle : UiState()
        object Loading : UiState()
        data class Success(val result: DiagnosisResult) : UiState()
        data class Error(val message: String) : UiState()
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _capturedImageUri = MutableStateFlow<Uri?>(null)
    val capturedImageUri: StateFlow<Uri?> = _capturedImageUri.asStateFlow()

    private val _selectedSpot = MutableStateFlow<Pair<Float, Float>?>(null)
    val selectedSpot: StateFlow<Pair<Float, Float>?> = _selectedSpot.asStateFlow()

    private val _luxValue = MutableStateFlow<Float?>(null)
    val luxValue: StateFlow<Float?> = _luxValue.asStateFlow()

    fun setCapturedImageUri(uri: Uri) {
        _capturedImageUri.value = uri
    }

    fun setSelectedSpot(x: Float, y: Float) {
        _selectedSpot.value = x to y
    }

    fun setLuxValue(lux: Float) {
        _luxValue.value = lux
    }

    fun startDiagnosis(mode: String) {
        val imageUri = _capturedImageUri.value ?: return
        val spot = _selectedSpot.value ?: (0.5f to 0.5f)
        val lux = _luxValue.value ?: 0f

        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val processedImage = processImage(imageUri, spot)
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)

                val result = repository.getDiagnosis(
                    image = processedImage,
                    lux = lux,
                    hour = hour,
                    date = date,
                    mode = mode
                )
                _uiState.value = UiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "진단 중 오류가 발생했습니다.")
            }
        }
    }

    private suspend fun processImage(uri: Uri, spot: Pair<Float, Float>): ByteArray = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // 1. Resize (Max 1024px)
        val scale = 1024f / Math.max(originalBitmap.width, originalBitmap.height).coerceAtLeast(1)
        val scaledWidth = (originalBitmap.width * scale).toInt()
        val scaledHeight = (originalBitmap.height * scale).toInt()
        val scaledBitmap = Bitmap.createScaledBitmap(originalBitmap, scaledWidth, scaledHeight, true)

        // 2. Draw Marker
        val mutableBitmap = scaledBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        val markerX = spot.first * scaledWidth
        val markerY = spot.second * scaledHeight
        canvas.drawCircle(markerX, markerY, 15f, paint)

        // 3. To ByteArray
        val outputStream = ByteArrayOutputStream()
        mutableBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        outputStream.toByteArray()
    }
    
    fun reset() {
        _uiState.value = UiState.Idle
        _capturedImageUri.value = null
        _selectedSpot.value = null
        _luxValue.value = null
    }
}
