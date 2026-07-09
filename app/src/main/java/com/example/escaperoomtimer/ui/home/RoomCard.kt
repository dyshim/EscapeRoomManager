package com.example.escaperoomtimer.ui.home

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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

@Composable
fun RoomCard(
    room: RoomInfo,
    onClick: () -> Unit
) {
    val timeText = when (room.status) {
        RoomStatus.WAITING -> "대기중"
        RoomStatus.FINISHED -> "종료"
        else -> formatTime(room.seconds)
    }

    val timeColor = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF9E9E9E)
        RoomStatus.FINISHED -> Color(0xFF777777)
        RoomStatus.WARNING -> Color(0xFFFF4B4B)
        RoomStatus.RUNNING -> Color(0xFF42E66F)
    }

    val badgeText = when (room.status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> if (room.isRunning) "진행중" else "준비"
        RoomStatus.WARNING -> "5분 이하"
        RoomStatus.FINISHED -> "종료"
    }

    val badgeColor = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF555555)
        RoomStatus.RUNNING -> Color(0xFF0F4A1E)
        RoomStatus.WARNING -> Color(0xFF5A1C1C)
        RoomStatus.FINISHED -> Color(0xFF444444)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171C20)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(timeColor, CircleShape)
                )

                Spacer(Modifier.width(8.dp))

                Column {
                    Text(
                        text = room.name,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = timeText,
                        color = timeColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "›",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
