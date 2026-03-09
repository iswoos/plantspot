package com.studio.plantspot.presentation.ui.plantdetail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.domain.entity.UserPlant
import com.studio.plantspot.domain.entity.PlantDiagnosisHistory
import com.studio.plantspot.presentation.ui.diagnosis.DiagnosisHistoryDialog
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

// ─────────────────────────────────────────────────────────────
// 캘린더 / 타임라인에서 사용하는 이벤트 모델
// ─────────────────────────────────────────────────────────────
sealed class PlantEvent {
    abstract val date: LocalDate
    data class Watering(override val date: LocalDate) : PlantEvent()
    data class Memo(override val date: LocalDate, val memo: PlantMemo) : PlantEvent()
    data class Diagnosis(override val date: LocalDate, val history: com.studio.plantspot.domain.entity.PlantDiagnosisHistory) : PlantEvent()
}

// ─────────────────────────────────────────────────────────────
// UI 모드 및 필터
// ─────────────────────────────────────────────────────────────
enum class DetailTab { CALENDAR, MEMO, DIAGNOSIS }
enum class EventFilter { ALL, WATERING, MEMO, DIAGNOSIS }
enum class DisplayMode { CALENDAR, TIMELINE }

// ─────────────────────────────────────────────────────────────
// 메인 화면
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PlantDetailScreen(
    plantId: String,
    onBack: () -> Unit,
    viewModel: PlantDetailViewModel = hiltViewModel()
) {
    // 화면 진입 시 데이터 로드
    LaunchedEffect(plantId) { viewModel.loadAll(plantId) }

    val uiState by viewModel.uiState.collectAsState()
    val memos by viewModel.memos.collectAsState()
    val wateringHistory by viewModel.wateringHistory.collectAsState()
    val diagnosisHistory by viewModel.diagnosisHistory.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // 스낵바 표시
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FFF8)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (val state = uiState) {
                is PlantDetailUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
                is PlantDetailUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = Color.Red)
                    }
                }
                is PlantDetailUiState.Success -> {
                    PlantDetailContent(
                        plant = state.plant,
                        memos = memos,
                        wateringHistory = wateringHistory,
                        diagnosisHistory = diagnosisHistory,
                        isLoading = isLoading,
                        onBack = onBack,
                        onUpdateNickname = viewModel::updateNickname,
                        onUpdateWaterPeriod = viewModel::updateWaterPeriod,
                        onAddMemo = viewModel::addMemo,
                        onUpdateMemo = viewModel::updateMemo,
                        onDeleteMemo = viewModel::deleteMemo
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 화면 본체
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlantDetailContent(
    plant: UserPlant,
    memos: List<PlantMemo>,
    wateringHistory: List<OffsetDateTime>,
    diagnosisHistory: List<com.studio.plantspot.domain.entity.PlantDiagnosisHistory>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onUpdateNickname: (String) -> Unit,
    onUpdateWaterPeriod: (Int) -> Unit,
    onAddMemo: (String, Uri?) -> Unit,
    onUpdateMemo: (String, String, Uri?, String?) -> Unit,
    onDeleteMemo: (String) -> Unit
) {
    // 다이얼로그 상태 변수
    var showNicknameDialog by remember { mutableStateOf(false) }
    var showWaterPeriodDialog by remember { mutableStateOf(false) }
    var showAddMemoDialog by remember { mutableStateOf(false) }
    var editingMemo by remember { mutableStateOf<PlantMemo?>(null) }
    var viewingMemoDetail by remember { mutableStateOf<PlantMemo?>(null) }
    var viewingDiagnosisHistory by remember { mutableStateOf<PlantDiagnosisHistory?>(null) }

    // 삭제 재확인 팝업 상태 (기존 longPressedMemo 대신 사용)
    var deleteConfirmMemo by remember { mutableStateOf<PlantMemo?>(null) }

    // 캘린더/타임라인 관련 상태
    var currentTab by remember { mutableStateOf(DetailTab.CALENDAR) }
    var selectedEventFilters by remember { mutableStateOf(setOf(EventFilter.ALL)) }
    var displayMode by remember { mutableStateOf(DisplayMode.CALENDAR) }
    var calendarMonth by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }

    // 날짜 포맷
    val systemZone = ZoneId.systemDefault()
    val defaultDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)
    val detailDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd a h시 mm분", Locale.KOREAN)

    // 이벤트 맵 생성 (필터 미적용, 빠른 조회용)
    val rawEventDayMap: Map<LocalDate, List<PlantEvent>> = remember(wateringHistory, memos, diagnosisHistory) {
        val allRaw = wateringHistory.map { PlantEvent.Watering(it.atZoneSameInstant(systemZone).toLocalDate()) } +
                memos.map { PlantEvent.Memo(it.createdAt.atZoneSameInstant(systemZone).toLocalDate(), it) } +
                diagnosisHistory.mapNotNull { hist ->
                    hist.createdAt?.let { dt -> PlantEvent.Diagnosis(dt.atZoneSameInstant(systemZone).toLocalDate(), hist) }
                }
        allRaw.groupBy { it.date }
    }

    // 필터링 적용된 목록 (다중 선택 대응)
    val filteredEvents: List<PlantEvent> = remember(wateringHistory, memos, diagnosisHistory, selectedEventFilters) {
        val waterEvents = if (selectedEventFilters.contains(EventFilter.ALL) || selectedEventFilters.contains(EventFilter.WATERING)) {
            wateringHistory.map { PlantEvent.Watering(it.atZoneSameInstant(systemZone).toLocalDate()) }
        } else emptyList()
        val memoEvents = if (selectedEventFilters.contains(EventFilter.ALL) || selectedEventFilters.contains(EventFilter.MEMO)) {
            memos.map { PlantEvent.Memo(it.createdAt.atZoneSameInstant(systemZone).toLocalDate(), it) }
        } else emptyList()
        val diagEvents = if (selectedEventFilters.contains(EventFilter.ALL) || selectedEventFilters.contains(EventFilter.DIAGNOSIS)) {
            diagnosisHistory.mapNotNull { hist ->
                hist.createdAt?.let { dt -> PlantEvent.Diagnosis(dt.atZoneSameInstant(systemZone).toLocalDate(), hist) }
            }
        } else emptyList()
        
        (waterEvents + memoEvents + diagEvents).sortedByDescending { it.date }
    }

    val filteredEventDayMap: Map<LocalDate, List<PlantEvent>> = remember(filteredEvents) {
        filteredEvents.groupBy { it.date }
    }

    // 다이얼로그 처리
    if (showNicknameDialog) {
        NicknameEditDialog(
            currentNickname = plant.nickname,
            onConfirm = { onUpdateNickname(it); showNicknameDialog = false },
            onDismiss = { showNicknameDialog = false }
        )
    }
    if (showWaterPeriodDialog) {
        WaterPeriodDialog(
            currentPeriod = plant.waterPeriod,
            onConfirm = { onUpdateWaterPeriod(it); showWaterPeriodDialog = false },
            onDismiss = { showWaterPeriodDialog = false }
        )
    }
    if (showAddMemoDialog) {
        MemoEditDialog(
            memo = null,
            onConfirm = { content, uri -> onAddMemo(content, uri); showAddMemoDialog = false },
            onDismiss = { showAddMemoDialog = false }
        )
    }
    editingMemo?.let { memo ->
        MemoEditDialog(
            memo = memo,
            onConfirm = { content, uri ->
                onUpdateMemo(memo.id, content, uri, memo.imageUrl)
                editingMemo = null
            },
            onDismiss = { editingMemo = null }
        )
    }
    deleteConfirmMemo?.let { memo ->
        AlertDialog(
            onDismissRequest = { deleteConfirmMemo = null },
            title = { Text("메모 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("정말 이 메모를 삭제하시겠습니까?", fontSize = 15.sp) },
            confirmButton = {
                TextButton(onClick = { onDeleteMemo(memo.id); deleteConfirmMemo = null }) {
                    Text("삭제", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmMemo = null }) { Text("취소", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
    viewingMemoDetail?.let { memo ->
        MemoDetailDialog(
            memo = memo,
            dateFormatter = detailDateFormatter,
            onEdit = {
                viewingMemoDetail = null
                editingMemo = memo
            },
            onDelete = {
                viewingMemoDetail = null
                deleteConfirmMemo = memo
            },
            onDismiss = { viewingMemoDetail = null }
        )
    }

    viewingDiagnosisHistory?.let { history ->
        DiagnosisHistoryDialog(
            history = history,
            onDismiss = { viewingDiagnosisHistory = null }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // ── 1. 헤더 (얇고 깔끔한 TopBar 스타일)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로가기", tint = Color(0xFF1B5E20))
                }
                Text(
                    text = plant.nickname,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )
                IconButton(onClick = { showNicknameDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "애칭 수정", tint = Color(0xFF2E7D32))
                }
            }
        }

        // ── 2. 식물 사진
        item {
            PlantImageSection(plant = plant)
        }

        // ── 3. 기본 정보 카드 (간격 정렬된 레이아웃)
        item {
            PlantInfoCard(
                plant = plant,
                dateFormatter = defaultDateFormatter,
                onWaterPeriodClick = { showWaterPeriodDialog = true }
            )
        }

        // ── 4. 탭 선택 (캘린더 / 메모 / 진단 내역)
        item {
            Spacer(Modifier.height(16.dp))
            TabRow(
                selectedTabIndex = currentTab.ordinal,
                containerColor = Color.Transparent,
                contentColor = Color(0xFF2E7D32),
                indicator = { tabPositions ->
                    if (currentTab.ordinal < tabPositions.size) {
                        TabRowDefaults.Indicator(
                            Modifier.tabIndicatorOffset(tabPositions[currentTab.ordinal]),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
            ) {
                Tab(
                    selected = currentTab == DetailTab.CALENDAR,
                    onClick = { currentTab = DetailTab.CALENDAR },
                    text = { Text("📅 기록 캘린더", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = currentTab == DetailTab.MEMO,
                    onClick = { currentTab = DetailTab.MEMO },
                    text = { Text("📝 메모", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
                Tab(
                    selected = currentTab == DetailTab.DIAGNOSIS,
                    onClick = { currentTab = DetailTab.DIAGNOSIS },
                    text = { Text("🔍 진단 내역", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                )
            }
            Spacer(Modifier.height(16.dp))
        }

        // ── 5. 탭 구성에 따른 콘텐츠 렌더링
        when (currentTab) {
            DetailTab.CALENDAR -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = displayMode == DisplayMode.CALENDAR,
                                onClick = { displayMode = DisplayMode.CALENDAR },
                                label = { Text("캘린더", fontSize = 12.sp) }
                            )
                            FilterChip(
                                selected = displayMode == DisplayMode.TIMELINE,
                                onClick = { displayMode = DisplayMode.TIMELINE },
                                label = { Text("타임라인", fontSize = 12.sp) }
                            )
                        }
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val onToggle = { filter: EventFilter ->
                            selectedEventFilters = if (filter == EventFilter.ALL) {
                                setOf(EventFilter.ALL)
                            } else {
                                val newSet = selectedEventFilters - EventFilter.ALL
                                if (newSet.contains(filter)) {
                                    (newSet - filter).ifEmpty { setOf(EventFilter.ALL) }
                                } else {
                                    newSet + filter
                                }
                            }
                        }

                        listOf(
                            EventFilter.ALL to "전체", 
                            EventFilter.WATERING to "💧 물주기", 
                            EventFilter.MEMO to "📝 메모",
                            EventFilter.DIAGNOSIS to "🔍 진단"
                        ).forEach { (filter, label) ->
                            FilterChip(
                                selected = selectedEventFilters.contains(filter),
                                onClick = { onToggle(filter) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF81C784).copy(alpha = 0.3f),
                                    selectedLabelColor = Color(0xFF1B5E20)
                                )
                            )
                        }
                    }
                }

                item {
                    if (displayMode == DisplayMode.CALENDAR) {
                        CalendarView(
                            calendarMonth = calendarMonth,
                            eventDayMap = filteredEventDayMap,
                            selectedDate = selectedCalendarDate,
                            onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                            onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                            onDayClick = { date -> 
                                selectedCalendarDate = if (selectedCalendarDate == date) null else date 
                            }
                        )
                        
                        androidx.compose.animation.AnimatedVisibility(visible = selectedCalendarDate != null) {
                            selectedCalendarDate?.let { date ->
                                val dayEvents = filteredEventDayMap[date] ?: emptyList()
                                SelectedDayEventsView(
                                    date = date,
                                    events = dayEvents,
                                    dateFormatter = defaultDateFormatter,
                                    onMemoClick = { memo -> viewingMemoDetail = memo },
                                    onDiagnosisClick = { history -> viewingDiagnosisHistory = history }
                                )
                            }
                        }
                    } else {
                        TimelineView(
                            events = filteredEvents, 
                            dateFormatter = defaultDateFormatter,
                            onMemoClick = { memo -> viewingMemoDetail = memo },
                            onDiagnosisClick = { history -> viewingDiagnosisHistory = history }
                        )
                    }
                }
            }
            DetailTab.MEMO -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color(0xFF2E7D32), strokeWidth = 2.dp)
                        } else {
                            TextButton(onClick = { showAddMemoDialog = true }) {
                                Text("+ 메모 첨부", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (memos.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("메모가 없습니다. 메모를 추가해 보세요! 📝", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(memos, key = { it.id }) { memo ->
                        MemoCard(
                            memo = memo,
                            dateFormatter = defaultDateFormatter,
                            onClick = { viewingMemoDetail = memo },
                            onEdit = {
                                viewingMemoDetail = null
                                editingMemo = memo
                            },
                            onDelete = {
                                viewingMemoDetail = null
                                deleteConfirmMemo = memo
                            }
                        )
                    }
                }
            }
            DetailTab.DIAGNOSIS -> {
                if (diagnosisHistory.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("진단 내역이 없습니다. AI 진단을 이용해 보세요! 🔍", color = Color.LightGray, fontSize = 14.sp)
                        }
                    }
                } else {
                    items(diagnosisHistory, key = { it.id ?: it.hashCode() }) { history ->
                        val mood = when (history.healthScore) {
                            in 90..100 -> "매우 좋음"
                            in 70..89 -> "좋음"
                            in 50..69 -> "보통"
                            in 30..49 -> "나쁨"
                            else -> "매우 나쁨"
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable { viewingDiagnosisHistory = history },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFF59D)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("🔍", fontSize = 24.sp)
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("기분 : $mood", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF1B5E20))
                                    Text("건강도 : ${history.healthScore}점", fontSize = 13.sp, color = Color(0xFF4CAF50))
                                    Spacer(Modifier.height(4.dp))
                                    val dateStr = history.createdAt?.atZoneSameInstant(systemZone)
                                        ?.toLocalDate()?.format(defaultDateFormatter) ?: "날짜 미상"
                                    Text(dateStr, fontSize = 11.sp, color = Color.LightGray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlantImageSection(plant: UserPlant) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .background(Color(0xFFE8F5E9)),
        contentAlignment = Alignment.Center
    ) {
        if (plant.imageUrl.isNullOrEmpty()) {
            Text("🌿", fontSize = 100.sp)
        } else {
            AsyncImage(
                model = plant.imageUrl,
                contentDescription = plant.nickname,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 정보 정렬 (4:6 비율 유지 간격 조절)
// ─────────────────────────────────────────────────────────────
@Composable
private fun PlantInfoCard(
    plant: UserPlant,
    dateFormatter: DateTimeFormatter,
    onWaterPeriodClick: () -> Unit
) {
    val systemZone = ZoneId.systemDefault()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow("🌱 학술명") {
                Text(
                    text = plant.officialName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End
                )
            }
            InfoRow("📅 키우기 시작한 날") {
                Text(
                    text = plant.createdAt.atZoneSameInstant(systemZone).toLocalDate().format(dateFormatter),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.End
                )
            }
            InfoRow("💧 마지막 물 준 날") {
                Text(
                    text = plant.lastWateredAt?.atZoneSameInstant(systemZone)?.toLocalDate()?.format(dateFormatter) ?: "기록 없음",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B5E20),
                    textAlign = TextAlign.End
                )
            }
            InfoRow("⏰ 주기") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (plant.waterPeriod > 0) "${plant.waterPeriod}일마다" else "미설정",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Spacer(Modifier.width(12.dp))
                    OutlinedButton(
                        onClick = onWaterPeriodClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Color(0xFF81C784)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("주기 수정", fontSize = 12.sp, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, valueContent: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 넓이를 여유있게 확보하고 maxLines = 1로 감싸 줄바꿈 원천 차단
        Text(
            text = label, 
            fontSize = 14.sp, 
            color = Color.Gray, 
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(136.dp)
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            valueContent()
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 캘린더 관련 뷰
// ─────────────────────────────────────────────────────────────
@Composable
private fun CalendarView(
    calendarMonth: LocalDate,
    eventDayMap: Map<LocalDate, List<PlantEvent>>,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREAN)
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    val firstDayOfWeek = calendarMonth.dayOfWeek.value % 7 
    val daysInMonth = calendarMonth.lengthOfMonth()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPreviousMonth) { Text("◀", color = Color(0xFF2E7D32)) }
                Text(
                    calendarMonth.format(monthFormatter),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1B5E20)
                )
                IconButton(onClick = onNextMonth) { Text("▶", color = Color(0xFF2E7D32)) }
            }
            Row(Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (day == "일") Color(0xFFE57373) else if (day == "토") Color(0xFF64B5F6) else Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            val totalCells = firstDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            for (row in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day = cellIndex - firstDayOfWeek + 1
                        if (day < 1 || day > daysInMonth) {
                            Spacer(Modifier.weight(1f).height(48.dp))
                        } else {
                            val date = calendarMonth.withDayOfMonth(day)
                            val events = eventDayMap[date] ?: emptyList()
                            val hasWatering = events.any { it is PlantEvent.Watering }
                            val hasMemo = events.any { it is PlantEvent.Memo }
                            val hasDiag = events.any { it is PlantEvent.Diagnosis }
                            val isToday = date == LocalDate.now()
                            val isSelected = date == selectedDate

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDayClick(date) }
                                    .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top
                            ) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isToday) Color(0xFF2E7D32) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontSize = 13.sp,
                                        color = if (isToday) Color.White else Color.DarkGray,
                                        fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                val iconsList = mutableListOf<String>()
                                if (hasWatering) iconsList.add("💧")
                                if (hasMemo) iconsList.add("📝")
                                if (hasDiag) iconsList.add("🔍")

                                if (iconsList.isNotEmpty()) {
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally), 
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                    ) {
                                        iconsList.forEach { Text(it, fontSize = 10.sp, lineHeight = 10.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SelectedDayEventsView(
    date: LocalDate,
    events: List<PlantEvent>,
    dateFormatter: DateTimeFormatter,
    onMemoClick: (PlantMemo) -> Unit,
    onDiagnosisClick: (com.studio.plantspot.domain.entity.PlantDiagnosisHistory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .animateContentSize(animationSpec = tween(400))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("${date.format(dateFormatter)} 이벤트", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        if(events.isEmpty()) {
            Text("기록이 없습니다.", color = Color.Gray, fontSize = 13.sp)
        } else {
            events.forEach { event ->
                when(event) {
                    is PlantEvent.Watering -> {
                        TimelineItem(
                            icon = "💧",
                            color = Color(0xFF64B5F6),
                            title = "물 주기 완료",
                            subtitle = "",
                            dateString = null,
                            onClick = null
                        )
                    }
                    is PlantEvent.Memo -> {
                        TimelineItem(
                            icon = "📝",
                            color = Color(0xFFFFF59D),
                            title = "식물 메모",
                            subtitle = "${event.memo.content.take(20)}${if (event.memo.content.length > 20) "…" else ""}",
                            dateString = null,
                            onClick = { onMemoClick(event.memo) }
                        )
                    }
                    is PlantEvent.Diagnosis -> {
                        val mood = when (event.history.healthScore) {
                            in 90..100 -> "매우 좋음"
                            in 70..89 -> "좋음"
                            in 50..69 -> "보통"
                            in 30..49 -> "나쁨"
                            else -> "매우 나쁨"
                        }
                        TimelineItem(
                            icon = "🔍",
                            color = Color(0xFFFFF59D),
                            title = "진단 이력",
                            subtitle = "기분 : $mood",
                            dateString = null,
                            onClick = { onDiagnosisClick(event.history) }
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 타임라인 뷰
// ─────────────────────────────────────────────────────────────
@Composable
private fun TimelineView(
    events: List<PlantEvent>,
    dateFormatter: DateTimeFormatter,
    onMemoClick: (PlantMemo) -> Unit,
    onDiagnosisClick: (com.studio.plantspot.domain.entity.PlantDiagnosisHistory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .animateContentSize(animationSpec = tween(400)),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (events.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text("기록이 없습니다.", color = Color.LightGray, fontSize = 14.sp)
            }
        } else {
            events.forEach { event ->
                when (event) {
                    is PlantEvent.Watering -> {
                        TimelineItem(
                            icon = "💧",
                            color = Color(0xFF64B5F6),
                            title = "물 주기 완료",
                            subtitle = "",
                            dateString = event.date.format(dateFormatter),
                            onClick = null
                        )
                    }
                    is PlantEvent.Memo -> {
                        TimelineItem(
                            icon = "📝",
                            color = Color(0xFFFFF59D),
                            title = "식물 메모",
                            subtitle = "${event.memo.content.take(20)}${if (event.memo.content.length > 20) "…" else ""}",
                            dateString = event.date.format(dateFormatter),
                            onClick = { onMemoClick(event.memo) }
                        )
                    }
                    is PlantEvent.Diagnosis -> {
                        val mood = when (event.history.healthScore) {
                            in 90..100 -> "매우 좋음"
                            in 70..89 -> "좋음"
                            in 50..69 -> "보통"
                            in 30..49 -> "나쁨"
                            else -> "매우 나쁨"
                        }
                        TimelineItem(
                            icon = "🔍",
                            color = Color(0xFFFFF59D),
                            title = "진단 이력",
                            subtitle = "기분 : $mood",
                            dateString = event.date.format(dateFormatter),
                            onClick = { onDiagnosisClick(event.history) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineItem(icon: String, color: Color, title: String, subtitle: String, dateString: String? = null, onClick: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable(enabled = onClick != null) { onClick?.invoke() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(icon, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF1B5E20))
            if (subtitle.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 13.sp, color = Color.Gray)
            }
            if (!dateString.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(text = dateString, fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 메모 카드
// ─────────────────────────────────────────────────────────────
@Composable
private fun MemoCard(
    memo: PlantMemo,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val systemZone = ZoneId.systemDefault()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!memo.imageUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = memo.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp))
                )
            }
            Text(memo.content, fontSize = 14.sp, color = Color(0xFF333333), lineHeight = 20.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = memo.createdAt.atZoneSameInstant(systemZone).toLocalDate().format(dateFormatter),
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE57373))
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 메모 상세 보기 다이얼로그 (읽기 전용, 크게 표현)
// ─────────────────────────────────────────────────────────────
@Composable
private fun MemoDetailDialog(
    memo: PlantMemo,
    dateFormatter: DateTimeFormatter,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val systemZone = ZoneId.systemDefault()
    val formattedDate = memo.createdAt.atZoneSameInstant(systemZone).format(dateFormatter)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("메모 상세", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color(0xFF2E7D32))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE57373))
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (!memo.imageUrl.isNullOrEmpty()) {
                    item {
                        AsyncImage(
                            model = memo.imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF5F5F5))
                            .padding(16.dp)
                    ) {
                        Text(memo.content, fontSize = 15.sp, color = Color(0xFF333333), lineHeight = 24.sp)
                    }
                }
                item {
                    Text(formattedDate, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {},
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// 닉네임 수정 다이얼로그
// ─────────────────────────────────────────────────────────────
@Composable
private fun NicknameEditDialog(
    currentNickname: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(currentNickname) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("애칭 수정", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("새 애칭") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) {
                Text("저장", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// 물 주기 룰렛 (Number Picker) 다이얼로그
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WaterPeriodDialog(
    currentPeriod: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val initialIndex = if (currentPeriod > 0) currentPeriod else 7
    var selectedWaterPeriod by remember { mutableStateOf(initialIndex) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("물 주기 설정", fontWeight = FontWeight.Bold) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("며칠마다 물을 주시나요?", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(24.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val listState = rememberLazyListState(initialFirstVisibleItemIndex = if (selectedWaterPeriod > 0) selectedWaterPeriod - 1 else 0)
                    
                    // 중앙 하이라이트 박스
                    Box(modifier = Modifier.fillMaxWidth().height(44.dp).background(Color.White.copy(alpha = 0.5f)))
                    
                    LaunchedEffect(listState.isScrollInProgress) {
                        if (!listState.isScrollInProgress) {
                            val visibleItems = listState.layoutInfo.visibleItemsInfo
                            if (visibleItems.isNotEmpty()) {
                                val viewportCenter = (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
                                val centerItem = visibleItems.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }
                                centerItem?.let {
                                    selectedWaterPeriod = it.index + 1 // 1일부터 시작하므로 +1
                                }
                            }
                        }
                    }

                    val snapFlingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
                    val daysScope = rememberCoroutineScope()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 48.dp),
                        flingBehavior = snapFlingBehavior
                    ) {
                        items(365) { index ->
                            val currentDay = index + 1
                            val isSelected = selectedWaterPeriod == currentDay
                            
                            Text(
                                text = "${currentDay}일",
                                fontSize = if (isSelected) 22.sp else 16.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF2E7D32) else Color.Gray,
                                modifier = Modifier
                                    .padding(vertical = 4.dp)
                                    .clickable {
                                        daysScope.launch { listState.animateScrollToItem(index) }
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedWaterPeriod) }) {
                Text("저장", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}

// ─────────────────────────────────────────────────────────────
// 메모 추가/수정 다이얼로그
// ─────────────────────────────────────────────────────────────
@Composable
private fun MemoEditDialog(
    memo: PlantMemo?,
    onConfirm: (String, Uri?) -> Unit,
    onDismiss: () -> Unit
) {
    var content by remember { mutableStateOf(memo?.content ?: "") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> selectedImageUri = uri }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (memo == null) "메모 추가" else "메모 수정", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (selectedImageUri != null) {
                    AsyncImage(
                        model = selectedImageUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                    )
                } else if (!memo?.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = memo?.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))
                    )
                }

                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF81C784))
                ) {
                    Text(
                        text = if (selectedImageUri != null) "📷 이미지 변경" else "📷 이미지 첨부 (선택)",
                        color = Color(0xFF2E7D32)
                    )
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("메모 내용") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (content.isNotBlank()) onConfirm(content.trim(), selectedImageUri)
            }) {
                Text("저장", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color.Gray) }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )
}
