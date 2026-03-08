package com.studio.plantspot.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.studio.plantspot.presentation.ui.auth.AuthViewModel
import com.studio.plantspot.presentation.ui.auth.LoginScreen
import com.studio.plantspot.presentation.ui.main.MainScreen
import com.studio.plantspot.presentation.ui.diagnosis.DiagnosisViewModel
import com.studio.plantspot.presentation.ui.diagnosis.CameraCaptureScreen
import com.studio.plantspot.presentation.ui.diagnosis.SpotSelectionScreen
import com.studio.plantspot.presentation.ui.diagnosis.LightMeasurementScreen
import com.studio.plantspot.presentation.ui.diagnosis.DiagnosisResultScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.studio.plantspot.presentation.ui.navigation.Screen

@Composable
fun NavGraph(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Main.route) {
            val userProfile = when (val state = uiState) {
                is AuthViewModel.UiState.Success -> state.profile
                is AuthViewModel.UiState.RequireNickname -> state.profile
                else -> null
            }
            val diagnosisViewModel: DiagnosisViewModel = hiltViewModel()
            MainScreen(
                user = userProfile,
                viewModel = diagnosisViewModel,
                onNavigateToDiagnosis = { mode ->
                    navController.navigate("diagnosis_camera/$mode")
                }
            )
        }

        composable(
            route = Screen.DiagnosisCamera.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "RECOMMEND"
            val diagnosisViewModel: DiagnosisViewModel = hiltViewModel(
                navController.getBackStackEntry(Screen.Main.route)
            )
            CameraCaptureScreen(
                mode = mode,
                onImagesCaptured = { envUri, closeUpUri ->
                    diagnosisViewModel.setEnvImageUri(envUri)
                    closeUpUri?.let { diagnosisViewModel.setCloseUpImageUri(it) }
                    
                    if (mode == "DIAGNOSE") {
                        // 식물 진단 모드에서는 지점 선택 생략하고 바로 조도 측정으로 이동
                        navController.navigate("diagnosis_light/$mode")
                    } else {
                        navController.navigate("diagnosis_spot/$mode")
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DiagnosisSpot.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "RECOMMEND"
            val diagnosisViewModel: DiagnosisViewModel = hiltViewModel(
                navController.getBackStackEntry(Screen.Main.route)
            )
            val imageUri by diagnosisViewModel.capturedImageUri.collectAsState()
            imageUri?.let { uri ->
                SpotSelectionScreen(
                    imageUri = uri,
                    onSpotSelected = { x, y -> diagnosisViewModel.setSelectedSpot(x, y) },
                    onNext = { navController.navigate("diagnosis_light/$mode") },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.DiagnosisLight.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val mode = backStackEntry.arguments?.getString("mode") ?: "RECOMMEND"
            val diagnosisViewModel: DiagnosisViewModel = hiltViewModel(
                navController.getBackStackEntry(Screen.Main.route)
            )
            LightMeasurementScreen(
                onMeasurementComplete = { lux ->
                    diagnosisViewModel.setLuxValue(lux)
                    diagnosisViewModel.startDiagnosis(mode)
                    navController.navigate("diagnosis_result/$mode")
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.DiagnosisResult.route,
            arguments = listOf(navArgument("mode") { type = NavType.StringType })
        ) { backStackEntry ->
            val diagnosisViewModel: DiagnosisViewModel = hiltViewModel(
                navController.getBackStackEntry(Screen.Main.route)
            )
            DiagnosisResultScreen(
                viewModel = diagnosisViewModel,
                onFinish = {
                    diagnosisViewModel.reset()
                    navController.popBackStack(Screen.Main.route, false)
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
