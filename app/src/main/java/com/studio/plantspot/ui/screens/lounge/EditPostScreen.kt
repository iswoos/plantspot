package com.studio.plantspot.ui.screens.lounge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studio.plantspot.data.repository.LoungeRepository
import com.studio.plantspot.data.repository.PostUiModel
import kotlinx.coroutines.launch

/**
 * 포스트 수정 화면
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPostScreen(
    post: PostUiModel,
    onBackClick: () -> Unit,
    onPostUpdated: () -> Unit
) {
    var content by remember { mutableStateOf(post.content) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("게시글 수정", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (content.isBlank()) return@TextButton
                            isSubmitting = true
                            scope.launch {
                                val success = LoungeRepository.updatePost(post.id, content)
                                if (success) {
                                    onPostUpdated()
                                } else {
                                    snackbarHostState.showSnackbar("수정에 실패했습니다.")
                                    isSubmitting = false
                                }
                            }
                        },
                        enabled = !isSubmitting && content.isNotBlank()
                    ) {
                        Text("저장", fontWeight = FontWeight.Bold)
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("내용") },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 200.dp),
                shape = RoundedCornerShape(12.dp),
                maxLines = 15
            )
            if (isSubmitting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
