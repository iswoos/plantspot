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
import com.studio.plantspot.ui.screens.home.components.profile.PlantAliasEditSheet
import com.studio.plantspot.ui.screens.home.components.share.ShareBottomSheet

@Composable
fun PlantCard(modifier: Modifier = Modifier) {
    var showShareSheet by remember { mutableStateOf(false) }
    var showAliasSheet by remember { mutableStateOf(false) }

    var aliasName by remember { mutableStateOf("초록이") }
    // We can store character icon state (index or ImageVector). For now, index 0 is Face.
    var currentIcon by remember { mutableStateOf(Icons.Filled.Face) } 

    if (showShareSheet) {
        ShareBottomSheet(
            onDismissRequest = { showShareSheet = false },
            onCameraSelect = { showShareSheet = false /* TODO */ },
            onGallerySelect = { showShareSheet = false /* TODO */ }
        )
    }

    if (showAliasSheet) {
        PlantAliasEditSheet(
            currentAlias = aliasName,
            onDismissRequest = { showAliasSheet = false },
            onSaveRequest = { newAlias, iconIndex ->
                aliasName = newAlias
                // Update icon based on index
                currentIcon = when(iconIndex) {
                    0 -> Icons.Filled.Face
                    1 -> Icons.Filled.Pets
                    2 -> Icons.Filled.Star
                    else -> Icons.Filled.Face
                }
                showAliasSheet = false
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showAliasSheet = true },
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
                // Character Avatar Placeholder (Tamagotchi emotion)
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = currentIcon, 
                        contentDescription = "Tamagotchi Character",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(32.dp)
                    )
                }

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
                text = "드라세나 마르기나타",
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
                    text = "햇살 만족도: 95%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "D-2",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { 0.6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }
    }
}
