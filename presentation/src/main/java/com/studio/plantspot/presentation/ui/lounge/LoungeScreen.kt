package com.studio.plantspot.presentation.ui.lounge

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.studio.plantspot.domain.entity.LoungePost
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoungeScreen(
    viewModel: LoungeViewModel = hiltViewModel(),
    onNavigateToWrite: () -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.listUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadPosts()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToWrite,
                containerColor = Color(0xFF2E7D32),
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "글쓰기")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFFE8F5E9), Color(0xFFF1F8E9))
                    )
                )
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is LoungeListUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2E7D32))
                    }
                }
                is LoungeListUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "오류: ${state.message}", color = Color.Red)
                    }
                }
                is LoungeListUiState.Success -> {
                    if (state.posts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "아직 작성된 게시글이 없습니다. 첫 글을 남겨보세요! 🌿", color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.posts) { post ->
                                PostItemCard(
                                    post = post,
                                    onClick = { onNavigateToDetail(post.id) },
                                    onLikeClick = { viewModel.togglePostLike(post.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostItemCard(
    post: LoungePost,
    onClick: () -> Unit,
    onLikeClick: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("MM.dd HH:mm").withZone(ZoneId.systemDefault())
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "🌿 ${post.authorNickname ?: "이름 모를 식집사"}",
                    fontSize = 12.sp,
                    color = Color(0xFF4CAF50),
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatter.format(post.createdAt),
                    fontSize = 13.sp,
                    color = Color.LightGray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = post.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = post.content,
                fontSize = 14.sp,
                color = Color.DarkGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Comments
                Text(text = "💬 ${post.commentCount}", fontSize = 18.sp, color = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                // Likes
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLikeClick() }
                ) {
                    Icon(
                        imageVector = if (post.isLikedByMe) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "좋아요",
                        tint = if (post.isLikedByMe) Color.Red else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${post.likeCount}", fontSize = 18.sp, color = if (post.isLikedByMe) Color.Red else Color.Gray)
                }
            }
        }
    }
}
