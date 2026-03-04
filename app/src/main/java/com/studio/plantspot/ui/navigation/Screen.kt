package com.studio.plantspot.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.AddCircle // Placeholder for Scanner
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "나의 정원", Icons.Filled.Home)
    object Encyclopedia : Screen("encyclopedia", "식물 백과", Icons.Filled.List)
    object Scanner : Screen("scanner", "스캐너", Icons.Filled.AddCircle)
    object Calendar : Screen("calendar", "통합 달력", Icons.Filled.DateRange)
    object Lounge : Screen("lounge", "라운지", Icons.Filled.Person)
}

val BottomNavItems = listOf(
    Screen.Home,
    Screen.Encyclopedia,
    Screen.Scanner,
    Screen.Calendar,
    Screen.Lounge
)
