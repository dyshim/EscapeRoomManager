package com.example.escaperoomdisplay

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.escaperoomdisplay.network.DisplaySyncManager
import com.example.escaperoomdisplay.ui.theme.EscapeRoomTimerTheme
import com.example.escaperoomdisplay.util.openHintApp
import com.example.escaperoomshared.model.SharedRoomState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        DisplaySyncManager.start(applicationContext)

        setContent {
            EscapeRoomTimerTheme {
                val room by DisplaySyncManager.selectedRoom
                GuestDisplayScreen(room)
            }
        }
    }

    override fun onDestroy() {
        DisplaySyncManager.stop()
        super.onDestroy()
    }
}

@Composable
private fun GuestDisplayScreen(room: SharedRoomState?) {
    val context = LocalContext.current
    val roomName = room?.name ?: "연결 대기 중"
    val timeText = room?.seconds?.let(::formatTime) ?: "--:--"
    val timeColor = if ((room?.seconds ?: Int.MAX_VALUE) <= 5 * 60) Color(0xFFFF4B4B) else Color.White

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080B0E))
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (room == null) "직원용 앱을 기다리는 중" else "손님용 화면",
            color = Color(0xFFFFB000),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(28.dp))

        Text(
            text = roomName,
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = timeText,
            color = timeColor,
            fontSize = 82.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = if (room == null) "두 기기를 같은 Wi-Fi에 연결해 주세요." else "직원용 앱과 실시간 동기화 중",
            color = Color(0xFF9AA4AD),
            fontSize = 14.sp
        )

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = { openHintApp(context) },
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5E2B86))
        ) {
            Text(
                text = "힌트앱 열기",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "힌트 사용 기록 연동은 다음 커밋에서 추가됩니다.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
        )
    }
}

private fun formatTime(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    return "%02d:%02d".format(seconds / 60, seconds % 60)
}
