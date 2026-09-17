package com.example.escaperoomtimer.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.escaperoomtimer.settings.ManagerAlarmPreferences

object ManagerGameEndAlarmController {
    private val handler = Handler(Looper.getMainLooper())
    private val activeAlarmRoomIds = LinkedHashSet<String>()
    private val _activeRoomIds = mutableStateOf<Set<String>>(emptySet())
    private val _isActive = mutableStateOf(false)
    val isActive: State<Boolean> = _isActive

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val autoStopRunnable = Runnable { stop() }

    fun isAlarmActiveFor(roomId: String): Boolean = roomId in _activeRoomIds.value

    @Synchronized
    fun play(context: Context, roomIds: Collection<String>) {
        if (roomIds.isEmpty()) return
        activeAlarmRoomIds.addAll(roomIds)
        val settings = ManagerAlarmPreferences.load(context)
        if (!settings.enabled) {
            stop()
            return
        }

        stopPlaybackOnly()
        val appContext = context.applicationContext
        val volume = settings.volumePercent.coerceIn(0, 100) / 100f
        mediaPlayer = createPlayingPlayer(appContext, resolveSoundUri(settings.soundUri), volume)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let {
                createPlayingPlayer(appContext, it, volume)
            }

        if (settings.vibrationEnabled) {
            runCatching {
                vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    appContext.getSystemService(VibratorManager::class.java).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                }
                val pattern = longArrayOf(0L, 500L, 350L, 500L, 350L)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }.onFailure {
                runCatching { vibrator?.cancel() }
                vibrator = null
            }
        }

        if (mediaPlayer == null && vibrator == null) {
            stop()
            return
        }
        _activeRoomIds.value = activeAlarmRoomIds.toSet()
        _isActive.value = true
        settings.autoStopSeconds.takeIf { it > 0 }?.let {
            handler.postDelayed(autoStopRunnable, it * 1_000L)
        }
    }

    @Synchronized
    fun acknowledge(roomId: String) {
        if (!activeAlarmRoomIds.remove(roomId)) return
        if (activeAlarmRoomIds.isEmpty()) {
            stop()
        } else {
            _activeRoomIds.value = activeAlarmRoomIds.toSet()
        }
    }

    private fun createPlayingPlayer(context: Context, soundUri: Uri, volume: Float): MediaPlayer? {
        var player: MediaPlayer? = null
        return try {
            player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, soundUri)
            player.isLooping = true
            player.setVolume(volume, volume)
            player.prepare()
            player.start()
            player
        } catch (_: Exception) {
            runCatching { player?.release() }
            null
        }
    }

    private fun resolveSoundUri(savedUri: String?): Uri {
        return savedUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    private fun stopPlaybackOnly() {
        handler.removeCallbacks(autoStopRunnable)
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        _isActive.value = false
    }

    @Synchronized
    fun stop() {
        stopPlaybackOnly()
        activeAlarmRoomIds.clear()
        _activeRoomIds.value = emptySet()
    }
}
