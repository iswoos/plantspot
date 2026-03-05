package com.studio.plantspot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "나의 정원", Icons.Filled.Home)
    object Encyclopedia : Screen("encyclopedia", "식물 백과", Icons.Filled.List)
    object Scanner : Screen("scanner?mode={mode}", "스캐너", Icons.Filled.AddCircle) {
        fun passMode(mode: String) = "scanner?mode=$mode"
    }
    object Calendar : Screen("calendar", "통합 달력", Icons.Filled.DateRange)
    object Lounge : Screen("lounge", "라운지", Icons.Filled.Person)
    object PlantDetail : Screen("plant_detail/{plantId}", "식물 상세", Icons.Filled.AddCircle)

    // 커뮤니티 화면 라우트
    object WritePost : Screen("write_post", "글 작성", Icons.Filled.AddCircle)
    // PostDetail은 PostUiModel을 직접 전달하므로 객체 직렬화 없이 직접 Composable 인자로 처리
    object EditPost : Screen("edit_post/{postId}", "글 수정", Icons.Filled.AddCircle)

    companion object {
        val BottomNavItems = listOf(
            Home,
            Encyclopedia,
            Calendar,
            Lounge
        )
    }
}

