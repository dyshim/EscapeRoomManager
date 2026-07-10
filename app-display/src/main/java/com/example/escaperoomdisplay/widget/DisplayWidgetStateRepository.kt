package com.example.escaperoomdisplay.widget

import android.content.Context
import com.example.escaperoomshared.model.SharedRoomState

object DisplayWidgetStateRepository {
    private const val PREFS_NAME = "display_widget_state"
    private const val KEY_ROOM_ID = "room_id"
    private const val KEY_ROOM_NAME = "room_name"
    private const val KEY_SECONDS = "seconds"
    private const val KEY_STATUS = "status"
    private const val KEY_IS_RUNNING = "is_running"
    private const val KEY_CONNECTED = "connected"

    data class SavedState(
        val roomId: String,
        val roomName: String,
        val seconds: Int,
        val status: String,
        val isRunning: Boolean,
        val connected: Boolean
    )

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
                .apply()
            return
        }

        editor
            .putString(KEY_ROOM_ID, room.id)
            .putString(KEY_ROOM_NAME, room.name)
            .putInt(KEY_SECONDS, room.seconds.coerceAtLeast(0))
            .putString(KEY_STATUS, room.status)
            .putBoolean(KEY_IS_RUNNING, room.isRunning)
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
            connected = prefs.getBoolean(KEY_CONNECTED, false)
        )
    }
}
