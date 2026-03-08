package com.studio.plantspot.presentation.ui.diagnosis

import android.content.Context
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

enum class CaptureStep { RECOMMEND, ENVIRONMENT, CLOSE_UP }

@Composable
fun CameraCaptureScreen(
    mode: String,
    onImagesCaptured: (Uri, Uri?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf<ImageCapture?>(null) }
    var isCameraReady by remember { mutableStateOf<Boolean>(false) }
    
    // Step management for DIAGNOSE mode
    var currentStep by remember { 
        mutableStateOf<CaptureStep>(if (mode == "DIAGNOSE") CaptureStep.ENVIRONMENT else CaptureStep.RECOMMEND) 
    }
    var envImageUri by remember { mutableStateOf<Uri?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    
                    imageCapture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageCapture
                        )
                        isCameraReady = true
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        if (!isCameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }

        // Guide Overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val guideText = when (currentStep) {
                CaptureStep.ENVIRONMENT -> "식물이 놓인 곳의 주변 환경이 모두 노출되게 찍어주세요"
                CaptureStep.CLOSE_UP -> "식물의 상태가 잘 보이도록 가까이서 찍어주세요"
                CaptureStep.RECOMMEND -> "식물을 놓을 곳의 주변 환경이 모두 노출되게 찍어주세요"
            }
            
            Surface(
                color = Color.Black.copy(alpha = 0.6f),
                shape = CircleShape
            ) {
                Text(
                    text = guideText,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
            
            if (currentStep == CaptureStep.CLOSE_UP) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "2 / 2 단계: 근접 촬영",
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } else if (currentStep == CaptureStep.ENVIRONMENT) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = CircleShape
                ) {
                    Text(
                        text = "1 / 2 단계: 환경 촬영",
                        color = Color.Black,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
             IconButton(
                onClick = {
                    if (currentStep == CaptureStep.CLOSE_UP) {
                        currentStep = CaptureStep.ENVIRONMENT
                        envImageUri = null
                    } else {
                        onBack()
                    }
                },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(
                    imageVector = if (currentStep == CaptureStep.CLOSE_UP) Icons.Default.ArrowBack else Icons.Default.Close, 
                    contentDescription = "Back", 
                    tint = Color.White
                )
            }

            // Capture Button
            Button(
                onClick = {
                    val fileName = when (currentStep) {
                        CaptureStep.ENVIRONMENT -> "captured_env_${System.currentTimeMillis()}.jpg"
                        CaptureStep.CLOSE_UP -> "captured_closeup_${System.currentTimeMillis()}.jpg"
                        CaptureStep.RECOMMEND -> "captured_recommend_${System.currentTimeMillis()}.jpg"
                    }
                    val file = File(context.cacheDir, fileName)
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    
                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                val uri = Uri.fromFile(file)
                                when (currentStep) {
                                    CaptureStep.ENVIRONMENT -> {
                                        envImageUri = uri
                                        currentStep = CaptureStep.CLOSE_UP
                                    }
                                    CaptureStep.CLOSE_UP -> {
                                        envImageUri?.let { onImagesCaptured(it, uri) }
                                    }
                                    CaptureStep.RECOMMEND -> {
                                        onImagesCaptured(uri, null)
                                    }
                                }
                            }

                            override fun onError(exception: ImageCaptureException) {
                                exception.printStackTrace()
                            }
                        }
                    )
                },
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(80.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(4.dp, Color.Gray)
            ) {
                // Inner circle
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color.White, CircleShape)
                )
            }
        }
    }
}
