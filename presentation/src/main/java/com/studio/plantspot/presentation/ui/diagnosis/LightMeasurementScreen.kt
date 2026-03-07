package com.studio.plantspot.presentation.ui.diagnosis

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LightMeasurementScreen(
    onMeasurementComplete: (Float) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val lightSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) }
    
    var currentLux by remember { mutableStateOf(-1f) } // -1로 초기화하여 데이터 수신 여부 확인
    var luxSum by remember { mutableStateOf(0f) }
    var measurementCount by remember { mutableStateOf(0) }
    var progress by remember { mutableStateOf(0f) }
    var isFinished by remember { mutableStateOf(false) }
    var hasStarted by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(targetValue = progress)

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null && hasStarted && !isFinished) {
                    currentLux = event.values[0]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        
        sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_GAME) // 더 빠른 반응을 위해 DELAY_GAME 사용
        
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Measurement Logic (3 seconds)
    LaunchedEffect(hasStarted) {
        if (!hasStarted) return@LaunchedEffect
        
        // 첫 번째 유효한 센서 값이 들어올 때까지 대기 (무조건 작동 보장)
        while (currentLux < 0f) {
            delay(50)
        }
        
        val startTime = System.currentTimeMillis()
        val duration = 3000L
        
        while (System.currentTimeMillis() - startTime < duration) {
            delay(100)
            luxSum += currentLux
            measurementCount++
            progress = (System.currentTimeMillis() - startTime).toFloat() / duration
        }
        
        isFinished = true
        progress = 1f
        val averageLux = if (measurementCount > 0) luxSum / measurementCount else currentLux
        delay(1000) // Show full progress for a moment
        onMeasurementComplete(averageLux)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F8E9)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = when {
                    isFinished -> "측정 완료! 🎉"
                    hasStarted -> "조도 측정 중..."
                    else -> "조도 측정 준비"
                },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            
            Spacer(modifier = Modifier.height(32.dp))

            // Progress Circle
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { if (hasStarted) animatedProgress else 0f },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 12.dp,
                    color = Color(0xFF2E7D32),
                    trackColor = Color(0xFFE8F5E9)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (hasStarted && currentLux >= 0f) "${currentLux.toInt()}" else "-",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Lux",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Guide Text
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = when {
                        isFinished -> "조도 측정이 완료되었습니다!\n분석 결과를 준비 중입니다."
                        hasStarted -> "측정하는 동안 기기를 움직이지 마세요."
                        else -> "창가에서 50cm만 멀어져도 빛은 절반으로 줄어들어요!\n꼭 식물을 놓을 곳에 화면이 하늘을 향하게(전면 카메라 렌즈가 가려지지 않게) 놓아주세요."
                    },
                    modifier = Modifier.padding(20.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            if (!hasStarted) {
                Button(
                    onClick = { hasStarted = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("측정 시작하기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = onBack) {
                    Text("뒤로 가기", color = Color.Gray)
                }
            } else if (!isFinished) {
                TextButton(onClick = onBack) {
                    Text("취소", color = Color.Gray)
                }
            }
        }
    }
}
