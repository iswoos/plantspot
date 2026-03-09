package com.studio.plantspot.presentation.ui.calendar

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.studio.plantspot.domain.entity.Memo
import com.studio.plantspot.domain.entity.PlantMemo
import com.studio.plantspot.presentation.ui.plantdetail.DisplayMode
import com.studio.plantspot.presentation.ui.plantdetail.EventFilter
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 통합 캘린더용 필터 추가
enum class IntegratedEventFilter { ALL, WATERING, PLANT_MEMO, GENERAL_MEMO, DIAGNOSIS }

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun IntegratedCalendarScreen(
    viewModel: IntegratedCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }

    var displayMode by remember { mutableStateOf(DisplayMode.CALENDAR) }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통합 캘린더", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                actions = {
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .background(Color(0xFFF5F5F5), RoundedCornerShape(20.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ModeButton(
                            text = "달력",
                            isSelected = displayMode == DisplayMode.CALENDAR,
                            onClick = { displayMode = DisplayMode.CALENDAR }
                        )
                        ModeButton(
                            text = "타임",
                            isSelected = displayMode == DisplayMode.TIMELINE,
                            onClick = { displayMode = DisplayMode.TIMELINE }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF9FBE7)
    ) { innerPadding ->
        when (val state = uiState) {
            is IntegratedCalendarUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            }
            is IntegratedCalendarUiState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is IntegratedCalendarUiState.Success -> {
                IntegratedCalendarContent(
                    state = state,
                    displayMode = displayMode,
                    modifier = Modifier.padding(innerPadding),
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
private fun IntegratedCalendarContent(
    state: IntegratedCalendarUiState.Success,
    displayMode: DisplayMode,
    modifier: Modifier,
    viewModel: IntegratedCalendarViewModel
) {
    var calendarMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    
    // 필터 상태
    var selectedEventTypes by remember { mutableStateOf(setOf(IntegratedEventFilter.ALL)) }
    // 다중 선택 지원 (null: 전체 이벤트, 대상 식별자)
    var selectedTargetFilters by remember { mutableStateOf(setOf<String?>(null)) }

    // 다이얼로그 상태
    var viewingEvent by remember { mutableStateOf<IntegratedEvent?>(null) }

    val defaultDateFormatter = DateTimeFormatter.ofPattern("yyyy. MM. dd", Locale.KOREA)
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)

    // 필터 적용 단 -- 타임라인 전용
    val filteredEvents = remember(state.events, selectedEventTypes, selectedTargetFilters) {
        state.events.filter { event ->
            val targetMatched = if (selectedTargetFilters.contains(null)) {
                true
            } else {
                when (event) {
                    is IntegratedEvent.Watering -> selectedTargetFilters.contains(event.plantId)
                    is IntegratedEvent.PlantSpecificMemo -> selectedTargetFilters.contains(event.plantId)
                    is IntegratedEvent.PlantDiagnosis -> selectedTargetFilters.contains(event.plantId)
                    is IntegratedEvent.GeneralMemo -> true
                }
            }
            val typeMatched = if (selectedEventTypes.contains(IntegratedEventFilter.ALL)) {
                true
            } else {
                when (event) {
                    is IntegratedEvent.Watering -> selectedEventTypes.contains(IntegratedEventFilter.WATERING)
                    is IntegratedEvent.PlantSpecificMemo -> selectedEventTypes.contains(IntegratedEventFilter.PLANT_MEMO)
                    is IntegratedEvent.GeneralMemo -> selectedEventTypes.contains(IntegratedEventFilter.GENERAL_MEMO)
                    is IntegratedEvent.PlantDiagnosis -> selectedEventTypes.contains(IntegratedEventFilter.DIAGNOSIS)
                }
            }
            targetMatched && typeMatched
        }
    }

    // 달력 그리드용: 필터도 적용된 이벤트만
    val eventDayMap = remember(filteredEvents) {
        filteredEvents.groupBy { it.date }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // 필터 칩 섹션
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(bottom = 8.dp)
        ) {
            // 대상 필터 (Row 1 - 가로 스크롤)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 전체 이벤트 (배타적)
                item {
                    FilterChip(
                        selected = selectedTargetFilters.contains(null),
                        onClick = { selectedTargetFilters = setOf(null) },
                        label = { Text("전체 이벤트") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFC8E6C9),
                            selectedLabelColor = Color(0xFF1B5E20)
                        )
                    )
                }

                // 삭제된 영역 (기존 Row 1의 일반 메모)
                // 식물 목록
                items(state.plants) { plant ->
                    val isSelected = selectedTargetFilters.contains(plant.id)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTargetFilters = if (isSelected) {
                                (selectedTargetFilters - plant.id).ifEmpty { setOf(null) }
                            } else {
                                (selectedTargetFilters - null) + plant.id
                            }
                        },
                        label = { Text(plant.nickname) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFE8F5E9),
                            selectedLabelColor = Color(0xFF2E7D32)
                        )
                    )
                }
            }

            // 종류 필터 (Row 2) - 모드 버튼 이동으로 여유 공간 확보
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clipToBounds(), // 오른쪽 침범 방지
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val onTypeToggle = { type: IntegratedEventFilter ->
                        selectedEventTypes = if (type == IntegratedEventFilter.ALL) {
                            setOf(IntegratedEventFilter.ALL)
                        } else {
                            val newSet = selectedEventTypes - IntegratedEventFilter.ALL
                            if (newSet.contains(type)) {
                                (newSet - type).ifEmpty { setOf(IntegratedEventFilter.ALL) }
                            } else {
                                newSet + type
                            }
                        }
                    }

                    item {
                        FilterChip(
                            selected = selectedEventTypes.contains(IntegratedEventFilter.ALL),
                            onClick = { onTypeToggle(IntegratedEventFilter.ALL) },
                            label = { Text("전체") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedEventTypes.contains(IntegratedEventFilter.GENERAL_MEMO),
                            onClick = { onTypeToggle(IntegratedEventFilter.GENERAL_MEMO) },
                            label = { Text("📝 일반 메모") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedEventTypes.contains(IntegratedEventFilter.WATERING),
                            onClick = { onTypeToggle(IntegratedEventFilter.WATERING) },
                            label = { Text("💧 물 주기") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedEventTypes.contains(IntegratedEventFilter.PLANT_MEMO),
                            onClick = { onTypeToggle(IntegratedEventFilter.PLANT_MEMO) },
                            label = { Text("🪴 식물 메모") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = selectedEventTypes.contains(IntegratedEventFilter.DIAGNOSIS),
                            onClick = { onTypeToggle(IntegratedEventFilter.DIAGNOSIS) },
                            label = { Text("🔍 진단") }
                        )
                    }
                }
            }
        }

        // 콘텐츠 영역: 캘린더 vs 타임라인
        if (displayMode == DisplayMode.CALENDAR) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    IntegratedCalendarGridView(
                        calendarMonth = calendarMonth,
                        eventDayMap = eventDayMap,
                        selectedDate = selectedDate,
                        onPreviousMonth = { calendarMonth = calendarMonth.minusMonths(1) },
                        onNextMonth = { calendarMonth = calendarMonth.plusMonths(1) },
                        onDayClick = { selectedDate = if (selectedDate == it) null else it }
                    )
                }
                item {
                    androidx.compose.animation.AnimatedVisibility(visible = selectedDate != null) {
                        selectedDate?.let { date ->
                            val dayEvents = eventDayMap[date] ?: emptyList()
                            IntegratedDayEventsView(
                                date = date,
                                events = dayEvents,
                                dateFormatter = defaultDateFormatter,
                                onEventClick = { viewingEvent = it }
                            )
                        }
                    }
                }
            }
        } else {
            // 타임라인: weight(1f)로 LazyColumn이 남은 bounded 공간을 정확히 받아야 크래시 방지
            IntegratedTimelineView(
                events = filteredEvents,
                dateFormatter = defaultDateFormatter,
                onEventClick = { viewingEvent = it },
                modifier = Modifier.weight(1f)
            )
        }
    }

    // 상세보기 모달
    viewingEvent?.let { event ->
        IntegratedEventDetailDialog(
            event = event,
            dateFormatter = defaultDateFormatter,
            onDismiss = { viewingEvent = null },
            viewModel = viewModel
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 모달 (모드 스위치) 버튼
// ─────────────────────────────────────────────────────────────
@Composable
private fun ModeButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .widthIn(min = 60.dp) // 너비 확보
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF1B5E20) else Color.Gray,
            maxLines = 1
        )
    }
}

