package com.nuvio.app.features.streams.prefs

import platform.Foundation.NSUserDefaults

internal actual object StreamPreferencesStorage {
    actual fun loadPayload(key: String): String? =
        NSUserDefaults.standardUserDefaults.stringForKey(key)

    actual fun savePayload(key: String, payload: String) {
        NSUserDefaults.standardUserDefaults.setObject(payload, forKey = key)
    }

    actual fun clear() {
        // No-op: iOS NSUserDefaults keys are scoped with ProfileScopedKey prefixes.
        // The repository's clearLocalState() resets in-memory state;
        // subsequent persists will overwrite stored values with defaults.
    }
}
