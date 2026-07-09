package com.example.escaperoomtimer.utils

import android.content.Context
import android.widget.Toast

fun openHintApp(context: Context) {
    val packageName = "com.sherlock.test"
    val intent = context.packageManager.getLaunchIntentForPackage(packageName)

    if (intent != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "힌트앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
    }
}