package com.nuvio.app.features.emby

import kotlinx.serialization.Serializable

/**
 * Persisted Emby authentication state. One blob per profile.
 *
 * - [serverUrl] is normalized (trimmed, no trailing slash).
 * - [accessToken] comes from `Users/AuthenticateByName` and is sent as `X-Emby-Token`.
 * - [userId] is required for `Users/{userId}/Items` lookups.
 * - [deviceId] is unique per profile so each profile shows up as a distinct
 *   client in the Emby server's session list.
 * - [serverName] is fetched from `System/Info` after sign-in and used for display only.
 */
@Serializable
internal data class EmbyAuthPayload(
    val serverUrl: String = "",
    val accessToken: String = "",
    val userId: String = "",
    val deviceId: String = "",
    val username: String = "",
    val serverName: String = "",
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() &&
            accessToken.isNotBlank() &&
            userId.isNotBlank() &&
            deviceId.isNotBlank()
}
