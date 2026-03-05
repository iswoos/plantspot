package com.studio.plantspot.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import java.time.LocalTime

/**
 * 식물 다마고치 Lottie 뷰
 *
 * 표시 우선순위:
 * 1. 22시 ~ 06시 → 취침 모드 (plant_sleep.json)
 * 2. matchScore ≥ 80% → 기쁜 상태 (plant_happy.json)
 * 3. matchScore 50~79% → 보통 상태 (plant_normal.json)
 * 4. matchScore < 50% → 슬픈 상태 (plant_sad.json)
 */
@Composable
fun PlantTamagotchiView(
    matchScore: Int,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    // 현재 시각 기반 취침 모드 판단 (22시 이후 또는 06시 이전)
    val currentHour = LocalTime.now().hour
    val isSleepMode = currentHour >= 22 || currentHour < 6

    val animationFile = when {
        isSleepMode -> "animations/plant_sleep.json"
        matchScore >= 80 -> "animations/plant_happy.json"
        matchScore >= 50 -> "animations/plant_normal.json"
        else -> "animations/plant_sad.json"
    }

    val composition by rememberLottieComposition(
        spec = LottieCompositionSpec.Asset(animationFile)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(size)
    )
}
