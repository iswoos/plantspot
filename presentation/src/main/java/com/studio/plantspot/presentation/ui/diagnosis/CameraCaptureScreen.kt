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

@Composable
fun CameraCaptureScreen(
    onImageCaptured: (Uri) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor: ExecutorService = remember { Executors.newSingleThreadExecutor() }
    
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    var isCameraReady by remember { mutableStateOf(false) }
    
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
                .padding(top = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.5f),
                shape = CircleShape
            ) {
                Text(
                    text = "주변 환경이 노출되게 찍어주세요",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontSize = 16.sp
                )
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 40.dp)
                    .size(48.dp)
                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Back", tint = Color.White)
            }

            // Capture Button
            Button(
                onClick = {
                    val file = File(context.cacheDir, "captured_space_${System.currentTimeMillis()}.jpg")
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()
                    
                    imageCapture?.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                onImageCaptured(Uri.fromFile(file))
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
