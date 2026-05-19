package com.nuvio.app.features.sourcecloud

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SourceCloudSearchRequestDto(
    val profileId: Int,
    val type: String,
    val videoId: String,
    val tmdbId: String? = null,
    val season: Int? = null,
    val episode: Int? = null,
)

@Serializable
internal data class SourceCloudProfileScopedRequestDto(
    val profileId: Int,
)

@Serializable
internal data class SourceCloudStatusResponseDto(
    val config: SourceCloudConfigStateDto? = null,
    val services: List<SourceCloudServiceStatusDto>? = null,
)

@Serializable
internal data class SourceCloudConfigStateDto(
    val status: String? = null,
    val label: String? = null,
    val message: String? = null,
    val advancedConfigAvailable: Boolean = false,
    val canReset: Boolean = false,
)

@Serializable
internal data class SourceCloudServiceStatusDto(
    val service: String? = null,
    val connected: Boolean = false,
    val label: String? = null,
    val message: String? = null,
)

@Serializable
internal data class SourceCloudAdvancedConfigSessionResponseDto(
    val url: String,
    val expiresAtEpochMillis: Long? = null,
    val message: String? = null,
)

@Serializable
internal data class SourceCloudSearchResponseDto(
    val streams: List<SourceCloudStreamDto>? = null,
)

@Serializable
internal data class SourceCloudStreamDto(
    val name: String? = null,
    val title: String? = null,
    val description: String? = null,
    val url: String? = null,
    val ytId: String? = null,
    val infoHash: String? = null,
    val fileIdx: Int? = null,
    val externalUrl: String? = null,
    val behaviorHints: SourceCloudBehaviorHintsDto? = null,
    val metadata: SourceCloudStreamMetadataDto? = null,
)

@Serializable
internal data class SourceCloudBehaviorHintsDto(
    val notWebReady: Boolean? = null,
    val bingeGroup: String? = null,
    val countryWhitelist: List<String>? = null,
    val requestHeaders: Map<String, String>? = null,
    val responseHeaders: Map<String, String>? = null,
    val videoHash: String? = null,
    val videoSize: Long? = null,
    val filename: String? = null,
)

@Serializable
internal data class SourceCloudStreamMetadataDto(
    val quality: String? = null,
    val sizeBytes: Long? = null,
    val codec: String? = null,
    val audio: String? = null,
    val hdr: String? = null,
    val language: String? = null,
    val cached: Boolean? = null,
    val sourceConfidence: Double? = null,
    val sourceService: String? = null,
)

/** Persistent payload for the per-profile enabled flag + connected services. */
@Serializable
internal data class SourceCloudSettingsPayload(
    val enabled: Boolean = true,
    @SerialName("connectedServices")
    val connectedServiceKeys: List<String> = emptyList(),
) {
    fun toDomain(): SourceCloudSettings = SourceCloudSettings(
        enabled = enabled,
        connectedServices = connectedServiceKeys
            .mapNotNull(SourceCloudService::fromKey)
            .toSet(),
    )

    companion object {
        fun fromDomain(settings: SourceCloudSettings): SourceCloudSettingsPayload =
            SourceCloudSettingsPayload(
                enabled = settings.enabled,
                connectedServiceKeys = settings.connectedServices
                    .map { it.key }
                    .sorted(),
            )
    }
}

internal fun SourceCloudSearchRequest.toDto(profileId: Int): SourceCloudSearchRequestDto =
    SourceCloudSearchRequestDto(
        profileId = profileId,
        type = type,
        videoId = videoId,
        tmdbId = tmdbId,
        season = season,
        episode = episode,
    )

internal fun SourceCloudStatusResponseDto.toDomain(
    enabled: Boolean,
    baseUrlConfigured: Boolean,
): SourceCloudStatus = SourceCloudStatus(
    enabled = enabled,
    baseUrlConfigured = baseUrlConfigured,
    config = config?.toDomain() ?: SourceCloudConfigState(),
    services = services.orEmpty().mapNotNull { it.toDomain() },
)

internal fun SourceCloudConfigStateDto.toDomain(): SourceCloudConfigState =
    SourceCloudConfigState(
        status = SourceCloudConfigStatus.fromKey(status),
        label = label,
        message = message,
        advancedConfigAvailable = advancedConfigAvailable,
        canReset = canReset,
    )

internal fun SourceCloudServiceStatusDto.toDomain(): SourceCloudServiceStatus? {
    val service = SourceCloudService.fromKey(service) ?: return null
    return SourceCloudServiceStatus(
        service = service,
        connected = connected,
        label = label?.takeIf { it.isNotBlank() } ?: service.displayName,
        message = message,
    )
}

internal fun SourceCloudAdvancedConfigSessionResponseDto.toDomain(): SourceCloudAdvancedConfigSession =
    SourceCloudAdvancedConfigSession(
        url = url,
        expiresAtEpochMillis = expiresAtEpochMillis,
        message = message,
    )

internal fun SourceCloudStreamDto.toDomain(): SourceCloudResolvedStream {
    val headers = behaviorHints?.requestHeaders.sanitizeHeaders()
    return SourceCloudResolvedStream(
        name = name,
        title = title,
        description = description,
        url = url,
        infoHash = infoHash,
        fileIdx = fileIdx,
        externalUrl = externalUrl,
        requestHeaders = headers,
        filename = behaviorHints?.filename,
        videoSize = behaviorHints?.videoSize,
        metadata = metadata?.toDomain(),
    )
}

internal fun SourceCloudStreamMetadataDto.toDomain(): SourceCloudStreamMetadata =
    SourceCloudStreamMetadata(
        quality = quality,
        sizeBytes = sizeBytes,
        codec = codec,
        audio = audio,
        hdr = hdr,
        language = language,
        cached = cached,
        sourceConfidence = sourceConfidence,
        sourceService = SourceCloudService.fromKey(sourceService),
    )

private fun Map<String, String>?.sanitizeHeaders(): Map<String, String>? {
    if (this.isNullOrEmpty()) return null
    val cleaned = LinkedHashMap<String, String>(size)
    forEach { (rawKey, rawValue) ->
        val key = rawKey.trim()
        val value = rawValue.trim()
        if (key.isEmpty() || value.isEmpty()) return@forEach
        if (key.equals("Range", ignoreCase = true)) return@forEach
        cleaned[key] = value
    }
    return cleaned.takeIf { it.isNotEmpty() }
}
