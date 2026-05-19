package com.nuvio.app.features.sourcecloud

import co.touchlab.kermit.Logger
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

/**
 * Singleton repository that mirrors the OmnioTV `SourceCloudRepository` +
 * `SourceCloudSettingsViewModel` pair. The UiState surface matches the
 * patterns used by other singletons (EmbyAuthRepository, TmdbSettingsRepository)
 * so the Compose layer can `collectAsStateWithLifecycle` it directly.
 */
internal object SourceCloudRepository {
    private val log = Logger.withTag("SourceCloud")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(SourceCloudUiState())
    val uiState: StateFlow<SourceCloudUiState> = _uiState.asStateFlow()

    private var hasLoaded = false
    private var settings: SourceCloudSettings = SourceCloudSettings()
    private var statusCache: SourceCloudStatus? = null

    /**
     * Loads the persisted enabled flag / connected services for the active
     * profile and emits an immediate offline snapshot. Network refresh runs
     * asynchronously so the UI is never blocked.
     */
    fun ensureLoaded() {
        if (hasLoaded) return
        loadFromDisk()
        scope.launch { refreshFromNetwork() }
    }

    fun onProfileChanged() {
        loadFromDisk()
        scope.launch { refreshFromNetwork() }
    }

    /** Whether at least one service is locally marked connected. */
    fun isConfigured(): Boolean {
        ensureLoaded()
        return settings.hasConnectedService && settings.enabled
    }

    fun currentSettings(): SourceCloudSettings {
        ensureLoaded()
        return settings
    }

    fun refresh() {
        ensureLoaded()
        scope.launch { refreshFromNetwork() }
    }

    fun setEnabled(enabled: Boolean) {
        ensureLoaded()
        settings = settings.copy(enabled = enabled)
        persist()
        publish()
        scope.launch { refreshFromNetwork() }
    }

    fun setServiceConnected(service: SourceCloudService, connected: Boolean) {
        ensureLoaded()
        val updated = settings.connectedServices.toMutableSet().apply {
            if (connected) add(service) else remove(service)
        }
        settings = settings.copy(connectedServices = updated)
        persist()
        publish()
        scope.launch { refreshFromNetwork() }
    }

    fun beginConnectService(service: SourceCloudService) {
        ensureLoaded()
        _uiState.update {
            it.copy(
                connectServiceTarget = service,
                connectServiceError = null,
                isConnectServiceSubmitting = false,
            )
        }
    }

    fun cancelConnectService() {
        _uiState.update {
            it.copy(
                connectServiceTarget = null,
                connectServiceError = null,
                isConnectServiceSubmitting = false,
            )
        }
    }

