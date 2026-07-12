package com.example.escaperoomtimer.settings

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object WebAdminPinPreferences {
    private const val PREFS_NAME = "web_admin_security"
    private const val KEY_SALT = "pin_salt"
    private const val KEY_HASH = "pin_hash"
    private const val DEFAULT_PIN = "1234"

    fun ensureInitialized(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_SALT) || !prefs.contains(KEY_HASH)) {
            saveNewPin(context, DEFAULT_PIN)
        }
    }

    fun verify(context: Context, pin: String): Boolean {
        ensureInitialized(context)
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val salt = prefs.getString(KEY_SALT, null) ?: return false
        val expected = prefs.getString(KEY_HASH, null) ?: return false
        return constantTimeEquals(expected, hash(pin, salt))
    }

    fun changePin(context: Context, currentPin: String, newPin: String): Boolean {
        if (!isValidPin(newPin) || !verify(context, currentPin)) return false
        saveNewPin(context, newPin)
        return true
    }

    fun resetToDefault(context: Context) {
        saveNewPin(context, DEFAULT_PIN)
    }

    fun isValidPin(pin: String): Boolean = pin.length in 4..8 && pin.all(Char::isDigit)

    private fun saveNewPin(context: Context, pin: String) {
        val saltBytes = ByteArray(16).also(SecureRandom()::nextBytes)
        val salt = Base64.encodeToString(saltBytes, Base64.NO_WRAP)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SALT, salt)
            .putString(KEY_HASH, hash(pin, salt))
            .apply()
    }

    private fun hash(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest("$salt:$pin".toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (index in a.indices) result = result or (a[index].code xor b[index].code)
        return result == 0
    }
}
