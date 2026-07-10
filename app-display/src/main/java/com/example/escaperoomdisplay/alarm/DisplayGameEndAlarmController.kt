package com.example.escaperoomdisplay.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

object DisplayGameEndAlarmController {
    private const val AUTO_STOP_MILLIS = 15_000L

    private val handler = Handler(Looper.getMainLooper())
    private val _isActive = mutableStateOf(false)
    val isActive: State<Boolean> = _isActive

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val autoStopRunnable = Runnable { stop() }

    @Synchronized
    fun play(context: Context) {
        stop()
        val appContext = context.applicationContext

        runCatching {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(appContext, alarmUri)
                isLooping = true
                prepare()
                start()
            }
        }

        runCatching {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                appContext.getSystemService(VibratorManager::class.java).defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                appContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            val pattern = longArrayOf(0L, 250L, 500L, 250L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        }

        _isActive.value = true
        handler.removeCallbacks(autoStopRunnable)
        handler.postDelayed(autoStopRunnable, AUTO_STOP_MILLIS)
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
