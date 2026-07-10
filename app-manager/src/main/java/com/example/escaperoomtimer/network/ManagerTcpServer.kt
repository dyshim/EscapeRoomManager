package com.example.escaperoomtimer.network

import com.example.escaperoomshared.model.SharedRoomState
import com.example.escaperoomshared.network.TcpProtocol
import com.example.escaperoomtimer.manager.HintProgressManager
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
    private data class Client(
        val socket: Socket,
        val writer: BufferedWriter
    )

    private val running = AtomicBoolean(false)
    private val clients = ConcurrentHashMap.newKeySet<Client>()
    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-tcp-accept").apply { isDaemon = true }
    }
    private val clientExecutor = Executors.newCachedThreadPool { runnable ->
        Thread(runnable, "manager-tcp-client").apply { isDaemon = true }
    }

    @Volatile
    private var serverSocket: ServerSocket? = null

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
    }

    fun broadcastRooms(rooms: List<RoomInfo>) {
        if (!running.get()) return
        val now = System.currentTimeMillis()
        val lines = rooms.map { room ->
            TcpProtocol.encodeRoom(
                SharedRoomState(
                    id = room.id,
                    name = room.name,
                    seconds = room.seconds,
                    status = room.status.name,
                    isRunning = room.isRunning,
                    updatedAtMillis = now
                )
            )
        }
        clients.toList().forEach { client ->
            runCatching {
                synchronized(client.writer) {
                    lines.forEach { line ->
                        client.writer.write(line)
                        client.writer.newLine()
                    }
                    client.writer.flush()
                }
            }.onFailure { removeClient(client) }
        }
    }

    private fun acceptLoop() {
        try {
            ServerSocket(TcpProtocol.PORT).use { server ->
                server.reuseAddress = true
                serverSocket = server
                while (running.get()) {
                    val socket = server.accept().apply {
                        tcpNoDelay = true
                        keepAlive = true
                    }
                    val client = Client(
                        socket = socket,
                        writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream()))
                    )
                    clients += client
                    clientExecutor.execute { readClient(client) }
                }
            }
        } catch (_: SocketException) {
            // Expected when the service stops.
        } catch (_: Exception) {
            // Keep the manager app alive even if the network is temporarily unavailable.
        } finally {
            serverSocket = null
            running.set(false)
        }
    }

    private fun readClient(client: Client) {
        try {
            BufferedReader(InputStreamReader(client.socket.getInputStream())).useLines { lines ->
                lines.forEach { line ->
                    when (val message = TcpProtocol.decode(line)) {
                        is TcpProtocol.Message.HintUsed -> {
                            HintProgressManager.recordHintUsage(message.event)
                        }
                        TcpProtocol.Message.Ping -> sendLine(client, TcpProtocol.encodePong())
                        else -> Unit
                    }
                }
            }
        } catch (_: Exception) {
            // The client will reconnect automatically.
        } finally {
            removeClient(client)
        }
    }

    private fun sendLine(client: Client, line: String) {
        runCatching {
            synchronized(client.writer) {
                client.writer.write(line)
                client.writer.newLine()
                client.writer.flush()
            }
        }.onFailure { removeClient(client) }
    }

    private fun removeClient(client: Client) {
        clients.remove(client)
        runCatching { client.socket.close() }
    }
}
