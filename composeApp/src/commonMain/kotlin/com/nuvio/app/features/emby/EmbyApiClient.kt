package com.nuvio.app.features.emby

import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpRequestRaw
import io.ktor.http.encodeURLParameter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Thin wrapper around the platform `httpRequestRaw` expect for Emby's REST API.
 *
 * Stays stateless — callers (the auth repo, media service, session service) provide
 * the [EmbyAuthPayload] for each call. Auth uses `X-Emby-Authorization` (and
 * `X-Emby-Token` when authenticated).
 */
internal object EmbyApiClient {
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private const val CLIENT_NAME = "OmnioMobile"
    private const val DEVICE_NAME = "OmnioMobile"
    private const val APP_VERSION = "1.0.0"

    suspend fun authenticateByName(
        serverUrl: String,
        deviceId: String,
        username: String,
        password: String,
    ): RawHttpResponse {
        val body = json.encodeToString(EmbyAuthByNameRequest(username = username, pw = password))
        return httpRequestRaw(
            method = "POST",
            url = "${serverUrl.trimEnd('/')}/Users/AuthenticateByName",
            headers = baseHeaders(deviceId = deviceId, accessToken = null) +
                ("Content-Type" to "application/json"),
            body = body,
        )
    }

    suspend fun getSystemInfo(payload: EmbyAuthPayload): RawHttpResponse {
        return httpRequestRaw(
            method = "GET",
            url = "${payload.serverUrl}/System/Info",
            headers = authedHeaders(payload),
            body = "",
        )
    }

    suspend fun getUserItemsByProviderIds(
        payload: EmbyAuthPayload,
        includeItemTypes: String,
        providerIdFilter: String,
        limit: Int = 1,
    ): RawHttpResponse {
        val fields = "ProviderIds,RunTimeTicks,MediaSources,ParentIndexNumber,IndexNumber,UserData"
        val url = buildString {
            append(payload.serverUrl)
            append("/Users/")
            append(payload.userId.encodeURLParameter())
            append("/Items?Recursive=true")
            append("&IncludeItemTypes=").append(includeItemTypes.encodeURLParameter())
            append("&AnyProviderIdEquals=").append(providerIdFilter.encodeURLParameter())
            append("&Fields=").append(fields.encodeURLParameter())
            append("&Limit=").append(limit)
        }
        return httpRequestRaw(
            method = "GET",
            url = url,
            headers = authedHeaders(payload),
            body = "",
        )
    }

    suspend fun getEpisodes(
        payload: EmbyAuthPayload,
        seriesId: String,
        season: Int,
    ): RawHttpResponse {
        val fields = "RunTimeTicks,MediaSources,ParentIndexNumber,IndexNumber,UserData"
        val url = buildString {
            append(payload.serverUrl)
            append("/Shows/")
            append(seriesId.encodeURLParameter())
            append("/Episodes?Season=").append(season)
            append("&UserId=").append(payload.userId.encodeURLParameter())
            append("&Fields=").append(fields.encodeURLParameter())
        }
        return httpRequestRaw(
            method = "GET",
            url = url,
            headers = authedHeaders(payload),
            body = "",
        )
    }

    suspend fun reportPlaybackStart(
        payload: EmbyAuthPayload,
        dto: EmbyPlaybackStartDto,
    ): RawHttpResponse {
        val body = json.encodeToString(dto)
        return httpRequestRaw(
            method = "POST",
            url = "${payload.serverUrl}/Sessions/Playing",
            headers = authedHeaders(payload) + ("Content-Type" to "application/json"),
            body = body,
        )
    }

    suspend fun reportPlaybackProgress(
        payload: EmbyAuthPayload,
        dto: EmbyPlaybackProgressDto,
    ): RawHttpResponse {
        val body = json.encodeToString(dto)
        return httpRequestRaw(
            method = "POST",
            url = "${payload.serverUrl}/Sessions/Playing/Progress",
            headers = authedHeaders(payload) + ("Content-Type" to "application/json"),
            body = body,
        )
    }

    suspend fun reportPlaybackStopped(
        payload: EmbyAuthPayload,
        dto: EmbyPlaybackStopDto,
    ): RawHttpResponse {
        val body = json.encodeToString(dto)
        return httpRequestRaw(
            method = "POST",
            url = "${payload.serverUrl}/Sessions/Playing/Stopped",
            headers = authedHeaders(payload) + ("Content-Type" to "application/json"),
            body = body,
        )
    }

    private fun baseHeaders(deviceId: String, accessToken: String?): Map<String, String> {
        val authorization = buildString {
            append("MediaBrowser Client=\"").append(CLIENT_NAME).append("\"")
            append(", Device=\"").append(DEVICE_NAME).append("\"")
            append(", DeviceId=\"").append(deviceId).append("\"")
            append(", Version=\"").append(APP_VERSION).append("\"")
            if (!accessToken.isNullOrBlank()) {
                append(", Token=\"").append(accessToken).append("\"")
            }
        }
        val headers = mutableMapOf(
            "X-Emby-Authorization" to authorization,
            "Accept" to "application/json",
        )
        if (!accessToken.isNullOrBlank()) {
            headers["X-Emby-Token"] = accessToken
        }
        return headers
    }

    private fun authedHeaders(payload: EmbyAuthPayload): Map<String, String> =
        baseHeaders(deviceId = payload.deviceId, accessToken = payload.accessToken)
}
