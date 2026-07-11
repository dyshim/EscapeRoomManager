package com.example.escaperoomtimer.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.network.ManagerTcpServer
import com.example.escaperoomtimer.util.localIpv4Address
import com.example.escaperoomtimer.util.nowText
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(
    rooms: List<RoomInfo>,
    onRoomClick: (RoomInfo) -> Unit,
    onSettingsClick: () -> Unit,
    onAddRoom: (name: String, defaultMinutes: Int) -> String
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(nowText()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var roomName by remember { mutableStateOf("") }
    var roomMinutes by remember { mutableStateOf("60") }
    val localIp = remember { localIpv4Address() }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = nowText()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☰", color = Color.White, fontSize = 32.sp)
            Text("방탈출 운영", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "⚙",
                color = Color.White,
                fontSize = 27.sp,
                modifier = Modifier.clickable(onClick = onSettingsClick)
            )
        }

        Spacer(Modifier.height(14.dp))
        HorizontalDivider(color = Color(0xFF343B42))
        Spacer(Modifier.height(14.dp))

        Text("현재 시간", color = Color(0xFFC8D0D7), fontSize = 15.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(currentTime, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("손님용 연결 주소  $localIp:45991", color = Color(0xFF65E38D), fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("PC 웹 주소  http://$localIp:8080", color = Color(0xFF7CC7FF), fontSize = 14.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("웹 초기 PIN  1234", color = Color(0xFFC8D0D7), fontSize = 13.sp, modifier = Modifier.align(Alignment.CenterHorizontally))

        Spacer(Modifier.height(18.dp))

        if (rooms.isEmpty()) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("사용 중인 방이 없습니다.", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("아래 버튼으로 방을 추가하거나 설정에서 방을 활성화하세요.", color = Color(0xFFC8D0D7), fontSize = 15.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomCard(
                        room = room,
                        connectedDisplays = ManagerTcpServer.connectedCount(room.id),
                        onClick = { onRoomClick(room) }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = {
                roomName = "ROOM ${rooms.size + 1}"
                roomMinutes = "60"
                showAddDialog = true
            },
            modifier = Modifier.fillMaxWidth().height(64.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
        ) {
            Text("+ 새 방 추가", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("새 방 추가") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = roomName,
                        onValueChange = { roomName = it },
                        label = { Text("방 이름") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = roomMinutes,
                        onValueChange = { roomMinutes = it.filter(Char::isDigit).take(3) },
                        label = { Text("기본 시간(분)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val minutes = roomMinutes.toIntOrNull()?.coerceIn(1, 240)
                    if (roomName.trim().isBlank() || minutes == null) {
                        Toast.makeText(context, "방 이름과 시간을 확인해 주세요.", Toast.LENGTH_SHORT).show()
                    } else {
                        onAddRoom(roomName, minutes)
                        showAddDialog = false
                        Toast.makeText(context, "${roomName.trim()} 추가 완료", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("추가") }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("취소") }
            }
        )
    }
}
