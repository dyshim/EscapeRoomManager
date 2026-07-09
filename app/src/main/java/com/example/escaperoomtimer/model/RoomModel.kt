package com.example.escaperoomtimer.model

data class RoomInfo(
    val id: String,
    val name: String,
    val seconds: Int,
    val status: RoomStatus,
    val isRunning: Boolean = false
)

enum class RoomStatus {
    WAITING,
    RUNNING,
    WARNING,
    FINISHED
}
