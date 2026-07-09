package com.example.escaperoomtimer.ui.timer

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
import com.example.escaperoomtimer.util.formatTime
import com.example.escaperoomtimer.util.openHintApp
import kotlinx.coroutines.delay

@Composable
fun TimerScreen(room: RoomInfo, onBack: () -> Unit) {
    val context = LocalContext.current
    var seconds by remember { mutableIntStateOf(if (room.seconds > 0) room.seconds else 60 * 60) }
    var running by remember { mutableStateOf(false) }

    LaunchedEffect(running) {
        while (running && seconds > 0) {
            delay(1000)
            seconds--
        }
        if (seconds <= 0) running = false
    }

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
            Text("←", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onBack() })
            Text(room.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("⋮", color = Color.White, fontSize = 26.sp)
        }

        Spacer(Modifier.height(70.dp))

        Text(
            formatTime(seconds),
            color = Color(0xFFFFB000),
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold
        )

        Text("남은 시간", color = Color.White, fontSize = 17.sp)
        Spacer(Modifier.height(45.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton(if (running) "Ⅱ 일시정지" else "▶ 시작", Color(0xFF0D6B24), Modifier.weight(1f)) { running = !running }
            ActionButton("■ 종료", Color(0xFF9B211B), Modifier.weight(1f)) { running = false; seconds = 0 }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            ActionButton("+5분", Color(0xFFC96D00), Modifier.weight(1f)) { seconds += 5 * 60 }
            ActionButton("-5분", Color(0xFF242A2F), Modifier.weight(1f)) { seconds = (seconds - 5 * 60).coerceAtLeast(0) }
        }
        Spacer(Modifier.height(12.dp))
        ActionButton("📱 힌트앱 실행", Color(0xFF5E2B86), Modifier.fillMaxWidth()) { openHintApp(context) }
        Spacer(Modifier.height(12.dp))
        ActionButton("📝 메모", Color(0xFF171C20), Modifier.fillMaxWidth()) { }
    }
}

@Composable
private fun ActionButton(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(62.dp),
        shape = RoundedCornerShape(11.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}
