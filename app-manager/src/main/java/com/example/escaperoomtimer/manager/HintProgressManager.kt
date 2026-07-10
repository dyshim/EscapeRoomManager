package com.example.escaperoomtimer.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateMapOf
import com.example.escaperoomshared.model.HintUsageEvent
import com.example.escaperoomshared.network.HintProtocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.SocketException
import java.util.concurrent.atomic.AtomicBoolean

object HintProgressManager {
    private const val PREFS_NAME = "hint_progress_preferences"
    private const val KEY_PREFIX = "room_"

    data class HintProgress(
        val lastHintNumber: Int = 0,
        val useCount: Int = 0,
        val lastUsedAtMillis: Long = 0L
    )

    val progressByRoom = mutableStateMapOf<String, HintProgress>()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listening = AtomicBoolean(false)
    private var socket: DatagramSocket? = null
    private var listenerThread: Thread? = null
    private var appContext: Context? = null

    @Synchronized
    fun start(context: Context) {
        appContext = context.applicationContext
        loadSavedProgress(context.applicationContext)
        if (!listening.compareAndSet(false, true)) return

        listenerThread = Thread({ listenLoop() }, "hint-progress-listener").apply {
            isDaemon = true
            start()
        }
    }

    @Synchronized
    fun stop() {
        listening.set(false)
        socket?.close()
        socket = null
        listenerThread = null
    }

    fun clear(roomId: String) {
        progressByRoom.remove(roomId)
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.remove(KEY_PREFIX + roomId)
            ?.apply()
    }

    private fun listenLoop() {
        try {
            DatagramSocket(HintProtocol.PORT).use { datagramSocket ->
                socket = datagramSocket
                datagramSocket.reuseAddress = true
                datagramSocket.broadcast = true

                val buffer = ByteArray(2048)
                while (listening.get()) {
                    val packet = DatagramPacket(buffer, buffer.size)
                    datagramSocket.receive(packet)
                    val event = HintProtocol.decode(packet.data, packet.length) ?: continue
                    updateProgress(event)
                }
            }
        } catch (_: SocketException) {
            // Socket is intentionally closed when the service stops.
        } catch (_: Exception) {
            // Keep the manager app alive if local networking is temporarily unavailable.
        } finally {
            socket = null
            listening.set(false)
        }
    }

    private fun updateProgress(event: HintUsageEvent) {
        mainHandler.post {
            val current = progressByRoom[event.roomId] ?: HintProgress()
            val updated = HintProgress(
                lastHintNumber = event.hintNumber,
                useCount = current.useCount + 1,
                lastUsedAtMillis = event.usedAtMillis
            )
            progressByRoom[event.roomId] = updated
            saveProgress(event.roomId, updated)
        }
    }

    private fun saveProgress(roomId: String, progress: HintProgress) {
        appContext
            ?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(
                KEY_PREFIX + roomId,
                "${progress.lastHintNumber},${progress.useCount},${progress.lastUsedAtMillis}"
            )
            ?.apply()
    }

    private fun loadSavedProgress(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.all.forEach { (key, value) ->
            if (!key.startsWith(KEY_PREFIX)) return@forEach
            val fields = (value as? String)?.split(",") ?: return@forEach
            if (fields.size != 3) return@forEach

            val roomId = key.removePrefix(KEY_PREFIX)
            val progress = HintProgress(
                lastHintNumber = fields[0].toIntOrNull() ?: 0,
                useCount = fields[1].toIntOrNull() ?: 0,
                lastUsedAtMillis = fields[2].toLongOrNull() ?: 0L
            )
            progressByRoom[roomId] = progress
        }
    }
}
