package com.nuvio.app.features.aiometadata

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException

/**
 * Thin one-shot repository for AIOMetadata profile provisioning. Unlike
 * [com.nuvio.app.features.sourcecloud.SourceCloudRepository], Stage 2 has
 * no runtime read-paths to maintain (no StateFlow, no settings cache) — the
 * mobile app does not consume the per-profile `manifestUrl` today, and kids
 * gating is handled separately by [com.nuvio.app.features.profiles.ProfileContentFilter].
 *
 * If/when mobile starts swapping the AIOMetadata addon manifest per profile
 * (OmnioTV does this; mobile currently does not), this object grows similar
 * cache/state machinery.
 */
internal object AioMetadataRepository {
    private val log = Logger.withTag("AioMetadata")

    sealed class ProvisionResult {
        data class Success(
            val aioUuid: String?,
            val manifestUrl: String?,
            val reused: Boolean,
        ) : ProvisionResult()

        data class Failure(val message: String) : ProvisionResult()
    }

    /**
     * Provision (or re-link) an AIOMetadata config for a freshly-created
     * profile. Idempotent server-side: re-calling for the same profile
     * returns the existing link with `reused: true`.
     *
     * For kids profiles the server requires Main to already have an
     * AIOMetadata config (returns 412 otherwise). Per Stage 2 design we
     * mirror the server contract — we do NOT auto-provision Main on the
     * fly. Failures surface to the profile-creation modal so the user can
     * configure Main first and retry.
     */
    suspend fun provisionForNewProfile(
        profileId: Int,
        isKids: Boolean,
        copyKeysFromMain: Boolean,
        kidsMaxAgeRating: String? = null,
    ): ProvisionResult {
        if (!AioMetadataApiClient.baseUrlConfigured) {
            return ProvisionResult.Failure("AIOMetadata is not configured")
        }
        val response = runCatching {
            AioMetadataApiClient.provisionProfile(
                profileId = profileId,
                kids = isKids,
                copyKeysFromMain = copyKeysFromMain,
                maxAgeRating = kidsMaxAgeRating.takeIf { isKids },
            )
        }.onFailure { error ->
            if (error is CancellationException) throw error
            log.w { "Provision profile failed: ${error.message}" }
        }.getOrNull() ?: return ProvisionResult.Failure("Couldn't reach AIOMetadata")

        if (response.status !in 200..299) {
            val message = when (response.status) {
                412 -> if (isKids) {
                    "Set up the Main profile's AIOMetadata first, then retry from this profile's settings."
                } else {
                    "AIOMetadata isn't ready for this profile yet."
                }
                in 400..499 -> "AIOMetadata rejected the request (HTTP ${response.status})"
                else -> "AIOMetadata provisioning failed (HTTP ${response.status})"
            }
            log.w { "Provision profile non-2xx status=${response.status} body=${response.body.take(200)}" }
            return ProvisionResult.Failure(message)
        }

        val decoded = AioMetadataApiClient.decodeOrNull<AioMetadataProvisionProfileResponseDto>(response)
            ?: return ProvisionResult.Failure("AIOMetadata returned an unexpected response")

        return ProvisionResult.Success(
            aioUuid = decoded.aioUuid,
            manifestUrl = decoded.manifestUrl,
            reused = decoded.reused,
        )
    }
}
