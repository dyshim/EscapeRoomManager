package com.example.escaperoomtimer.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast

private const val HINT_PACKAGE_NAME = "com.sherlock.test"
private const val HINT_ACTIVITY_NAME = "com.unity3d.player.UnityPlayerActivity"

fun openHintApp(context: Context) {
    val packageManager = context.packageManager

    val launchIntent = packageManager.getLaunchIntentForPackage(HINT_PACKAGE_NAME)

    val intent = launchIntent ?: Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        component = ComponentName(HINT_PACKAGE_NAME, HINT_ACTIVITY_NAME)
    }

    try {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "힌트앱이 설치되어 있지 않습니다.",
            Toast.LENGTH_SHORT
        ).show()
    }
}

fun isHintAppInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo(HINT_PACKAGE_NAME, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
