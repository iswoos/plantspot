package com.studio.plantspot.ui.screens.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WaterDrop
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
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarScreen() {
    val calendarViewModel: CalendarViewModel = viewModel()
    var selectedMonth by remember { mutableStateOf(YearMonth.now()) }
    val careEventDays by calendarViewModel.careEventDays.collectAsStateWithLifecycle()
    val plants by calendarViewModel.plants.collectAsStateWithLifecycle()
    val selectedPlantId by calendarViewModel.selectedPlantId.collectAsStateWithLifecycle()

    val daysInMonth = selectedMonth.lengthOfMonth()
    val firstDayOfWeek = selectedMonth.atDay(1).dayOfWeek.value % 7

    // 월 변경 시 케어 이벤트 로드
    LaunchedEffect(selectedMonth) {
        val yearMonthStr = "${selectedMonth.year}-${selectedMonth.monthValue.toString().padStart(2, '0')}"
        calendarViewModel.loadCareEvents(yearMonthStr)
    }
    LaunchedEffect(Unit) {
        calendarViewModel.loadPlants()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        // 월 헤더
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${selectedMonth.year}년 ${selectedMonth.monthValue}월",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Row {
                TextButton(onClick = { selectedMonth = selectedMonth.minusMonths(1) }) { Text("‹") }
                TextButton(onClick = { selectedMonth = selectedMonth.plusMonths(1) }) { Text("›") }
            }
        }

        // 식물 필터 칩 스크롤
        androidx.compose.foundation.lazy.LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedPlantId == null,
                    onClick = { calendarViewModel.selectPlant(null) },
                    label = { Text("전체") }
                )
            }
            items(plants.size) { index ->
                val plant = plants[index]
                FilterChip(
                    selected = selectedPlantId == plant.id,
                    onClick = { calendarViewModel.selectPlant(plant.id) },
                    label = { Text(plant.aliasName) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 요일 라벨
        Row(modifier = Modifier.fillMaxWidth()) {
            val weekdays = listOf("일", "월", "화", "수", "목", "금", "토")
            weekdays.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 달력 그리드
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(firstDayOfWeek) {
                Box(modifier = Modifier.aspectRatio(1f))
            }
            items(daysInMonth) { index ->
                val day = index + 1
                val hasEvent = careEventDays.contains(day)
                val isToday = day == LocalDate.now().dayOfMonth && selectedMonth == YearMonth.now()
                CalendarDayCell(day = day, hasEvent = hasEvent, isToday = isToday)
            }
        }
    }
}

@Composable
fun CalendarDayCell(day: Int, hasEvent: Boolean, isToday: Boolean) {
    Box(
        modifier = Modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isToday) MaterialTheme.colorScheme.primaryContainer else Color.Transparent),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(4.dp)
        ) {
            Text(
                text = day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (hasEvent) {
                Spacer(modifier = Modifier.height(4.dp))
                Icon(
                    imageVector = Icons.Filled.WaterDrop,
                    contentDescription = "물주기",
                    tint = if (day < LocalDate.now().dayOfMonth) Color.Gray else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
