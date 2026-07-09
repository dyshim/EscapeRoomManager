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
import com.example.escaperoomtimer.ui.setting.SettingScreen
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
    var isSettingOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            TimerManager.tickAll()
        }
    }

    when {
        isSettingOpen -> {
            SettingScreen(
                rooms = TimerManager.rooms,
                onBack = { isSettingOpen = false },
                onSaveRoom = { roomId, name, defaultMinutes ->
                    TimerManager.updateRoomSetting(roomId, name, defaultMinutes)
                }
            )
        }

        selectedRoomId == null -> {
            HomeScreen(
                rooms = TimerManager.rooms,
                onRoomClick = { room -> selectedRoomId = room.id },
                onSettingsClick = { isSettingOpen = true }
            )
        }

        else -> {
            TimerScreen(
                roomId = selectedRoomId!!,
                onBack = { selectedRoomId = null }
            )
        }
    }
}
