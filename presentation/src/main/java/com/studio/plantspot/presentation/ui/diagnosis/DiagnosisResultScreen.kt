package com.studio.plantspot.presentation.ui.diagnosis

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.studio.plantspot.domain.entity.DiagnosisResult
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisResultScreen(
    viewModel: DiagnosisViewModel,
    onFinish: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val adoptionState by viewModel.adoptionState.collectAsState()
    val selectedPlantId by viewModel.selectedPlantId.collectAsState()
    val userPlants by viewModel.userPlants.collectAsState()
    val selectedPlantNickname = userPlants.find { plant -> plant.id == selectedPlantId }?.nickname ?: "식물"
    
    var showAdoptionDialog by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(adoptionState) {
        when (adoptionState) {
            is DiagnosisSaveEvent.Success -> {
                Toast.makeText(context, "새로운 식구가 생겼어요! 🎉", Toast.LENGTH_SHORT).show()
                showAdoptionDialog = false
                viewModel.resetAdoptionState()
                onFinish()
            }
            is DiagnosisSaveEvent.Error -> {
                Toast.makeText(context, (adoptionState as DiagnosisSaveEvent.Error).message, Toast.LENGTH_LONG).show()
                viewModel.resetAdoptionState()
            }
            else -> Unit
        }
    }

    val saveEvent by viewModel.saveEvent.collectAsState()

    LaunchedEffect(saveEvent) {
        saveEvent?.let { event ->
            when (event) {
                is DiagnosisSaveEvent.Loading -> {
                    // 저장 중 로딩 상태 처리 (필요시 Toast 등 추가 가능)
                }
                is DiagnosisSaveEvent.Success -> {
                    Toast.makeText(context, "앨범에 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    viewModel.clearSaveEvent()
                }
                is DiagnosisSaveEvent.Error -> {
                    Toast.makeText(context, "저장 실패: ${event.message}", Toast.LENGTH_SHORT).show()
                    viewModel.clearSaveEvent()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("진단 결과", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // 상단 체크 버튼 제거 (사용자 요청)
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
                is DiagnosisUiState.Loading -> {
                    LoadingScreen()
                }
                is DiagnosisUiState.Success -> {
                    val capturedImageUri by viewModel.capturedImageUri.collectAsState()
                    val selectedSpot by viewModel.selectedSpot.collectAsState()
                    
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    ResultContent(
                        result = state.result, 
                        onSave = {
                            val uri = capturedImageUri
                            val spot = selectedSpot ?: (0.5f to 0.5f)
                            if (uri != null) viewModel.saveResultToGallery(uri, spot, state.result)
                        },
                        onShare = {
                            val uri = capturedImageUri
                            if (uri != null) {
                                scope.launch {
                                    val sharedUri = viewModel.generateResultImage(uri, state.result)
                                    if (sharedUri != null) {
                                        try {
                                            val contentUri = if (sharedUri.scheme == "file") {
                                                val file = File(sharedUri.path ?: "")
                                                FileProvider.getUriForFile(context, "com.studio.plantspot.fileprovider", file)
                                            } else {
                                                sharedUri
                                            }

                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/jpeg"
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                putExtra(Intent.EXTRA_TEXT, "[Plantspot] AI 진단 결과")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            
                                            val chooser = Intent.createChooser(shareIntent, "진단 공유")
                                            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(chooser)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "공유 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        },
                        onFinish = onFinish,
                        isNewPlant = selectedPlantId == null,
                        onAdoptClick = { 
                            nickname = ""
                            viewModel.resetAdoptionState()
                            showAdoptionDialog = true 
                        },
                        plantNickname = selectedPlantNickname
                    )
                }
                is DiagnosisUiState.Error -> {
                    ErrorScreen(state.message) {
                        viewModel.retryDiagnosis()
                    }
                }
                else -> Unit
            }
        }

        if (showAdoptionDialog) {
            AlertDialog(
                onDismissRequest = { 
                    if (adoptionState !is DiagnosisSaveEvent.Loading) {
                        showAdoptionDialog = false 
                    }
                },
                title = { Text("식물 입양하기", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text("정원에서 부를 식물의 닉네임을 지어주세요.")
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = nickname,
                            onValueChange = { nickname = it },
                            placeholder = { Text("예: 초록이, 자밀로") },
                            singleLine = true,
                            enabled = adoptionState !is DiagnosisSaveEvent.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (nickname.isNotBlank()) {
                                viewModel.adoptPlant(nickname)
                                // showAdoptionDialog = false // 즉시 닫지 않음 (성공 시 LaunchedEffect에서 닫힘)
                            }
                        },
                        enabled = adoptionState !is DiagnosisSaveEvent.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        if (adoptionState is DiagnosisSaveEvent.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("정원에 추가")
                        }
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showAdoptionDialog = false },
                        enabled = adoptionState !is DiagnosisSaveEvent.Loading
                    ) {
                        Text("취소")
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = Color.White
            )
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
private fun ResultContent(
    result: DiagnosisResult, 
    onSave: () -> Unit,
    onShare: () -> Unit,
    onFinish: () -> Unit,
    isNewPlant: Boolean = false,
    onAdoptClick: () -> Unit = {},
    plantNickname: String = ""
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        result.spaceAnalysis?.let { analysis ->
            item {
                HealthStatusCard(
                    status = result.healthStatus ?: "normal", 
                    score = result.suitabilityScore ?: 0,
                    isPlantDiagnosis = false
                )
            }
            item {
                ResultCard(title = "공간 분석", content = analysis, icon = Icons.Default.Info)
            }
        }

        result.recommendedPlants?.let { plants ->
            item {
                Text("추천 식물", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            }
            // 적합도 점수(suitabilityScore) 기준 내림차순 정렬
            val sortedPlants = plants.sortedByDescending { it.suitabilityScore }
            items(sortedPlants) { plant ->
                PlantRecommendationCard(plant)
            }
        }

        result.plantName?.let { name ->
            item {
                Text(name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
            }
            item {
                HealthStatusCard(
                    status = result.healthStatus ?: "normal", 
                    score = result.matchScore ?: 0,
                    isPlantDiagnosis = true
                )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onSave,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                ) {
                    Text("앨범에 저장", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("진단 공유", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            if (isNewPlant) {
                Button(
                    onClick = onAdoptClick,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("내 정원으로 입양하기 🌿", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE57373), // 진단 결과와 통일감을 주는 빨간색
                        contentColor = Color.White        // 글자색 하얀색
                    )
                ) {
                    Text("입양하지 않기", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${plantNickname}의 건강 상태를 대시보드에 기록했어요! ✨",
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
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
    val (color, label) = when {
        plant.suitabilityScore >= 80 -> Color(0xFF81C784) to "명당"
        plant.suitabilityScore >= 40 -> Color(0xFFFFB74D) to "보통"
        else -> Color(0xFFE57373) to "아쉬움"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            Column {
                Text(
                    plant.name, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 18.sp,
                    modifier = Modifier.fillMaxWidth(0.85f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(plant.reason, fontSize = 14.sp, color = Color.Gray)
            }
            
            Surface(
                color = color.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    color = color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun HealthStatusCard(status: String, score: Int, isPlantDiagnosis: Boolean = false) {
    val (color, label, icon) = when (status) {
        "danger" -> {
            val txt = if (isPlantDiagnosis) "도움이 필요해요" else "아쉬운 자리"
            Triple(Color(0xFFE57373), txt, Icons.Default.Warning)
        }
        "warning" -> {
            val txt = if (isPlantDiagnosis) "관심이 필요해요" else "보통의 공간"
            Triple(Color(0xFFFFB74D), txt, Icons.Default.Info)
        }
        "fair" -> {
            val txt = "안정적이에요"
            Triple(Color(0xFFAED581), txt, Icons.Default.CheckCircle)
        }
        else -> {
            val txt = if (isPlantDiagnosis) "매우 건강함" else "완벽한 명당"
            Triple(Color(0xFF81C784), txt, Icons.Default.CheckCircle)
        }
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
            Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = color)
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
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
        ) {
            Text("다시 시도", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}
