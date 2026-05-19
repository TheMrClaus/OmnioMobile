package com.nuvio.app.features.sourcecloud

import com.nuvio.app.core.network.SupabaseConfig
import com.nuvio.app.core.network.SupabaseProvider
import com.nuvio.app.features.addons.RawHttpResponse
import com.nuvio.app.features.addons.httpRequestRaw
import io.github.jan.supabase.auth.auth
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * HTTP client for the Omnio Source Cloud Supabase Edge Functions.
 *
 * OmnioTV reaches the same endpoints via Retrofit + a dedicated OkHttpClient
 * that injects the Supabase apikey and bearer token. The mobile fork mirrors
 * the wire-level approach (instead of supabase-kt's `functions.invoke`) so the
 * call sites remain ergonomic and the behavior stays predictable across
 * supabase-kt versions.
 */
internal object SourceCloudApiClient {
    internal val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    val baseUrlConfigured: Boolean
        get() = SupabaseConfig.URL.isNotBlank() && SupabaseConfig.ANON_KEY.isNotBlank()

    suspend fun status(profileId: Int): SourceCloudStatusResponseDto? {
        val response = request(
            method = "GET",
            function = "source-cloud-status",
            query = mapOf("profileId" to profileId.toString()),
        ) ?: return null
        return decodeOrNull(response)
    }

    suspend fun search(
        searchRequest: SourceCloudSearchRequest,
        profileId: Int,
    ): SourceCloudSearchResponseDto? {
        val body = json.encodeToString(searchRequest.toDto(profileId))
        val response = request(method = "POST", function = "source-cloud-search", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun createAdvancedConfigSession(
        profileId: Int,
    ): SourceCloudAdvancedConfigSessionResponseDto? {
        val body = json.encodeToString(SourceCloudProfileScopedRequestDto(profileId))
        val response = request(method = "POST", function = "source-cloud-advanced-session", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun resetConfig(profileId: Int): SourceCloudStatusResponseDto? {
        val body = json.encodeToString(SourceCloudProfileScopedRequestDto(profileId))
        val response = request(method = "POST", function = "source-cloud-reset", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun connectService(
        profileId: Int,
        service: SourceCloudService,
        apiKey: String,
    ): SourceCloudStatusResponseDto? {
        val body = json.encodeToString(
            SourceCloudConnectServiceRequestDto(
                profileId = profileId,
                service = service.key,
                apiKey = apiKey,
            ),
        )
        val response = request(method = "POST", function = "source-cloud-connect-service", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun disconnectService(
        profileId: Int,
        service: SourceCloudService,
    ): SourceCloudStatusResponseDto? {
        val body = json.encodeToString(
            SourceCloudDisconnectServiceRequestDto(
                profileId = profileId,
                service = service.key,
            ),
        )
        val response = request(method = "POST", function = "source-cloud-disconnect-service", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun getConfigSummary(profileId: Int): SourceCloudConfigSummaryResponseDto? {
        val body = json.encodeToString(SourceCloudConfigSummaryRequestDto(profileId))
        val response = request(method = "POST", function = "source-cloud-get-config-summary", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun updateConfig(request: SourceCloudUpdateConfigRequestDto): SourceCloudStatusResponseDto? {
        val body = json.encodeToString(request)
        val response = this.request(method = "POST", function = "source-cloud-update-config", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    private suspend fun request(
        method: String,
        function: String,
        body: String = "",
        query: Map<String, String> = emptyMap(),
    ): RawHttpResponse? {
        if (!baseUrlConfigured) return null
        val base = "${SupabaseConfig.URL.trimEnd('/')}/functions/v1/$function"
        val url = if (query.isEmpty()) base else {
            base + query.entries.joinToString(prefix = "?", separator = "&") { (k, v) ->
                "${encodeQuery(k)}=${encodeQuery(v)}"
            }
        }
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

    private inline fun <reified T> decodeOrNull(response: RawHttpResponse): T? {
        if (response.status !in 200..299) return null
        val raw = response.body
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull()
    }

    private fun encodeQuery(value: String): String {
        val builder = StringBuilder(value.length)
        for (c in value) {
            when {
                c.isLetterOrDigit() || c == '-' || c == '_' || c == '.' || c == '~' -> builder.append(c)
                else -> {
                    val bytes = c.toString().encodeToByteArray()
                    for (b in bytes) {
                        builder.append('%')
                        builder.append(((b.toInt() and 0xFF) shr 4).toString(16).uppercase())
                        builder.append((b.toInt() and 0x0F).toString(16).uppercase())
                    }
                }
            }
        }
        return builder.toString()
    }
}
