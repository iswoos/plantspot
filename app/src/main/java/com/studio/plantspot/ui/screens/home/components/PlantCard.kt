package com.studio.plantspot.ui.screens.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studio.plantspot.ui.components.PlantTamagotchiView
import com.studio.plantspot.ui.model.PlantUiModel
import com.studio.plantspot.ui.screens.home.components.profile.PlantAliasEditSheet
import com.studio.plantspot.ui.screens.home.components.share.ShareBottomSheet

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun PlantCard(
    plant: PlantUiModel,
    onNavigateToDetail: (String) -> Unit,
    onShareSuccess: (android.net.Uri) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showShareSheet by remember { mutableStateOf(false) }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) onShareSuccess(uri)
    }

    // 카메라 런처 (프리뷰 버전 - 실제 상업용 앱에서는 FileProvider와 Uri 사용 권장)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // 비트맵을 URI로 변환하는 로직은 복잡하므로 여기서는 갤러리 위주로 작동 확인
    }

    // We can still keep local state for "preview" editing before saving to DB
    var aliasName by remember { mutableStateOf(plant.aliasName) }
    var currentIcon by remember { mutableStateOf(plant.characterIcon) } 

    if (showShareSheet) {
        ShareBottomSheet(
            onDismissRequest = { showShareSheet = false },
            onCameraSelect = { 
                showShareSheet = false 
                cameraLauncher.launch(null)
            },
            onGallerySelect = { 
                showShareSheet = false 
                galleryLauncher.launch("image/*")
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail(plant.id) },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header: Character Avatar & Edit Option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // 다마고치 Lottie 애니메이션 (matchScore + 취침 모드 기반)
                PlantTamagotchiView(
                    matchScore = plant.matchScore,
                    size = 64.dp
                )

                Row {
                    // Share Button
                    IconButton(onClick = { showShareSheet = true }) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Share Status", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Plant Info
            Text(
                text = aliasName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = plant.species,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Match Score & Water Gauge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "햇살 만족도: ${plant.matchScore}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "D-${plant.nextWaterDDay}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { plant.waterGaugePercent },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}
