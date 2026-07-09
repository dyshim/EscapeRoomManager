package com.example.escaperoomtimer.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(seconds: Int): String {
    val safeSeconds = seconds.coerceAtLeast(0)
    val min = safeSeconds / 60
    val sec = safeSeconds % 60
    return "%02d:%02d".format(min, sec)
}

fun nowText(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    return formatter.format(Date())
}
