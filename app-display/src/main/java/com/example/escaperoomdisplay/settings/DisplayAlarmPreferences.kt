package com.example.escaperoomdisplay.settings

import android.content.Context

data class DisplayAlarmSettings(
    val soundUri: String?,
    val autoStopSeconds: Int,
    val vibrationEnabled: Boolean
)

object DisplayAlarmPreferences {
    private const val PREFS_NAME = "display_alarm_settings"
    private const val KEY_SOUND_URI = "sound_uri"
    private const val KEY_AUTO_STOP_SECONDS = "auto_stop_seconds"
    private const val KEY_VIBRATION_ENABLED = "vibration_enabled"

    fun load(context: Context): DisplayAlarmSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return DisplayAlarmSettings(
            soundUri = prefs.getString(KEY_SOUND_URI, null),
            autoStopSeconds = prefs.getInt(KEY_AUTO_STOP_SECONDS, 15),
            vibrationEnabled = prefs.getBoolean(KEY_VIBRATION_ENABLED, true)
        )
    }

    fun save(context: Context, settings: DisplayAlarmSettings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SOUND_URI, settings.soundUri)
            .putInt(KEY_AUTO_STOP_SECONDS, settings.autoStopSeconds)
            .putBoolean(KEY_VIBRATION_ENABLED, settings.vibrationEnabled)
            .apply()
    }
}
