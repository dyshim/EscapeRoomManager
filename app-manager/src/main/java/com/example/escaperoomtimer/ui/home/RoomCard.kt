package com.example.escaperoomtimer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RoomCard(
    room: RoomInfo,
    connectedDisplays: Int,
    onClick: () -> Unit
) {
    val timeText = if (room.isMaintenance) "유지보수" else formatTime(room.seconds)

    val timeColor = if (room.isMaintenance) Color(0xFF64B5F6) else when (room.status) {
        RoomStatus.WAITING -> Color(0xFF9E9E9E)
        RoomStatus.FINISHED -> Color(0xFF777777)
        RoomStatus.WARNING -> Color(0xFFFF4B4B)
        RoomStatus.PAUSED -> Color(0xFFFFB000)
        RoomStatus.RUNNING -> Color(0xFF42E66F)
    }

    val badgeText = if (room.isMaintenance) "유지보수" else when (room.status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행 중"
        RoomStatus.WARNING -> if (room.isRunning) "5분 이하" else "일시정지"
        RoomStatus.PAUSED -> "일시정지"
        RoomStatus.FINISHED -> "종료"
    }

    val badgeColor = if (room.isMaintenance) Color(0xFF174A6E) else when (room.status) {
        RoomStatus.WAITING -> Color(0xFF555555)
        RoomStatus.RUNNING -> Color(0xFF0F4A1E)
        RoomStatus.WARNING -> Color(0xFF5A1C1C)
        RoomStatus.PAUSED -> Color(0xFF6A5300)
        RoomStatus.FINISHED -> Color(0xFF444444)
    }

    val footer = when {
        room.isMaintenance -> "운영 제외" to "손님 화면 숨김"
        room.status == RoomStatus.WAITING ->
            "기본 시간 ${formatTime(room.defaultMinutes * 60)}" to "시작 전"
        room.status == RoomStatus.FINISHED ->
            timestampLabel("시작", room.startedAtEpochMillis) to
                timestampLabel("종료", room.finishedAtEpochMillis)
        room.isRunning ->
            timestampLabel("시작", room.startedAtEpochMillis) to
                "종료 예정 ${formatClock(System.currentTimeMillis() + room.seconds * 1_000L)}"
        else -> timestampLabel("시작", room.startedAtEpochMillis) to "현재 일시정지"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(146.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF151B20)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = timeText,
                    color = timeColor,
                    fontSize = if (room.isMaintenance) 29.sp else 35.sp,
                    fontWeight = FontWeight.Bold
                )
                when {
                    room.isMaintenance -> Text(
                        text = "손님용 선택에서 숨김",
                        color = Color(0xFFB8C0C8),
                        fontSize = 12.sp
                    )
                    room.status == RoomStatus.WAITING -> Text(
                        text = if (connectedDisplays > 0) {
                            "손님 기기 ${connectedDisplays}대 연결"
                        } else {
                            "시작 준비 완료"
                        },
                        color = if (connectedDisplays > 0) Color(0xFF74C98C) else Color(0xFFABB3BA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    room.status != RoomStatus.WAITING && room.elapsedSeconds > 0 -> Text(
                        text = "소요 시간 ${formatTime(room.elapsedSeconds)}",
                        color = timeColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .background(badgeColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = badgeText,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "›",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = Color(0xFF3A444C)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 5.dp, end = 48.dp, bottom = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    footer.first,
                    color = Color(0xFFABB3BA),
                    fontSize = 11.sp
                )
                Text(
                    footer.second,
                    color = when {
                        room.isRunning -> Color(0xFF74C98C)
                        room.status == RoomStatus.PAUSED || !room.isRunning && room.status == RoomStatus.WARNING -> Color(0xFFD6A84A)
                        else -> Color(0xFFABB3BA)
                    },
                    fontSize = 11.sp
                )
            }
        }
    }
}

private fun formatClock(epochMillis: Long): String =
    SimpleDateFormat("HH:mm", Locale.KOREA).format(Date(epochMillis))

private fun timestampLabel(label: String, epochMillis: Long?): String =
    epochMillis?.let { "$label ${formatClock(it)}" } ?: "$label --:--"
