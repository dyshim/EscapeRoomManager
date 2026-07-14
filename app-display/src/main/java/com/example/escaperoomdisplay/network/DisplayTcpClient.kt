package com.example.escaperoomdisplay.network

import android.util.Log
import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.model.SharedRoomState
import com.example.escaperoomshared.network.TcpProtocol
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class DisplayTcpClient(
    private val onRoomReceived: (SharedRoomState) -> Unit,
    private val onRoomCatalogReceived: (Set<String>) -> Unit,
    private val onConnectionChanged: (Boolean) -> Unit
) {
    private companion object {
        const val TAG = "DisplayConnection"
    }

    private val running = AtomicBoolean(false)
    private val connectionGeneration = AtomicLong(0L)
    private val pendingStartRoomId = AtomicReference<String?>(null)
    private val sendLock = Any()

    @Volatile private var host: String = ""
    @Volatile private var registeredRoomId: String = ""
    @Volatile private var socket: Socket? = null
    @Volatile private var writer: BufferedWriter? = null
    private var worker: Thread? = null

    fun connect(serverHost: String) {
        host = serverHost.trim()
        if (host.isBlank()) {
            Log.w(TAG, "connect ignored: host is blank")
            return
        }
        Log.i(TAG, "requested host=$host port=${TcpProtocol.PORT}")
        if (running.compareAndSet(false, true)) {
            val generation = connectionGeneration.incrementAndGet()
            worker = Thread({ connectionLoop(generation) }, "display-tcp-client").apply {
                isDaemon = true
                start()
            }
        } else {
            Log.i(TAG, "restarting current socket connection")
            closeSocket()
        }
    }

    fun disconnect() {
        Log.i(TAG, "disconnect requested")
        running.set(false)
        connectionGeneration.incrementAndGet()
        pendingStartRoomId.set(null)
        closeSocket()
        worker = null
        onConnectionChanged(false)
    }

    fun registerRoom(roomId: String?) {
        registeredRoomId = roomId.orEmpty()
        if (registeredRoomId.isNotBlank()) {
            sendRawChecked(TcpProtocol.encodeRegisterDisplay(registeredRoomId))
        } else {
            sendRawChecked(TcpProtocol.encodeUnregisterDisplay())
        }
    }

    fun sendStartRequest(roomId: String): Boolean {
        if (roomId.isBlank() || !running.get()) return false
        pendingStartRoomId.set(roomId)
        sendPendingStartIfPossible()
        return true
    }
    fun sendHint(event: HintUsageEvent): Boolean = sendRawChecked(TcpProtocol.encodeHint(event))

    private fun connectionLoop(generation: Long) {
        Log.i(TAG, "connection loop started")
        while (running.get() && connectionGeneration.get() == generation) {
            try {
                Socket().use { connectedSocket ->
                    socket = connectedSocket
                    connectedSocket.tcpNoDelay = true
                    connectedSocket.keepAlive = true
                    val attemptHost = host
                    Log.i(TAG, "socket connect start: host=$attemptHost port=${TcpProtocol.PORT}")
                    connectedSocket.connect(InetSocketAddress(attemptHost, TcpProtocol.PORT), 4_000)
                    Log.i(TAG, "socket connected: remote=${connectedSocket.remoteSocketAddress}")
                    writer = BufferedWriter(OutputStreamWriter(connectedSocket.getOutputStream()))
                    Log.i(TAG, "writer created")
                    onConnectionChanged(true)
                    val initialMessage = if (registeredRoomId.isNotBlank()) {
                        TcpProtocol.encodeRegisterDisplay(registeredRoomId)
                    } else {
                        TcpProtocol.encodeUnregisterDisplay()
                    }
                    if (sendRawChecked(initialMessage)) {
                        Log.i(TAG, "initial message sent: ${if (registeredRoomId.isBlank()) "UNREGISTER" else "REGISTER room=$registeredRoomId"}")
                    }
                    sendPendingStartIfPossible()

                    Log.i(TAG, "reader loop started")
                    BufferedReader(InputStreamReader(connectedSocket.getInputStream())).useLines { lines ->
                        lines.forEach { line ->
                            when (val message = TcpProtocol.decode(line)) {
                                is TcpProtocol.Message.RoomState -> onRoomReceived(message.room)
                                is TcpProtocol.Message.RoomCatalog -> onRoomCatalogReceived(message.activeRoomIds)
                                is TcpProtocol.Message.StartAccepted -> {
                                    pendingStartRoomId.compareAndSet(message.roomId, null)
                                }
                                TcpProtocol.Message.Ping -> sendRaw(TcpProtocol.encodePong())
                                else -> Unit
                            }
                        }
                    }
                    Log.i(TAG, "reader loop stopped: server closed stream")
                }
            } catch (error: Exception) {
                if (running.get() && connectionGeneration.get() == generation) {
                    Log.e(TAG, "socket exception: ${error.javaClass.simpleName}: ${error.message}", error)
                } else {
                    Log.i(TAG, "socket closed during disconnect: ${error.javaClass.simpleName}")
                }
            } finally {
                writer = null
                socket = null
                onConnectionChanged(false)
            }
            if (running.get() && connectionGeneration.get() == generation) {
                try {
                    Thread.sleep(2_000L)
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
        Log.i(TAG, "connection loop stopped")
    }

    private fun sendPendingStartIfPossible() {
        val roomId = pendingStartRoomId.get() ?: return
        if (!sendRawChecked(TcpProtocol.encodeStartRequest(roomId))) {
            closeSocket()
        }
    }

    private fun sendRawChecked(line: String): Boolean = runCatching {
        val currentWriter = writer ?: return false
        synchronized(sendLock) { currentWriter.write(line); currentWriter.newLine(); currentWriter.flush() }
        true
    }.getOrElse { error ->
        Log.e(TAG, "send failed: ${error.javaClass.simpleName}: ${error.message}", error)
        closeSocket()
        false
    }

    private fun sendRaw(line: String) {
        runCatching {
            val currentWriter = writer ?: return
            synchronized(sendLock) { currentWriter.write(line); currentWriter.newLine(); currentWriter.flush() }
        }.onFailure { error ->
            Log.e(TAG, "send failed: ${error.javaClass.simpleName}: ${error.message}", error)
            closeSocket()
        }
    }

    private fun closeSocket() {
        val closingSocket = socket
        runCatching { closingSocket?.close() }
            .onFailure { error -> Log.w(TAG, "socket close failed", error) }
        socket = null
        writer = null
    }
}
