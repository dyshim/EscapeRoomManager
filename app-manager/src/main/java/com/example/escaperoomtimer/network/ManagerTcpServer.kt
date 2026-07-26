package com.example.escaperoomtimer.network

import android.os.Handler
import android.os.Looper
import android.util.Log
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
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object ManagerTcpServer {
    private const val TAG = "ManagerTcpServer"
    private const val STALE_CLIENT_MILLIS = 15_000L
    private const val CONNECTION_UI_GRACE_MILLIS = 15_000L

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
    private val pendingCountDecreases = ConcurrentHashMap<String, ScheduledFuture<*>>()
    val connectedDisplayCounts: Map<String, Int> get() = _connectedDisplayCounts

    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-tcp-accept").apply { isDaemon = true }
    }
    private val clientExecutor = Executors.newFixedThreadPool(8) { runnable ->
        Thread(runnable, "manager-tcp-client").apply { isDaemon = true }
    }
    private val sendExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-tcp-send").apply { isDaemon = true }
    }
    private val connectionStateExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "manager-connection-state").apply { isDaemon = true }
    }

    @Volatile private var serverSocket: ServerSocket? = null

    val isRunning: Boolean
        get() = running.get() && serverSocket?.isBound == true && serverSocket?.isClosed == false

    @Synchronized
    fun start() {
        if (!running.compareAndSet(false, true)) {
            Log.i(TAG, "server start ignored: already running")
            return
        }
        Log.i(TAG, "server starting: port=${TcpProtocol.PORT}")
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
                Log.w(
                    TAG,
                    "removing stale client: remote=${client.socket.remoteSocketAddress}, " +
                        "lastSeen=${now - client.lastSeenAt}ms ago"
                )
                removeClient(client)
            } else {
                Log.d(TAG, "heartbeat PING: remote=${client.socket.remoteSocketAddress}")
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

    private fun broadcastRoom(roomId: String) {
        if (!running.get()) return
        val room = TimerManager.getRoom(roomId) ?: return
        if (!room.isEnabled || room.isMaintenance || !room.guestScreenEnabled) return

        val line = TcpProtocol.encodeRoom(
            SharedRoomState(
                id = room.id,
                name = room.name,
                seconds = room.seconds,
                status = room.status.name,
                isRunning = room.isRunning,
                updatedAtMillis = System.currentTimeMillis()
            )
        )
        clients.toList().forEach { connectedClient -> sendLine(connectedClient, line) }
    }

    private fun acceptLoop() {
        try {
            ServerSocket(TcpProtocol.PORT).use { server ->
                server.reuseAddress = true
                serverSocket = server
                Log.i(TAG, "server started: port=${TcpProtocol.PORT}")
                while (running.get()) {
                    val socket = server.accept().apply { tcpNoDelay = true; keepAlive = true }
                    Log.i(TAG, "accepted client: remote=${socket.remoteSocketAddress}")
                    val client = Client(socket, BufferedWriter(OutputStreamWriter(socket.getOutputStream())))
                    clients += client
                    publishConnectionCounts()
                    clientExecutor.execute { readClient(client) }
                    sendCurrentRooms(client)
                }
            }
        } catch (error: SocketException) {
            if (running.get()) Log.e(TAG, "server socket exception: ${error.message}", error)
            else Log.i(TAG, "server socket closed")
        } catch (error: Exception) {
            Log.e(TAG, "server exception: ${error.javaClass.simpleName}: ${error.message}", error)
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
                            Log.i(TAG, "start request received: room=${message.roomId}, remote=${client.socket.remoteSocketAddress}")
                            mainHandler.post {
                                TimerManager.start(message.roomId)
                                sendLine(client, TcpProtocol.encodeStartAccepted(message.roomId))
                                broadcastRoom(message.roomId)
                            }
                        }
                        is TcpProtocol.Message.RegisterDisplay -> {
                            Log.i(TAG, "room selection received: room=${message.roomId}, remote=${client.socket.remoteSocketAddress}")
                            client.roomId = message.roomId
                            publishConnectionCounts()
                        }
                        TcpProtocol.Message.UnregisterDisplay -> {
                            Log.i(TAG, "room selection cleared: remote=${client.socket.remoteSocketAddress}")
                            client.roomId = null
                            publishConnectionCounts()
                        }
                        TcpProtocol.Message.Ping -> sendLine(client, TcpProtocol.encodePong())
                        TcpProtocol.Message.Pong -> {
                            Log.d(TAG, "heartbeat PONG: remote=${client.socket.remoteSocketAddress}")
                        }
                        else -> Unit
                    }
                }
            }
        } catch (error: Exception) {
            if (!client.socket.isClosed) {
                Log.e(TAG, "client exception: remote=${client.socket.remoteSocketAddress}, ${error.javaClass.simpleName}: ${error.message}", error)
            }
        } finally {
            removeClient(client)
        }
    }

    private fun sendLines(client: Client, lines: List<String>) {
        val pendingLines = lines.toList()
        sendExecutor.execute {
            if (client !in clients || client.socket.isClosed) return@execute
            runCatching {
                synchronized(client.writer) {
                    pendingLines.forEach { line ->
                        client.writer.write(line)
                        client.writer.newLine()
                    }
                    client.writer.flush()
                }
            }.onFailure { error ->
                Log.e(TAG, "send failed: remote=${client.socket.remoteSocketAddress}, ${error.javaClass.simpleName}: ${error.message}", error)
                removeClient(client)
            }
        }
    }

    private fun sendLine(client: Client, line: String) = sendLines(client, listOf(line))

    private fun removeClient(client: Client) {
        val removed = clients.remove(client)
        runCatching { client.socket.close() }
        if (removed) Log.i(TAG, "client removed: remote=${client.socket.remoteSocketAddress}, room=${client.roomId}")
        publishConnectionCounts()
    }

    private fun publishConnectionCounts() {
        mainHandler.post { publishConnectionCountsOnMain() }
    }

    private fun publishConnectionCountsOnMain() {
        val actualCounts = clients.mapNotNull { it.roomId }.groupingBy { it }.eachCount()
        val roomIds = _connectedDisplayCounts.keys + actualCounts.keys

        roomIds.forEach { roomId ->
            val displayedCount = _connectedDisplayCounts[roomId] ?: 0
            val actualCount = actualCounts[roomId] ?: 0

            if (actualCount >= displayedCount) {
                pendingCountDecreases.remove(roomId)?.cancel(false)
                if (actualCount > 0 && displayedCount != actualCount) {
                    _connectedDisplayCounts[roomId] = actualCount
                }
                return@forEach
            }

            if (pendingCountDecreases.containsKey(roomId)) return@forEach

            val pending = connectionStateExecutor.schedule({
                pendingCountDecreases.remove(roomId)
                val confirmedCount = clients.count { it.roomId == roomId }
                mainHandler.post {
                    val currentDisplayedCount = _connectedDisplayCounts[roomId] ?: 0
                    if (confirmedCount >= currentDisplayedCount) return@post

                    if (confirmedCount > 0) {
                        _connectedDisplayCounts[roomId] = confirmedCount
                    } else {
                        _connectedDisplayCounts.remove(roomId)
                    }
                }
            }, CONNECTION_UI_GRACE_MILLIS, TimeUnit.MILLISECONDS)

            pendingCountDecreases[roomId] = pending
        }
    }
}
