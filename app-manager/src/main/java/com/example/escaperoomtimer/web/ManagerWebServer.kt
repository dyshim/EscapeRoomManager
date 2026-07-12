package com.example.escaperoomtimer.web

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Lightweight local HTTP server used by the manager dashboard.
 *
 * The server binds to 0.0.0.0 so another device on the same Wi-Fi can connect.
 * It retries automatically after temporary bind/network errors and exposes a
 * small status API for the Android UI.
 */
object ManagerWebServer {
    const val PORT = 8080
    private const val DEFAULT_PIN = "1234"
    private const val RETRY_DELAY_MS = 2_000L

    private val desiredRunning = AtomicBoolean(false)
    private val accepting = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-web-accept").apply { isDaemon = true }
    }
    private val requestExecutor = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable, "manager-web-request").apply { isDaemon = true }
    }

    @Volatile private var serverSocket: ServerSocket? = null
    @Volatile private var appContext: Context? = null
    @Volatile private var lastErrorMessage: String? = null
    @Volatile private var lastStartedAt: Long = 0L

    val isRunning: Boolean
        get() = desiredRunning.get() && serverSocket?.isBound == true && serverSocket?.isClosed == false

    fun statusText(): String = when {
        isRunning -> "웹 서버 실행 중 · 포트 $PORT"
        desiredRunning.get() && !lastErrorMessage.isNullOrBlank() -> "웹 서버 재시도 중 · ${lastErrorMessage}"
        desiredRunning.get() -> "웹 서버 시작 중…"
        else -> "웹 서버 중지됨"
    }

    @Synchronized
    fun start(context: Context) {
        appContext = context.applicationContext
        desiredRunning.set(true)
        if (isRunning || !accepting.compareAndSet(false, true)) return
        acceptExecutor.execute(::acceptLoop)
    }

    @Synchronized
    fun stop() {
        desiredRunning.set(false)
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    fun ensureStarted(context: Context) {
        appContext = context.applicationContext
        if (!isRunning) start(context)
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
        socket.use { client ->
            runCatching {
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
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

                    method == "GET" && path == "/favicon.ico" ->
                        HttpResponse(204, "image/x-icon", "")

                    method == "GET" && path == "/health" ->
                        HttpResponse(200, "application/json; charset=utf-8", "{\"ok\":true,\"port\":$PORT,\"startedAt\":$lastStartedAt}")

                    method == "POST" && path == "/api/login" -> {
                        val submittedPin = parseForm(body)["pin"].orEmpty()
                        if (submittedPin == DEFAULT_PIN) {
                            HttpResponse(200, "application/json; charset=utf-8", "{\"ok\":true}")
                        } else {
                            HttpResponse(401, "application/json; charset=utf-8", "{\"ok\":false,\"error\":\"invalid_pin\"}")
                        }
                    }

                    method == "GET" && path == "/api/state" -> {
                        if (!isAuthorized(headers, target)) HttpResponse(401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                        else HttpResponse(200, "application/json; charset=utf-8", stateJson())
                    }

                    method == "POST" && path == "/api/action" -> {
                        if (!isAuthorized(headers, target)) {
                            HttpResponse(401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                        } else {
                            val accepted = dispatchAction(parseForm(body))
                            HttpResponse(if (accepted) 200 else 400, "application/json; charset=utf-8", if (accepted) "{\"ok\":true}" else "{\"ok\":false}")
                        }
                    }

                    else -> HttpResponse(404, "text/plain; charset=utf-8", "Not found")
                }
                respond(client, response)
            }.onFailure { error ->
                lastErrorMessage = error.message ?: error.javaClass.simpleName
            }
        }
    }

    private data class HttpResponse(val status: Int, val contentType: String, val body: String)

    private fun respond(socket: Socket, response: HttpResponse) {
        val reason = when (response.status) {
            200 -> "OK"
            204 -> "No Content"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val bodyBytes = response.body.toByteArray(StandardCharsets.UTF_8)
        val header = buildString {
            append("HTTP/1.1 ${response.status} $reason\r\n")
            append("Content-Type: ${response.contentType}\r\n")
            append("Content-Length: ${bodyBytes.size}\r\n")
            append("Cache-Control: no-store\r\n")
            append("Access-Control-Allow-Origin: *\r\n")
            append("Connection: close\r\n\r\n")
        }.toByteArray(StandardCharsets.US_ASCII)
        socket.getOutputStream().buffered().use { output ->
            output.write(header)
            output.write(bodyBytes)
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
        return headerPin == DEFAULT_PIN || queryPin == DEFAULT_PIN
    }

    private fun dispatchAction(params: Map<String, String>): Boolean {
        val roomId = params["roomId"] ?: return false
        val action = params["action"] ?: return false
        val room = snapshotRooms().firstOrNull { it.id == roomId } ?: return false
        if (!room.isEnabled || room.isMaintenance) return false

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
        }
        return action in setOf("start", "pause", "resume", "stop", "reset", "adjust", "set")
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
            append("{\"serverTime\":").append(now).append(",\"rooms\":[")
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
}
