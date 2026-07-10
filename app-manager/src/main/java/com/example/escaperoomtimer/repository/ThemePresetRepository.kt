package com.example.escaperoomtimer.repository

import android.content.Context
import com.example.escaperoomtimer.model.ThemePreset
import org.json.JSONArray
import org.json.JSONObject

object ThemePresetRepository {
    private const val PREFS_NAME = "escape_room_theme_presets"
    private const val KEY_PRESETS = "presets_json"

    fun load(context: Context): List<ThemePreset> {
        val rawJson = context
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_PRESETS, null)
            ?: return emptyList()

        return runCatching {
            val array = JSONArray(rawJson)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    val name = item.optString("name").trim()
                    if (name.isBlank()) continue

                    add(
                        ThemePreset(
                            id = item.optString("id", "preset_$index"),
                            name = name,
                            defaultMinutes = item.optInt("defaultMinutes", 60).coerceIn(1, 240),
                            emoji = item.optString("emoji", "🎭").ifBlank { "🎭" }
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, presets: List<ThemePreset>) {
        val array = JSONArray()
        presets.forEach { preset ->
            array.put(
                JSONObject().apply {
                    put("id", preset.id)
                    put("name", preset.name.trim())
                    put("defaultMinutes", preset.defaultMinutes.coerceIn(1, 240))
                    put("emoji", preset.emoji.ifBlank { "🎭" })
                }
            )
        }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PRESETS, array.toString())
            .apply()
    }
}
