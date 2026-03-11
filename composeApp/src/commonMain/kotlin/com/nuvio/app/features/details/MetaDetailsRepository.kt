package com.nuvio.app.features.details

import com.nuvio.app.features.addons.AddonManifest
import com.nuvio.app.features.addons.AddonRepository
import com.nuvio.app.features.addons.httpGetText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object MetaDetailsRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _uiState = MutableStateFlow(MetaDetailsUiState())
    val uiState: StateFlow<MetaDetailsUiState> = _uiState.asStateFlow()

    fun load(type: String, id: String) {
        _uiState.value = MetaDetailsUiState(isLoading = true)

        scope.launch {
            val manifests = AddonRepository.uiState.value.addons
                .mapNotNull { it.manifest }
                .filter { manifest ->
                    manifest.resources.any { resource ->
                        resource.name == "meta" &&
                            resource.types.contains(type) &&
                            (resource.idPrefixes.isEmpty() || resource.idPrefixes.any { id.startsWith(it) })
                    }
                }

            if (manifests.isEmpty()) {
                _uiState.value = MetaDetailsUiState(
                    errorMessage = "No addon provides meta for this content.",
                )
                return@launch
            }

            for (manifest in manifests) {
                val result = tryFetchMeta(manifest, type, id)
                if (result != null) {
                    _uiState.value = MetaDetailsUiState(meta = result)
                    return@launch
                }
            }

            _uiState.value = MetaDetailsUiState(
                errorMessage = "Could not load details from any addon.",
            )
        }
    }

    fun clear() {
        _uiState.value = MetaDetailsUiState()
    }

    private suspend fun tryFetchMeta(
        manifest: AddonManifest,
        type: String,
        id: String,
    ): MetaDetails? {
        return try {
            val baseUrl = manifest.transportUrl
                .substringBefore("?")
                .removeSuffix("/manifest.json")
            val url = "$baseUrl/meta/$type/$id.json"
            val payload = httpGetText(url)
            MetaDetailsParser.parse(payload)
        } catch (_: Throwable) {
            null
        }
    }
}
