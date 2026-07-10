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
    private val mainHandler = Handler(Looper.getMainLooper())
    private val listening = AtomicBoolean(false)
    private val roomsById = linkedMapOf<String, SharedRoomState>()
    private val _selectedRoom = mutableStateOf<SharedRoomState?>(null)
    val selectedRoom: State<SharedRoomState?> = _selectedRoom

    private var socket: DatagramSocket? = null
    private var multicastLock: WifiManager.MulticastLock? = null
    private var listenerThread: Thread? = null

    @Synchronized
    fun start(context: Context) {
        if (!listening.compareAndSet(false, true)) return

        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
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
        listening.set(false)
        socket?.close()
        socket = null
        listenerThread = null
        multicastLock?.let { lock -> if (lock.isHeld) lock.release() }
        multicastLock = null
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
            // Keep the display app alive even if the local network is temporarily unavailable.
        } finally {
            socket = null
            listening.set(false)
        }
    }

    @Synchronized
    private fun updateRoom(room: SharedRoomState) {
        roomsById[room.id] = room
        val nextRoom = chooseDisplayRoom()
        mainHandler.post { _selectedRoom.value = nextRoom }
    }

    private fun chooseDisplayRoom(): SharedRoomState? {
        return roomsById.values.firstOrNull { it.isRunning }
            ?: roomsById.values.firstOrNull { it.status == "PAUSED" || it.status == "WARNING" }
            ?: roomsById.values.firstOrNull()
    }
}
