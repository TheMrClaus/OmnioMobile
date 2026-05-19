package com.nuvio.app.features.streams.prefs

internal expect object StreamPreferencesStorage {
    fun loadPayload(key: String): String?
    fun savePayload(key: String, payload: String)
    fun clear()
}
