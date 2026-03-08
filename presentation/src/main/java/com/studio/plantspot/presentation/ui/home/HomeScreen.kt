package com.studio.plantspot.presentation.ui.home

import android.content.Intent
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.plantspot.domain.entity.UserProfile
import dev.shreyaspatil.capturable.Capturable
import dev.shreyaspatil.capturable.controller.rememberCaptureController
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.draw.clip
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    user: UserProfile?,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDiagnosis: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LaunchedEffect(Unit) {
        viewModel.loadUserPlants()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val captureController = rememberCaptureController()
    
    var sharingPlant by remember { mutableStateOf<UserPlantUiModel?>(null) }
    var sharingMemo by remember { mutableStateOf("") }
    var showMemoDialog by remember { mutableStateOf(false) }

    // Memo Dialog at HomeScreen level
    if (showMemoDialog && sharingPlant != null) {
        var tempMemo by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showMemoDialog = false },
            title = { Text("포토카드 메모", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("포토카드 하단에 남길 메시지를 적어주세요.", fontSize = 14.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempMemo,
                        onValueChange = { tempMemo = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("(선택사항)", fontSize = 14.sp) },
                        maxLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { 
                    sharingMemo = tempMemo
                    showMemoDialog = false
                    // Start capture process
                    captureController.capture()
                }) {
                    Text("공유하기", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMemoDialog = false }) {
                    Text("취소", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Advanced Background: Multi-layered soft gradient
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9))
                )
            )
    ) {
        Column(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxSize()
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
                        text = "${user?.nickname ?: user?.displayName ?: "집사"}님의 정원 🪴",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "오늘 우리 식물들은 어떤 기분일까요?",
                        fontSize = 14.sp,
                        color = Color(0xFF4CAF50),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (val state = uiState) {
                is HomeUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32), strokeWidth = 3.dp)
                    }
                }
                is HomeUiState.Empty -> {
                    EmptyForestView()
                }
                is HomeUiState.Success -> {
                    PlantGridSection(
                        plants = state.plants, 
                        viewModel = viewModel, 
                        onNavigateToDiagnosis = onNavigateToDiagnosis,
                        onShareClick = { plant ->
                            sharingPlant = plant
                            showMemoDialog = true
                        }
                    )
                }
                is HomeUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "오류 발생: ${state.message}", color = Color.Red)
                    }
                }
            }
        }

        // Unified Hidden Capturable Area - At the end of Box to avoid intercepting touches
        sharingPlant?.let { plant ->
            Box(
                modifier = Modifier
                    .size(0.1.dp)
                    .zIndex(-1f) // Push to the very back
                    .pointerInput(Unit) { /* Transparent to touches */ }
                    .wrapContentSize(unbounded = true)
                    .graphicsLayer { alpha = 0f }
            ) {
                Capturable(
                    controller = captureController,
                    onCaptured = { bitmap, error ->
                        // Reset sharing state immediately after capture to remove the layer
                        sharingPlant = null
                        
                        if (bitmap != null) {
                            scope.launch(Dispatchers.IO) {
                                val uri = saveBitmapToCache(context, bitmap.asAndroidBitmap())
                                if (uri != null) {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "[PlantSpot] ${plant.nickname}의 포토카드 🌿")
                                        putExtra(Intent.EXTRA_TEXT, "🌿 PlantSpot에서 만든 ${plant.nickname}의 추억 가득 포토카드입니다!")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "포토카드 공유하기"))
                                }
                            }
                        }
                    }
                ) {
                    PlantPhotoCard(plant, sharingMemo)
                }
            }
        }
    }
}

@Composable
fun PlantGridSection(
    plants: List<UserPlantUiModel>,
    viewModel: HomeViewModel,
    onNavigateToDiagnosis: (String) -> Unit,
    onShareClick: (UserPlantUiModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = plants,
            key = { it.id } // Explicit key for better performance and fixing interaction bugs
        ) { plant ->
            PlantGridCard(
                plant = plant,
                onWaterClick = { viewModel.waterPlant(plant.id) },
                onDiagnosisClick = { onNavigateToDiagnosis(plant.id) },
                onShareClick = { onShareClick(plant) },
                onCancelWaterClick = { viewModel.cancelWatering(plant.id) }
            )
        }
    }
}

