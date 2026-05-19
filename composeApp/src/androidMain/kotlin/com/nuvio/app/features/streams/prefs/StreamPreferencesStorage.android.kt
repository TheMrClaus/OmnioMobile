package com.nuvio.app.features.streams.prefs

import android.content.Context
import android.content.SharedPreferences

internal actual object StreamPreferencesStorage {
    internal const val preferencesName = "nuvio_stream_prefs"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(key: String): String? =
        preferences?.getString(key, null)

    actual fun savePayload(key: String, payload: String) {
        preferences
            ?.edit()
            ?.putString(key, payload)
            ?.apply()
    }

    actual fun clear() {
        preferences
            ?.edit()
            ?.clear()
            ?.apply()
    }
}
