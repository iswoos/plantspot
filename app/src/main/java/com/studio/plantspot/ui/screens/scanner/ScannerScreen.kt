package com.studio.plantspot.ui.screens.scanner

import android.Manifest
import android.content.Context
import android.hardware.SensorManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import android.graphics.BitmapFactory
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.LottieConstants
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.studio.plantspot.R
import com.studio.plantspot.ui.screens.scanner.components.LightMeterOverlay

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    mode: String = "pre",
    onClose: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: ScannerViewModel = viewModel()
    val luxValue by viewModel.luxValue.collectAsState()
    val currentStep by viewModel.currentStep.collectAsState()
    val countdownTimer by viewModel.countdownTimer.collectAsState()
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val imageCapture = remember { ImageCapture.Builder().build() }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        viewModel.startLightMapping(sensorManager)
        onDispose {
            viewModel.stopLightMapping()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Camera Preview or Permission Request
        if (cameraPermissionState.status.isGranted) {
            Crossfade(targetState = currentStep, label = "scanner_step") { step ->
                when (step) {
                    ScannerStep.PHOTO_CAPTURE -> {
                        Box(modifier = Modifier.fillMaxSize()) {
                            CameraPreview(modifier = Modifier.fillMaxSize(), imageCapture = imageCapture)
                            
                            // 상단 여백 및 캡처 가이드
                            Column(
                                modifier = Modifier.fillMaxSize().padding(top = 48.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val captureGuideText = if (mode == "pre") "식물을 배치할 공간을 선명하게 찍어주세요." else "진단할 식물이 잘 보이도록 사진을 찍어주세요."
                                Text(
                                    text = captureGuideText,
                                    color = Color.White,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .background(Color.Black.copy(alpha = 0.5f), shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            // 하단 촬영 버튼
                            Button(
                                onClick = {
                                    takePhoto(
                                        imageCapture = imageCapture,
                                        context = context,
                                        onPhotoCaptured = { bitmap ->
                                            viewModel.onPhotoCaptured(bitmap)
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 64.dp)
                                    .size(72.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                            ) {}
                        }
                    }
                    ScannerStep.LIGHT_MEASURE_PROMPT -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.CenterFocusStrong, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("정확한 조도 측정을 위해", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text("기기를 바닥에 조심스럽게 눕혀주세요.", color = Color.LightGray, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
                            Spacer(modifier = Modifier.height(32.dp))
                            Button(onClick = { viewModel.startLightMeasurement(mode ?: "pre") }) {
                                Text("측정 시작")
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            val measureGuideText = if (mode == "pre") {
                                "💡 참고사항\n창가에서 50cm만 멀어져도 빛은 절반으로 줄어들어요! 우리 눈엔 비슷해 보여도 식물에겐 천차만별이니, 꼭 식물을 둘 자리에 핸드폰을 천장을 바라보게 눕힌 후 측정해 주세요."
                            } else {
                                "💡 참고사항\n창가에서 50cm만 멀어져도 빛은 절반으로 줄어들어요! 우리 눈엔 비슷해 보여도 식물에겐 천차만별이니, 꼭 식물이 있는 자리에 핸드폰을 천장을 바라보게 눕힌 후 측정해 주세요."
                            }
                            Text(
                                measureGuideText,
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                    ScannerStep.LIGHT_MEASURING -> {
                        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                        val pulseScale by infiniteTransition.animateFloat(
                            initialValue = 0.8f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scale"
                        )
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.6f,
                            targetValue = 0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1200, easing = LinearOutSlowInEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "alpha"
                        )

                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(240.dp)) {
                                // 파동 애니메이션
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(pulseScale)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = pulseAlpha), shape = CircleShape)
                                )
                                // 중앙 타이머 배경
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$countdownTimer",
                                        color = Color.White,
                                        fontSize = 72.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(64.dp))

                            // 측정값 정보 카드
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp))
                                    .padding(horizontal = 24.dp, vertical = 12.dp)
                            ) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = Color.Yellow, modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "${luxValue.toInt()} LUX",
                                    color = Color.White,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))
                            Text("빛을 수집하고 있습니다. 기기를 움직이지 마세요...", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    ScannerStep.DIAGNOSING -> {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("AI가 빛 환경과 사진을 분석하고 있습니다...", color = Color.White)
                        }
                    }
                    ScannerStep.RESULT -> {
                        val resultText by viewModel.diagnosisResult.collectAsState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "진단 결과",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.1f), shape = RoundedCornerShape(12.dp))
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = resultText ?: "결과를 불러올 수 없습니다.",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            }
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { viewModel.resetScanner() },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("확인")
                            }
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("카메라 권한이 필요합니다.", color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "권한 요청 창이 뜨지 않는다면\n기기 설정 > 애플리케이션 > PlantSpot에서\n카메라 권한을 직접 허용해주세요.",
                    color = Color.LightGray,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                    Text("권한 요청 / 재시도")
                }
            }
        }

        // 닫기 버튼 (최상단)
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color.White)
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier, imageCapture: ImageCapture) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageCapture)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

private fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    onPhotoCaptured: (android.graphics.Bitmap) -> Unit
) {
    imageCapture.takePicture(
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                
                // 회전 처리 (CameraX 기본 캡처는 회전값이 적용 안될 수 있음)
                val matrix = Matrix()
                matrix.postRotate(image.imageInfo.rotationDegrees.toFloat())
                val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                
                onPhotoCaptured(rotatedBitmap)
                image.close()
            }

            override fun onError(exception: ImageCaptureException) {
                exception.printStackTrace()
            }
        }
    )
}
