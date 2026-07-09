package com.example.escaperoomtimer.manager

import androidx.compose.runtime.mutableStateListOf
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.repository.RoomRepository

object TimerManager {
    val rooms = mutableStateListOf(
        RoomInfo("room1", "ROOM 1", 32 * 60 + 45, RoomStatus.PAUSED, isRunning = false, defaultMinutes = 60),
        RoomInfo("room2", "ROOM 2", 4 * 60 + 58, RoomStatus.WARNING, isRunning = false, defaultMinutes = 60),
        RoomInfo("room3", "ROOM 3", 21 * 60 + 30, RoomStatus.PAUSED, isRunning = false, defaultMinutes = 60),
        RoomInfo("room4", "ROOM 4", 60 * 60, RoomStatus.WAITING, isRunning = false, defaultMinutes = 60),
        RoomInfo("room5", "ROOM 5", 0, RoomStatus.FINISHED, isRunning = false, defaultMinutes = 60)
    )

    fun getRoom(roomId: String): RoomInfo? {
        return rooms.firstOrNull { it.id == roomId }
    }

    fun startOrPause(roomId: String) {
        updateRoom(roomId) { room ->
            if (room.status == RoomStatus.FINISHED && room.seconds <= 0) return@updateRoom room

            val fixedSeconds = if (room.seconds <= 0) room.defaultMinutes * 60 else room.seconds
            val nextRunning = !room.isRunning

            room.copy(
                seconds = fixedSeconds,
                isRunning = nextRunning,
                status = if (nextRunning) runningStatusFromSeconds(fixedSeconds) else RoomStatus.PAUSED
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
            room.copy(
                seconds = nextSeconds,
                status = statusForRoom(nextSeconds, room.isRunning)
            )
        }
    }

    fun minusFiveMinutes(roomId: String) {
        updateRoom(roomId) { room ->
            val nextSeconds = (room.seconds - 5 * 60).coerceAtLeast(0)
            val nextRunning = room.isRunning && nextSeconds > 0
            room.copy(
                seconds = nextSeconds,
                isRunning = nextRunning,
                status = if (nextSeconds == 0) RoomStatus.FINISHED else statusForRoom(nextSeconds, nextRunning)
            )
        }
    }

    fun reset(roomId: String) {
        updateRoom(roomId) { room ->
            room.copy(
                seconds = room.defaultMinutes * 60,
                isRunning = false,
                status = RoomStatus.WAITING
            )
        }
    }

    fun updateRoomSetting(roomId: String, name: String, defaultMinutes: Int) {
        updateRoom(roomId) { room ->
            val cleanName = RoomRepository.sanitizeRoomName(name)
            val cleanMinutes = RoomRepository.sanitizeDefaultMinutes(defaultMinutes)
            val shouldResetTime = !room.isRunning && room.status != RoomStatus.PAUSED

            room.copy(
                name = cleanName,
                defaultMinutes = cleanMinutes,
                seconds = if (shouldResetTime) cleanMinutes * 60 else room.seconds,
                status = if (shouldResetTime) RoomStatus.WAITING else room.status
            )
        }
    }

    fun tickAll() {
        rooms.forEachIndexed { index, room ->
            if (room.isRunning && room.seconds > 0) {
                val nextSeconds = (room.seconds - 1).coerceAtLeast(0)
                rooms[index] = room.copy(
                    seconds = nextSeconds,
                    isRunning = nextSeconds > 0,
                    status = if (nextSeconds == 0) RoomStatus.FINISHED else runningStatusFromSeconds(nextSeconds)
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

    private fun statusForRoom(seconds: Int, isRunning: Boolean): RoomStatus {
        return when {
            seconds <= 0 -> RoomStatus.FINISHED
            isRunning -> runningStatusFromSeconds(seconds)
            else -> RoomStatus.PAUSED
        }
    }

    private fun runningStatusFromSeconds(seconds: Int): RoomStatus {
        return when {
            seconds <= 0 -> RoomStatus.FINISHED
            seconds <= 5 * 60 -> RoomStatus.WARNING
            else -> RoomStatus.RUNNING
        }
    }
}