    fun submitConnectService(service: SourceCloudService, apiKey: String) {
        ensureLoaded()
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            _uiState.update { it.copy(connectServiceError = "API key is required") }
            return
        }
        _uiState.update {
            it.copy(
                isConnectServiceSubmitting = true,
                connectServiceError = null,
            )
        }
        scope.launch {
            val response = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) null
                else SourceCloudApiClient.connectService(activeProfileId(), service, trimmedKey)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Connect service failed: ${error.message}" }
            }.getOrNull()

            if (response == null) {
                _uiState.update {
                    it.copy(
                        isConnectServiceSubmitting = false,
                        connectServiceError = "Couldn't reach Source Cloud. Try again.",
                    )
                }
                return@launch
            }

            // Update local connected-set so resolveStream() can short-circuit.
            val newSet = settings.connectedServices + service
            settings = settings.copy(connectedServices = newSet)
            persist()

            val newStatus = response.toDomain(
                enabled = settings.enabled,
                baseUrlConfigured = true,
            )
            statusCache = newStatus
            _uiState.update {
                it.copy(
                    isConnectServiceSubmitting = false,
                    connectServiceTarget = null,
                    connectServiceError = null,
                    status = newStatus,
                    connectedServiceKeys = settings.connectedServices.map { it.key }.toSet(),
                )
            }
        }
    }

    fun disconnectService(service: SourceCloudService) {
        ensureLoaded()
        _uiState.update { it.copy(isLoading = true) }
        scope.launch {
            val response = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) null
                else SourceCloudApiClient.disconnectService(activeProfileId(), service)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Disconnect service failed: ${error.message}" }
            }.getOrNull()

            // Locally drop the service regardless of network result — the user
            // wanted it off; if the backend write failed the next refresh
            // re-syncs anyway.
            val newSet = settings.connectedServices - service
            settings = settings.copy(connectedServices = newSet)
            persist()

            val newStatus = response?.toDomain(
                enabled = settings.enabled,
                baseUrlConfigured = true,
            )
            if (newStatus != null) statusCache = newStatus

            _uiState.update {
                it.copy(
                    isLoading = false,
                    status = newStatus ?: it.status,
                    connectedServiceKeys = settings.connectedServices.map { it.key }.toSet(),
                    errorMessage = if (response == null) "Disconnect didn't reach Source Cloud — retry to sync" else null,
                )
            }
        }
    }

    fun requestAdvancedConfigSession(autoOpen: Boolean = false) {
        ensureLoaded()
        _uiState.update {
            it.copy(
                isAdvancedConfigLoading = true,
                advancedConfigSession = null,
                pendingAdvancedSessionOpen = autoOpen,
                errorMessage = null,
            )
        }
        scope.launch {
            val session = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) {
                    null
                } else {
                    SourceCloudApiClient.createAdvancedConfigSession(activeProfileId())?.toDomain()
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Advanced session failed: ${error.message}" }
            }.getOrNull()

            _uiState.update {
                it.copy(
                    isAdvancedConfigLoading = false,
                    advancedConfigSession = session,
                    pendingAdvancedSessionOpen = autoOpen && session != null,
                    errorMessage = if (session == null) {
                        SOURCE_CLOUD_ADVANCED_UNAVAILABLE_MESSAGE
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun consumePendingAdvancedSessionOpen() {
        _uiState.update { it.copy(pendingAdvancedSessionOpen = false) }
    }

    fun resetConfig() {
        ensureLoaded()
        _uiState.update {
            it.copy(
                isLoading = true,
                advancedConfigSession = null,
                errorMessage = null,
            )
        }
        scope.launch {
            val status = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) {
                    offlineStatus(false)
                } else {
                    SourceCloudApiClient.resetConfig(activeProfileId())?.toDomain(
                        enabled = settings.enabled,
                        baseUrlConfigured = true,
                    ) ?: offlineStatus(true)
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Reset failed: ${error.message}" }
            }.getOrElse { offlineStatus(SourceCloudApiClient.baseUrlConfigured) }

            statusCache = status
            _uiState.update {
                it.copy(
                    isLoading = false,
                    status = status,
                    errorMessage = null,
                )
            }
        }
    }

    fun consumeAdvancedSession() {
        _uiState.update {
            it.copy(
                advancedConfigSession = null,
                pendingAdvancedSessionOpen = false,
            )
        }
    }

    /** Resolve all streams from Source Cloud (already ranked + deduped by AIOStreams). */
    suspend fun resolveStreams(request: SourceCloudSearchRequest): List<SourceCloudResolvedStream> {
        ensureLoaded()
        if (!SourceCloudApiClient.baseUrlConfigured) return emptyList()
        val current = settings
        if (!current.enabled || !current.hasConnectedService) return emptyList()
        val response = runCatching { SourceCloudApiClient.search(request, activeProfileId()) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Search failed: ${error.message}" }
            }
            .getOrNull() ?: return emptyList()
        return response.streams.orEmpty().mapNotNull { dto ->
            val resolved = dto.toDomain()
            if (resolved.url.isNullOrBlank() &&
                resolved.infoHash.isNullOrBlank() &&
                resolved.externalUrl.isNullOrBlank()
            ) null else resolved
        }
    }

    fun clearLocalState() {
        settings = SourceCloudSettings()
        statusCache = null
        hasLoaded = false
        persist()
        _uiState.value = SourceCloudUiState()
    }

    private suspend fun refreshFromNetwork() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        val baseConfigured = SourceCloudApiClient.baseUrlConfigured
        val status = if (!baseConfigured) {
            offlineStatus(false)
        } else {
            runCatching { SourceCloudApiClient.status(activeProfileId()) }
                .onFailure { error ->
                    if (error is CancellationException) throw error
                    log.w { "Status fetch failed: ${error.message}" }
                }
                .getOrNull()
                ?.toDomain(enabled = settings.enabled, baseUrlConfigured = true)
                ?: offlineStatus(true)
        }
        statusCache = status
        _uiState.update {
            it.copy(
                isLoading = false,
                status = status,
                errorMessage = null,
            )
        }
    }

    private fun loadFromDisk() {
        hasLoaded = true
        val raw = SourceCloudAuthStorage.loadPayload().orEmpty().trim()
        settings = if (raw.isBlank()) {
            SourceCloudSettings()
        } else {
            runCatching { SourceCloudApiClient.json.decodeFromString<SourceCloudSettingsPayload>(raw) }
                .getOrElse {
                    log.w { "Failed to decode Source Cloud payload: ${it.message}" }
                    SourceCloudSettingsPayload()
                }
                .toDomain()
        }
        publish()
    }

    private fun persist() {
        val payload = SourceCloudSettingsPayload.fromDomain(settings)
        SourceCloudAuthStorage.savePayload(SourceCloudApiClient.json.encodeToString(payload))
    }

    private fun publish() {
        _uiState.update {
            it.copy(
                status = statusCache ?: offlineStatus(SourceCloudApiClient.baseUrlConfigured),
                enabled = settings.enabled,
                connectedServiceKeys = settings.connectedServices.map { it.key }.toSet(),
            )
        }
    }

    fun refreshConfigSummary() {
        ensureLoaded()
        _uiState.update { it.copy(isConfigSummaryLoading = true, configSummaryError = null) }
        scope.launch {
            val response = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) null
                else SourceCloudApiClient.getConfigSummary(activeProfileId())
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Config summary fetch failed: ${error.message}" }
            }.getOrNull()

            if (response == null) {
                _uiState.update {
                    it.copy(
                        isConfigSummaryLoading = false,
                        configSummaryError = "Couldn't load API keys",
                    )
                }
                return@launch
            }

            val summary = SourceCloudConfigSummary(
                tmdbApiKey = response.tmdbApiKey.orEmpty(),
                tmdbAccessToken = response.tmdbAccessToken.orEmpty(),
                tvdbApiKey = response.tvdbApiKey.orEmpty(),
                rpdbApiKey = response.rpdbApiKey.orEmpty(),
                animeToshoEnabled = response.animeToshoEnabled,
                debridioApiKey = response.debridioApiKey.orEmpty(),
                provisioned = response.provisioned,
            )
            _uiState.update {
                it.copy(
                    isConfigSummaryLoading = false,
                    configSummary = summary,
                    configSummaryError = null,
                )
            }
        }
    }

    fun saveConfigSummary(updated: SourceCloudConfigSummary) {
        ensureLoaded()
        val current = _uiState.value.configSummary
        // Only send fields that actually changed. Empty → null clears server-side.
        fun diffStr(now: String, before: String?): String? {
            if (now == (before ?: "")) return null // unchanged → don't include in request (handled below)
            return if (now.isEmpty()) "" else now
        }

        val request = SourceCloudUpdateConfigRequestDto(
            profileId = activeProfileId(),
            tmdbApiKey = diffStr(updated.tmdbApiKey, current?.tmdbApiKey),
            tmdbAccessToken = diffStr(updated.tmdbAccessToken, current?.tmdbAccessToken),
            tvdbApiKey = diffStr(updated.tvdbApiKey, current?.tvdbApiKey),
            rpdbApiKey = diffStr(updated.rpdbApiKey, current?.rpdbApiKey),
            animeToshoEnabled = if (updated.animeToshoEnabled != current?.animeToshoEnabled) updated.animeToshoEnabled else null,
            debridioApiKey = diffStr(updated.debridioApiKey, current?.debridioApiKey),
        )

        _uiState.update { it.copy(isConfigSummarySaving = true, configSummaryError = null) }
        scope.launch {
            val response = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) null
                else SourceCloudApiClient.updateConfig(request)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Update config failed: ${error.message}" }
            }.getOrNull()

            if (response == null) {
                _uiState.update {
                    it.copy(
                        isConfigSummarySaving = false,
                        configSummaryError = "Couldn't save — try again",
                    )
                }
                return@launch
            }
            val newStatus = response.toDomain(
                enabled = settings.enabled,
                baseUrlConfigured = true,
            )
            statusCache = newStatus
            _uiState.update {
                it.copy(
                    isConfigSummarySaving = false,
                    status = newStatus,
                    configSummary = updated,
                    configSummaryError = null,
                )
            }
        }
    }

    private fun activeProfileId(): Int = ProfileRepository.activeProfileId

    private fun offlineStatus(baseUrlConfigured: Boolean): SourceCloudStatus = SourceCloudStatus(
        enabled = settings.enabled,
        baseUrlConfigured = baseUrlConfigured,
        config = SourceCloudConfigState(),
        services = SourceCloudService.entries.map { service ->
            SourceCloudServiceStatus(
                service = service,
                connected = service in settings.connectedServices,
                label = service.displayName,
                message = null,
            )
        },
    )
}

