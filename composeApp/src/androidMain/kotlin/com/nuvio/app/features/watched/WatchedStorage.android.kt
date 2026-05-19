package com.nuvio.app.features.watched

import android.content.Context
import android.content.SharedPreferences

actual object WatchedStorage {
    private const val preferencesName = "nuvio_watched"
    private fun payloadKey(profileId: Int) = "watched_payload_$profileId"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString(payloadKey(profileId), null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString(payloadKey(profileId), payload)
            ?.apply()
    }

    actual fun loadLastSuccessfulPushMs(profileId: Int): Long =
        preferences?.getLong(pushMsKey(profileId), 0L) ?: 0L

    actual fun saveLastSuccessfulPushMs(profileId: Int, value: Long) {
        preferences
            ?.edit()
            ?.putLong(pushMsKey(profileId), value)
            ?.apply()
    }

    private fun pushMsKey(profileId: Int) = "watched_last_successful_push_ms_$profileId"
}

