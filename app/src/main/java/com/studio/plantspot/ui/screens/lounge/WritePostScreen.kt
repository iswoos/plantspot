package com.studio.plantspot.ui.screens.lounge

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.LoungeRepository
import kotlinx.coroutines.launch

/**
 * 새 포스트 작성 화면
 * 카메라 / 갤러리 이미지 첨부 지원
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePostScreen(
    initialImageUri: Uri? = null,
    onBackClick: () -> Unit,
    onPostCreated: () -> Unit
) {
    var authorName by remember { mutableStateOf("나") }
    var plantAlias by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(initialImageUri) }
    var isSubmitting by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 갤러리 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // 카메라 런처 (임시 URI 필요 - 여기서는 갤러리 대체)
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        // TODO: Bitmap을 URI로 변환하여 selectedImageUri 설정
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("새 글 작성", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (content.isBlank() || plantAlias.isBlank()) return@TextButton
                            isSubmitting = true
                            scope.launch {
                                val userId = UserPreferences.getUserId()
                                val success = LoungeRepository.createPost(
                                    userId = userId,
                                    authorName = authorName,
                                    plantAlias = plantAlias,
                                    content = content,
                                    imageUrl = selectedImageUri?.toString()
                                )
                                if (success) {
                                    onPostCreated()
                                } else {
                                    snackbarHostState.showSnackbar("포스트 작성에 실패했습니다.")
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && content.isNotBlank() && plantAlias.isNotBlank()
                    ) {
                        Text("게시", fontWeight = FontWeight.Bold)
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 닉네임
            OutlinedTextField(
                value = authorName,
                onValueChange = { authorName = it },
                label = { Text("닉네임") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 식물 별명
            OutlinedTextField(
                value = plantAlias,
                onValueChange = { plantAlias = it },
                label = { Text("식물 별명") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            // 본문
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("내 식물 자랑하기") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 160.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 10
            )

            // 이미지 첨부 영역
            if (selectedImageUri != null) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "📷 이미지 선택됨",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // 사진 선택 버튼
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Photo, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("갤러리")
                }
                OutlinedButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Camera, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("카메라")
                }
            }

            if (isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
