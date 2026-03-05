package com.studio.plantspot.ui.main

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraEnhance
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import android.widget.Toast
import android.content.Intent
import android.provider.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studio.plantspot.data.repository.PostUiModel
import com.studio.plantspot.ui.navigation.Screen
import com.studio.plantspot.ui.screens.calendar.CalendarScreen
import com.studio.plantspot.ui.screens.encyclopedia.EncyclopediaScreen
import com.studio.plantspot.ui.screens.home.HomeScreen
import com.studio.plantspot.ui.screens.lounge.EditPostScreen
import com.studio.plantspot.ui.screens.lounge.LoungeScreen
import com.studio.plantspot.ui.screens.lounge.PostDetailScreen
import com.studio.plantspot.ui.screens.lounge.WritePostScreen
import com.studio.plantspot.ui.screens.scanner.ScannerScreen
import com.studio.plantspot.ui.screens.detail.PlantDetailScreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import androidx.compose.runtime.LaunchedEffect
import android.Manifest

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val context = LocalContext.current
    var isFabExpanded by remember { mutableStateOf(false) }
    var pendingFabExpand by remember { mutableStateOf(false) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(cameraPermissionState.status.isGranted) {
        if (cameraPermissionState.status.isGranted && pendingFabExpand) {
            isFabExpanded = true
            pendingFabExpand = false
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isScannerScreen = currentRoute?.startsWith("scanner") == true

    Scaffold(
        bottomBar = { 
            if (!isScannerScreen) {
                BottomNavigationBar(navController = navController) 
            }
        },
        floatingActionButton = {
            if (!isScannerScreen) {
                FloatingActionButton(
                    onClick = { 
                        if (isFabExpanded) {
                            isFabExpanded = false
                        } else {
                            if (cameraPermissionState.status.isGranted) {
                                isFabExpanded = true
                            } else {
                                val status = cameraPermissionState.status
                                val isPermanentlyDenied = status is PermissionStatus.Denied && !status.shouldShowRationale
                                
                                if (hasRequestedPermission && isPermanentlyDenied) {
                                    Toast.makeText(context, "[권한] 메뉴에서 카메라를 허용해주세요.", Toast.LENGTH_LONG).show()
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    hasRequestedPermission = true
                                    pendingFabExpand = true
                                    cameraPermissionState.launchPermissionRequest()
                                }
                            }
                        }
                    },
                    shape = CircleShape,
                    containerColor = if (isFabExpanded) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary,
                    contentColor = if (isFabExpanded) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = 50.dp)
                ) {
                    val rotation by animateFloatAsState(targetValue = if (isFabExpanded) 45f else 0f, label = "fab_rotation")
                    Icon(
                        imageVector = if (isFabExpanded) Icons.Filled.Add else Icons.Filled.CenterFocusStrong,
                        contentDescription = if (isFabExpanded) "닫기" else "스캐너 열기",
                        modifier = Modifier.size(32.dp).graphicsLayer(rotationZ = rotation)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavigationHost(
                navController = navController,
                modifier = Modifier.padding(innerPadding)
            )

            // Dim Background
            if (isFabExpanded && !isScannerScreen) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isFabExpanded = false }
                )
            }

            // Expanded Menus as Overlay
            AnimatedVisibility(
                visible = isFabExpanded && !isScannerScreen,
                enter = fadeIn() + expandHorizontally(expandFrom = Alignment.CenterHorizontally),
                exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.CenterHorizontally),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 120.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(80.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExtendedFloatingActionButton(
                        text = { Text("공간 진단\n(입양 전)", textAlign = TextAlign.Center) },
                        icon = { Icon(Icons.Filled.Search, null) },
                        onClick = {
                            isFabExpanded = false
                            navController.navigate(Screen.Scanner.passMode("pre"))
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        expanded = true
                    )
                    ExtendedFloatingActionButton(
                        text = { Text("식물 진단\n(입양 후)", textAlign = TextAlign.Center) },
                        icon = { Icon(Icons.Filled.CameraEnhance, null) },
                        onClick = {
                            isFabExpanded = false
                            navController.navigate(Screen.Scanner.passMode("post"))
                        },
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary,
                        expanded = true
                    )
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val items = Screen.BottomNavItems

        // 좌측 2개
        items.take(2).forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // FAB 공간
        Spacer(modifier = Modifier.weight(1f))

        // 우측 2개
        items.takeLast(2).forEach { screen ->
            NavigationBarItem(
                icon = { Icon(imageVector = screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun NavigationHost(navController: NavHostController, modifier: Modifier = Modifier) {
    // PostDetail 화면에 넘길 PostUiModel 임시 저장
    var pendingPostDetail by remember { mutableStateOf<PostUiModel?>(null) }
    var pendingEditPost by remember { mutableStateOf<PostUiModel?>(null) }
    var pendingImageUri by remember { mutableStateOf<Uri?>(null) }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToDetail = { plantId ->
                    navController.navigate("plant_detail/$plantId")
                },
                onNavigateToWritePost = { uri ->
                    pendingImageUri = uri
                    navController.navigate(Screen.WritePost.route)
                }
            )
        }
        composable(Screen.Encyclopedia.route) { EncyclopediaScreen() }
        composable(
            route = Screen.Scanner.route,
            arguments = listOf(navArgument("mode") { defaultValue = "pre" })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "pre"
            ScannerScreen(
                mode = mode,
                onClose = { navController.popBackStack() }
            )
        }
        composable(Screen.Calendar.route) { CalendarScreen() }
        composable(Screen.Lounge.route) {
            LoungeScreen(
                onNavigateToWritePost = { navController.navigate(Screen.WritePost.route) },
                onNavigateToPostDetail = { post ->
                    pendingPostDetail = post
                    navController.navigate("post_detail")
                }
            )
        }
        composable(
            Screen.PlantDetail.route,
            arguments = listOf(navArgument("plantId") { type = NavType.StringType })
        ) { backStackEntry ->
            val plantId = backStackEntry.arguments?.getString("plantId") ?: ""
            PlantDetailScreen(
                plantId = plantId,
                onBackClick = { navController.popBackStack() }
            )
        }
        // ── 커뮤니티 라우트 ──
        composable(Screen.WritePost.route) {
            WritePostScreen(
                initialImageUri = pendingImageUri,
                onBackClick = { 
                    pendingImageUri = null
                    navController.popBackStack() 
                },
                onPostCreated = {
                    pendingImageUri = null
                    navController.popBackStack()
                }
            )
        }
        composable("post_detail") {
            val post = pendingPostDetail
            if (post != null) {
                PostDetailScreen(
                    post = post,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
        composable("edit_post") {
            val post = pendingEditPost
            if (post != null) {
                EditPostScreen(
                    post = post,
                    onBackClick = { navController.popBackStack() },
                    onPostUpdated = { navController.popBackStack() }
                )
            }
        }
    }
}
