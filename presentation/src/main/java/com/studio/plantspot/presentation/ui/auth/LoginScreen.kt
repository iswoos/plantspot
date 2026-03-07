package com.studio.plantspot.presentation.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onNavigateToMain: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.navigateToHome.collect { onNavigateToMain() }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthViewModel.UiState.Success) {
            onNavigateToMain()
        }
        if (uiState is AuthViewModel.UiState.Error) {
            snackbarHostState.showSnackbar((uiState as AuthViewModel.UiState.Error).message)
        }
        if (uiState !is AuthViewModel.UiState.CheckingSession && uiState !is AuthViewModel.UiState.Success) {
            visible = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FBF9))
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 4 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🪴 PlantSpot",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "오늘 우리 집 식물은\n행복하게 잘 크고 있을까요?",
                    fontSize = 18.sp,
                    color = Color(0xFF555555),
                    textAlign = TextAlign.Center,
                    lineHeight = 26.sp
                )

                Spacer(Modifier.height(80.dp))

                Button(
                    onClick = { viewModel.signInWithGoogle(context) },
                    enabled = uiState !is AuthViewModel.UiState.Loading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Google로 시작하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        if (uiState is AuthViewModel.UiState.Loading || uiState is AuthViewModel.UiState.CheckingSession) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF9FBF9)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🪴 PlantSpot",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator(color = Color(0xFF2E7D32))
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        if (uiState is AuthViewModel.UiState.RequireNickname) {
            NicknameDialog(
                onConfirm = { nickname -> viewModel.registerNickname(nickname) },
                isLoading = uiState is AuthViewModel.UiState.Loading
            )
        }
    }
}
