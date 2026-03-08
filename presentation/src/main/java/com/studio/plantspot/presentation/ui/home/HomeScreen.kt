package com.studio.plantspot.presentation.ui.home

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.plantspot.domain.entity.UserProfile

@Composable
fun HomeScreen(
    user: UserProfile?,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadUserPlants()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF1F8E9) // Light green background
    ) {
        Column(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${user?.nickname ?: user?.displayName ?: "집사"}님, 환영합니다! 🌿",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "오늘 우리 집 식물은 행복하게 잘 크고 있을까요?",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
                is HomeUiState.Empty -> {
                    EmptyForestView()
                }
                is HomeUiState.Success -> {
                    ForestDashboard(state.plants, viewModel)
                }
                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "오류가 발생했습니다: ${state.message}", color = Color.Red)
                    }
                }
            }
        }
    }
}

@Composable
fun ForestDashboard(plants: List<UserPlantUiModel>, viewModel: HomeViewModel) {
    val pagerState = rememberPagerState(pageCount = { plants.size })

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp
        ) { page ->
            PlantCard(plants[page])
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Quick Care Section
        QuickCareSection(plants[pagerState.currentPage], viewModel)
    }
}

@Composable
fun PlantCard(plant: UserPlantUiModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Character Image Placeholder (using large text/emoji)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(CircleShape)
                .background(if (plant.isNight) Color(0xFF1A237E).copy(alpha = 0.1f) else Color(0xFFC8E6C9)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (plant.isNight) "😴" else when (plant.expression) {
                    CharacterExpression.HAPPY -> "😊"
                    CharacterExpression.NORMAL -> "🙂"
                    CharacterExpression.SAD -> "😢"
                    else -> "🙂"
                },
                fontSize = 100.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = plant.nickname,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = plant.officialName,
            fontSize = 16.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("햇살 만족도", "${plant.matchScore}%")
            StatItem("수분 게이지", "D-${plant.dDay}")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Water Gauge
        LinearProgressIndicator(
            progress = { plant.waterPercentage },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = Color(0xFF2196F3),
            trackColor = Color(0xFFE3F2FD)
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
    }
}

@Composable
fun QuickCareSection(currentPlant: UserPlantUiModel, viewModel: HomeViewModel) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
        color = Color.White,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "퀵 케어 (Quick Care)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFE3F2FD),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "오늘 물 주셨나요?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "${currentPlant.nickname}에게 수분 충전!", fontSize = 12.sp, color = Color.Gray)
                    }
                }
                Button(
                    onClick = { viewModel.waterPlant(currentPlant.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("물 주기 완료")
                }
            }
        }
    }
}

@Composable
fun EmptyForestView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "아직 정원에 식물이 없어요. 🪴", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "중앙 버튼을 눌러 첫 식물을 입양해보세요!", fontSize = 14.sp, color = Color.LightGray)
        }
    }
}
