package com.nuvio.app.features.emby

import co.touchlab.kermit.Logger
import com.nuvio.app.features.profiles.ProfileRepository
import com.nuvio.app.features.trakt.TraktPlatformClock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Per-profile Emby authentication state.
 *
 * Flow:
 *  1. UI calls [signIn] with serverUrl/username/password.
 *  2. Repository POSTs to `Users/AuthenticateByName` to get an accessToken+userId,
 *     then GETs `System/Info` to display the server name.
 *  3. The resulting [EmbyAuthPayload] is persisted via [EmbyAuthStorage] under the
 *     active profile's scoped key.
 *  4. Other Emby services read [currentPayload] (or pass it explicitly) to make API calls.
 *
 *  Profile changes refresh from disk; [copyFromMain] mirrors the OmnioTV "Copy from Main"
 *  helper but mints a new deviceId so each profile shows up as its own client.
 */
@OptIn(ExperimentalUuidApi::class)
internal object EmbyAuthRepository {
    private const val MAIN_PROFILE_ID = 1

    private val log = Logger.withTag("EmbyAuth")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(EmbyAuthUiState())
    val uiState: StateFlow<EmbyAuthUiState> = _uiState.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private var hasLoaded = false
    private var payload: EmbyAuthPayload = EmbyAuthPayload()

    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
    }

    fun onProfileChanged() {
        loadFromDisk()
    }

    fun snapshot(): EmbyAuthUiState {
        ensureLoaded()
        return _uiState.value
    }

    fun currentPayload(): EmbyAuthPayload? {
        ensureLoaded()
        return payload.takeIf { it.isConfigured }
    }

    fun isConfigured(): Boolean {
        ensureLoaded()
        return payload.isConfigured
    }

    /** Whether the active profile is non-Main, has no creds yet, and Main has creds to copy. */
    fun canCopyFromMain(): Boolean {
        ensureLoaded()
        val activeId = ProfileRepository.activeProfileId
        if (activeId == MAIN_PROFILE_ID) return false
        if (payload.isConfigured) return false
        val mainPayload = decodePayloadForProfile(MAIN_PROFILE_ID) ?: return false
        return mainPayload.isConfigured
    }

    fun onSignInRequested(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        ensureLoaded()
        val normalizedServerUrl = serverUrl.trim().trimEnd('/')
        val normalizedUsername = username.trim()
        if (normalizedServerUrl.isBlank() || normalizedUsername.isBlank()) {
            publish(
                isLoading = false,
                statusMessage = null,
                errorMessage = "Server URL and username are required",
                isStatusSuccess = false,
            )
            return
        }
        scope.launch {
            performSignIn(
                serverUrl = normalizedServerUrl,
                username = normalizedUsername,
                password = password,
            )
        }
    }

    fun onCopyFromMainRequested() {
        ensureLoaded()
        if (!canCopyFromMain()) return
        scope.launch {
            performCopyFromMain()
        }
    }

    fun onDisconnectRequested() {
        ensureLoaded()
        payload = EmbyAuthPayload()
        persist()
        publish(
            isLoading = false,
            statusMessage = "Disconnected from Emby",
            errorMessage = null,
            isStatusSuccess = true,
        )
    }

    private suspend fun performSignIn(
        serverUrl: String,
        username: String,
        password: String,
    ) {
        publish(isLoading = true, statusMessage = null, errorMessage = null, isStatusSuccess = false)

        val deviceId = payload.deviceId.takeIf { it.isNotBlank() }
            ?: Uuid.random().toString()

        val authResponse = runCatching {
            EmbyApiClient.authenticateByName(
                serverUrl = serverUrl,
                deviceId = deviceId,
                username = username,
                password = password,
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w { "Emby sign-in HTTP error: ${error.message}" }
        }.getOrNull()

        if (authResponse == null) {
            publish(
                isLoading = false,
                statusMessage = null,
                errorMessage = "Could not reach Emby server",
                isStatusSuccess = false,
            )
            return
        }

        if (authResponse.status !in 200..299) {
            val message = when (authResponse.status) {
                401 -> "Invalid username or password"
                else -> "Sign-in failed (HTTP ${authResponse.status})"
            }
            publish(
                isLoading = false,
                statusMessage = null,
                errorMessage = message,
                isStatusSuccess = false,
            )
            return
        }

        val parsed = runCatching {
            EmbyApiClient.json.decodeFromString<EmbyAuthResponse>(authResponse.body)
        }.getOrNull()

        if (parsed == null || parsed.accessToken.isBlank() || parsed.user.id.isBlank()) {
            publish(
                isLoading = false,
                statusMessage = null,
                errorMessage = "Sign-in succeeded but the response was malformed",
                isStatusSuccess = false,
            )
            return
        }

        // Provisional payload so System/Info can be fetched with the new token.
        val provisional = EmbyAuthPayload(
            serverUrl = serverUrl,
            accessToken = parsed.accessToken,
            userId = parsed.user.id,
            deviceId = deviceId,
            username = parsed.user.name ?: username,
            serverName = "",
        )

        val serverName = runCatching {
            val response = EmbyApiClient.getSystemInfo(provisional)
            if (response.status in 200..299) {
                EmbyApiClient.json.decodeFromString<EmbySystemInfoDto>(response.body).serverName
            } else null
        }.getOrNull().orEmpty()

        payload = provisional.copy(serverName = serverName.ifBlank { "Emby Server" })
        persist()
        publish(
            isLoading = false,
            statusMessage = "Connected to ${payload.serverName} as ${payload.username}",
            errorMessage = null,
            isStatusSuccess = true,
        )
    }

    private suspend fun performCopyFromMain() {
        publish(isLoading = true, statusMessage = null, errorMessage = null, isStatusSuccess = false)
        val mainPayload = decodePayloadForProfile(MAIN_PROFILE_ID)
        if (mainPayload == null || !mainPayload.isConfigured) {
            publish(
                isLoading = false,
                statusMessage = null,
                errorMessage = "Main profile has no Emby credentials to copy",
                isStatusSuccess = false,
            )
            return
        }
        // Mint a fresh deviceId so the Emby server sees this profile as its own client.
        payload = mainPayload.copy(deviceId = Uuid.random().toString())
        persist()
        publish(
            isLoading = false,
            statusMessage = "Emby credentials copied from Main",
            errorMessage = null,
            isStatusSuccess = true,
        )
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val raw = EmbyAuthStorage.loadPayload().orEmpty().trim()
        payload = if (raw.isBlank()) {
            EmbyAuthPayload()
        } else {
            runCatching { EmbyApiClient.json.decodeFromString<EmbyAuthPayload>(raw) }
                .getOrElse {
                    log.w { "Failed to parse Emby auth payload: ${it.message}" }
                    EmbyAuthPayload()
                }
        }
        publish(isLoading = false, statusMessage = null, errorMessage = null, isStatusSuccess = false)
    }

    private fun decodePayloadForProfile(profileId: Int): EmbyAuthPayload? {
        val raw = EmbyAuthStorage.loadPayloadForProfile(profileId).orEmpty().trim()
        if (raw.isBlank()) return null
        return runCatching {
            EmbyApiClient.json.decodeFromString<EmbyAuthPayload>(raw)
        }.getOrNull()
    }

    private fun persist() {
        EmbyAuthStorage.savePayload(EmbyApiClient.json.encodeToString(payload))
    }

    private fun publish(
        isLoading: Boolean,
        statusMessage: String?,
        errorMessage: String?,
        isStatusSuccess: Boolean,
    ) {
        _isAuthenticated.value = payload.isConfigured
        _uiState.value = EmbyAuthUiState(
            isConfigured = payload.isConfigured,
            serverUrl = payload.serverUrl,
            username = payload.username,
            serverName = payload.serverName,
            isLoading = isLoading,
            statusMessage = statusMessage,
            errorMessage = errorMessage,
            isStatusSuccess = isStatusSuccess,
            canCopyFromMain = canCopyFromMain(),
            updatedAtMs = TraktPlatformClock.nowEpochMs(),
        )
    }
}

data class EmbyAuthUiState(
    val isConfigured: Boolean = false,
    val serverUrl: String = "",
    val username: String = "",
    val serverName: String = "",
    val isLoading: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val isStatusSuccess: Boolean = false,
    val canCopyFromMain: Boolean = false,
    val updatedAtMs: Long = 0L,
)
