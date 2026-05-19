package com.nuvio.app.features.sourcecloud

import co.touchlab.kermit.Logger
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

    fun requestAdvancedConfigSession() {
        ensureLoaded()
        _uiState.update {
            it.copy(
                isAdvancedConfigLoading = true,
                advancedConfigSession = null,
                errorMessage = null,
            )
        }
        scope.launch {
            val session = runCatching {
                if (!SourceCloudApiClient.baseUrlConfigured) {
                    null
                } else {
                    SourceCloudApiClient.createAdvancedConfigSession()?.toDomain()
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Advanced session failed: ${error.message}" }
            }.getOrNull()

            _uiState.update {
                it.copy(
                    isAdvancedConfigLoading = false,
                    advancedConfigSession = session,
                    errorMessage = if (session == null) {
                        SOURCE_CLOUD_ADVANCED_UNAVAILABLE_MESSAGE
                    } else {
                        null
                    },
                )
            }
        }
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
                    SourceCloudApiClient.resetConfig()?.toDomain(
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
        _uiState.update { it.copy(advancedConfigSession = null) }
    }

    /** Resolve a stream from Source Cloud, if enabled and at least one service is connected. */
    suspend fun resolveStream(request: SourceCloudSearchRequest): SourceCloudResolvedStream? {
        ensureLoaded()
        if (!SourceCloudApiClient.baseUrlConfigured) return null
        val current = settings
        if (!current.enabled || !current.hasConnectedService) return null
        val response = runCatching { SourceCloudApiClient.search(request) }
            .onFailure { error ->
                if (error is CancellationException) throw error
                log.w { "Search failed: ${error.message}" }
            }
            .getOrNull() ?: return null
        return response.streams.orEmpty().firstNotNullOfOrNull { dto ->
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
            runCatching { SourceCloudApiClient.status() }
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

data class SourceCloudUiState(
    val isLoading: Boolean = false,
    val isAdvancedConfigLoading: Boolean = false,
    val status: SourceCloudStatus? = null,
    val advancedConfigSession: SourceCloudAdvancedConfigSession? = null,
    val errorMessage: String? = null,
    val enabled: Boolean = true,
    val connectedServiceKeys: Set<String> = emptySet(),
)
