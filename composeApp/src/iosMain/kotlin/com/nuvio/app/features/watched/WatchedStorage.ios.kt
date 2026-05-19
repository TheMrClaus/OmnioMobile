package com.nuvio.app.features.watched

import platform.Foundation.NSUserDefaults

actual object WatchedStorage {
    private fun payloadKey(profileId: Int) = "watched_payload_$profileId"

    actual fun loadPayload(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(payloadKey(profileId))

    actual fun savePayload(profileId: Int, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = payloadKey(profileId))
    }

    actual fun loadLastSuccessfulPushMs(profileId: Int): Long =
        NSUserDefaults.standardUserDefaults.objectForKey(pushMsKey(profileId)) as? Long ?: 0L

    actual fun saveLastSuccessfulPushMs(profileId: Int, value: Long) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = pushMsKey(profileId))
    }

    private fun pushMsKey(profileId: Int) = "watched_last_successful_push_ms_$profileId"
}

