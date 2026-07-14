package com.example.escaperoomdisplay.widget

import android.content.Context
import android.os.SystemClock
import com.example.escaperoomshared.model.SharedRoomState

object DisplayWidgetStateRepository {
    private const val PREFS_NAME = "display_widget_state"
    private const val KEY_ROOM_ID = "room_id"
    private const val KEY_ROOM_NAME = "room_name"
    private const val KEY_SECONDS = "seconds"
    private const val KEY_STATUS = "status"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_CONNECTED = "connected"
    private const val KEY_SAVED_AT_MILLIS = "saved_at_millis"
    private const val KEY_SAVED_AT_ELAPSED_REALTIME = "saved_at_elapsed_realtime"

    data class SavedState(
        val roomId: String,
        val roomName: String,
        val seconds: Int,
        val status: String,
        val isRunning: Boolean,
        val connected: Boolean,
        val savedAtMillis: Long,
        val savedAtElapsedRealtime: Long
    ) {
        fun remainingSeconds(
            nowMillis: Long = System.currentTimeMillis(),
            nowElapsedRealtime: Long = SystemClock.elapsedRealtime()
        ): Int {
            if (!isRunning || seconds <= 0) return seconds.coerceAtLeast(0)
            val elapsedMillis = if (
                savedAtElapsedRealtime > 0L && nowElapsedRealtime >= savedAtElapsedRealtime
            ) {
                nowElapsedRealtime - savedAtElapsedRealtime
            } else {
                (nowMillis - savedAtMillis).coerceAtLeast(0L)
            }
            val elapsedSeconds = (elapsedMillis / 1_000L).toInt()
            return (seconds - elapsedSeconds).coerceAtLeast(0)
        }
    }

    fun save(context: Context, room: SharedRoomState?, connected: Boolean) {
        val editor = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_CONNECTED, connected)

        if (room == null) {
            editor
                .remove(KEY_ROOM_ID)
                .remove(KEY_ROOM_NAME)
                .remove(KEY_SECONDS)
                .remove(KEY_STATUS)
                .remove(KEY_IS_RUNNING)
                .remove(KEY_SAVED_AT_MILLIS)
                .remove(KEY_SAVED_AT_ELAPSED_REALTIME)
                .apply()
            return
        }

        editor
            .putString(KEY_ROOM_ID, room.id)
            .putString(KEY_ROOM_NAME, room.name)
            .putInt(KEY_SECONDS, room.seconds.coerceAtLeast(0))
            .putString(KEY_STATUS, room.status)
            .putBoolean(KEY_IS_RUNNING, room.isRunning)
            .putLong(KEY_SAVED_AT_MILLIS, System.currentTimeMillis())
            .putLong(KEY_SAVED_AT_ELAPSED_REALTIME, SystemClock.elapsedRealtime())
            .apply()
    }

    fun load(context: Context): SavedState? {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val roomId = prefs.getString(KEY_ROOM_ID, null) ?: return null
        return SavedState(
            roomId = roomId,
            roomName = prefs.getString(KEY_ROOM_NAME, "ROOM").orEmpty(),
            seconds = prefs.getInt(KEY_SECONDS, 0).coerceAtLeast(0),
            status = prefs.getString(KEY_STATUS, "WAITING").orEmpty(),
            isRunning = prefs.getBoolean(KEY_IS_RUNNING, false),
            connected = prefs.getBoolean(KEY_CONNECTED, false),
            savedAtMillis = prefs.getLong(KEY_SAVED_AT_MILLIS, System.currentTimeMillis()),
            savedAtElapsedRealtime = prefs.getLong(KEY_SAVED_AT_ELAPSED_REALTIME, 0L)
        )
    }
}
