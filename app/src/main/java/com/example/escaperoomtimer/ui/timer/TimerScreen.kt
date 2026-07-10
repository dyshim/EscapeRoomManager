package com.example.escaperoomtimer.ui.timer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime

@Composable
fun TimerScreen(
    roomId: String,
    onBack: () -> Unit,
    onGuestClick: () -> Unit
) {
    val room = TimerManager.getRoom(roomId) ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F12))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "←",
                color = Color.White,
                fontSize = 28.sp,
                modifier = Modifier.clickable { onBack() }
            )
            Text(room.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("직원", color = Color(0xFFFFB000), fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(34.dp))

        StatusBadge(room.status, room.isRunning)

        Spacer(Modifier.height(18.dp))

        Text(
            text = formatTime(room.seconds),
            color = timerColor(room.status),
            fontSize = 76.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = when (room.status) {
                RoomStatus.WAITING -> "대기중"
                RoomStatus.RUNNING -> "진행중"
                RoomStatus.WARNING -> "종료 임박"
                RoomStatus.PAUSED -> "일시정지"
                RoomStatus.FINISHED -> "종료"
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(30.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerButton(
                text = if (room.isRunning) "Ⅱ 일시정지" else "▶ 시작",
                color = if (room.isRunning) Color(0xFFC96D00) else Color(0xFF0D6B24),
                modifier = Modifier.weight(1f),
                onClick = { TimerManager.startOrPause(room.id) }
            )
            TimerButton(
                text = "■ 종료",
                color = Color(0xFF9B211B),
                modifier = Modifier.weight(1f),
                onClick = { TimerManager.stop(room.id) }
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TimerButton(
                text = "+5분",
                color = Color(0xFFC96D00),
                modifier = Modifier.weight(1f),
                onClick = { TimerManager.addFiveMinutes(room.id) }
            )
            TimerButton(
                text = "-5분",
                color = Color(0xFF242A2F),
                modifier = Modifier.weight(1f),
                onClick = { TimerManager.minusFiveMinutes(room.id) }
            )
        }

        Spacer(Modifier.height(12.dp))

        TimerButton(
            text = "↺ 기본시간 초기화",
            color = Color(0xFF242A2F),
            modifier = Modifier.fillMaxWidth(),
            onClick = { TimerManager.reset(room.id) }
        )

        Spacer(Modifier.height(12.dp))

        TimerButton(
            text = "👥 손님 화면",
            color = Color(0xFF0D3E6B),
            modifier = Modifier.fillMaxWidth(),
            onClick = onGuestClick
        )

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF231B2D), RoundedCornerShape(12.dp))
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column {
                Text(
                    text = "힌트 진행도",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "손님용 앱 연동 준비 중",
                    color = Color(0xFFB9A7C9),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun StatusBadge(status: RoomStatus, isRunning: Boolean) {
    val label = when (status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> if (isRunning) "5분 이하" else "일시정지"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }

    val color = when (status) {
        RoomStatus.WAITING -> Color(0xFF555555)
        RoomStatus.RUNNING -> Color(0xFF0F4A1E)
        RoomStatus.WARNING -> Color(0xFF5A1C1C)
        RoomStatus.PAUSED -> Color(0xFF6A5300)
        RoomStatus.FINISHED -> Color(0xFF444444)
    }

    Box(
        modifier = Modifier
            .background(color, RoundedCornerShape(999.dp))
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun TimerButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

fun timerColor(status: RoomStatus): Color {
    return when (status) {
        RoomStatus.WARNING -> Color(0xFFFF4B4B)
        RoomStatus.FINISHED -> Color(0xFFFF4B4B)
        RoomStatus.RUNNING -> Color(0xFF42E66F)
        RoomStatus.PAUSED -> Color(0xFFFFB000)
        RoomStatus.WAITING -> Color(0xFFFFB000)
    }
}
