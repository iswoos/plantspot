package com.studio.plantspot.presentation.ui.diagnosis

import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning

@Composable
fun SpotSelectionScreen(
    imageUri: Uri,
    onSpotSelected: (Float, Float) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    var selectedOffset by remember { mutableStateOf<Offset?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // Captured Image
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            SubcomposeAsyncImage(
                model = imageUri,
                contentDescription = "Captured Space",
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            selectedOffset = offset
                            onSpotSelected(offset.x, offset.y)
                        }
                    },
                contentScale = ContentScale.Crop,
                loading = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                },
                error = {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Error loading image",
                            tint = Color.Red,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }
            )
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
                    text = "식물이 놓일 자리를 손가락으로 터치해 주세요.",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    fontSize = 16.sp
                )
            }
        }

        // Selection Marker
        selectedOffset?.let { offset ->
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color(0xFFE53935),
                    radius = 20.dp.toPx(),
                    center = offset,
                    alpha = 0.6f
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = offset
                )
            }
        }

        // Bottom Controls
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(start = 40.dp, end = 40.dp, bottom = 60.dp)
        ) {
            TextButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Text("재촬영", color = Color.White)
            }

            if (selectedOffset != null) {
                Button(
                    onClick = onNext,
                    modifier = Modifier.align(Alignment.CenterEnd),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("선택 완료")
                }
            }
        }
    }
}
