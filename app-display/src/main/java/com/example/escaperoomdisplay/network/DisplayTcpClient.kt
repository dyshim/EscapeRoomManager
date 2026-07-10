package com.example.escaperoomdisplay.network

import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.network.TcpProtocol
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class DisplayTcpClient(
    private val onRoomReceived: (com.example.escaperoomshared.model.SharedRoomState) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit
) {
    private val running = AtomicBoolean(false)
    private val sendLock = Any()

    @Volatile private var host: String = ""
    @Volatile private var socket: Socket? = null
    @Volatile private var writer: BufferedWriter? = null
    private var worker: Thread? = null

    fun connect(serverHost: String) {
        host = serverHost.trim()
        if (host.isBlank()) return
        if (running.compareAndSet(false, true)) {
            worker = Thread(::connectionLoop, "display-tcp-client").apply {
                isDaemon = true
                start()
            }
        } else {
            closeSocket()
        }
    }

    fun disconnect() {
        running.set(false)
        closeSocket()
        worker = null
        onConnectionChanged(false)
    }


    fun sendStartRequest(roomId: String): Boolean {
        if (roomId.isBlank()) return false
        return sendRawChecked(TcpProtocol.encodeStartRequest(roomId))
    }

    fun sendHint(event: HintUsageEvent): Boolean {
        val line = TcpProtocol.encodeHint(event)
        return runCatching {
            val currentWriter = writer ?: return false
            synchronized(sendLock) {
                currentWriter.write(line)
                currentWriter.newLine()
                currentWriter.flush()
            }
            true
        }.getOrElse {
            closeSocket()
            false
        }
    }

    private fun connectionLoop() {
        while (running.get()) {
            try {
                Socket().use { connectedSocket ->
                    connectedSocket.tcpNoDelay = true
                    connectedSocket.keepAlive = true
                    connectedSocket.connect(InetSocketAddress(host, TcpProtocol.PORT), 4_000)
                    socket = connectedSocket
                    writer = BufferedWriter(OutputStreamWriter(connectedSocket.getOutputStream()))
                    onConnectionChanged(true)

                    BufferedReader(InputStreamReader(connectedSocket.getInputStream())).useLines { lines ->
                        lines.forEach { line ->
                            when (val message = TcpProtocol.decode(line)) {
                                is TcpProtocol.Message.RoomState -> onRoomReceived(message.room)
                                TcpProtocol.Message.Ping -> sendRaw(TcpProtocol.encodePong())
                                else -> Unit
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Retry below.
            } finally {
                writer = null
                socket = null
                onConnectionChanged(false)
            }

            if (running.get()) {
                try { Thread.sleep(2_000L) } catch (_: InterruptedException) { }
            }
        }
    }


    private fun sendRawChecked(line: String): Boolean {
        return runCatching {
            val currentWriter = writer ?: return false
            synchronized(sendLock) {
                currentWriter.write(line)
                currentWriter.newLine()
                currentWriter.flush()
            }
            true
        }.getOrElse {
            closeSocket()
            false
        }
    }

    private fun sendRaw(line: String) {
        runCatching {
            val currentWriter = writer ?: return
            synchronized(sendLock) {
                currentWriter.write(line)
                currentWriter.newLine()
                currentWriter.flush()
            }
        }
    }

    private fun closeSocket() {
        runCatching { socket?.close() }
        socket = null
        writer = null
    }
}
