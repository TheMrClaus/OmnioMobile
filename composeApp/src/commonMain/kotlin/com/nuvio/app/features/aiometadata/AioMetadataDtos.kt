package com.nuvio.app.features.aiometadata

import kotlinx.serialization.Serializable

@Serializable
internal data class AioMetadataProvisionProfileRequestDto(
    val profileId: Int,
    val kids: Boolean,
    val copyKeysFromMain: Boolean,
    val maxAgeRating: String? = null,
)

@Serializable
internal data class AioMetadataProvisionProfileResponseDto(
    val aioUuid: String? = null,
    val manifestUrl: String? = null,
    val reused: Boolean = false,
)
