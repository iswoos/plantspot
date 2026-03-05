package com.studio.plantspot.ui.screens.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studio.plantspot.ui.model.PlantUiModel

/**
 * 오늘의 케어 행
 * CareViewModel과 연동하여 물주기 완료 버튼 클릭 시 Supabase에 기록합니다.
 */
@Composable
fun QuickCareRow(
    carePlants: List<PlantUiModel>,
    wateredTodayIds: Set<String>,
    onWaterClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "오늘의 케어",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if (carePlants.isEmpty()) {
            Text(
                text = "오늘 케어할 식물이 없어요 🎉",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            carePlants.forEach { plant ->
                val isWatered = wateredTodayIds.contains(plant.id)
                CareItem(
                    plant = plant,
                    isWatered = isWatered,
                    onWaterClick = { onWaterClick(plant.id) }
                )
                Spacer(modifier = Modifier.padding(bottom = 8.dp))
            }
        }
    }
}

@Composable
private fun CareItem(
    plant: PlantUiModel,
    isWatered: Boolean,
    onWaterClick: () -> Unit
) {
    // 완료 시 색상 애니메이션
    val cardColor by animateColorAsState(
        targetValue = if (isWatered)
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
        else
            MaterialTheme.colorScheme.primaryContainer,
        animationSpec = tween(durationMillis = 400),
        label = "careCardColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WaterDrop,
                        contentDescription = "물주기",
                        tint = if (isWatered)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "${plant.aliasName} (${plant.species})",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isWatered) "✓ 오늘 물주기 완료!" else "물 주는 날",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isWatered)
                            MaterialTheme.colorScheme.tertiary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 완료 버튼 (완료 시 비활성화)
            IconButton(
                onClick = onWaterClick,
                enabled = !isWatered,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = if (isWatered)
                        MaterialTheme.colorScheme.tertiary
                    else
                        MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.tertiary,
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = "완료")
            }
        }
    }
}
