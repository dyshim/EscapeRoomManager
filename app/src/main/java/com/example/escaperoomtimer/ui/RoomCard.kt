package com.example.escaperoomtimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.escaperoomtimer.utils.formatTime

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
        RoomStatus.FINISHED -> Color(0xFF777777)
        RoomStatus.WAITING -> Color(0xFF9E9E9E)
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
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171C20)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = room.name,
                    color = Color.White,
                    fontSize = 16.sp
                )

                Text(
                    text = timeText,
                    color = timeColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeColor, RoundedCornerShape(7.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.width(12.dp))

                Text(
                    text = "›",
                    color = Color.White,
                    fontSize = 32.sp
                )
            }
        }
    }
}