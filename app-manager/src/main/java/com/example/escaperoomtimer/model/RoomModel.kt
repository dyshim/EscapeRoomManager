package com.example.escaperoomtimer.model

data class RoomInfo(
    val id: String,
    val name: String,
    val seconds: Int,
    val status: RoomStatus,
    val isRunning: Boolean = false,
    val defaultMinutes: Int = 60,
    val defaultSeconds: Int = defaultMinutes * 60,
    val hintEnabled: Boolean = true,
    val guestScreenEnabled: Boolean = true,
    val isEnabled: Boolean = true,
    val isMaintenance: Boolean = false,
    val startedAtEpochMillis: Long? = null,
    val finishedAtEpochMillis: Long? = null,
    val elapsedSeconds: Int = 0
)

enum class RoomStatus {
    WAITING,
    RUNNING,
    WARNING,
    PAUSED,
    FINISHED
}
