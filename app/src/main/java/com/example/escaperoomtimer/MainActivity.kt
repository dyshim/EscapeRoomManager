package com.example.escaperoomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.ui.home.HomeScreen
import com.example.escaperoomtimer.ui.timer.TimerScreen
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EscapeRoomManagerApp()
        }
    }
}

@Composable
fun EscapeRoomManagerApp() {
    var selectedRoomId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            TimerManager.tickAll()
        }
    }

    if (selectedRoomId == null) {
        HomeScreen(
            rooms = TimerManager.rooms,
            onRoomClick = { room -> selectedRoomId = room.id }
        )
    } else {
        TimerScreen(
            roomId = selectedRoomId!!,
            onBack = { selectedRoomId = null }
        )
    }
}
