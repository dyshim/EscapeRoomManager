package com.example.escaperoomtimer.manager

import android.content.Context
import androidx.compose.runtime.mutableStateListOf
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.repository.RoomRepository
import com.example.escaperoomtimer.repository.RoomStateRepository
import java.util.ArrayDeque

object TimerManager {
    private const val SAVE_INTERVAL_TICKS = 5

    private var appContext: Context? = null
    private var initialized = false
    private var ticksSinceLastSave = 0
    private val naturallyCompletedRoomIds = ArrayDeque<String>()

    val rooms = mutableStateListOf(
        RoomInfo("room1", "ROOM 1", 32 * 60 + 45, RoomStatus.PAUSED, defaultMinutes = 60),
        RoomInfo("room2", "ROOM 2", 4 * 60 + 58, RoomStatus.WARNING, defaultMinutes = 60),
        RoomInfo("room3", "ROOM 3", 21 * 60 + 30, RoomStatus.PAUSED, defaultMinutes = 60),
        RoomInfo("room4", "ROOM 4", 60 * 60, RoomStatus.WAITING, defaultMinutes = 60),
        RoomInfo("room5", "ROOM 5", 0, RoomStatus.FINISHED, defaultMinutes = 60)
    )

    val enabledRooms: List<RoomInfo>
        get() = rooms.filter { it.isEnabled }

    @Synchronized
    fun initialize(context: Context) {
        appContext = context.applicationContext
        if (initialized) return

        RoomStateRepository.load(context.applicationContext)?.let { savedRooms ->
            rooms.clear()
            rooms.addAll(savedRooms)
        }
        initialized = true
        persistNow()
    }

    fun getRoom(roomId: String): RoomInfo? = rooms.firstOrNull { it.id == roomId }

    fun addRoom(name: String, defaultMinutes: Int): String {
        val cleanName = RoomRepository.sanitizeRoomName(name)
        val cleanMinutes = RoomRepository.sanitizeDefaultMinutes(defaultMinutes)
        val id = nextRoomId()
        rooms.add(
            RoomInfo(
                id = id,
                name = cleanName,
                seconds = cleanMinutes * 60,
                status = RoomStatus.WAITING,
                defaultMinutes = cleanMinutes,
                isEnabled = true
            )
        )
        persistNow()
        return id
    }

