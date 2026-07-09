package com.example.escaperoomtimer.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatTime(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return "%02d:%02d".format(min, sec)
}

fun nowText(): String {
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA)
    return formatter.format(Date())
}