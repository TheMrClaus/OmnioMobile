package com.nuvio.app.features.watchprogress

import platform.Foundation.NSUserDefaults

actual object WatchProgressStorage {
    private const val payloadKey = "watch_progress_payload"

    actual fun loadPayload(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("${payloadKey}_$profileId")

    actual fun savePayload(profileId: Int, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = "${payloadKey}_$profileId")
    }

    actual fun loadLastSuccessfulPushMs(profileId: Int): Long =
        NSUserDefaults.standardUserDefaults.objectForKey(pushMsKey(profileId)) as? Long ?: 0L

    actual fun saveLastSuccessfulPushMs(profileId: Int, value: Long) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = pushMsKey(profileId))
    }

    private fun pushMsKey(profileId: Int) = "watch_progress_last_successful_push_ms_$profileId"
}
