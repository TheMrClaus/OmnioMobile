package com.nuvio.app.features.aiometadata

import com.nuvio.app.core.network.SupabaseConfig
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpRequestRaw
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * HTTP client for the Omnio AIOMetadata Supabase Edge Functions.
 *
 * Mirrors [com.nuvio.app.features.sourcecloud.SourceCloudApiClient]'s wire-level
 * approach (JWT + apikey via [httpRequestRaw]) rather than going through
 * supabase-kt's `functions.invoke`. Stage 2 only consumes the
 * `aio-metadata-provision-profile` endpoint; future endpoints (status,
 * update-config, fetch-manifest…) can land here as siblings without
 * restructuring.
 */
internal object AioMetadataApiClient {
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    val baseUrlConfigured: Boolean
        get() = SupabaseConfig.URL.isNotBlank() && SupabaseConfig.ANON_KEY.isNotBlank()

    /**
     * Idempotent: the edge function returns the existing config link with
     * `reused: true` when `(user, profileId)` already has an `aio_metadata_links`
     * row. Kids profiles force inheritance of Main's API keys regardless of
     * [copyKeysFromMain], and the server returns 412 if Main has no
     * AIOMetadata config yet — surfaced here as a null response, mapped to
     * a kids-specific user message in [AioMetadataRepository].
     */
    suspend fun provisionProfile(
        profileId: Int,
        kids: Boolean,
        copyKeysFromMain: Boolean,
        maxAgeRating: String? = null,
    ): RawHttpResponse? {
        val body = json.encodeToString(
            AioMetadataProvisionProfileRequestDto(
                profileId = profileId,
                kids = kids,
                copyKeysFromMain = copyKeysFromMain,
                maxAgeRating = maxAgeRating,
            ),
        )
        return request(
            method = "POST",
            function = "aio-metadata-provision-profile",
            body = body,
        )
    }

    internal inline fun <reified T> decodeOrNull(response: RawHttpResponse): T? {
        if (response.status !in 200..299) return null
        val raw = response.body
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull()
    }

    private suspend fun request(
        method: String,
        function: String,
        body: String = "",
    ): RawHttpResponse? {
        if (!baseUrlConfigured) return null
        val url = "${SupabaseConfig.URL.trimEnd('/')}/functions/v1/$function"
        val anonKey = SupabaseConfig.ANON_KEY
        val sessionToken = runCatching {
            SupabaseProvider.client.auth.currentSessionOrNull()?.accessToken
        }.getOrNull()
        val bearer = sessionToken?.takeIf { it.isNotBlank() } ?: anonKey
        val headers = buildMap {
            put("apikey", anonKey)
            put("Authorization", "Bearer $bearer")
            put("Accept", "application/json")
            if (method.equals("POST", ignoreCase = true) && body.isNotEmpty()) {
                put("Content-Type", "application/json")
            }
        }
        return runCatching {
            httpRequestRaw(
                method = method.uppercase(),
                url = url,
                headers = headers,
                body = body,
            )
        }.getOrNull()
    }
}
