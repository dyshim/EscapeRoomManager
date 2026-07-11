package com.example.escaperoomtimer.network

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import com.example.escaperoomshared.model.SharedRoomState
import com.example.escaperoomshared.network.TcpProtocol
import com.example.escaperoomtimer.manager.HintProgressManager
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object ManagerTcpServer {
    private const val STALE_CLIENT_MILLIS = 15_000L

    private class Client(
        val socket: Socket,
        val writer: BufferedWriter,
        @Volatile var roomId: String? = null,
        @Volatile var lastSeenAt: Long = System.currentTimeMillis()
    )

    private val running = AtomicBoolean(false)
    private val clients = ConcurrentHashMap.newKeySet<Client>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val _connectedDisplayCounts = mutableStateMapOf<String, Int>()
    val connectedDisplayCounts: Map<String, Int> get() = _connectedDisplayCounts

    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-tcp-accept").apply { isDaemon = true }
    }
    private val clientExecutor = Executors.newFixedThreadPool(8) { runnable ->
        Thread(runnable, "manager-tcp-client").apply { isDaemon = true }
    }

    @Volatile private var serverSocket: ServerSocket? = null

    @Synchronized
    fun start() {
        if (!running.compareAndSet(false, true)) return
        acceptExecutor.execute(::acceptLoop)
    }

    @Synchronized
    fun stop() {
        running.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
        clients.toList().forEach(::removeClient)
        publishConnectionCounts()
    }

    fun connectedCount(roomId: String): Int = _connectedDisplayCounts[roomId] ?: 0

    fun heartbeat() {
        if (!running.get()) return
        val now = System.currentTimeMillis()
        clients.toList().forEach { client ->
            if (now - client.lastSeenAt > STALE_CLIENT_MILLIS) {
                removeClient(client)
            } else {
                sendLine(client, TcpProtocol.encodePing())
            }
        }
    }

    fun broadcastRooms(rooms: List<RoomInfo>) {
        if (!running.get()) return
        val guestRooms = rooms.filter { it.isEnabled && !it.isMaintenance && it.guestScreenEnabled }
        val now = System.currentTimeMillis()
        val lines = buildList {
            add(TcpProtocol.encodeRoomCatalog(guestRooms.map { it.id }))
            guestRooms.forEach { room ->
                add(TcpProtocol.encodeRoom(SharedRoomState(room.id, room.name, room.seconds, room.status.name, room.isRunning, now)))
            }
        }
        clients.toList().forEach { client -> sendLines(client, lines) }
    }

    private fun acceptLoop() {
        try {
            ServerSocket(TcpProtocol.PORT).use { server ->
                server.reuseAddress = true
                serverSocket = server
                while (running.get()) {
                    val socket = server.accept().apply { tcpNoDelay = true; keepAlive = true }
                    val client = Client(socket, BufferedWriter(OutputStreamWriter(socket.getOutputStream())))
                    clients += client
                    publishConnectionCounts()
                    clientExecutor.execute { readClient(client) }
                    sendCurrentRooms(client)
                }
            }
        } catch (_: SocketException) {
        } catch (_: Exception) {
        } finally {
            serverSocket = null
            running.set(false)
        }
    }

    private fun sendCurrentRooms(client: Client) {
        val active = TimerManager.rooms.filter { it.isEnabled && !it.isMaintenance && it.guestScreenEnabled }
        val now = System.currentTimeMillis()
        val lines = buildList {
            add(TcpProtocol.encodeRoomCatalog(active.map { it.id }))
            active.forEach { room ->
                add(TcpProtocol.encodeRoom(SharedRoomState(room.id, room.name, room.seconds, room.status.name, room.isRunning, now)))
            }
        }
        sendLines(client, lines)
    }

    private fun readClient(client: Client) {
        try {
            BufferedReader(InputStreamReader(client.socket.getInputStream())).useLines { lines ->
                lines.forEach { line ->
                    client.lastSeenAt = System.currentTimeMillis()
                    when (val message = TcpProtocol.decode(line)) {
                        is TcpProtocol.Message.HintUsed -> HintProgressManager.recordHintUsage(message.event)
                        is TcpProtocol.Message.StartRequest -> {
                            TimerManager.start(message.roomId)
                            broadcastRooms(TimerManager.rooms)
                        }
                        is TcpProtocol.Message.RegisterDisplay -> {
                            client.roomId = message.roomId
                            publishConnectionCounts()
                        }
                        TcpProtocol.Message.UnregisterDisplay -> {
                            client.roomId = null
                            publishConnectionCounts()
                        }
                        TcpProtocol.Message.Ping -> sendLine(client, TcpProtocol.encodePong())
                        TcpProtocol.Message.Pong -> Unit
                        else -> Unit
                    }
                }
            }
        } catch (_: Exception) {
        } finally {
            removeClient(client)
        }
    }

    private fun sendLines(client: Client, lines: List<String>) {
        runCatching {
            synchronized(client.writer) {
                lines.forEach { line -> client.writer.write(line); client.writer.newLine() }
                client.writer.flush()
            }
        }.onFailure { removeClient(client) }
    }

    private fun sendLine(client: Client, line: String) = sendLines(client, listOf(line))

    private fun removeClient(client: Client) {
        clients.remove(client)
        runCatching { client.socket.close() }
        publishConnectionCounts()
    }

    private fun publishConnectionCounts() {
        val counts = clients.mapNotNull { it.roomId }.groupingBy { it }.eachCount()
        mainHandler.post {
            _connectedDisplayCounts.clear()
            _connectedDisplayCounts.putAll(counts)
        }
    }
}
