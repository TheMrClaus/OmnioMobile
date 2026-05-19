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

    suspend fun status(): SourceCloudStatusResponseDto? {
        val response = request(method = "GET", function = "source-cloud-status") ?: return null
        return decodeOrNull(response)
    }

    suspend fun search(searchRequest: SourceCloudSearchRequest): SourceCloudSearchResponseDto? {
        val body = json.encodeToString(searchRequest.toDto())
        val response = request(method = "POST", function = "source-cloud-search", body = body)
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun createAdvancedConfigSession(): SourceCloudAdvancedConfigSessionResponseDto? {
        val response = request(method = "POST", function = "source-cloud-advanced-session")
            ?: return null
        return decodeOrNull(response)
    }

    suspend fun resetConfig(): SourceCloudStatusResponseDto? {
        val response = request(method = "POST", function = "source-cloud-reset") ?: return null
        return decodeOrNull(response)
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

    private inline fun <reified T> decodeOrNull(response: RawHttpResponse): T? {
        if (response.status !in 200..299) return null
        val raw = response.body
        if (raw.isBlank()) return null
        return runCatching { json.decodeFromString<T>(raw) }.getOrNull()
    }
}
