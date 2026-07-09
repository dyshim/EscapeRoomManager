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
import androidx.compose.ui.draw.shadow
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
    val displayTime = when (room.status) {
        RoomStatus.WAITING -> "대기중"
        RoomStatus.FINISHED -> "종료"
        else -> formatTime(room.seconds)
    }

    val accentColor = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF8B8B8B)
        RoomStatus.RUNNING -> Color(0xFF22C55E)
        RoomStatus.WARNING -> Color(0xFFFF3B30)
        RoomStatus.FINISHED -> Color(0xFF5F6368)
    }

    val badgeText = when (room.status) {
        RoomStatus.WAITING -> "대기"
        RoomStatus.RUNNING -> "진행중"
        RoomStatus.WARNING -> "5분 이하"
        RoomStatus.FINISHED -> "종료"
    }

    val badgeBackground = when (room.status) {
        RoomStatus.WAITING -> Color(0xFF30363D)
        RoomStatus.RUNNING -> Color(0xFF123D24)
        RoomStatus.WARNING -> Color(0xFF4A1717)
        RoomStatus.FINISHED -> Color(0xFF262A2E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(104.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF171C20)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(accentColor, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = room.name,
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = displayTime,
                    color = accentColor,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(badgeBackground, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = badgeText,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "›",
                    color = Color(0xFFB8B8B8),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
