package com.example.escaperoomtimer.manager

import androidx.compose.runtime.mutableStateListOf
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus

object TimerManager {
    val rooms = mutableStateListOf(
        RoomInfo("room1", "ROOM 1", 32 * 60 + 45, RoomStatus.RUNNING, isRunning = false),
        RoomInfo("room2", "ROOM 2", 4 * 60 + 58, RoomStatus.WARNING, isRunning = false),
        RoomInfo("room3", "ROOM 3", 21 * 60 + 30, RoomStatus.RUNNING, isRunning = false),
        RoomInfo("room4", "ROOM 4", 0, RoomStatus.WAITING, isRunning = false),
        RoomInfo("room5", "ROOM 5", 0, RoomStatus.FINISHED, isRunning = false)
    )

    fun getRoom(roomId: String): RoomInfo? {
        return rooms.firstOrNull { it.id == roomId }
    }

    fun startOrPause(roomId: String) {
        updateRoom(roomId) { room ->
            if (room.status == RoomStatus.FINISHED) return@updateRoom room

            val fixedSeconds = if (room.seconds <= 0) 60 * 60 else room.seconds
            val nextRunning = !room.isRunning
            room.copy(
                seconds = fixedSeconds,
                isRunning = nextRunning,
                status = if (nextRunning) statusFromSeconds(fixedSeconds) else RoomStatus.WAITING
            )
        }
    }

    fun stop(roomId: String) {
        updateRoom(roomId) { room ->
            room.copy(seconds = 0, isRunning = false, status = RoomStatus.FINISHED)
        }
    }

    fun addFiveMinutes(roomId: String) {
        updateRoom(roomId) { room ->
            val nextSeconds = room.seconds + 5 * 60
            room.copy(seconds = nextSeconds, status = statusFromSeconds(nextSeconds))
        }
    }

    fun minusFiveMinutes(roomId: String) {
        updateRoom(roomId) { room ->
            val nextSeconds = (room.seconds - 5 * 60).coerceAtLeast(0)
            room.copy(
                seconds = nextSeconds,
                isRunning = if (nextSeconds == 0) false else room.isRunning,
                status = if (nextSeconds == 0) RoomStatus.FINISHED else statusFromSeconds(nextSeconds)
            )
        }
    }

    fun reset(roomId: String, minutes: Int = 60) {
        updateRoom(roomId) { room ->
            room.copy(seconds = minutes * 60, isRunning = false, status = RoomStatus.WAITING)
        }
    }

    fun tickAll() {
        rooms.forEachIndexed { index, room ->
            if (room.isRunning && room.seconds > 0) {
                val nextSeconds = (room.seconds - 1).coerceAtLeast(0)
                rooms[index] = room.copy(
                    seconds = nextSeconds,
                    isRunning = nextSeconds > 0,
                    status = if (nextSeconds == 0) RoomStatus.FINISHED else statusFromSeconds(nextSeconds)
                )
            }
        }
    }

    private fun updateRoom(roomId: String, block: (RoomInfo) -> RoomInfo) {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index >= 0) {
            rooms[index] = block(rooms[index])
        }
    }

    private fun statusFromSeconds(seconds: Int): RoomStatus {
        return when {
            seconds <= 0 -> RoomStatus.FINISHED
            seconds <= 5 * 60 -> RoomStatus.WARNING
            else -> RoomStatus.RUNNING
        }
    }
}
