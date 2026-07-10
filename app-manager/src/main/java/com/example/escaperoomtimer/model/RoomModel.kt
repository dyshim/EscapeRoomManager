package com.example.escaperoomtimer.model

data class RoomInfo(
    val id: String,
    val name: String,
    val seconds: Int,
    val status: RoomStatus,
    val isRunning: Boolean = false,
    val defaultMinutes: Int = 60,
    val hintEnabled: Boolean = true,
    val guestScreenEnabled: Boolean = true
)

enum class RoomStatus {
    WAITING,
    RUNNING,
    WARNING,
    PAUSED,
    FINISHED
}
