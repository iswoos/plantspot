package com.studio.plantspot.presentation.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector? = null) {
    object Login : Screen("login", "로그인")
    object Main : Screen("main", "메인")
    
    // Bottom Tabs
    object Home : Screen("home", "나의 정원", Icons.Default.Home)
    object Memo : Screen("memo", "메모하기", Icons.Default.Edit)
    object Calendar : Screen("calendar", "통합 캘린더", Icons.Default.DateRange)
    object Lounge : Screen("lounge", "커뮤니티", Icons.Default.Person)
    
    // Diagnosis Flow
    object DiagnosisCamera : Screen("diagnosis_camera/{mode}", "공간 촬영")
    object DiagnosisSpot : Screen("diagnosis_spot/{mode}", "위치 선택")
    object DiagnosisLight : Screen("diagnosis_light/{mode}", "조도 측정")
    object DiagnosisResult : Screen("diagnosis_result/{mode}", "진단 결과")

    // Plant Detail
    object PlantDetail : Screen("plant_detail/{plantId}", "식물 상세")

    // Lounge (Community) Flow
    object LoungeDetail : Screen("lounge_detail/{postId}", "게시글 상세")
    object LoungeWrite : Screen("lounge_write", "글쓰기")
    object LoungeEdit : Screen("lounge_edit/{postId}", "글수정")
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Memo,
    Screen.Calendar,
    Screen.Lounge
)
