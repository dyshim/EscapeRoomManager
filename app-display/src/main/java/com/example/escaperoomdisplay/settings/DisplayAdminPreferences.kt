package com.example.escaperoomdisplay.settings

import android.content.Context

object DisplayAdminPreferences {
    private const val PREFS_NAME = "display_admin_preferences"
    private const val KEY_ADMIN_PIN = "admin_pin"
    const val DEFAULT_PIN = "1234"

    fun verifyPin(context: Context, input: String): Boolean {
        return input == getPin(context)
    }

    fun setPin(context: Context, newPin: String) {
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ADMIN_PIN, newPin)
            .apply()
    }

    private fun getPin(context: Context): String {
        return context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_ADMIN_PIN, DEFAULT_PIN)
            ?: DEFAULT_PIN
    }
}
