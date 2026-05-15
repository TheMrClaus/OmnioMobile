package com.nuvio.app.features.emby

import com.nuvio.app.core.storage.ProfileScopedKey
import platform.Foundation.NSUserDefaults

internal actual object EmbyAuthStorage {
    private const val payloadKey = "emby_auth_payload"

    actual fun loadPayload(): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(ProfileScopedKey.of(payloadKey))

    actual fun savePayload(payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = ProfileScopedKey.of(payloadKey))
    }

    actual fun loadPayloadForProfile(profileId: Int): String? =
        NSUserDefaults.standardUserDefaults.stringForKey("${payloadKey}_$profileId")
}
