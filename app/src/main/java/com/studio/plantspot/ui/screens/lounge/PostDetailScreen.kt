package com.studio.plantspot.ui.screens.lounge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.studio.plantspot.data.local.UserPreferences
import com.studio.plantspot.data.repository.CommentUiModel
import com.studio.plantspot.data.repository.LoungeRepository
import com.studio.plantspot.data.repository.PostUiModel
import kotlinx.coroutines.launch

/**
 * 포스트 상세 화면 (댓글 목록 + 댓글 입력)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    post: PostUiModel,
    onBackClick: () -> Unit
) {
    var comments by remember { mutableStateOf<List<CommentUiModel>>(emptyList()) }
    var commentInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    // 댓글 로드
    LaunchedEffect(post.id) {
        val dtoList = LoungeRepository.getComments(post.id)
        comments = dtoList.map { dto ->
            CommentUiModel(
                id = dto.id ?: "",
                authorName = dto.authorName,
                content = dto.content,
                createdAt = dto.createdAt ?: ""
            )
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("포스트 상세") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 포스트 본문
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(post.authorName, fontWeight = FontWeight.Bold)
                            Text(
                                post.plantAlias,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(post.content, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "댓글 ${comments.size}개",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // 댓글 목록
                if (isLoading) {
                    item { CircularProgressIndicator(modifier = Modifier.padding(16.dp)) }
                } else {
                    items(comments) { comment ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        comment.authorName.take(1),
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                            Column {
                                Text(comment.authorName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text(comment.content, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            // 댓글 입력 바
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentInput,
                    onValueChange = { commentInput = it },
                    placeholder = { Text("댓글 입력...") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 2
                )
                IconButton(
                    onClick = {
                        if (commentInput.isBlank()) return@IconButton
                        val text = commentInput
                        commentInput = ""
                        scope.launch {
                            val userId = UserPreferences.getUserId()
                            val success = LoungeRepository.addComment(
                                postId = post.id,
                                userId = userId,
                                authorName = "나",
                                content = text
                            )
                            if (success) {
                                val dtoList = LoungeRepository.getComments(post.id)
                                comments = dtoList.map { dto ->
                                    CommentUiModel(
                                        id = dto.id ?: "",
                                        authorName = dto.authorName,
                                        content = dto.content,
                                        createdAt = dto.createdAt ?: ""
                                    )
                                }
                            }
                        }
                    },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Filled.Send, contentDescription = "댓글 전송")
                }
            }
        }
    }
}
