package com.nuvio.app.features.emby

internal expect object EmbyAuthStorage {
    fun loadPayload(): String?
    fun savePayload(payload: String)

    /**
     * Reads payload for [profileId] without changing the active profile.
     * Used to enable "Copy from Main" eligibility checks.
     */
    fun loadPayloadForProfile(profileId: Int): String?
}
