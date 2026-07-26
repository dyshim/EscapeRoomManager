package com.example.escaperoomtimer.repository

object RoomRepository {
    fun sanitizeRoomName(name: String): String {
        return name.trim().ifBlank { "ROOM" }
    }

    fun sanitizeDefaultMinutes(minutes: Int): Int {
        return minutes.coerceIn(1, 240)
    }

    fun sanitizeDefaultSeconds(seconds: Int): Int = seconds.coerceIn(1, 240 * 60)
}