// ─────────────────────────────────────────────────────────────
// 달력 뷰 (Grid)
// ─────────────────────────────────────────────────────────────
@Composable
private fun IntegratedCalendarGridView(
    calendarMonth: YearMonth,
    eventDayMap: Map<LocalDate, List<IntegratedEvent>>,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit
) {
    val monthFormatter = DateTimeFormatter.ofPattern("yyyy년 M월", Locale.KOREA)
    val daysOfWeek = listOf("일", "월", "화", "수", "목", "금", "토")
    // YearMonth.atDay(1)의 요일 (월=1 ~ 일=7) -> (일=0 ~ 토=6) 일요일 시작으로 변경
    val firstDayOfWeek = calendarMonth.atDay(1).dayOfWeek.value % 7
    val daysInMonth = calendarMonth.lengthOfMonth()

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 헤더
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

            // 요일
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

            // 날짜 그리드
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
                            val date = calendarMonth.atDay(day)
                            val events = eventDayMap[date] ?: emptyList()
                            
                            val hasWatering = events.any { it is IntegratedEvent.Watering }
                            val hasPlantMemo = events.any { it is IntegratedEvent.PlantSpecificMemo }
                            val hasGenMemo = events.any { it is IntegratedEvent.GeneralMemo }
                            val hasDiagnosis = events.any { it is IntegratedEvent.PlantDiagnosis }

                            val isToday = date == LocalDate.now()
                            val isSelected = date == selectedDate

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp) // 4개 이모티콘 2x2 표시 공간 확보
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onDayClick(date) }
                                    .background(if (isSelected) Color(0xFFE8F5E9) else Color.Transparent),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Top // 상단 정렬로 변경하여 공간 확보
                            ) {
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
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
                                Spacer(Modifier.height(2.dp))
                                // 이모티콘 존재 여부를 일반 목록으로 수집
                                val iconList = mutableListOf<String>()
                                if (hasWatering) iconList.add("💧")
                                if (hasPlantMemo) iconList.add("🪴")
                                if (hasGenMemo) iconList.add("📝")
                                if (hasDiagnosis) iconList.add("🔍")
                                
                                if (iconList.size <= 2) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight() // height 제한 해제 및 패딩 방지
                                    ) {
                                        iconList.forEach { Text(it, fontSize = 10.sp, lineHeight = 10.sp) }
                                    }
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.fillMaxWidth().wrapContentHeight()
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.wrapContentHeight()) {
                                            iconList.take(2).forEach { Text(it, fontSize = 10.sp, lineHeight = 10.sp) }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.wrapContentHeight()) {
                                            iconList.drop(2).take(2).forEach { Text(it, fontSize = 10.sp, lineHeight = 10.sp) }
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
}

