package com.example.escaperoomtimer.settings

import android.content.Context

data class StoreInfo(
    val storeName: String = "",
    val branchName: String = ""
) {
    val displayName: String
        get() = listOf(storeName, branchName).filter { it.isNotBlank() }.joinToString(" · ")
}

object StoreInfoPreferences {
    private const val PREFS_NAME = "manager_store_info"
    private const val KEY_STORE_NAME = "store_name"
    private const val KEY_BRANCH_NAME = "branch_name"

    fun load(context: Context): StoreInfo {
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return StoreInfo(
            storeName = prefs.getString(KEY_STORE_NAME, "").orEmpty(),
            branchName = prefs.getString(KEY_BRANCH_NAME, "").orEmpty()
        )
    }

    fun save(context: Context, info: StoreInfo) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STORE_NAME, info.storeName.trim())
            .putString(KEY_BRANCH_NAME, info.branchName.trim())
            .apply()
    }

    fun clear(context: Context) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }
}
