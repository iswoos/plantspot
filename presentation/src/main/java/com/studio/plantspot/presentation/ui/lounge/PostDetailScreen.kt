package com.studio.plantspot.presentation.ui.lounge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.plantspot.domain.entity.LoungeComment
import com.studio.plantspot.domain.entity.LoungePost
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDetailScreen(
    postId: String,
    currentUserId: String?,
    viewModel: LoungeViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateToEdit: (String) -> Unit
) {
    val uiState by viewModel.detailUiState.collectAsState()
    var commentText by remember { mutableStateOf("") }
    var replyToComment by remember { mutableStateOf<LoungeComment?>(null) }
    var commentEditTarget by remember { mutableStateOf<LoungeComment?>(null) }

    // Bottom sheet & dropdown state
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showCommentDeleteConfirm by remember { mutableStateOf(false) }
    var commentActionTarget by remember { mutableStateOf<LoungeComment?>(null) }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(postId) {
        viewModel.loadPostDetail(postId)
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("게시글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("정말로 게시글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { 
                    showDeleteConfirm = false
                    viewModel.deletePost(postId) {
                        viewModel.loadPosts()
                        onBack()
                    }
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    if (showCommentDeleteConfirm && commentActionTarget != null) {
        AlertDialog(
            onDismissRequest = { showCommentDeleteConfirm = false },
            title = { Text("댓글 삭제", fontWeight = FontWeight.Bold) },
            text = { Text("정말로 이 댓글을 삭제하시겠습니까?") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.deleteComment(commentActionTarget!!.id)
                    showCommentDeleteConfirm = false
                    commentActionTarget = null
                }) {
                    Text("삭제", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCommentDeleteConfirm = false }) {
                    Text("취소", color = Color.Gray)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("게시글", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "뒤로")
                    }
                },
                actions = {
                    if (uiState is LoungeDetailUiState.Success) {
                        val post = (uiState as LoungeDetailUiState.Success).post
                        if (post.userId == currentUserId) {
                            Box {
                                IconButton(onClick = { menuExpanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "더보기")
                                }
                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("수정") },
                                        onClick = { 
                                            menuExpanded = false
                                            onNavigateToEdit(postId) 
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("삭제", color = Color.Red) },
                                        onClick = { 
                                            menuExpanded = false
                                            showDeleteConfirm = true 
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red) }
                                    )
                                }
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            // 댓글 입력창
            if (uiState is LoungeDetailUiState.Success) {
                Surface(
                    color = Color.White,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (commentEditTarget != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "댓글 수정 중...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "취소",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.clickable { 
                                        commentEditTarget = null
                                        commentText = "" 
                                    }.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        } else if (replyToComment != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "@${replyToComment?.authorNickname ?: "이름 모를 식집사"} 에게 답글 남기는 중...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32)
                                )
                                Text(
                                    text = "취소",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.clickable { replyToComment = null }.padding(4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = commentText,
                                onValueChange = { commentText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text(if (replyToComment == null) "댓글을 입력하세요..." else "답글을 입력하세요...") },
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF2E7D32),
                                    unfocusedBorderColor = Color(0xFFE0E0E0)
                                ),
                                maxLines = 3
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (commentText.isNotBlank()) {
                                        if (commentEditTarget != null) {
                                            viewModel.updateComment(commentEditTarget!!.id, commentText)
                                            commentEditTarget = null
                                        } else {
                                            viewModel.createComment(
                                                postId = postId,
                                                content = commentText,
                                                parentId = replyToComment?.id
                                            )
                                            replyToComment = null
                                        }
                                        commentText = ""
                                    }
                                },
                                enabled = commentText.isNotBlank(),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("등록")
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF9F9F9)
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (val state = uiState) {
                is LoungeDetailUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
                is LoungeDetailUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "오류: ${state.message}", color = Color.Red)
                    }
                }
                is LoungeDetailUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        // 1. 게시글 본문 영역
                        item {
                            PostDetailBody(
                                post = state.post,
                                onLikeClick = { viewModel.togglePostLike(postId) }
                            )
                            Divider(color = Color(0xFFEEEEEE), thickness = 8.dp)
                            
                            PaddingValues(horizontal = 16.dp, vertical = 12.dp).let {
                                Text(
                                    text = "댓글 ${state.post.commentCount}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                )
                            }
                        }

                        // 2. 댓글 리스트
                        if (state.comments.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(40.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("가장 먼저 댓글을 남겨보세요!", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(state.comments) { comment ->
                                CommentItem(
                                    comment = comment,
                                    currentUserId = currentUserId,
                                    onReplyClick = { 
                                        replyToComment = comment 
                                        commentEditTarget = null
                                        commentText = ""
                                    },
                                    onLikeClick = { viewModel.toggleCommentLike(comment.id) },
                                    onEditClick = { 
                                        commentEditTarget = comment
                                        replyToComment = null
                                        commentText = comment.content
                                    },
                                    onDeleteClick = { 
                                        commentActionTarget = comment
                                        showCommentDeleteConfirm = true
                                    }
                                )
                                
                                // 대댓글 표시
                                comment.replies.forEach { reply ->
                                    CommentItem(
                                        comment = reply,
                                        isReply = true,
                                        currentUserId = currentUserId,
                                        onReplyClick = { 
                                            replyToComment = comment 
                                            commentEditTarget = null
                                            commentText = ""
                                        }, // 답글의 답글도 부모에 종속
                                        onLikeClick = { viewModel.toggleCommentLike(reply.id) },
                                        onEditClick = { 
                                            commentEditTarget = reply
                                            replyToComment = null
                                            commentText = reply.content
                                        },
                                        onDeleteClick = { 
                                            commentActionTarget = reply
                                            showCommentDeleteConfirm = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostDetailBody(post: LoungePost, onLikeClick: () -> Unit) {
    val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm").withZone(ZoneId.systemDefault())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        // 작성자 정보
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFE8F5E9),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("🌿", fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = post.authorNickname ?: "이름 모를 식집사",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = formatter.format(post.createdAt),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Text(
            text = post.title,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = post.content,
            fontSize = 16.sp,
            color = Color.DarkGray,
            lineHeight = 24.sp
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // 좋아요 수 표시 및 버튼
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = onLikeClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (post.isLikedByMe) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
                    contentColor = if (post.isLikedByMe) Color.Red else Color.DarkGray
                ),
                shape = RoundedCornerShape(20.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "좋아요",
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("좋아요 ${post.likeCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: LoungeComment,
    isReply: Boolean = false,
    currentUserId: String?,
    onReplyClick: () -> Unit,
    onLikeClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MM.dd HH:mm").withZone(ZoneId.systemDefault())
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isReply) Color(0xFFFAFAFA) else Color.White)
            .padding(
                start = if (isReply) 48.dp else 16.dp,
                end = 16.dp,
                top = 12.dp,
                bottom = 12.dp
            )
    ) {
        if (isReply) {
            Text("↳", color = Color.Gray, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp, top = 2.dp))
        }
        
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = comment.authorNickname ?: "이름 모를 식집사",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formatter.format(comment.createdAt),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                
                if (comment.userId == currentUserId) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "더보기",
                            tint = Color.Gray,
                            modifier = Modifier
                                .size(28.dp)
                                .clickable { expanded = true }
                                .padding(4.dp)
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("수정") },
                                onClick = { 
                                    expanded = false
                                    onEditClick() 
                                },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(24.dp)) }
                            )
                            DropdownMenuItem(
                                text = { Text("삭제", color = Color.Red) },
                                onClick = { 
                                    expanded = false
                                    onDeleteClick() 
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color.Red, modifier = Modifier.size(24.dp)) }
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.content,
                fontSize = 14.sp,
                color = Color.Black
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clickable { onLikeClick() }.padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (comment.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if (comment.isLikedByMe) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "좋아요 ${comment.likeCount}",
                        fontSize = 14.sp,
                        color = if (comment.isLikedByMe) Color.Red else Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                if (!isReply) {
                    Text(
                        text = "답글 달기",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onReplyClick() }.padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
        }
    }
    Divider(color = Color(0xFFF5F5F5))
}
