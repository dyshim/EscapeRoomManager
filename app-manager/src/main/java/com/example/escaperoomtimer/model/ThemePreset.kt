package com.example.escaperoomtimer.model

data class ThemePreset(
    val id: String,
    val name: String,
    val defaultMinutes: Int,
    val emoji: String = "🎭",
    val defaultSeconds: Int = defaultMinutes * 60
)
