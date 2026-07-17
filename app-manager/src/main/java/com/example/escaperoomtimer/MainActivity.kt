package com.example.escaperoomtimer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.escaperoomtimer.manager.HintProgressManager
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.service.TimerForegroundService
import com.example.escaperoomtimer.ui.home.HomeScreen
import com.example.escaperoomtimer.ui.setting.SettingScreen
import com.example.escaperoomtimer.ui.timer.TimerScreen
import com.example.escaperoomtimer.ui.theme.EscapeRoomTimerTheme
import com.example.escaperoomtimer.web.ManagerWebServer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        TimerManager.initialize(applicationContext)
        HintProgressManager.start(applicationContext)
        ManagerWebServer.start(applicationContext)
        requestNotificationPermissionIfNeeded()
        startTimerForegroundService()

        setContent { EscapeRoomTimerTheme { EscapeRoomManagerApp() } }
    }

    override fun onStop() {
        TimerManager.persistNow()
        super.onStop()
    }

    private fun startTimerForegroundService() {
        ContextCompat.startForegroundService(this, Intent(this, TimerForegroundService::class.java))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001
                )
            }
        }
    }
}

private enum class ScreenMode { HOME, SETTING, STAFF_TIMER }

@Composable
fun EscapeRoomManagerApp() {
    var selectedRoomId by remember { mutableStateOf<String?>(null) }
    var screenMode by remember { mutableStateOf(ScreenMode.HOME) }

    when (screenMode) {
        ScreenMode.HOME -> HomeScreen(
            rooms = TimerManager.enabledRooms,
            onRoomClick = { room ->
                selectedRoomId = room.id
                screenMode = ScreenMode.STAFF_TIMER
            },
            onSettingsClick = { screenMode = ScreenMode.SETTING },
            onAddRoom = { name, minutes -> TimerManager.addRoom(name, minutes) }
        )

        ScreenMode.SETTING -> SettingScreen(
            rooms = TimerManager.rooms,
            onBack = { screenMode = ScreenMode.HOME },
            onSaveRoom = TimerManager::updateRoomSetting,
            onSetRoomEnabled = TimerManager::setRoomEnabled,
            onSetMaintenance = TimerManager::setMaintenance,
            onDeleteRoom = TimerManager::deleteRoom,
            onMoveRoom = TimerManager::moveRoom,
            onAddRoom = TimerManager::addRoom,
            onRestoreRooms = TimerManager::restoreConfiguration
        )

        ScreenMode.STAFF_TIMER -> {
            val roomId = selectedRoomId
            val room = roomId?.let(TimerManager::getRoom)
            if (roomId == null || room == null || !room.isEnabled) {
                screenMode = ScreenMode.HOME
            } else {
                TimerScreen(roomId = roomId, onBack = { screenMode = ScreenMode.HOME })
            }
        }
    }
}
