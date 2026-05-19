package com.nuvio.app.features.watchprogress

import android.content.Context
import android.content.SharedPreferences

actual object WatchProgressStorage {
    private const val preferencesName = "nuvio_watch_progress"
    private const val payloadKey = "watch_progress_payload"

    private var preferences: SharedPreferences? = null

    fun initialize(context: Context) {
        preferences = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
    }

    actual fun loadPayload(profileId: Int): String? =
        preferences?.getString("${payloadKey}_$profileId", null)

    actual fun savePayload(profileId: Int, payload: String) {
        preferences
            ?.edit()
            ?.putString("${payloadKey}_$profileId", payload)
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

    private fun pushMsKey(profileId: Int) = "watch_progress_last_successful_push_ms_$profileId"
}
