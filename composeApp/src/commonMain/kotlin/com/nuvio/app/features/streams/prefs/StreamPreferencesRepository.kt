package com.nuvio.app.features.streams.prefs

import co.touchlab.kermit.Logger
import com.nuvio.app.core.storage.ProfileScopedKey
import com.nuvio.app.features.profiles.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal object StreamPreferencesRepository {
    private val log = Logger.withTag("StreamPrefsRepo")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val storageKey = ProfileScopedKey.of("stream_preferences_json")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _uiState = MutableStateFlow(StreamPreferences.DEFAULT)
    val uiState: StateFlow<StreamPreferences> = _uiState.asStateFlow()

    private var hasLoaded = false

    fun ensureLoaded() {
        if (hasLoaded) return
        hasLoaded = true
        val payload = StreamPreferencesStorage.loadPayload(storageKey).orEmpty().trim()
        if (payload.isNotEmpty()) {
            _uiState.value = runCatching {
                json.decodeFromString<StreamPreferences>(payload)
            }.getOrDefault(StreamPreferences.DEFAULT)
        }
    }

    fun onProfileChanged() {
        hasLoaded = false
        _uiState.value = StreamPreferences.DEFAULT
        ensureLoaded()
    }

    fun clearLocalState() {
        hasLoaded = false
        _uiState.value = StreamPreferences.DEFAULT
    }

    fun setPreferences(prefs: StreamPreferences) {
        _uiState.value = prefs
        persist()
    }

    fun update(transform: (StreamPreferences) -> StreamPreferences) {
        _uiState.value = transform(_uiState.value)
        persist()
    }

    private fun persist() {
        StreamPreferencesStorage.savePayload(
            storageKey,
            json.encodeToString(_uiState.value),
        )
    }
}
