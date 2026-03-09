package com.studio.plantspot.presentation.ui.diagnosis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.studio.plantspot.domain.entity.PlantDiagnosisHistory
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosisHistoryDialog(
    history: PlantDiagnosisHistory,
    onDismiss: () -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    val dateStr = history.createdAt?.format(dateFormatter) ?: ""
    val score = history.healthScore
    val mood = when (score) {
        in 90..100 -> "매우 좋음"
        in 70..89 -> "좋음"
        in 50..69 -> "보통"
        in 30..49 -> "나쁨"
        else -> "매우 나쁨"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White,
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 헤더
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("진단 기록 ($dateStr)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B5E20))
                        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "닫기", tint = Color.Gray)
                        }
                    }

                    // 사진
                    if (!history.imageUrl.isNullOrBlank()) {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = history.imageUrl,
                                contentDescription = "진단 식물 사진",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // 점수/기분 정보
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F8E9), RoundedCornerShape(12.dp))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("건강 분석 점수", fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("${history.healthScore}점", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1B5E20))
                            Text("기분 : $mood", fontSize = 13.sp, color = Color.Gray)
                        }
                    }

                    // 상태 상세
                    Text("분석 결과", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Text(history.analysis ?: "분석 내역이 없습니다.", fontSize = 14.sp, color = Color(0xFF555555), lineHeight = 20.sp)

                    // 해결책
                    if (!history.solution.isNullOrBlank()) {
                        Text("추천 해결책", fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                        Text(history.solution ?: "", fontSize = 14.sp, color = Color(0xFF555555), lineHeight = 20.sp)
                    }
                    
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    )
}
