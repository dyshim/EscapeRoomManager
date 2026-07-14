package com.example.escaperoomtimer.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Base64
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import com.example.escaperoomtimer.settings.WebAdminPinPreferences
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Lightweight local HTTP + WebSocket server for the manager dashboard.
 *
 * The HTTP API remains available as a fallback. Connected web browsers receive
 * live room-state updates over WebSocket, reducing repeated polling on the A7.
 */
object ManagerWebServer {
    const val PORT = 8080
    private const val RETRY_DELAY_MS = 2_000L
    private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"
    private const val CONNECTION_COUNT_GRACE_MS = 15_000L

    private val desiredRunning = AtomicBoolean(false)
    private val accepting = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-web-accept").apply { isDaemon = true }
    }
    private val requestExecutor = Executors.newFixedThreadPool(6) { runnable ->
        Thread(runnable, "manager-web-request").apply { isDaemon = true }
    }
    private val webSocketClients = ConcurrentHashMap.newKeySet<WebSocketClient>()
    private val displayedWebSocketCount = AtomicInteger(0)
    private val connectionStateExecutor = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "manager-web-connection-state").apply { isDaemon = true }
    }
    private val connectionCountGeneration = AtomicInteger(0)
    private val loginAttempts = ConcurrentHashMap<String, LoginAttempt>()
    private const val MAX_LOGIN_FAILURES = 5
    private const val LOGIN_LOCK_MS = 30_000L

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var lastErrorMessage: String? = null
    @Volatile private var lastStartedAt: Long = 0L

    val isRunning: Boolean
        get() = desiredRunning.get() && serverSocket?.isBound == true && serverSocket?.isClosed == false

    val connectedWebCount: Int
        get() = displayedWebSocketCount.get()

    fun statusText(): String = when {
        isRunning -> "웹 서버 실행 중 · 포트 $PORT · 웹 ${displayedWebSocketCount.get()}대"
        desiredRunning.get() && !lastErrorMessage.isNullOrBlank() -> "웹 서버 재시도 중 · ${lastErrorMessage}"
        desiredRunning.get() -> "웹 서버 시작 중…"
        else -> "웹 서버 중지됨"
    }

    @Synchronized
    fun start(context: Context) {
        appContext = context.applicationContext
        WebAdminPinPreferences.ensureInitialized(context)
        desiredRunning.set(true)
        if (isRunning || !accepting.compareAndSet(false, true)) return
        acceptExecutor.execute(::acceptLoop)
    }

    @Synchronized
    fun stop() {
        desiredRunning.set(false)
        webSocketClients.toList().forEach { it.close() }
        webSocketClients.clear()
        connectionCountGeneration.incrementAndGet()
        displayedWebSocketCount.set(0)
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        if (!isRunning) start(context)
    }

    /** Pushes the latest room state to all connected browser dashboards. */
    fun broadcastState() {
        if (webSocketClients.isEmpty()) return
        val payload = stateJson()
        webSocketClients.toList().forEach { client ->
            if (!client.sendText(payload)) {
                webSocketClients.remove(client)
                publishWebSocketCount()
                client.close()
            }
        }
    }

    private fun acceptLoop() {
        try {
            while (desiredRunning.get()) {
                try {
                    val server = ServerSocket()
                    server.reuseAddress = true
                    server.bind(InetSocketAddress("0.0.0.0", PORT), 32)
                    serverSocket = server
                    lastErrorMessage = null
                    lastStartedAt = System.currentTimeMillis()

                    while (desiredRunning.get() && !server.isClosed) {
                        val socket = server.accept().apply {
                            soTimeout = 8_000
                            tcpNoDelay = true
                            keepAlive = true
                        }
                        requestExecutor.execute { handle(socket) }
                    }
                } catch (_: SocketException) {
                    if (desiredRunning.get()) lastErrorMessage = "포트 $PORT 연결 재시도"
                } catch (error: Exception) {
                    lastErrorMessage = error.message ?: error.javaClass.simpleName
                } finally {
                    runCatching { serverSocket?.close() }
                    serverSocket = null
                }

                if (desiredRunning.get()) Thread.sleep(RETRY_DELAY_MS)
            }
        } finally {
            accepting.set(false)
            if (desiredRunning.get()) appContext?.let(::start)
        }
    }

    private fun handle(socket: Socket) {
        var upgradedToWebSocket = false
        try {
            val input = BufferedInputStream(socket.getInputStream())
            val reader = BufferedReader(InputStreamReader(input, StandardCharsets.UTF_8))
            val requestLine = reader.readLine() ?: return
            val parts = requestLine.split(' ')
            if (parts.size < 2) return

            val method = parts[0].uppercase(Locale.US)
            val target = parts[1]
            val path = target.substringBefore('?')
            val headers = linkedMapOf<String, String>()
            while (true) {
                val line = reader.readLine() ?: break
                if (line.isEmpty()) break
                val separator = line.indexOf(':')
                if (separator > 0) {
                    headers[line.substring(0, separator).trim().lowercase(Locale.US)] =
                        line.substring(separator + 1).trim()
                }
            }

            if (method == "GET" && path == "/ws" &&
                headers["upgrade"]?.equals("websocket", ignoreCase = true) == true
            ) {
                if (!isAuthorized(headers, target)) {
                    respond(socket, HttpResponse(401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}"))
                    return
                }
                upgradedToWebSocket = upgradeToWebSocket(socket, input, headers)
                return
            }

            val contentLength = headers["content-length"]?.toIntOrNull()?.coerceIn(0, 16_384) ?: 0
            val body = if (contentLength > 0) {
                val chars = CharArray(contentLength)
                var offset = 0
                while (offset < contentLength) {
                    val read = reader.read(chars, offset, contentLength - offset)
                    if (read <= 0) break
                    offset += read
                }
                String(chars, 0, offset)
            } else ""

            val response = when {
                method == "GET" && (path == "/" || path == "/index.html") ->
                    assetResponse("web/index.html", "text/html; charset=utf-8")

                method == "GET" && path == "/style.css" ->
                    assetResponse("web/style.css", "text/css; charset=utf-8")

                method == "GET" && path == "/app.js" ->
                    assetResponse("web/app.js", "application/javascript; charset=utf-8")

                method == "GET" && path == "/manifest.webmanifest" ->
                    assetResponse("web/manifest.webmanifest", "application/manifest+json; charset=utf-8")

                method == "GET" && path == "/sw.js" ->
                    assetResponse("web/sw.js", "application/javascript; charset=utf-8")

                method == "GET" && path == "/icons/icon-192.png" ->
                    binaryAssetResponse("web/icons/icon-192.png", "image/png")

                method == "GET" && path == "/icons/icon-512.png" ->
                    binaryAssetResponse("web/icons/icon-512.png", "image/png")

                method == "GET" && path == "/favicon.ico" ->
                    binaryAssetResponse("web/icons/icon-192.png", "image/png")

                method == "GET" && path == "/health" ->
                    HttpResponse(
                        200,
                        "application/json; charset=utf-8",
                        "{\"ok\":true,\"port\":$PORT,\"startedAt\":$lastStartedAt,\"webSockets\":${webSocketClients.size}}"
                    )

                method == "POST" && path == "/api/login" -> {
                    val clientKey = socket.inetAddress?.hostAddress ?: "unknown"
                    val now = System.currentTimeMillis()
                    val attempt = loginAttempts[clientKey]
                    if (attempt != null && attempt.lockedUntil > now) {
                        val retryAfter = ((attempt.lockedUntil - now + 999L) / 1000L).coerceAtLeast(1L)
                        HttpResponse(
                            429,
                            "application/json; charset=utf-8",
                            "{\"ok\":false,\"error\":\"locked\",\"retryAfter\":$retryAfter}"
                        )
                    } else {
                        val submittedPin = parseForm(body)["pin"].orEmpty()
                        val context = appContext
                        if (context != null && WebAdminPinPreferences.verify(context, submittedPin)) {
                            loginAttempts.remove(clientKey)
                            HttpResponse(200, "application/json; charset=utf-8", "{\"ok\":true}")
                        } else {
                            val failures = (attempt?.failures ?: 0) + 1
                            val lockedUntil = if (failures >= MAX_LOGIN_FAILURES) now + LOGIN_LOCK_MS else 0L
                            loginAttempts[clientKey] = LoginAttempt(
                                failures = if (lockedUntil > 0L) 0 else failures,
                                lockedUntil = lockedUntil
                            )
                            HttpResponse(
                                401,
                                "application/json; charset=utf-8",
                                "{\"ok\":false,\"error\":\"invalid_pin\",\"remaining\":${(MAX_LOGIN_FAILURES - failures).coerceAtLeast(0)}}"
                            )
                        }
                    }
                }

                method == "GET" && path == "/api/state" -> {
                    if (!isAuthorized(headers, target)) {
                        HttpResponse(401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                    } else {
                        HttpResponse(200, "application/json; charset=utf-8", stateJson())
                    }
                }

                method == "POST" && path == "/api/action" -> {
                    if (!isAuthorized(headers, target)) {
                        HttpResponse(401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                    } else {
                        val accepted = dispatchAction(parseForm(body))
                        HttpResponse(
                            if (accepted) 200 else 400,
                            "application/json; charset=utf-8",
                            if (accepted) "{\"ok\":true}" else "{\"ok\":false}"
                        )
                    }
                }

                else -> HttpResponse(404, "text/plain; charset=utf-8", "Not found")
            }
            respond(socket, response)
        } catch (error: Exception) {
            lastErrorMessage = error.message ?: error.javaClass.simpleName
        } finally {
            if (!upgradedToWebSocket) runCatching { socket.close() }
        }
    }

    private fun upgradeToWebSocket(
        socket: Socket,
        input: BufferedInputStream,
        headers: Map<String, String>
    ): Boolean {
        val key = headers["sec-websocket-key"] ?: return false
        val accept = Base64.encodeToString(
            MessageDigest.getInstance("SHA-1")
                .digest((key + WEBSOCKET_GUID).toByteArray(StandardCharsets.ISO_8859_1)),
            Base64.NO_WRAP
        )
        val output = socket.getOutputStream()
        val handshake = buildString {
            append("HTTP/1.1 101 Switching Protocols\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Accept: $accept\r\n\r\n")
        }
        output.write(handshake.toByteArray(StandardCharsets.US_ASCII))
        output.flush()
        socket.soTimeout = 0

        val client = WebSocketClient(socket, output)
        webSocketClients.add(client)
        publishWebSocketCount()
        client.sendText(stateJson())
        try {
            while (desiredRunning.get() && !socket.isClosed) {
                when (val frame = readFrame(input) ?: break) {
                    is WebSocketFrame.Close -> break
                    is WebSocketFrame.Ping -> client.sendPong(frame.payload)
                    is WebSocketFrame.Text -> {
                        if (frame.text == "state") client.sendText(stateJson())
                    }
                    WebSocketFrame.Other -> Unit
                }
            }
        } finally {
            webSocketClients.remove(client)
            publishWebSocketCount()
            client.close()
        }
        return true
    }


    private fun publishWebSocketCount() {
        val actualCount = webSocketClients.size
        val displayedCount = displayedWebSocketCount.get()

        if (actualCount >= displayedCount) {
            connectionCountGeneration.incrementAndGet()
            displayedWebSocketCount.set(actualCount)
            return
        }

        val generation = connectionCountGeneration.incrementAndGet()
        connectionStateExecutor.schedule({
            if (connectionCountGeneration.get() != generation) return@schedule
            displayedWebSocketCount.set(webSocketClients.size)
        }, CONNECTION_COUNT_GRACE_MS, TimeUnit.MILLISECONDS)
    }

    private sealed interface WebSocketFrame {
        data class Text(val text: String) : WebSocketFrame
        data class Ping(val payload: ByteArray) : WebSocketFrame
        data object Close : WebSocketFrame
        data object Other : WebSocketFrame
    }

    private fun readFrame(input: BufferedInputStream): WebSocketFrame? {
        val first = input.read()
        if (first < 0) return null
        val second = input.read()
        if (second < 0) return null
        val opcode = first and 0x0F
        val masked = second and 0x80 != 0
        var length = (second and 0x7F).toLong()
        if (length == 126L) {
            val a = input.read(); val b = input.read()
            if (a < 0 || b < 0) return null
            length = ((a shl 8) or b).toLong()
        } else if (length == 127L) {
            length = 0L
            repeat(8) {
                val value = input.read()
                if (value < 0) return null
                length = (length shl 8) or value.toLong()
            }
        }
        if (length > 65_536L) return WebSocketFrame.Close
        val mask = if (masked) ByteArray(4).also { readFully(input, it) } else null
        val payload = ByteArray(length.toInt())
        readFully(input, payload)
        if (mask != null) {
            payload.indices.forEach { index ->
                payload[index] = (payload[index].toInt() xor mask[index % 4].toInt()).toByte()
            }
        }
        return when (opcode) {
            0x1 -> WebSocketFrame.Text(String(payload, StandardCharsets.UTF_8))
            0x8 -> WebSocketFrame.Close
            0x9 -> WebSocketFrame.Ping(payload)
            else -> WebSocketFrame.Other
        }
    }

    private fun readFully(input: BufferedInputStream, buffer: ByteArray) {
        var offset = 0
        while (offset < buffer.size) {
            val count = input.read(buffer, offset, buffer.size - offset)
            if (count < 0) throw SocketException("WebSocket connection closed")
            offset += count
        }
    }

    private class WebSocketClient(
        private val socket: Socket,
        private val output: OutputStream
    ) {
        @Synchronized
        fun sendText(text: String): Boolean = runCatching {
            sendFrame(0x1, text.toByteArray(StandardCharsets.UTF_8))
            true
        }.getOrDefault(false)

        @Synchronized
        fun sendPong(payload: ByteArray): Boolean = runCatching {
            sendFrame(0xA, payload)
            true
        }.getOrDefault(false)

        @Synchronized
        private fun sendFrame(opcode: Int, payload: ByteArray) {
            output.write(0x80 or opcode)
            when {
                payload.size <= 125 -> output.write(payload.size)
                payload.size <= 65_535 -> {
                    output.write(126)
                    output.write((payload.size shr 8) and 0xFF)
                    output.write(payload.size and 0xFF)
                }
                else -> {
                    output.write(127)
                    repeat(4) { output.write(0) }
                    output.write((payload.size shr 24) and 0xFF)
                    output.write((payload.size shr 16) and 0xFF)
                    output.write((payload.size shr 8) and 0xFF)
                    output.write(payload.size and 0xFF)
                }
            }
            output.write(payload)
            output.flush()
        }

        fun close() = runCatching { socket.close() }.getOrNull()
    }

    private data class LoginAttempt(
        val failures: Int,
        val lockedUntil: Long
    )

    private data class HttpResponse(
        val status: Int,
        val contentType: String,
        val bodyBytes: ByteArray
    ) {
        constructor(status: Int, contentType: String, body: String) : this(
            status,
            contentType,
            body.toByteArray(StandardCharsets.UTF_8)
        )
    }

    private fun respond(socket: Socket, response: HttpResponse) {
        val reason = when (response.status) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            503 -> "Service Unavailable"
            else -> "Error"
        }
        val header = buildString {
            append("HTTP/1.1 ${response.status} $reason\r\n")
            append("Content-Type: ${response.contentType}\r\n")
            append("Content-Length: ${response.bodyBytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        socket.getOutputStream().buffered().use { output ->
            output.write(header)
            output.write(response.bodyBytes)
            output.flush()
        }
    }

    private fun isAuthorized(headers: Map<String, String>, target: String): Boolean {
        val headerPin = headers["x-manager-pin"]
        val queryPin = target.substringAfter('?', "")
            .split('&')
            .mapNotNull {
                val index = it.indexOf('=')
                if (index <= 0) null else decode(it.substring(0, index)) to decode(it.substring(index + 1))
            }
            .firstOrNull { it.first == "pin" }
            ?.second
        val context = appContext ?: return false
        return WebAdminPinPreferences.verify(context, headerPin.orEmpty()) ||
            WebAdminPinPreferences.verify(context, queryPin.orEmpty())
    }

    private fun dispatchAction(params: Map<String, String>): Boolean {
        val roomId = params["roomId"] ?: return false
        val action = params["action"] ?: return false
        val room = snapshotRooms().firstOrNull { it.id == roomId } ?: return false
        if (!room.isEnabled || room.isMaintenance) return false
        if (action !in setOf("start", "pause", "resume", "stop", "reset", "adjust", "set")) return false

        mainHandler.post {
            when (action) {
                "start" -> TimerManager.start(roomId)
                "pause" -> if (TimerManager.getRoom(roomId)?.isRunning == true) TimerManager.startOrPause(roomId)
                "resume" -> if (TimerManager.getRoom(roomId)?.isRunning == false) TimerManager.startOrPause(roomId)
                "stop" -> TimerManager.stop(roomId)
                "reset" -> TimerManager.reset(roomId)
                "adjust" -> TimerManager.adjustSeconds(roomId, params["seconds"]?.toIntOrNull() ?: 0)
                "set" -> TimerManager.setTime(roomId, params["seconds"]?.toIntOrNull() ?: return@post)
            }
            broadcastState()
        }
        return true
    }

    private fun snapshotRooms(): List<RoomInfo> {
        if (Looper.myLooper() == Looper.getMainLooper()) return TimerManager.rooms.toList()
        val latch = CountDownLatch(1)
        var snapshot: List<RoomInfo> = emptyList()
        mainHandler.post {
            snapshot = TimerManager.rooms.toList()
            latch.countDown()
        }
        latch.await(2, TimeUnit.SECONDS)
        return snapshot
    }

    private fun stateJson(): String {
        val now = System.currentTimeMillis()
        val formatter = SimpleDateFormat("a h:mm", Locale.KOREA)
        val rooms = snapshotRooms().filter { it.isEnabled }
        return buildString {
            append("{\"serverTime\":").append(now)
            append(",\"transport\":\"websocket\"")
            append(",\"rooms\":[")
            rooms.forEachIndexed { index, room ->
                if (index > 0) append(',')
                val endLabel = when {
                    room.isMaintenance -> "유지보수"
                    room.status == RoomStatus.FINISHED || room.seconds <= 0 -> "종료됨"
                    room.isRunning -> formatter.format(Date(now + room.seconds * 1000L))
                    room.status == RoomStatus.PAUSED -> "일시정지 중"
                    else -> "시작 후 표시"
                }
                append('{')
                append("\"id\":\"").append(jsonEscape(room.id)).append("\",")
                append("\"name\":\"").append(jsonEscape(room.name)).append("\",")
                append("\"seconds\":").append(room.seconds.coerceAtLeast(0)).append(',')
                append("\"running\":").append(room.isRunning).append(',')
                append("\"status\":\"").append(room.status.name).append("\",")
                append("\"maintenance\":").append(room.isMaintenance).append(',')
                append("\"endLabel\":\"").append(jsonEscape(endLabel)).append("\"")
                append('}')
            }
            append("]}")
        }
    }

    private fun parseForm(body: String): Map<String, String> = body
        .split('&')
        .mapNotNull {
            val index = it.indexOf('=')
            if (index <= 0) null else decode(it.substring(0, index)) to decode(it.substring(index + 1))
        }
        .toMap()

    private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")

    private fun jsonEscape(value: String): String = buildString(value.length + 8) {
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }

    private fun assetResponse(assetPath: String, contentType: String): HttpResponse {
        val context = appContext
            ?: return HttpResponse(503, "text/plain; charset=utf-8", "Web server context unavailable")
        return runCatching {
            val body = context.assets.open(assetPath).bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
            HttpResponse(200, contentType, body)
        }.getOrElse { error ->
            lastErrorMessage = "Asset load failed: $assetPath (${error.message ?: error.javaClass.simpleName})"
            HttpResponse(500, "text/plain; charset=utf-8", "Asset load failed")
        }
    }

    private fun binaryAssetResponse(assetPath: String, contentType: String): HttpResponse {
        val context = appContext
            ?: return HttpResponse(503, "text/plain; charset=utf-8", "Web server context unavailable")
        return runCatching {
            HttpResponse(200, contentType, context.assets.open(assetPath).use { it.readBytes() })
        }.getOrElse { error ->
            lastErrorMessage = "Asset load failed: $assetPath (${error.message ?: error.javaClass.simpleName})"
            HttpResponse(500, "text/plain; charset=utf-8", "Asset load failed")
        }
    }
}