@Composable
fun PlantGridCard(
    plant: UserPlantUiModel,
    onWaterClick: () -> Unit,
    onDiagnosisClick: () -> Unit,
    onShareClick: () -> Unit,
    onCancelWaterClick: () -> Unit = {}
) {
    var showDiagnosisAlert by remember { mutableStateOf(false) }
    var showWaterCancelAlert by remember { mutableStateOf(false) }
    
    if (showDiagnosisAlert) {
        AlertDialog(
            onDismissRequest = { showDiagnosisAlert = false },
            title = { Text("AI 건강 진단", fontWeight = FontWeight.Bold) },
            text = { Text("${plant.nickname}의 상태를 AI가 정밀 분석합니다.\n진단을 시작할까요?") },
            confirmButton = {
                TextButton(onClick = { showDiagnosisAlert = false; onDiagnosisClick() }) {
                    Text("시작하기", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiagnosisAlert = false }) {
                    Text("나중에", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
    
    // 물 주기 취소 확인 다이얼로그
    if (showWaterCancelAlert) {
        AlertDialog(
            onDismissRequest = { showWaterCancelAlert = false },
            title = { Text("물 주기 취소", fontWeight = FontWeight.Bold) },
            text = { Text("오늘 물 준 기록을 취소하시겠어요?\n(D-Day가 다시 계산됩니다)") },
            confirmButton = {
                TextButton(onClick = { 
                    showWaterCancelAlert = false
                    onCancelWaterClick()
                }) {
                    Text("취소하기", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWaterCancelAlert = false }) {
                    Text("닫기", color = Color.Gray)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* 상세 정보 이동 준비 */ },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Action Bar (Diagnosis Badge & Share Button)
            Box(modifier = Modifier.fillMaxWidth()) {
                // Diagnosis Badge (Top Left)
                Surface(
                    modifier = Modifier.align(Alignment.CenterStart),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF59D).copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, Color(0xFFFFF176))
                ) {
                    Text(
                        text = plant.diagnosisDDayText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF57F17),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                
                // Share button (Small)
                IconButton(
                    onClick = onShareClick,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color(0xFF4CAF50), modifier = Modifier.size(18.dp))
                }
            }

            // Circular Image / Icon with D-Day Badge
            Box {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = Color(0xFFF1F8E9),
                    border = BorderStroke(2.dp, Color(0xFF81C784).copy(alpha = 0.5f))
                ) {
                    if (plant.imageUrl.isNullOrEmpty()) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = if (plant.isNight) "😴" else "🌿", fontSize = 40.sp)
                        }
                    } else {
                        coil.compose.AsyncImage(
                            model = plant.imageUrl,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // D-Day Badge (Only if period is set and it's not waiting for the first water)
                if (plant.isWateringPeriodSet && plant.waterDDayText != "첫 급수 대기") {
                    val badgeColor = if (plant.isWaterUrgent) Color(0xFFE57373) else if (plant.isWateredToday) Color(0xFF81C784) else Color(0xFF64B5F6)
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = -4.dp, y = -4.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = badgeColor,
                        border = BorderStroke(1.dp, Color.White)
                    ) {
                        Text(
                            text = plant.waterDDayText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Info
            Text(
                text = plant.nickname,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(6.dp))
            
            Text(
                text = "기분: ${plant.sunshineLabel}",
                fontSize = 12.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(2.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "최근 물 준 시간",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = plant.lastWateredDate,
                    fontSize = 11.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (plant.isWateringPeriodSet) {
                    if (plant.isWateredToday) {
                        // Watered today -> Toggle allows cancellation
                        OutlinedButton(
                            onClick = { showWaterCancelAlert = true },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Text("완료 💧", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    } else {
                        // Needs water or upcoming
                        val btnColor = if (plant.isWaterUrgent) Color(0xFFE57373) else Color(0xFF42A5F5)
                        Button(
                            onClick = onWaterClick,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = btnColor)
                        ) {
                            Text("물 주기 💧", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // No watering period set 
                    if (plant.isWateredToday) {
                        OutlinedButton(
                            onClick = { showWaterCancelAlert = true },
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF81C784))
                        ) {
                            Text("완료 💧", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    } else {
                        Button(
                            onClick = onWaterClick,
                            modifier = Modifier.weight(1f).height(36.dp),
                            contentPadding = PaddingValues(0.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5))
                        ) {
                            Text("물 주기 💧", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
                
                OutlinedButton(
                    onClick = { showDiagnosisAlert = true },
                    modifier = Modifier.weight(1f).height(36.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFF42A5F5))
                ) {
                    Text("진단 🔍", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5))
                }
            }
        }
    }
}

/**
 * Premium Photo Card layout (Polaroid Style)
 */
@Composable
fun PlantPhotoCard(plant: UserPlantUiModel, memo: String) {
    Surface(
        modifier = Modifier
            .width(360.dp)
            .wrapContentHeight(),
        color = Color.White,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Photo Area
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF5F5F5)
            ) {
                if (plant.imageUrl.isNullOrEmpty()) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🌿", fontSize = 100.sp)
                    }
                } else {
                    coil.compose.AsyncImage(
                        model = plant.imageUrl,
                        contentDescription = null,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plant Info (Polaroid Handwritten Feel)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = plant.nickname,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF1B5E20)
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "현재 기분:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = plant.sunshineLabel, fontSize = 16.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.ExtraBold)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "최근 급수:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = plant.lastWateredDate, fontSize = 14.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "최근 진단:", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = plant.diagnosisDDayText, fontSize = 14.sp, color = Color(0xFFF57F17), fontWeight = FontWeight.Medium)
                }

                // Plant Species Name
                Text(
                    text = "식물 명칭: ${plant.officialName}",
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (memo.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                // Memo Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    Text(
                        text = memo,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            
            // Minimalist Branding
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = "플랜트스팟",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun IdInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 15.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        Text(
            text = value, 
            fontSize = 15.sp, 
            color = Color(0xFF1B5E20), 
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun saveBitmapToCache(context: android.content.Context, bitmap: android.graphics.Bitmap): Uri? {
    return try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        val file = File(cachePath, "plant_id_card_${System.currentTimeMillis()}.png")
        val stream = FileOutputStream(file)
        bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
    }
}

@Composable
fun EmptyForestView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "아직 정원에 식물이 없어요. 🪴", fontSize = 18.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "입양 버튼을 눌러 첫 식물을 추가해보세요!", fontSize = 14.sp, color = Color.LightGray)
        }
    }
}


