package com.example.escaperoomtimer.repository

import android.content.Context
import com.example.escaperoomtimer.model.RoomInfo
import com.example.escaperoomtimer.model.RoomStatus
import org.json.JSONArray
import org.json.JSONObject

object RoomStateRepository {
    private const val PREFS_NAME = "escape_room_timer_state"
    private const val KEY_ROOMS = "rooms_json"
    private const val KEY_SAVED_AT = "saved_at_epoch_ms"

    fun save(context: Context, rooms: List<RoomInfo>) {
        val jsonArray = JSONArray()
        rooms.forEach { room ->
            jsonArray.put(JSONObject().apply {
                put("id", room.id)
                put("name", room.name)
                put("seconds", room.seconds)
                put("status", room.status.name)
                put("isRunning", room.isRunning)
                put("defaultMinutes", room.defaultMinutes)
                put("hintEnabled", room.hintEnabled)
                put("guestScreenEnabled", room.guestScreenEnabled)
                put("isEnabled", room.isEnabled)
                put("isMaintenance", room.isMaintenance)
                put("startedAtEpochMillis", room.startedAtEpochMillis ?: JSONObject.NULL)
                put("finishedAtEpochMillis", room.finishedAtEpochMillis ?: JSONObject.NULL)
                put("elapsedSeconds", room.elapsedSeconds)
            })
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ROOMS, jsonArray.toString())
            .putLong(KEY_SAVED_AT, System.currentTimeMillis())
            .apply()
    }

    fun load(context: Context): List<RoomInfo>? {
        val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = preferences.getString(KEY_ROOMS, null) ?: return null
        val savedAt = preferences.getLong(KEY_SAVED_AT, System.currentTimeMillis())
        val elapsedSeconds = ((System.currentTimeMillis() - savedAt) / 1000L)
            .coerceAtLeast(0L)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()

        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val wasRunning = item.optBoolean("isRunning", false)
                    val savedSeconds = item.optInt("seconds", 0).coerceAtLeast(0)
                    val restoredSeconds = if (wasRunning) (savedSeconds - elapsedSeconds).coerceAtLeast(0) else savedSeconds
                    val restoredRunning = wasRunning && restoredSeconds > 0
                    val storedFinishedAt = item.optNullableLong("finishedAtEpochMillis")
                    val restoredFinishedAt = when {
                        restoredSeconds > 0 -> null
                        storedFinishedAt != null -> storedFinishedAt
                        wasRunning -> savedAt + savedSeconds * 1_000L
                        else -> null
                    }

                    add(
                        RoomInfo(
                            id = item.getString("id"),
                            name = item.optString("name", "ROOM"),
                            seconds = restoredSeconds,
                            status = restoredStatus(
                                storedStatus = item.optString("status", RoomStatus.WAITING.name),
                                seconds = restoredSeconds,
                                isRunning = restoredRunning
                            ),
                            isRunning = restoredRunning,
                            defaultMinutes = item.optInt("defaultMinutes", 60).coerceIn(1, 240),
                            hintEnabled = item.optBoolean("hintEnabled", true),
                            guestScreenEnabled = item.optBoolean("guestScreenEnabled", true),
                            isEnabled = item.optBoolean("isEnabled", true),
                            isMaintenance = item.optBoolean("isMaintenance", false),
                            startedAtEpochMillis = item.optNullableLong("startedAtEpochMillis"),
                            finishedAtEpochMillis = restoredFinishedAt,
                            elapsedSeconds = (
                                item.optInt("elapsedSeconds", 0).coerceAtLeast(0).toLong() +
                                    if (wasRunning) minOf(elapsedSeconds, savedSeconds) else 0
                                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                        )
                    )
                }
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun restoredStatus(storedStatus: String, seconds: Int, isRunning: Boolean): RoomStatus {
        if (seconds <= 0) return RoomStatus.FINISHED
        if (isRunning) return if (seconds <= 5 * 60) RoomStatus.WARNING else RoomStatus.RUNNING

        return runCatching { RoomStatus.valueOf(storedStatus) }
            .getOrDefault(RoomStatus.PAUSED)
            .let { status ->
                when (status) {
                    RoomStatus.RUNNING, RoomStatus.WARNING, RoomStatus.FINISHED -> RoomStatus.PAUSED
                    else -> status
                }
            }
    }

    private fun JSONObject.optNullableLong(name: String): Long? =
        if (has(name) && !isNull(name)) optLong(name) else null
}
