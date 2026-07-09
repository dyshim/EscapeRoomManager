package com.example.escaperoomtimer.util

import android.content.Context
import android.widget.Toast

fun openHintApp(context: Context) {
    val hintPackageName = "com.sherlock.test"
    val intent = context.packageManager.getLaunchIntentForPackage(hintPackageName)

    if (intent != null) {
        context.startActivity(intent)
    } else {
        Toast.makeText(context, "힌트앱이 설치되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
    }
}
