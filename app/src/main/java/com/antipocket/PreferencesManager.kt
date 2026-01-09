package com.antipocket

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var secureCallsEnabled: Boolean
        get() = prefs.getBoolean(KEY_SECURE_CALLS, false)
        set(value) = prefs.edit().putBoolean(KEY_SECURE_CALLS, value).apply()

    var onlyWhenInPocket: Boolean
        get() = prefs.getBoolean(KEY_ONLY_WHEN_IN_POCKET, false)
        set(value) = prefs.edit().putBoolean(KEY_ONLY_WHEN_IN_POCKET, value).apply()

    var antiPocketEnabled: Boolean
        get() = prefs.getBoolean(KEY_ANTI_POCKET, false)
        set(value) = prefs.edit().putBoolean(KEY_ANTI_POCKET, value).apply()

    var lockWhenInPocket: Boolean
        get() = prefs.getBoolean(KEY_LOCK_IN_POCKET, false)
        set(value) = prefs.edit().putBoolean(KEY_LOCK_IN_POCKET, value).apply()

    var hideLauncherIcon: Boolean
        get() = prefs.getBoolean(KEY_HIDE_LAUNCHER, false)
        set(value) = prefs.edit().putBoolean(KEY_HIDE_LAUNCHER, value).apply()

    var pendingCallNumber: String?
        get() = prefs.getString(KEY_PENDING_CALL, null)
        set(value) = prefs.edit().putString(KEY_PENDING_CALL, value).apply()

    companion object {
        private const val PREFS_NAME = "antipocket_prefs"
        private const val KEY_SECURE_CALLS = "secure_calls_enabled"
        private const val KEY_ONLY_WHEN_IN_POCKET = "only_when_in_pocket"
        private const val KEY_ANTI_POCKET = "anti_pocket_enabled"
        private const val KEY_LOCK_IN_POCKET = "lock_when_in_pocket"
        private const val KEY_HIDE_LAUNCHER = "hide_launcher_icon"
        private const val KEY_PENDING_CALL = "pending_call_number"

        @Volatile
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            return instance ?: synchronized(this) {
                instance ?: PreferencesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
