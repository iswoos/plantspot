package com.studio.plantspot.presentation.ui.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studio.plantspot.domain.entity.DiagnosisResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisResultScreen(
    viewModel: DiagnosisViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("진단 결과", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF1F8E9)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DiagnosisViewModel.UiState.Loading -> {
                    LoadingScreen()
                }
                is DiagnosisViewModel.UiState.Success -> {
                    ResultContent(state.result, onFinish)
                }
                is DiagnosisViewModel.UiState.Error -> {
                    ErrorScreen(state.message, onBack)
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "식물 의사가 사진과 조도를\n정밀 분석 중입니다...",
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF2E7D32)
        )
    }
}

@Composable
private fun ResultContent(result: DiagnosisResult, onFinish: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Space Analysis (RECOMMEND mode)
        result.spaceAnalysis?.let { analysis ->
            item {
                ResultCard(title = "공간 분석", content = analysis, icon = Icons.Default.Info)
            }
        }

        // Recommended Plants
        result.recommendedPlants?.let { plants ->
            item {
                Text("추천 식물", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            }
            items(plants) { plant ->
                PlantRecommendationCard(plant)
            }
        }

        // Plant Diagnosis (DIAGNOSE mode)
        result.plantName?.let { name ->
            item {
                Text(name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
            }
            item {
                HealthStatusCard(result.healthStatus ?: "normal", result.matchScore ?: 0)
            }
            item {
                ResultCard(title = "정밀 진단", content = result.analysis ?: "", icon = Icons.Default.Search)
            }
            item {
                ResultCard(title = "해결 처방", content = result.solution ?: "", icon = Icons.Default.CheckCircle)
            }
            result.careTips?.let { tips ->
                item {
                    CareTipsCard(tips)
                }
            }
        }

        item {
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("확인", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultCard(title: String, content: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2E7D32), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(content, fontSize = 15.sp, color = Color.DarkGray, lineHeight = 22.sp)
        }
    }
}

@Composable
private fun PlantRecommendationCard(plant: com.studio.plantspot.domain.entity.RecommendedPlant) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(plant.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "${plant.suitabilityScore}점",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(plant.reason, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun HealthStatusCard(status: String, score: Int) {
    val (color, label, icon) = when (status) {
        "danger" -> Triple(Color(0xFFE57373), "주의 요함", Icons.Default.Warning)
        "warning" -> Triple(Color(0xFFFFB74D), "관심 필요", Icons.Default.Info)
        else -> Triple(Color(0xFF81C784), "매우 건강", Icons.Default.CheckCircle)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text("상태: $label", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
            Text("명당 궁합 점수: ${score}점", fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun CareTipsCard(tips: com.studio.plantspot.domain.entity.CareTips) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Tip! 맞춤 관리 정보", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFFBC02D))
            Spacer(modifier = Modifier.height(12.dp))
            Row {
                Text("💧 습도: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tips.humidity, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row {
                Text("🪴 토양: ", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(tips.soil, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun ErrorScreen(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("다시 시도")
        }
    }
}