internal const val SOURCE_CLOUD_ADVANCED_UNAVAILABLE_MESSAGE =
    "Advanced Source Config is unavailable right now."

data class SourceCloudConfigSummary(
    val tmdbApiKey: String = "",
    val tmdbAccessToken: String = "",
    val tvdbApiKey: String = "",
    val rpdbApiKey: String = "",
    val animeToshoEnabled: Boolean = false,
    val debridioApiKey: String = "",
    val provisioned: Boolean = false,
)

data class SourceCloudUiState(
    val isLoading: Boolean = false,
    val isAdvancedConfigLoading: Boolean = false,
    val status: SourceCloudStatus? = null,
    val advancedConfigSession: SourceCloudAdvancedConfigSession? = null,
    val pendingAdvancedSessionOpen: Boolean = false,
    val errorMessage: String? = null,
    val enabled: Boolean = true,
    val connectedServiceKeys: Set<String> = emptySet(),
    val connectServiceTarget: SourceCloudService? = null,
    val isConnectServiceSubmitting: Boolean = false,
    val connectServiceError: String? = null,
    val configSummary: SourceCloudConfigSummary? = null,
    val isConfigSummaryLoading: Boolean = false,
    val isConfigSummarySaving: Boolean = false,
    val configSummaryError: String? = null,
)
