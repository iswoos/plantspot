package com.studio.plantspot.presentation.ui.main

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.studio.plantspot.domain.entity.UserProfile
import com.studio.plantspot.presentation.ui.calendar.CalendarScreen
import com.studio.plantspot.presentation.ui.explorer.ExplorerScreen
import com.studio.plantspot.presentation.ui.home.HomeScreen
import com.studio.plantspot.presentation.ui.lounge.LoungeScreen
import com.studio.plantspot.presentation.ui.navigation.Screen
import com.studio.plantspot.presentation.ui.navigation.bottomNavItems

@Composable
fun MainScreen(
    user: UserProfile?,
    onNavigateToDiagnosis: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isScannerExpanded by remember { mutableStateOf(false) }
    
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isScannerExpanded = true
        } else {
            val activity = context as? Activity
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)) {
                Toast.makeText(context, "권한에서 카메라 권한을 허용해야 스마트 스캐너를 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } else {
                Toast.makeText(context, "카메라 권한이 거부되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color.White,
                    contentColor = Color(0xFF2E7D32),
                    tonalElevation = 8.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    bottomNavItems.forEachIndexed { index, screen ->
                        if (index == 2) {
                            Spacer(Modifier.weight(0.4f))
                        }
                        NavigationBarItem(
                            icon = { screen.icon?.let { Icon(it, contentDescription = screen.title) } },
                            label = { Text(screen.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                isScannerExpanded = false
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF2E7D32),
                                selectedTextColor = Color(0xFF2E7D32),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color(0xFFE8F5E9)
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                LargeFloatingActionButton(
                    onClick = {
                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        
                        if (hasPermission) {
                            isScannerExpanded = !isScannerExpanded
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    shape = CircleShape,
                    containerColor = Color(0xFF2E7D32),
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 50.dp)
                ) {
                    Crossfade(targetState = isScannerExpanded) { expanded ->
                        Icon(
                            imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                            contentDescription = "Smart Scanner",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            },
            floatingActionButtonPosition = FabPosition.Center
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(Screen.Home.route) { HomeScreen(user) }
                composable(Screen.Explorer.route) { ExplorerScreen() }
                composable(Screen.Calendar.route) { CalendarScreen() }
                composable(Screen.Lounge.route) { LoungeScreen() }
            }
        }

        // Diagnosis Mode Overlay
        AnimatedVisibility(
            visible = isScannerExpanded,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { isScannerExpanded = false },
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier
                        .padding(bottom = 140.dp)
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    DiagnosisCard(
                        modifier = Modifier.weight(1f),
                        title = "공간 진단",
                        description = "새 친구를 맞이할 준비!\n우리의 명당은 어디일까요?",
                        icon = Icons.Default.Search,
                        onClick = {
                            isScannerExpanded = false
                            onNavigateToDiagnosis("RECOMMEND")
                        }
                    )
                    DiagnosisCard(
                        modifier = Modifier.weight(1f),
                        title = "식물 진단",
                        description = "초록이의 건강 체크!\n지금 기분이 어떤지 봐요.",
                        icon = Icons.Default.Face,
                        onClick = {
                            isScannerExpanded = false
                            onNavigateToDiagnosis("DIAGNOSE")
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DiagnosisCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF2E7D32)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}
