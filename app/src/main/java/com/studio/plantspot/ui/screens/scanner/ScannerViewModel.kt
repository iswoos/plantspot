package com.studio.plantspot.ui.screens.scanner

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.studio.plantspot.data.repository.GeminiRepository
import com.studio.plantspot.data.repository.ConfigRepository
import com.studio.plantspot.data.remote.GeminiDataSource
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class ScannerStep {
    PHOTO_CAPTURE,
    LIGHT_MEASURE_PROMPT,
    LIGHT_MEASURING,
    DIAGNOSING,
    RESULT
}

class ScannerViewModel : ViewModel() {
    private val _currentStep = MutableStateFlow(ScannerStep.PHOTO_CAPTURE)
    val currentStep: StateFlow<ScannerStep> = _currentStep.asStateFlow()

    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage.asStateFlow()

    private val _capturedLux = MutableStateFlow<Float?>(null)
    val capturedLux: StateFlow<Float?> = _capturedLux.asStateFlow()
    
    // 카운트다운 타이머 (기본 3초)
    private val _countdownTimer = MutableStateFlow(3)
    val countdownTimer: StateFlow<Int> = _countdownTimer.asStateFlow()

    private val _luxValue = MutableStateFlow(0f)
    val luxValue: StateFlow<Float> = _luxValue.asStateFlow()

    // 진단 결과 (JSON String 또는 객체)
    private val _diagnosisResult = MutableStateFlow<String?>(null)
    val diagnosisResult: StateFlow<String?> = _diagnosisResult.asStateFlow()

    // Gemini Repository (DI가 없으므로 임시 직접 생성, 보안 강화 로직 포함)
    private val configRepository = ConfigRepository()
    private val geminiRepository = GeminiRepository(GeminiDataSource(configRepository))

    private var sensorManager: SensorManager? = null
    private var lightSensor: Sensor? = null

    private val lightSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            if (event?.sensor?.type == Sensor.TYPE_LIGHT) {
                _luxValue.value = event.values[0]
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun startLightMapping(manager: SensorManager) {
        sensorManager = manager
        lightSensor = manager.getDefaultSensor(Sensor.TYPE_LIGHT)
        lightSensor?.let {
            manager.registerListener(lightSensorListener, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    fun stopLightMapping() {
        sensorManager?.unregisterListener(lightSensorListener)
    }

    override fun onCleared() {
        super.onCleared()
        stopLightMapping()
    }
    
    // 흐름 제어 함수들
    fun onPhotoCaptured(bitmap: Bitmap) {
        _capturedImage.value = bitmap
        _currentStep.value = ScannerStep.LIGHT_MEASURE_PROMPT
    }

    fun startLightMeasurement(mode: String) {
        _countdownTimer.value = 3
        _currentStep.value = ScannerStep.LIGHT_MEASURING
        
        viewModelScope.launch {
            while (_countdownTimer.value > 0) {
                delay(1000)
                _countdownTimer.value -= 1
            }
            // 카운트다운이 0이 되면 현재 조도를 확정하고 AI 진단으로 넘어감
            completeMeasurement(_luxValue.value, mode)
        }
    }

    private fun completeMeasurement(lux: Float, mode: String) {
        _capturedLux.value = lux
        _currentStep.value = ScannerStep.DIAGNOSING
        
        performDiagnosis(mode)
    }

    private fun performDiagnosis(mode: String) {
        val image = _capturedImage.value ?: return
        val lux = _capturedLux.value?.toInt() ?: 0
        val currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        viewModelScope.launch {
            val result = if (mode == "pre") {
                geminiRepository.analyzeSpace(image, lux, currentTime)
            } else {
                geminiRepository.diagnosePlant(image, lux, currentTime)
            }

            if (result.isSuccess) {
                val data = result.getOrNull()
                _diagnosisResult.value = data.toString()
                _currentStep.value = ScannerStep.RESULT
            } else {
                val error = result.exceptionOrNull()
                _diagnosisResult.value = "진단 실패: ${error?.message}"
                _currentStep.value = ScannerStep.RESULT
            }
        }
    }

    fun resetScanner() {
        _capturedImage.value = null
        _capturedLux.value = null
        _diagnosisResult.value = null
        _countdownTimer.value = 3
        _currentStep.value = ScannerStep.PHOTO_CAPTURE
    }
}
