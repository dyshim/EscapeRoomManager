package com.example.escaperoomtimer.alarm

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper

object ManagerAlarmPreviewController {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private val stopRunnable = Runnable { stopPreview() }

    @Synchronized
    fun playPreview(context: Context, soundUri: String?, volumePercent: Int) {
        stopPreview()
        val appContext = context.applicationContext
        val uri = soundUri?.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val volume = volumePercent.coerceIn(0, 100) / 100f
        mediaPlayer = createPlayingPlayer(appContext, uri, volume)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)?.let {
                createPlayingPlayer(appContext, it, volume)
            }
        if (mediaPlayer != null) handler.postDelayed(stopRunnable, 3_000L)
    }

    private fun createPlayingPlayer(context: Context, uri: Uri, volume: Float): MediaPlayer? {
        var player: MediaPlayer? = null
        return try {
            player = MediaPlayer()
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            player.setDataSource(context, uri)
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

    @Synchronized
    fun stopPreview() {
        handler.removeCallbacks(stopRunnable)
        mediaPlayer?.let { player ->
            runCatching { player.stop() }
            runCatching { player.release() }
        }
        mediaPlayer = null
    }
}
