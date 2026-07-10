package com.example.escaperoomdisplay.network

import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.network.HintProtocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.Executors

object HintUsageSender {
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "hint-usage-sender").apply { isDaemon = true }
    }

    fun send(roomId: String, hintNumber: Int) {
        val event = HintUsageEvent(
            roomId = roomId,
            hintNumber = hintNumber.coerceAtLeast(1),
            usedAtMillis = System.currentTimeMillis()
        )

        executor.execute {
            runCatching {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val payload = HintProtocol.encode(event)
                    val address = InetAddress.getByName("255.255.255.255")
                    socket.send(
                        DatagramPacket(payload, payload.size, address, HintProtocol.PORT)
                    )
                }
            }
        }
    }
}
