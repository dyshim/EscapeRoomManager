package com.example.escaperoomtimer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.ui.home.HomeScreen
import com.example.escaperoomtimer.ui.timer.TimerScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { EscapeRoomManagerApp() }
    }
}

@Composable
fun EscapeRoomManagerApp() {
    var selectedRoom by remember { mutableStateOf<RoomInfo?>(null) }

    if (selectedRoom == null) {
        HomeScreen(onRoomClick = { selectedRoom = it })
    } else {
        TimerScreen(
            room = selectedRoom!!,
            onBack = { selectedRoom = null }
        )
    }
}
