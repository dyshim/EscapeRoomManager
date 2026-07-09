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
import com.example.escaperoomtimer.ui.guest.GuestScreen
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

private enum class ScreenMode {
    HOME,
    SETTING,
    STAFF_TIMER,
    GUEST_TIMER
}

@Composable
fun EscapeRoomManagerApp() {
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var screenMode by remember { mutableStateOf(ScreenMode.HOME) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            TimerManager.tickAll()
        }
    }

    when (screenMode) {
        ScreenMode.HOME -> {
            HomeScreen(
                rooms = TimerManager.rooms,
                onRoomClick = { room ->
                    selectedRoomId = room.id
                    screenMode = ScreenMode.STAFF_TIMER
                },
                onSettingsClick = {
                    screenMode = ScreenMode.SETTING
                }
            )
        }

        ScreenMode.SETTING -> {
            SettingScreen(
                rooms = TimerManager.rooms,
                onBack = { screenMode = ScreenMode.HOME },
                onSaveRoom = { roomId, name, defaultMinutes ->
                    TimerManager.updateRoomSetting(roomId, name, defaultMinutes)
                }
            )
        }

        ScreenMode.STAFF_TIMER -> {
            val roomId = selectedRoomId
            if (roomId == null) {
                screenMode = ScreenMode.HOME
            } else {
                TimerScreen(
                    roomId = roomId,
                    onBack = { screenMode = ScreenMode.HOME },
                    onGuestClick = { screenMode = ScreenMode.GUEST_TIMER }
                )
            }
        }

        ScreenMode.GUEST_TIMER -> {
            val roomId = selectedRoomId
            if (roomId == null) {
                screenMode = ScreenMode.HOME
            } else {
                GuestScreen(
                    roomId = roomId,
                    onBack = { screenMode = ScreenMode.STAFF_TIMER }
                )
            }
        }
    }
}
