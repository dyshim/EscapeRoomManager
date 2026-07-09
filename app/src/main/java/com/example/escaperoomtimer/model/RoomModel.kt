package com.example.escaperoomtimer.model

data class RoomInfo(
    val name: String,
    val seconds: Int,
    val status: RoomStatus
)

enum class RoomStatus {
    WAITING,
    RUNNING,
    WARNING,
    FINISHED
}
