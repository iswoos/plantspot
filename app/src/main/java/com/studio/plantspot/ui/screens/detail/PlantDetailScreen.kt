package com.studio.plantspot.ui.screens.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.studio.plantspot.ui.components.PlantTamagotchiView
import com.studio.plantspot.ui.model.PlantUiModel
import com.studio.plantspot.ui.screens.home.components.profile.PlantAliasEditSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBackClick: () -> Unit,
    onPlantDeleted: () -> Unit = onBackClick
) {
    // 실제 식물 데이터 상태 (초기값 null)
    var plant by remember { mutableStateOf<PlantUiModel?>(null) }
    var showEditSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 메모 ViewModel
    val memoViewModel: MemoViewModel = viewModel()
    val memos by memoViewModel.memos.collectAsStateWithLifecycle()
    var memoInput by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    // 화면 진입 시 식물 정보 및 메모 목록 로드
    LaunchedEffect(plantId) {
        scope.launch {
            plant = com.studio.plantspot.data.repository.PlantRepository.getPlant(plantId)
        }
        memoViewModel.loadMemos(plantId)
    }

    // ── 편집 바텀시트 ──
    if (showEditSheet && plant != null) {
        PlantAliasEditSheet(
            currentAlias = plant!!.aliasName,
            onDismissRequest = { showEditSheet = false },
            onSaveRequest = { newAlias, iconIndex ->
                plant = plant?.copy(aliasName = newAlias, iconIndex = iconIndex)
                showEditSheet = false
            }
        )
    }

    // ── 삭제 확인 다이얼로그 ──
    if (showDeleteDialog && plant != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("식물 삭제") },
            text = { Text("'${plant!!.aliasName}'을(를) 정말 삭제하시겠습니까?\n삭제 후에는 복구할 수 없습니다.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            val success = com.studio.plantspot.data.repository.PlantRepository.deletePlant(plantId)
                            if (success) {
                                showDeleteDialog = false
                                onPlantDeleted()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("삭제")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("식물 상세 정보") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로 가기")
                    }
                },
                actions = {
                    // 삭제 아이콘 (에디트 좌측)
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "식물 삭제",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    // 편집 아이콘
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Filled.Edit, contentDescription = "편집")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (plant == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val currentPlant = plant!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 다마고치 Lottie 애니메이션 (상세 화면 큰 표시)
                PlantTamagotchiView(
                    matchScore = currentPlant.matchScore,
                    size = 160.dp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // 이름 & 종
                Text(
                    text = currentPlant.aliasName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = currentPlant.species,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(32.dp))

                // 상태 카드
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusCard(
                        label = "햇살 만족도",
                        value = "${currentPlant.matchScore}%",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    )
                    StatusCard(
                        label = "물 주기",
                        value = "D-${currentPlant.nextWaterDDay}",
                        modifier = Modifier.weight(1f),
                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 수분 게이지
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("수분 상태", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${(currentPlant.waterGaugePercent * 100).toInt()}%",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { currentPlant.waterGaugePercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── 관리 메모 섹션 ──
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "관리 메모",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // 메모 입력 행
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = memoInput,
                                onValueChange = { memoInput = it },
                                placeholder = { Text("메모를 입력하세요") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                maxLines = 3
                            )
                            IconButton(
                                onClick = {
                                    memoViewModel.saveMemo(plantId, memoInput)
                                    memoInput = ""
                                },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Filled.Send, contentDescription = "메모 저장")
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 메모 목록
                        if (memos.isEmpty()) {
                            Text(
                                "작성된 메모가 없습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            memos.forEach { memo ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = memo.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { memoViewModel.deleteMemo(plantId, memo.id) }
                                    ) {
                                        Icon(
                                            Icons.Filled.Delete,
                                            contentDescription = "메모 삭제",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                if (memos.last() != memo) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatusCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    containerColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
