package com.studio.plantspot.presentation.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun NicknameDialog(
    onConfirm: (String) -> Unit,
    isLoading: Boolean
) {
    var nickname by remember { mutableStateOf("") }
    val isError = nickname.isNotEmpty() && (nickname.length < 2 || nickname.length > 10)

    Dialog(
        onDismissRequest = { },
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "환영합니다! 🌿",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "플랜트스팟에서 사용할 활동명을 정해주세요.",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(24.dp))

                OutlinedTextField(
                    value = nickname,
                    onValueChange = { if (it.length <= 10) nickname = it },
                    label = { Text("닉네임 (2~10자)") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (isError) {
                    Text(
                        text = "2자 이상 10자 이하로 입력해주세요.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.align(Alignment.Start).padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { onConfirm(nickname) },
                    enabled = !isLoading && nickname.length in 2..10,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("시작하기", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
