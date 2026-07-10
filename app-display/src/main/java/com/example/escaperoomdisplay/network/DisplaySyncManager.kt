package com.example.escaperoomdisplay.network

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.escaperoomshared.model.SharedRoomState
import com.example.escaperoomshared.network.SyncProtocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

object DisplaySyncManager {
    private const val PREFS_NAME = "display_sync_preferences"
    private const val KEY_SELECTED_ROOM_ID = "selected_room_id"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listening = AtomicBoolean(false)
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

    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var listenerThread: Thread? = null
    private var debugTickRunnable: Runnable? = null

    @Synchronized
    fun start(context: Context) {
        if (!listening.compareAndSet(false, true)) return

        val appContext = context.applicationContext
        _selectedRoomId.value = appContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_ROOM_ID, null)

        val wifiManager = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        multicastLock = wifiManager?.createMulticastLock("escape-room-display-sync")?.apply {
            setReferenceCounted(false)
            acquire()
        }

        listenerThread = Thread({ listenLoop() }, "display-sync-listener").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        stopDebugDemo()
        listening.set(false)
        socket?.close()
        socket = null
        listenerThread = null
        multicastLock?.let { lock -> if (lock.isHeld) lock.release() }
        multicastLock = null
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

    /** Debug 빌드 전용 UI 테스트용 가짜 서버입니다. */
    fun startDebugDemo() {
        stopDebugDemo()

        val now = System.currentTimeMillis()
        roomsById.clear()
        roomsById["room-1"] = SharedRoomState(
            id = "room-1",
            name = "미녀와 야수",
            seconds = 60 * 60,
            status = "RUNNING",
            isRunning = true,
            updatedAtMillis = now
        )
        roomsById["room-2"] = SharedRoomState(
            id = "room-2",
            name = "Fancy",
            seconds = 4 * 60 + 58,
            status = "WARNING",
            isRunning = true,
            updatedAtMillis = now
        )
        roomsById["room-3"] = SharedRoomState(
            id = "room-3",
            name = "도둑들",
            seconds = 70 * 60,
            status = "WAITING",
            isRunning = false,
            updatedAtMillis = now
        )

        _debugDemoActive.value = true
        publishCurrentRooms(now)

        val runnable = object : Runnable {
            override fun run() {
                if (!_debugDemoActive.value) return

                val tickAt = System.currentTimeMillis()
                val updated = roomsById.mapValues { (_, room) ->
                    if (room.isRunning && room.seconds > 0) {
                        val nextSeconds = room.seconds - 1
                        room.copy(
                            seconds = nextSeconds,
                            status = if (nextSeconds <= 0) "FINISHED"
                            else if (nextSeconds <= 5 * 60) "WARNING"
                            else "RUNNING",
                            isRunning = nextSeconds > 0,
                            updatedAtMillis = tickAt
                        )
                    } else {
                        room.copy(updatedAtMillis = tickAt)
                    }
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

    private fun listenLoop() {
        try {
            DatagramSocket(SyncProtocol.PORT).use { datagramSocket ->
                socket = datagramSocket
                datagramSocket.reuseAddress = true
                datagramSocket.broadcast = true

                val buffer = ByteArray(4096)
                while (listening.get()) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    datagramSocket.receive(packet)
                    val room = SyncProtocol.decodeRoom(packet.data, packet.length) ?: continue
                    updateRoom(room)
                }
            }
        } catch (_: SocketException) {
            // Socket is intentionally closed when the app stops.
        } catch (_: Exception) {
            // Keep the display app alive if the local network is temporarily unavailable.
        } finally {
            socket = null
            listening.set(false)
        }
    }

    @Synchronized
    private fun updateRoom(room: SharedRoomState) {
        if (_debugDemoActive.value) return

        roomsById[room.id] = room
        publishCurrentRooms(System.currentTimeMillis())
    }

    private fun publishCurrentRooms(receivedAt: Long) {
        val roomList = roomsById.values.sortedBy { it.id }
        val selected = _selectedRoomId.value?.let(roomsById::get)

        mainHandler.post {
            _rooms.value = roomList
            _selectedRoom.value = selected
            _lastReceivedAtMillis.value = receivedAt
        }
    }
}