    fun setMaintenance(roomId: String, maintenance: Boolean): Boolean {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index < 0) return false
        val room = rooms[index]
        if (maintenance && room.isRunning) return false
        rooms[index] = room.copy(
            isMaintenance = maintenance,
            isRunning = if (maintenance) false else room.isRunning,
            status = if (maintenance) RoomStatus.WAITING else room.status
        )
        persistNow()
        return true
    }

    fun setRoomEnabled(roomId: String, enabled: Boolean): Boolean {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index < 0) return false
        val room = rooms[index]
        if (!enabled && room.isRunning) return false
        rooms[index] = room.copy(isEnabled = enabled)
        persistNow()
        return true
    }

    fun deleteRoom(roomId: String): Boolean {
        val room = getRoom(roomId) ?: return false
        if (room.isRunning || rooms.size <= 1) return false
        rooms.removeAll { it.id == roomId }
        persistNow()
        return true
    }

    fun moveRoom(roomId: String, direction: Int): Boolean {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index < 0) return false
        val target = (index + direction).coerceIn(0, rooms.lastIndex)
        if (target == index) return false
        val room = rooms.removeAt(index)
        rooms.add(target, room)
        persistNow()
        return true
    }

    fun start(roomId: String) {
        updateRoom(roomId) { room ->
            if (!room.isEnabled || room.isMaintenance || room.isRunning) return@updateRoom room
            val startSeconds = if (room.seconds <= 0) room.defaultMinutes * 60 else room.seconds
            room.copy(
                seconds = startSeconds,
                isRunning = true,
                status = runningStatusFromSeconds(startSeconds)
            )
        }
    }

    fun startOrPause(roomId: String) {
        updateRoom(roomId) { room ->
            if (!room.isEnabled || room.isMaintenance || (room.status == RoomStatus.FINISHED && room.seconds <= 0)) {
                return@updateRoom room
            }
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
        updateRoom(roomId) { room -> room.copy(seconds = 0, isRunning = false, status = RoomStatus.FINISHED) }
    }

    fun adjustSeconds(roomId: String, deltaSeconds: Int) {
        updateRoom(roomId) { room ->
            val nextSeconds = (room.seconds + deltaSeconds).coerceAtLeast(0)
            val nextRunning = room.isRunning && nextSeconds > 0
            room.copy(
                seconds = nextSeconds,
                isRunning = nextRunning,
                status = manualStatus(room, nextSeconds, nextRunning)
            )
        }
    }

    fun setTime(roomId: String, totalSeconds: Int) {
        updateRoom(roomId) { room ->
            val nextSeconds = totalSeconds.coerceAtLeast(0)
            val nextRunning = room.isRunning && nextSeconds > 0
            room.copy(
                seconds = nextSeconds,
                isRunning = nextRunning,
                status = manualStatus(room, nextSeconds, nextRunning)
            )
        }
    }

    fun restoreTimeState(roomId: String, seconds: Int, isRunning: Boolean, status: RoomStatus) {
        updateRoom(roomId) { room ->
            val safeSeconds = seconds.coerceAtLeast(0)
            room.copy(
                seconds = safeSeconds,
                isRunning = isRunning && safeSeconds > 0,
                status = if (safeSeconds <= 0) RoomStatus.FINISHED else status
            )
        }
    }

    fun addFiveMinutes(roomId: String) = adjustSeconds(roomId, 5 * 60)
    fun minusFiveMinutes(roomId: String) = adjustSeconds(roomId, -5 * 60)

    fun reset(roomId: String) {
        updateRoom(roomId) { room ->
            room.copy(seconds = room.defaultMinutes * 60, isRunning = false, status = RoomStatus.WAITING)
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
        var changed = false
        rooms.forEachIndexed { index, room ->
            if (room.isEnabled && !room.isMaintenance && room.isRunning && room.seconds > 0) {
                val nextSeconds = (room.seconds - 1).coerceAtLeast(0)
                rooms[index] = room.copy(
                    seconds = nextSeconds,
                    isRunning = nextSeconds > 0,
                    status = if (nextSeconds == 0) RoomStatus.FINISHED else runningStatusFromSeconds(nextSeconds)
                )
                if (nextSeconds == 0) naturallyCompletedRoomIds.addLast(room.id)
                changed = true
            }
        }

        if (changed) {
            ticksSinceLastSave++
            if (ticksSinceLastSave >= SAVE_INTERVAL_TICKS) persistNow()
        }
    }

    @Synchronized
    fun consumeNaturallyCompletedRoomIds(): List<String> {
        if (naturallyCompletedRoomIds.isEmpty()) return emptyList()
        val result = naturallyCompletedRoomIds.toList()
        naturallyCompletedRoomIds.clear()
        return result
    }

    fun persistNow() {
        ticksSinceLastSave = 0
        appContext?.let { RoomStateRepository.save(it, rooms) }
    }

    private fun updateRoom(roomId: String, block: (RoomInfo) -> RoomInfo) {
        val index = rooms.indexOfFirst { it.id == roomId }
        if (index >= 0) {
            rooms[index] = block(rooms[index])
            persistNow()
        }
    }

    private fun nextRoomId(): String {
        var number = 1
        while (rooms.any { it.id == "room$number" }) number++
        return "room$number"
    }

    private fun manualStatus(room: RoomInfo, seconds: Int, isRunning: Boolean): RoomStatus = when {
        seconds <= 0 -> RoomStatus.FINISHED
        isRunning -> runningStatusFromSeconds(seconds)
        room.status == RoomStatus.WAITING -> RoomStatus.WAITING
        else -> RoomStatus.PAUSED
    }

    private fun runningStatusFromSeconds(seconds: Int): RoomStatus = when {
        seconds <= 0 -> RoomStatus.FINISHED
        seconds <= 5 * 60 -> RoomStatus.WARNING
        else -> RoomStatus.RUNNING
    }
}
