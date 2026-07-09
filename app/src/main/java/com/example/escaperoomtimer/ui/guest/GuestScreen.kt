package com.example.escaperoomtimer.ui.guest

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.util.formatTime

@Composable
fun GuestScreen(
    roomId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val room = TimerManager.getRoom(roomId) ?: return

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val timeColor = when (room.status) {
        RoomStatus.WARNING, RoomStatus.FINISHED -> Color(0xFFFF4B4B)
        RoomStatus.RUNNING -> Color(0xFF42E66F)
        RoomStatus.PAUSED -> Color(0xFFFFB000)
        RoomStatus.WAITING -> Color(0xFFFFB000)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070A))
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "← 직원화면",
                color = Color(0xFF8D96A0),
                fontSize = 16.sp,
                modifier = Modifier.clickable { onBack() }
            )

            Text(
                text = "손님용",
                color = Color(0xFFFFB000),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(90.dp))

        Text(
            text = room.name,
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(44.dp))

        Text(
            text = formatTime(room.seconds),
            color = timeColor,
            fontSize = 92.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = when (room.status) {
                RoomStatus.WAITING -> "잠시만 기다려 주세요"
                RoomStatus.RUNNING -> "남은 시간"
                RoomStatus.WARNING -> "종료 임박"
                RoomStatus.PAUSED -> "일시정지"
                RoomStatus.FINISHED -> "종료되었습니다"
            },
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
