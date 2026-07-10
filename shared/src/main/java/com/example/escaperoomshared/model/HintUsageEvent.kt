package com.example.escaperoomshared.model

data class HintUsageEvent(
    val roomId: String,
    val hintNumber: Int,
    val usedAtMillis: Long
)
