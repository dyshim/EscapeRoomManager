package com.example.escaperoomtimer.web

import android.os.Handler
import android.os.Looper
import com.example.escaperoomtimer.manager.TimerManager
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
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
 * A lightweight local web server for the manager dashboard.
 * It intentionally uses only the Java/Android standard library so the A7 has
 * very little extra memory or dependency overhead.
 */
object ManagerWebServer {
    const val PORT = 8080

    // Local-network protection for the first release. This can be made
    // configurable in a later settings commit.
    private const val DEFAULT_PIN = "1234"

    private val running = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val acceptExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "manager-web-accept").apply { isDaemon = true }
    }
    private val requestExecutor = Executors.newFixedThreadPool(4) { runnable ->
        Thread(runnable, "manager-web-request").apply { isDaemon = true }
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
    }

    private fun acceptLoop() {
        try {
            ServerSocket(PORT).use { server ->
                server.reuseAddress = true
                serverSocket = server
                while (running.get()) {
                    val socket = server.accept().apply {
                        soTimeout = 5_000
                        tcpNoDelay = true
                    }
                    requestExecutor.execute { handle(socket) }
                }
            }
        } catch (_: SocketException) {
            // Expected when stopping the service.
        } catch (_: Exception) {
        } finally {
            serverSocket = null
            running.set(false)
        }
    }

    private fun handle(socket: Socket) {
        socket.use { client ->
            runCatching {
                val reader = BufferedReader(InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))
                val writer = BufferedWriter(OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))
                val requestLine = reader.readLine() ?: return
                val parts = requestLine.split(' ')
                if (parts.size < 2) return

                val method = parts[0].uppercase(Locale.US)
                val target = parts[1]
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

                when {
                    method == "GET" && (target == "/" || target.startsWith("/index")) -> {
                        respond(writer, 200, "text/html; charset=utf-8", dashboardHtml())
                    }

                    method == "GET" && target.startsWith("/api/state") -> {
                        if (!isAuthorized(headers, target)) {
                            respond(writer, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                        } else {
                            respond(writer, 200, "application/json; charset=utf-8", stateJson())
                        }
                    }

                    method == "POST" && target.startsWith("/api/action") -> {
                        if (!isAuthorized(headers, target)) {
                            respond(writer, 401, "application/json; charset=utf-8", "{\"error\":\"unauthorized\"}")
                        } else {
                            val params = parseForm(body)
                            val accepted = dispatchAction(params)
                            val response = if (accepted) "{\"ok\":true}" else "{\"ok\":false}"
                            respond(writer, if (accepted) 200 else 400, "application/json; charset=utf-8", response)
                        }
                    }

                    else -> respond(writer, 404, "text/plain; charset=utf-8", "Not found")
                }
            }
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

    private fun respond(writer: BufferedWriter, status: Int, contentType: String, body: String) {
        val reason = when (status) {
            200 -> "OK"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            404 -> "Not Found"
            else -> "Error"
        }
        val bytes = body.toByteArray(StandardCharsets.UTF_8)
        writer.write("HTTP/1.1 $status $reason\r\n")
        writer.write("Content-Type: $contentType\r\n")
        writer.write("Content-Length: ${bytes.size}\r\n")
        writer.write("Cache-Control: no-store\r\n")
        writer.write("Connection: close\r\n\r\n")
        writer.flush()
        clientWriteBytes(writer, bytes)
    }

    // BufferedWriter cannot safely write pre-counted UTF-8 bytes directly. The
    // page content only uses valid Unicode and the socket stream is UTF-8, so a
    // String write keeps the Content-Length calculation correct.
    private fun clientWriteBytes(writer: BufferedWriter, bytes: ByteArray) {
        writer.write(String(bytes, StandardCharsets.UTF_8))
        writer.flush()
    }

    private fun dashboardHtml(): String = """
        <!doctype html>
        <html lang="ko">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width,initial-scale=1">
          <title>방탈출 운영</title>
          <style>
            :root{color-scheme:dark;font-family:system-ui,-apple-system,"Noto Sans KR",sans-serif}
            *{box-sizing:border-box} body{margin:0;background:#000;color:#fff}
            header{position:sticky;top:0;background:#0b0f12;border-bottom:1px solid #343b42;padding:16px;z-index:2}
            h1{font-size:24px;margin:0}.sub{color:#c8d0d7;margin-top:6px;font-size:14px}
            main{max-width:980px;margin:auto;padding:16px;display:grid;gap:14px}
            .room{background:#171c20;border:1px solid #30383f;border-radius:14px;padding:16px}
            .top{display:flex;justify-content:space-between;gap:12px;align-items:center}
            .name{font-size:22px;font-weight:800}.badge{font-size:14px;padding:6px 10px;border-radius:999px;background:#29323a}
            .time{font-size:54px;font-weight:900;letter-spacing:1px;margin:12px 0 2px}.end{font-size:17px;color:#e0e6eb;margin-bottom:14px}
            .controls{display:grid;grid-template-columns:repeat(4,1fr);gap:8px}
            button{min-height:48px;border:0;border-radius:10px;background:#263039;color:white;font-size:16px;font-weight:750;cursor:pointer}
            button.start{background:#126b2d}button.pause{background:#a45b00}button.stop{background:#9b211b}button.adjust{background:#3a2e57}
            .setrow{display:flex;gap:8px;margin-top:10px}.setrow input{flex:1;min-width:0;background:#0b0f12;border:1px solid #53606b;color:white;border-radius:10px;padding:12px;font-size:17px}
            .empty{text-align:center;color:#c8d0d7;padding:50px}.error{color:#ff6b6b}
            #login{position:fixed;inset:0;background:#000e;display:flex;align-items:center;justify-content:center;z-index:5}
            .loginbox{width:min(360px,90vw);background:#171c20;padding:22px;border-radius:16px}.loginbox input{width:100%;padding:14px;margin:14px 0;background:#090c0e;border:1px solid #58636b;color:#fff;border-radius:10px;font-size:20px}
            @media(max-width:650px){.controls{grid-template-columns:repeat(2,1fr)}.time{font-size:45px}}
          </style>
        </head>
        <body>
          <div id="login"><div class="loginbox"><h2>직원용 웹 접속</h2><div class="sub">관리자 PIN을 입력하세요.</div><input id="pinInput" type="password" inputmode="numeric" maxlength="8" placeholder="PIN"><button onclick="login()">접속</button><div id="loginError" class="error"></div></div></div>
          <header><h1>방탈출 운영</h1><div class="sub" id="connection">연결 확인 중…</div></header>
          <main id="rooms"><div class="empty">불러오는 중…</div></main>
          <script>
            let pin=localStorage.getItem('managerPin')||'';
            const loginBox=document.getElementById('login');
            if(pin) loginBox.style.display='none';
            function login(){pin=document.getElementById('pinInput').value.trim();localStorage.setItem('managerPin',pin);loginBox.style.display='none';refresh();}
            function esc(v){return String(v).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));}
            function timeText(s){s=Math.max(0,Number(s)||0);return String(Math.floor(s/60)).padStart(2,'0')+':'+String(s%60).padStart(2,'0');}
            function statusText(r){if(r.maintenance)return '유지보수';if(r.seconds<=0)return '종료';if(r.running)return r.seconds<=300?'5분 이하':'진행중';if(r.status==='PAUSED')return '일시정지';return '대기';}
            async function action(roomId,action,seconds){const body=new URLSearchParams({roomId,action});if(seconds!==undefined)body.set('seconds',seconds);const res=await fetch('/api/action',{method:'POST',headers:{'X-Manager-Pin':pin,'Content-Type':'application/x-www-form-urlencoded'},body});if(res.status===401){localStorage.removeItem('managerPin');pin='';loginBox.style.display='flex';document.getElementById('loginError').textContent='PIN을 확인해 주세요.';}setTimeout(refresh,100);}
            function setTime(id){const m=Number(document.getElementById('m_'+id).value)||0;const s=Math.min(59,Number(document.getElementById('s_'+id).value)||0);action(id,'set',Math.max(0,m*60+s));}
            function card(r){
              const id=esc(r.id), running=r.running;
              let html='<section class="room"><div class="top"><div class="name">'+esc(r.name)+'</div><div class="badge">'+statusText(r)+'</div></div><div class="time">'+timeText(r.seconds)+'</div><div class="end">종료 예정 '+esc(r.endLabel)+'</div>';
              if(r.maintenance){html+='<div class="error">유지보수 중인 방입니다.</div>';}
              else{
                html+='<div class="controls">'
                  +'<button class="start" onclick="action(\''+id+'\',\''+(running?'pause':'start')+'\')">'+(running?'일시정지':'시작')+'</button>'
                  +'<button class="stop" onclick="action(\''+id+'\',\'stop\')">종료</button>'
                  +'<button class="adjust" onclick="action(\''+id+'\',\'adjust\',300)">+5분</button>'
                  +'<button class="adjust" onclick="action(\''+id+'\',\'adjust\',-300)">-5분</button>'
                  +'<button onclick="action(\''+id+'\',\'adjust\',10)">+10초</button>'
                  +'<button onclick="action(\''+id+'\',\'adjust\',-10)">-10초</button>'
                  +'<button onclick="action(\''+id+'\',\'reset\')">초기화</button></div>'
                  +'<div class="setrow"><input id="m_'+id+'" inputmode="numeric" placeholder="분"><input id="s_'+id+'" inputmode="numeric" placeholder="초"><button onclick="setTime(\''+id+'\')">시간 적용</button></div>';
              }
              return html+'</section>';
            }
            async function refresh(){if(!pin)return;try{const res=await fetch('/api/state',{headers:{'X-Manager-Pin':pin},cache:'no-store'});if(res.status===401){localStorage.removeItem('managerPin');pin='';loginBox.style.display='flex';return;}const data=await res.json();document.getElementById('connection').textContent='A7 직원용 앱과 연결됨 · 1초마다 자동 갱신';document.getElementById('rooms').innerHTML=data.rooms.length?data.rooms.map(card).join(''):'<div class="empty">사용 중인 방이 없습니다.</div>';}catch(e){document.getElementById('connection').textContent='연결 끊김 · A7과 같은 Wi-Fi인지 확인하세요.';}}
            refresh();setInterval(refresh,1000);
          </script>
        </body></html>
    """.trimIndent()
}
