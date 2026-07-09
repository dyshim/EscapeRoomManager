package com.example.escaperoomtimer.ui.home

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime
import com.example.escaperoomtimer.util.nowText
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(onRoomClick: (RoomInfo) -> Unit) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf(nowText()) }

    val rooms = remember {
        listOf(
            RoomInfo("ROOM 1", 32 * 60 + 45, RoomStatus.RUNNING),
            RoomInfo("ROOM 2", 8 * 60 + 12, RoomStatus.WARNING),
            RoomInfo("ROOM 3", 21 * 60 + 30, RoomStatus.RUNNING),
            RoomInfo("ROOM 4", 0, RoomStatus.WAITING),
            RoomInfo("ROOM 5", 0, RoomStatus.FINISHED)
        )
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            currentTime = nowText()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F12))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("☰", color = Color.White, fontSize = 28.sp)
            Text("방탈출 운영", color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            Text("⚙", color = Color.White, fontSize = 25.sp)
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = Color(0xFF2A2F35))
        Spacer(Modifier.height(14.dp))

        Text(
            text = "현재 시간  $currentTime",
            color = Color(0xFFB0B0B0),
            fontSize = 14.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(20.dp))

        rooms.forEach { room ->
            RoomCard(room = room, onClick = { onRoomClick(room) })
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(14.dp))

        Button(
            onClick = { Toast.makeText(context, "새 방 추가는 다음 버전에서 넣을게요.", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF7A00))
        ) {
            Text("+ 새 방 추가", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RoomCard(room: RoomInfo, onClick: () -> Unit) {
    val timeText = when (room.status) {
        RoomStatus.WAITING -> "대기중"
        RoomStatus.FINISHED -> "종료"
        else -> formatTime(room.seconds)
    }

    val timeColor = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF9E9E9E)
        RoomStatus.FINISHED -> Color(0xFF777777)
        RoomStatus.WARNING -> Color(0xFFFF4B4B)
        RoomStatus.RUNNING -> Color(0xFFFFB000)
    }

    val badgeText = when (room.status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> "5분 이하"
        RoomStatus.FINISHED -> "종료"
    }

    val badgeColor = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF555555)
        RoomStatus.RUNNING -> Color(0xFF6A5300)
        RoomStatus.WARNING -> Color(0xFF5A1C1C)
        RoomStatus.FINISHED -> Color(0xFF444444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171C20))
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(room.name, color = Color.White, fontSize = 16.sp)
                Text(timeText, color = timeColor, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(badgeText, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(12.dp))
                Text("›", color = Color.White, fontSize = 32.sp)
            }
        }
    }
}
