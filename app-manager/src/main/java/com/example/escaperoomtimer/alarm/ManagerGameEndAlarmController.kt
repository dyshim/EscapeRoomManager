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
    private val _isActive = mutableStateOf(false)
    val isActive: State<Boolean> = _isActive

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val autoStopRunnable = Runnable { stop() }

    @Synchronized
    fun play(context: Context) {
        val settings = ManagerAlarmPreferences.load(context)
        if (!settings.enabled) {
            stop()
            return
        }
        playInternal(
            context = context,
            soundUri = resolveSoundUri(settings.soundUri),
            vibrationEnabled = settings.vibrationEnabled,
            autoStopMillis = settings.autoStopSeconds.takeIf { it > 0 }?.times(1_000L),
            volume = settings.volumePercent.coerceIn(0, 100) / 100f,
            markActive = true
        )
    }

    @Synchronized
    fun preview(context: Context, soundUri: String?, volumePercent: Int = 100) {
        playInternal(
            context = context,
            soundUri = resolveSoundUri(soundUri),
            vibrationEnabled = false,
            autoStopMillis = 3_000L,
            volume = volumePercent.coerceIn(0, 100) / 100f,
            markActive = false
        )
    }

    private fun playInternal(
        context: Context,
        soundUri: Uri,
        vibrationEnabled: Boolean,
        autoStopMillis: Long?,
        volume: Float,
        markActive: Boolean
    ) {
        stop()
        val appContext = context.applicationContext

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(appContext, soundUri)
                isLooping = true
                setVolume(volume, volume)
                prepare()
                start()
            }
        }.onFailure {
            runCatching {
                val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    setDataSource(appContext, fallback)
                    isLooping = true
                    setVolume(volume, volume)
                    prepare()
                    start()
                }
            }
        }

        if (vibrationEnabled) {
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
            }
        }

        _isActive.value = markActive
        handler.removeCallbacks(autoStopRunnable)
        autoStopMillis?.let { handler.postDelayed(autoStopRunnable, it) }
    }

    private fun resolveSoundUri(savedUri: String?): Uri {
        return savedUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
    }

    @Synchronized
    fun stop() {
        handler.removeCallbacks(autoStopRunnable)
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        runCatching { vibrator?.cancel() }
        vibrator = null
        _isActive.value = false
    }
}
