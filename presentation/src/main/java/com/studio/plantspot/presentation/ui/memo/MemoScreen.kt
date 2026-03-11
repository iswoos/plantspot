package com.studio.plantspot.presentation.ui.memo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.plantspot.domain.entity.Memo
import java.time.format.DateTimeFormatter
import java.util.Locale

// ───────────────────────────────────────────────
// 색상 정의
// ───────────────────────────────────────────────
private val GreenPrimary = Color(0xFF2E7D32)
private val GreenLight = Color(0xFFE8F5E9)
private val GreenSurface = Color(0xFFF1F8F1)

// ───────────────────────────────────────────────
// 상태 관리 (어떤 화면을 보여줄지)
// ───────────────────────────────────────────────
private sealed class MemoNavState {
    object List : MemoNavState()
    object Create : MemoNavState()
    data class Edit(val memo: Memo) : MemoNavState()
}

// ───────────────────────────────────────────────
// 진입점 Composable
// ───────────────────────────────────────────────
@Composable
fun MemoScreen(
    viewModel: MemoViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var navState by remember { mutableStateOf<MemoNavState>(MemoNavState.List) }

    // 목록 → 작성/수정: 오른쪽에서 슬라이드인
    // 작성/수정 → 목록: 왼쪽으로 슬라이드아웃
    val isDetailScreen = navState != MemoNavState.List

    AnimatedContent(
        targetState = navState,
        transitionSpec = {
            if (targetState != MemoNavState.List) {
                // 목록 → 에디터: 오른쪽에서 밀려들어옴
                (slideInHorizontally { it } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 3 } + fadeOut())
            } else {
                // 에디터 → 목록: 왼쪽으로 사라짐
                (slideInHorizontally { -it / 3 } + fadeIn()) togetherWith
                        (slideOutHorizontally { it } + fadeOut())
            }
        },
        label = "MemoNavAnimation"
    ) { nav ->
        when (nav) {
            is MemoNavState.List -> MemoListScreen(
                uiState = uiState,
                onClickMemo = { memo -> navState = MemoNavState.Edit(memo) },
                onClickCreate = { navState = MemoNavState.Create }
            )

            is MemoNavState.Create -> MemoEditorScreen(
                memo = null,
                onSave = { title, content ->
                    viewModel.createMemo(title, content) { navState = MemoNavState.List }
                },
                onDelete = null,
                onBack = { navState = MemoNavState.List }
            )

            is MemoNavState.Edit -> MemoEditorScreen(
                memo = nav.memo,
                onSave = { title, content ->
                    viewModel.updateMemo(nav.memo.id, title, content) { navState = MemoNavState.List }
                },
                onDelete = {
                    viewModel.deleteMemo(nav.memo.id) { navState = MemoNavState.List }
                },
                onBack = { navState = MemoNavState.List }
            )
        }
    }
}


// ───────────────────────────────────────────────
// 목록 화면
// ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoListScreen(
    uiState: MemoUiState,
    onClickMemo: (Memo) -> Unit,
    onClickCreate: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "메모",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = GreenPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onClickCreate,
                containerColor = GreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(16.dp))
            ) {
                Icon(Icons.Default.Add, contentDescription = "새 메모")
            }
        },
        containerColor = GreenSurface
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when (uiState) {
                is MemoUiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = GreenPrimary)
                    }
                }

                is MemoUiState.Empty -> {
                    EmptyMemoView()
                }

                is MemoUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.memos, key = { it.id }) { memo ->
                            MemoCard(memo = memo, onClick = { onClickMemo(memo) })
                        }
                        // FAB 공간 확보
                        item { Spacer(Modifier.height(72.dp)) }
                    }
                }

                is MemoUiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "오류: ${uiState.message}",
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMemoView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📝", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "아직 메모가 없어요",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5A7A5A)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "오른쪽 아래 버튼을 눌러 첫 메모를 작성해 보세요!",
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun MemoCard(memo: Memo, onClick: () -> Unit) {
    val dateFormatter = DateTimeFormatter.ofPattern("yy.MM.dd HH:mm", Locale.KOREAN)
    val formattedDate = try {
        memo.updatedAt
            .atZoneSameInstant(java.time.ZoneId.systemDefault())
            .format(dateFormatter)
    } catch (e: Exception) { "" }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
            if (memo.title.isNotBlank()) {
                Text(
                    text = memo.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF222222),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = memo.content,
                fontSize = 14.sp,
                color = Color(0xFF555555),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = formattedDate,
                fontSize = 11.sp,
                color = Color.LightGray
            )
        }
    }
}

// ───────────────────────────────────────────────
// 에디터 화면 (작성 / 수정 공용)
// ───────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemoEditorScreen(
    memo: Memo?,
    onSave: (title: String, content: String) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf(memo?.title ?: "") }
    var content by remember { mutableStateOf(memo?.content ?: "") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val isNew = memo == null
    val canSave = title.isNotBlank() || content.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isNew) "새 메모" else "메모 수정",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = GreenPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로", tint = GreenPrimary)
                    }
                },
                actions = {
                    // 삭제 버튼 (수정 모드에서만 표시)
                    AnimatedVisibility(visible = onDelete != null, enter = fadeIn(), exit = fadeOut()) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "삭제", tint = Color(0xFFE53935))
                        }
                    }
                    // 저장 버튼
                    TextButton(
                        onClick = { if (canSave) onSave(title, content) },
                        enabled = canSave
                    ) {
                        Text(
                            text = "저장",
                            color = if (canSave) GreenPrimary else Color.LightGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 제목 입력
            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = {
                    Text("제목", fontSize = 22.sp, color = Color.LightGray, fontWeight = FontWeight.SemiBold)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF222222)
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = GreenPrimary
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider(color = GreenLight, thickness = 1.dp)

            // 본문 입력
            TextField(
                value = content,
                onValueChange = { content = it },
                placeholder = {
                    Text("내용을 입력하세요...", fontSize = 16.sp, color = Color.LightGray)
                },
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = Color(0xFF333333),
                    lineHeight = 26.sp
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = GreenPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
        }
    }

    // 삭제 확인 다이얼로그
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("메모 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("이 메모를 삭제할까요? 삭제 후 복구할 수 없어요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete?.invoke()
                    }
                ) {
                    Text("삭제", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("취소", color = GreenPrimary)
                }
            }
        )
    }
}
