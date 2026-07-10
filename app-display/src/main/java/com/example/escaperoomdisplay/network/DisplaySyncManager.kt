package com.example.escaperoomdisplay.network

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.model.SharedRoomState

object DisplaySyncManager {
    private const val PREFS_NAME = "display_sync_preferences"
    private const val KEY_SELECTED_ROOM_ID = "selected_room_id"
    private const val KEY_SERVER_HOST = "server_host"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val roomsById = linkedMapOf<String, SharedRoomState>()

    private val _rooms = mutableStateOf<List<SharedRoomState>>(emptyList())
    val rooms: State<List<SharedRoomState>> = _rooms

    private val _selectedRoomId = mutableStateOf<String?>(null)
    val selectedRoomId: State<String?> = _selectedRoomId

    private val _selectedRoom = mutableStateOf<SharedRoomState?>(null)
    val selectedRoom: State<SharedRoomState?> = _selectedRoom

    private val _lastReceivedAtMillis = mutableStateOf(0L)
    val lastReceivedAtMillis: State<Long> = _lastReceivedAtMillis

    private val _debugDemoActive = mutableStateOf(false)
    val debugDemoActive: State<Boolean> = _debugDemoActive

    private val _serverHost = mutableStateOf("")
    val serverHost: State<String> = _serverHost

    private val _isConnected = mutableStateOf(false)
    val isConnected: State<Boolean> = _isConnected

    private var appContext: Context? = null
    private var debugTickRunnable: Runnable? = null

    private val tcpClient = DisplayTcpClient(
        onRoomReceived = ::updateRoom,
        onConnectionChanged = { connected ->
            mainHandler.post {
                _isConnected.value = connected
                if (connected && _debugDemoActive.value) stopDebugDemo()
            }
        }
    )

    @Synchronized
    fun start(context: Context) {
        val contextApp = context.applicationContext
        if (appContext != null) return
        appContext = contextApp

        val prefs = contextApp.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _selectedRoomId.value = prefs.getString(KEY_SELECTED_ROOM_ID, null)
        val savedHost = prefs.getString(KEY_SERVER_HOST, "")?.trim().orEmpty()
        _serverHost.value = savedHost
        if (savedHost.isNotBlank()) tcpClient.connect(savedHost)
    }

    @Synchronized
    fun stop() {
        stopDebugDemo()
        tcpClient.disconnect()
        appContext = null
    }

    fun setServerHost(context: Context, host: String) {
        val normalized = host.trim()
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SERVER_HOST, normalized)
            .apply()

        mainHandler.post {
            _serverHost.value = normalized
            _isConnected.value = false
            roomsById.clear()
            _rooms.value = emptyList()
            _selectedRoom.value = null
            _lastReceivedAtMillis.value = 0L
        }

        if (normalized.isBlank()) tcpClient.disconnect() else tcpClient.connect(normalized)
    }

    fun reconnect() {
        val host = _serverHost.value
        if (host.isNotBlank()) tcpClient.connect(host)
    }

    fun selectRoom(context: Context, roomId: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SELECTED_ROOM_ID, roomId)
            .apply()

        mainHandler.post {
            _selectedRoomId.value = roomId
            _selectedRoom.value = roomsById[roomId]
        }
    }

    fun clearSelectedRoom(context: Context) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_SELECTED_ROOM_ID)
            .apply()

        mainHandler.post {
            _selectedRoomId.value = null
            _selectedRoom.value = null
        }
    }


    fun requestStart(roomId: String): Boolean {
        if (_debugDemoActive.value) {
            startDebugRoom(roomId)
            return true
        }
        return tcpClient.sendStartRequest(roomId)
    }

    private fun startDebugRoom(roomId: String) {
        val room = roomsById[roomId] ?: return
        val now = System.currentTimeMillis()
        val startSeconds = room.seconds.coerceAtLeast(1)
        roomsById[roomId] = room.copy(
            seconds = startSeconds,
            status = if (startSeconds <= 300) "WARNING" else "RUNNING",
            isRunning = true,
            updatedAtMillis = now
        )
        publishCurrentRooms(now)
    }

    fun sendHintUsage(roomId: String, hintNumber: Int): Boolean {
        return tcpClient.sendHint(
            HintUsageEvent(
                roomId = roomId,
                hintNumber = hintNumber.coerceAtLeast(1),
                usedAtMillis = System.currentTimeMillis()
            )
        )
    }

    fun startDebugDemo() {
        stopDebugDemo()
        val now = System.currentTimeMillis()
        roomsById.clear()
        roomsById["room-1"] = SharedRoomState("room-1", "미녀와 야수", 3600, "RUNNING", true, now)
        roomsById["room-2"] = SharedRoomState("room-2", "Fancy", 298, "WARNING", true, now)
        roomsById["room-3"] = SharedRoomState("room-3", "도둑들", 4200, "WAITING", false, now)
        _debugDemoActive.value = true
        publishCurrentRooms(now)

        val runnable = object : Runnable {
            override fun run() {
                if (!_debugDemoActive.value) return
                val tickAt = System.currentTimeMillis()
                val updated = roomsById.mapValues { (_, room) ->
                    if (room.isRunning && room.seconds > 0) {
                        val next = room.seconds - 1
                        room.copy(
                            seconds = next,
                            status = if (next <= 0) "FINISHED" else if (next <= 300) "WARNING" else "RUNNING",
                            isRunning = next > 0,
                            updatedAtMillis = tickAt
                        )
                    } else room.copy(updatedAtMillis = tickAt)
                }
                roomsById.clear()
                roomsById.putAll(updated)
                publishCurrentRooms(tickAt)
                mainHandler.postDelayed(this, 1_000L)
            }
        }
        debugTickRunnable = runnable
        mainHandler.postDelayed(runnable, 1_000L)
    }

    fun stopDebugDemo() {
        debugTickRunnable?.let(mainHandler::removeCallbacks)
        debugTickRunnable = null
        if (_debugDemoActive.value) {
            _debugDemoActive.value = false
            roomsById.clear()
            _rooms.value = emptyList()
            _selectedRoom.value = null
            _lastReceivedAtMillis.value = 0L
        }
    }

    @Synchronized
    private fun updateRoom(room: SharedRoomState) {
        if (_debugDemoActive.value) return
        roomsById[room.id] = room
        publishCurrentRooms(System.currentTimeMillis())
    }

    private fun publishCurrentRooms(receivedAt: Long) {
        val list = roomsById.values.sortedBy { it.id }
        val selected = _selectedRoomId.value?.let(roomsById::get)
        mainHandler.post {
            _rooms.value = list
            _selectedRoom.value = selected
            _lastReceivedAtMillis.value = receivedAt
        }
    }
}
