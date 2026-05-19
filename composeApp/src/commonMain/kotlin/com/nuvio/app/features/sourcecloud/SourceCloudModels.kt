package com.nuvio.app.features.sourcecloud

/**
 * Domain models for the Omnio Source Cloud feature, ported from the OmnioTV
 * `core-domain/.../SourceCloud.kt` types. Phrased to be platform-neutral so the
 * same shape can flow between commonMain UI and the platform storage layer.
 */
enum class SourceCloudService(val key: String, val displayName: String) {
    REAL_DEBRID("real_debrid", "Real-Debrid"),
    TORBOX("torbox", "Torbox"),
    ;

    companion object {
        fun fromKey(key: String?): SourceCloudService? =
            key?.let { lookup -> entries.firstOrNull { it.key == lookup } }
    }
}

enum class SourceCloudConfigStatus(val key: String) {
    UNKNOWN("unknown"),
    NOT_PROVISIONED("not_provisioned"),
    READY("ready"),
    PROVISIONING_FAILED("provisioning_failed"),
    UNAVAILABLE("unavailable"),
    INVALID("invalid"),
    ;

    companion object {
        fun fromKey(key: String?): SourceCloudConfigStatus =
            entries.firstOrNull { it.key == key } ?: UNKNOWN
    }
}

data class SourceCloudSettings(
    val enabled: Boolean = true,
    val connectedServices: Set<SourceCloudService> = emptySet(),
) {
    val hasConnectedService: Boolean
        get() = connectedServices.isNotEmpty()
}

data class SourceCloudConfigState(
    val status: SourceCloudConfigStatus = SourceCloudConfigStatus.UNKNOWN,
    val label: String? = null,
    val message: String? = null,
    val advancedConfigAvailable: Boolean = false,
    val canReset: Boolean = false,
)

data class SourceCloudServiceStatus(
    val service: SourceCloudService,
    val connected: Boolean,
    val label: String = service.displayName,
    val message: String? = null,
)

data class SourceCloudStatus(
    val enabled: Boolean,
    val baseUrlConfigured: Boolean,
    val config: SourceCloudConfigState = SourceCloudConfigState(),
    val services: List<SourceCloudServiceStatus> = emptyList(),
) {
    val hasConnectedService: Boolean
        get() = services.any { it.connected }
}

data class SourceCloudAdvancedConfigSession(
    val url: String,
    val expiresAtEpochMillis: Long? = null,
    val message: String? = null,
)

data class SourceCloudSearchRequest(
    val type: String,
    val videoId: String,
    val tmdbId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
)

data class SourceCloudStreamMetadata(
    val quality: String? = null,
    val sizeBytes: Long? = null,
    val codec: String? = null,
    val audio: String? = null,
    val hdr: String? = null,
    val language: String? = null,
    val cached: Boolean? = null,
    val sourceConfidence: Double? = null,
    val sourceService: SourceCloudService? = null,
)

data class SourceCloudResolvedStream(
    val name: String?,
    val title: String?,
    val description: String?,
    val url: String?,
    val infoHash: String?,
    val fileIdx: Int?,
    val externalUrl: String?,
    val requestHeaders: Map<String, String>?,
    val filename: String?,
    val videoSize: Long?,
    val metadata: SourceCloudStreamMetadata?,
)

/** Identifier used everywhere stream provenance is tagged. */
const val SOURCE_CLOUD_PROVIDER = "source_cloud"

/** Display label used for the Source Cloud stream group in the streams screen. */
const val SOURCE_CLOUD_GROUP_NAME = "Omnio Source Cloud"

/** Stable addonId for the synthetic Source Cloud stream group. */
const val SOURCE_CLOUD_ADDON_ID = "source_cloud"
