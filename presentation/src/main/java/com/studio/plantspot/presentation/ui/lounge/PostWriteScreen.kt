package com.studio.plantspot.presentation.ui.lounge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostWriteScreen(
    postId: String? = null,
    viewModel: LoungeViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // 수정 모드인 경우 기존 데이터 불러오기
    val detailState by viewModel.detailUiState.collectAsState()
    var isInitialized by remember { mutableStateOf(false) }

    LaunchedEffect(postId, detailState) {
        if (postId != null && !isInitialized) {
            when (detailState) {
                is LoungeDetailUiState.Success -> {
                    val post = (detailState as LoungeDetailUiState.Success).post
                    if (post.id == postId) {
                        title = post.title
                        content = post.content
                        isInitialized = true
                    } else {
                        viewModel.loadPostDetail(postId)
                    }
                }
                is LoungeDetailUiState.Loading -> {
                    // 이미 로딩 중이면 대기
                }
                else -> {
                    // 데이터가 없거나 에러 상태면 로드 시도
                    viewModel.loadPostDetail(postId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (postId == null) "새 글 쓰기" else "글 수정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로가기")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (title.isBlank() || content.isBlank()) return@TextButton
                            isLoading = true
                            scope.launch {
                                if (postId == null) {
                                    val newId = viewModel.createPost(title, content)
                                    if (newId != null) {
                                        viewModel.loadPosts() // 새로고침
                                        onBack() // 작성 성공 시 뒤로
                                    } else {
                                        isLoading = false
                                    }
                                } else {
                                    viewModel.updatePost(postId, title, content) {
                                        viewModel.loadPostDetail(postId) // 수정 후 최신화
                                        onBack()
                                    }
                                }
                            }
                        },
                        enabled = title.isNotBlank() && content.isNotBlank() && !isLoading
                    ) {
                        Text("완료", color = if (title.isNotBlank() && content.isNotBlank()) Color(0xFF2E7D32) else Color.Gray, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("제목을 입력하세요") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                placeholder = { Text("식물에 관한 자유로운 이야기를 나눠보세요!\n(사진 첨부는 준비 중입니다 📷)") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2E7D32),
                    unfocusedBorderColor = Color(0xFFE0E0E0)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            
            if (isLoading) {
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Color(0xFF2E7D32))
            }
        }
    }
}
