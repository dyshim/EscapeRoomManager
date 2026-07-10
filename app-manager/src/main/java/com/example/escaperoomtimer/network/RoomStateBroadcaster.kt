package com.example.escaperoomtimer.network

import com.example.escaperoomshared.model.SharedRoomState
import com.example.escaperoomshared.network.SyncProtocol
import com.example.escaperoomtimer.model.RoomInfo
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

object RoomStateBroadcaster {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "room-state-broadcaster").apply { isDaemon = true }
    }

    fun broadcast(rooms: List<RoomInfo>) {
        val snapshot = rooms.map { room ->
            SharedRoomState(
                id = room.id,
                name = room.name,
                seconds = room.seconds,
                status = room.status.name,
                isRunning = room.isRunning,
                updatedAtMillis = System.currentTimeMillis()
            )
        }

        executor.execute {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val address = InetAddress.getByName("255.255.255.255")
                    snapshot.forEach { state ->
                        val payload = SyncProtocol.encodeRoom(state)
                        socket.send(DatagramPacket(payload, payload.size, address, SyncProtocol.PORT))
                    }
                }
            }
        }
    }
}
