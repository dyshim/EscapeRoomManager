package com.example.escaperoomdisplay.network

object HintUsageSender {
    fun send(roomId: String, hintNumber: Int): Boolean {
        return DisplaySyncManager.sendHintUsage(roomId, hintNumber)
    }
}
