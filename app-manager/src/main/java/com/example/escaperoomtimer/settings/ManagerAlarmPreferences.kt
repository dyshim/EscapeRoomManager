package com.example.escaperoomtimer.settings

import android.content.Context

data class ManagerAlarmSettings(
    val soundUri: String?,
    val autoStopSeconds: Int,
    val vibrationEnabled: Boolean,
    val volumePercent: Int = 100
)

object ManagerAlarmPreferences {
    private const val PREFS_NAME = "manager_alarm_settings"
    private const val KEY_SOUND_URI = "sound_uri"
    private const val KEY_AUTO_STOP_SECONDS = "auto_stop_seconds"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"
    private const val KEY_VOLUME_PERCENT = "volume_percent"

    fun load(context: Context): ManagerAlarmSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return ManagerAlarmSettings(
            soundUri = prefs.getString(KEY_SOUND_URI, null),
            autoStopSeconds = prefs.getInt(KEY_AUTO_STOP_SECONDS, 30),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true),
            volumePercent = prefs.getInt(KEY_VOLUME_PERCENT, 100).coerceIn(0, 100)
        )
    }

    fun save(context: Context, settings: ManagerAlarmSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND_URI, settings.soundUri)
            .putInt(KEY_AUTO_STOP_SECONDS, settings.autoStopSeconds)
            .putBoolean(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
            .putInt(KEY_VOLUME_PERCENT, settings.volumePercent.coerceIn(0, 100))
            .apply()
    }
}
