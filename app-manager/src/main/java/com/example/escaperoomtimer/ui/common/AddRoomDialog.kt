package com.example.escaperoomtimer.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AddRoomAccent = Color(0xFF74C98C)

@Composable
fun AddRoomDialog(
    suggestedName: String,
    onDismiss: () -> Unit,
    onConfirm: (name: String, defaultSeconds: Int) -> Unit
) {
    var roomName by remember(suggestedName) { mutableStateOf(suggestedName) }
    var roomSeconds by remember { mutableStateOf(60 * 60) }
    var validationMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF171C20),
        titleContentColor = Color.White,
        textContentColor = Color(0xFFB8C0C8),
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("새 테마 추가", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(
                    "테마 이름과 기본 제한 시간을 설정하세요.",
                    color = Color(0xFF9AA3AC),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = roomName,
                    onValueChange = {
                        roomName = it
                        validationMessage = null
                    },
                    label = { Text("테마 이름") },
                    placeholder = { Text("예: ROOM 6") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("기본 시간", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                DefaultTimePicker(
                    totalSeconds = roomSeconds,
                    onSecondsChange = {
                        roomSeconds = it
                        validationMessage = null
                    }
                )

                validationMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x22FF6B6B), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanName = roomName.trim()
                    when {
                        cleanName.isBlank() -> validationMessage = "테마 이름을 입력해 주세요."
                        roomSeconds !in 1..240 * 60 -> validationMessage = "기본 시간을 확인해 주세요."
                        else -> onConfirm(cleanName, roomSeconds)
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AddRoomAccent, contentColor = Color(0xFF07130B))
            ) { Text("테마 추가", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("취소", color = Color(0xFFB8C0C8)) }
        }
    )
}
