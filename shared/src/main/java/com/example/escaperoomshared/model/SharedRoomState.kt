package com.example.escaperoomshared.model

data class SharedRoomState(
    val id: String,
    val name: String,
    val seconds: Int,
    val status: String,
    val isRunning: Boolean,
    val updatedAtMillis: Long
)