// ─────────────────────────────────────────────────────────────
// 달력 하단 당일 이벤트 리스트 뷰
// ─────────────────────────────────────────────────────────────
@Composable
private fun IntegratedDayEventsView(
    date: LocalDate,
    events: List<IntegratedEvent>,
    dateFormatter: DateTimeFormatter,
    onEventClick: (IntegratedEvent) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "${date.format(DateTimeFormatter.ofPattern("yyyy. MM. dd"))} 기록", 
            fontWeight = FontWeight.Bold, 
            color = Color(0xFF2E7D32),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        if(events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("기록이 없습니다.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            events.forEach { event ->
                IntegratedEventItem(event, dateFormatter, onEventClick)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 타임라인 뷰
// ─────────────────────────────────────────────────────────────
@Composable
private fun IntegratedTimelineView(
    events: List<IntegratedEvent>,
    dateFormatter: DateTimeFormatter,
    onEventClick: (IntegratedEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (events.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("기록이 없습니다.", color = Color.LightGray, fontSize = 14.sp)
                }
            }
        } else {
            items(events) { event ->
                IntegratedEventItem(event, dateFormatter, onEventClick)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 공용 이벤트 아이템 컴포넌트
// ─────────────────────────────────────────────────────────────
@Composable
private fun IntegratedEventItem(
    event: IntegratedEvent,
    dateFormatter: DateTimeFormatter,
    onClick: (IntegratedEvent) -> Unit
) {
    val icon: String
    val bgColor: Color
    val titleText: String
    val subtitleText: String
    val clickEnabled: Boolean

    when (event) {
        is IntegratedEvent.Watering -> {
            icon = "💧"
            bgColor = Color(0xFF64B5F6)
            titleText = "[${event.plantNickname}] 물 주기 완료"
            subtitleText = ""
            clickEnabled = false
        }
        is IntegratedEvent.PlantSpecificMemo -> {
            val contentPreview = event.memo.content.take(20) + if(event.memo.content.length > 20) "…" else ""
            val imageHint = if(!event.memo.imageUrl.isNullOrBlank()) "🖼️ " else ""
            icon = "🪴"
            bgColor = Color(0xFFFFF59D)
            titleText = "[${event.plantNickname}] 식물 메모"
            subtitleText = "$imageHint$contentPreview"
            clickEnabled = true
        }
        is IntegratedEvent.GeneralMemo -> {
            icon = "📝"
            bgColor = Color(0xFFE4E6EB)
            titleText = "일반 메모"
            subtitleText = event.memo.title
            clickEnabled = true
        }
        is IntegratedEvent.PlantDiagnosis -> {
            val mood = when (event.history.healthScore) {
                in 90..100 -> "매우 좋음"
                in 70..89 -> "좋음"
                in 50..69 -> "보통"
                in 30..49 -> "나쁨"
                else -> "매우 나쁨"
            }
            icon = "🔍"
            bgColor = Color(0xFFFFF59D)
            titleText = "[${event.plantNickname}] 진단 이력"
            subtitleText = "기분 : $mood"
            clickEnabled = true // 다이얼로그 연동 예정
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(BorderStroke(1.dp, Color(0xFFEEEEEE)), RoundedCornerShape(12.dp))
            .clickable(enabled = clickEnabled) { if (clickEnabled) onClick(event) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(bgColor.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(text = icon, fontSize = 22.sp)
        }
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = titleText, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF333333))
            Spacer(Modifier.height(2.dp))
            Text(text = subtitleText, fontSize = 13.sp, color = Color.Gray)
            Spacer(Modifier.height(4.dp))
            Text(text = event.date.format(dateFormatter), fontSize = 11.sp, color = Color.LightGray)
        }
    }
}

// ─────────────────────────────────────────────────────────────
// 상세 조회 + 삭제 다이얼로그
// ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegratedEventDetailDialog(
    event: IntegratedEvent,
    dateFormatter: DateTimeFormatter,
    onDismiss: () -> Unit,
    viewModel: IntegratedCalendarViewModel
) {
    if (event is IntegratedEvent.Watering) return
    
    // 진단 다이얼로그 호출 (편집 불가, 단순 조회)
    if (event is IntegratedEvent.PlantDiagnosis) {
        com.studio.plantspot.presentation.ui.diagnosis.DiagnosisHistoryDialog(
            history = event.history,
            onDismiss = onDismiss
        )
        return
    }
    
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    // 수정용 상태
    var editContent by remember { mutableStateOf("") }
    var editTitle by remember { mutableStateOf("") } // 일반 메모용
    var editImageUri by remember { mutableStateOf<Uri?>(null) }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> editImageUri = uri }

    // 초기화
    LaunchedEffect(event) {
        when (event) {
            is IntegratedEvent.PlantSpecificMemo -> {
                editContent = event.memo.content
            }
            is IntegratedEvent.GeneralMemo -> {
                editTitle = event.memo.title
                editContent = event.memo.content
            }
            else -> {}
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("메모 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("정말 이 메모를 삭제하시겠습니까?", fontSize = 15.sp) },
            confirmButton = {
                TextButton(
                    onClick = {
                        when (event) {
                            is IntegratedEvent.PlantSpecificMemo -> viewModel.deletePlantMemo(event.memo.id)
                            is IntegratedEvent.GeneralMemo -> viewModel.deleteGeneralMemo(event.memo.id)
                            else -> {}
                        }
                        showDeleteConfirm = false
                        onDismiss()
                    }
                ) {
                    Text("삭제", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("취소", color = Color.Gray) }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            content = {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier.padding(16.dp).fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 헤더
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val titleText = if (isEditing) {
                                if (event is IntegratedEvent.GeneralMemo) "일반 메모 수정" else "식물 메모 수정"
                            } else {
                                when (event) {
                                    is IntegratedEvent.PlantSpecificMemo -> "[${event.plantNickname}] 메모"
                                    is IntegratedEvent.GeneralMemo -> "일반 메모"
                                    else -> ""
                                }
                            }
                            Text(titleText, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                            
                            Row {
                                if (!isEditing) {
                                    IconButton(onClick = { isEditing = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "수정", tint = Color.Gray)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color.Gray)
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.Gray)
                                }
                            }
                        }

                        if (isEditing) {
                            // 수정 모드
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (event is IntegratedEvent.GeneralMemo) {
                                    OutlinedTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        label = { Text("제목") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }
                                
                                // 이미지 영역
                                if (event is IntegratedEvent.PlantSpecificMemo) {
                                    Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp))) {
                                        val imageModel = editImageUri ?: event.memo.imageUrl
                                        if (imageModel != null) {
                                            AsyncImage(
                                                model = imageModel,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(Modifier.fillMaxSize().background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center) {
                                                Text("이미지 없음", color = Color.Gray, fontSize = 12.sp)
                                            }
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = { imagePickerLauncher.launch("image/*") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color(0xFF81C784))
                                    ) {
                                        Text("📷 이미지 변경", color = Color(0xFF2E7D32))
                                    }
                                }

                                OutlinedTextField(
                                    value = editContent,
                                    onValueChange = { editContent = it },
                                    label = { Text("내용") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 3,
                                    maxLines = 5,
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        when (event) {
                                            is IntegratedEvent.PlantSpecificMemo -> {
                                                viewModel.updatePlantMemo(
                                                    memoId = event.memo.id,
                                                    plantId = event.plantId,
                                                    content = editContent,
                                                    imageUri = editImageUri,
                                                    existingImageUrl = event.memo.imageUrl
                                                )
                                            }
                                            is IntegratedEvent.GeneralMemo -> {
                                                viewModel.updateGeneralMemo(
                                                    memoId = event.memo.id,
                                                    title = editTitle,
                                                    content = editContent
                                                )
                                            }
                                            else -> {}
                                        }
                                        onDismiss()
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("수정 완료", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            // 조회 모드
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (event is IntegratedEvent.PlantSpecificMemo && !event.memo.imageUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = event.memo.imageUrl,
                                        contentDescription = "메모 이미지",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                    )
                                }
                                
                                if (event is IntegratedEvent.GeneralMemo) {
                                    Text(
                                        text = event.memo.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }

                                // 내용 영역 배경 추가
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                                        .padding(16.dp)
                                ) {
                                    val content = when (event) {
                                        is IntegratedEvent.PlantSpecificMemo -> event.memo.content
                                        is IntegratedEvent.GeneralMemo -> event.memo.content
                                        else -> ""
                                    }
                                    Text(
                                        text = content,
                                        fontSize = 15.sp,
                                        color = Color.DarkGray,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text(
                                    text = when (event) {
                                        is IntegratedEvent.PlantSpecificMemo -> event.memo.createdAt
                                        is IntegratedEvent.GeneralMemo -> event.memo.createdAt
                                        else -> java.time.OffsetDateTime.now()
                                    }.atZoneSameInstant(ZoneId.systemDefault()).format(dateFormatter),
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        )
    }
}
