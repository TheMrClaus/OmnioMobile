package com.nuvio.app.features.emby

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.decodeFromString

/**
 * Resolved Emby stream for a given content lookup. Contains everything callers need
 * to (a) play the file via direct streaming and (b) report sessions back to the
 * Emby server (item id + media source id).
 */
data class EmbyResolvedStream(
    val streamUrl: String,
    val streamHeaders: Map<String, String>,
    val displayName: String,
    val itemId: String,
    val mediaSourceId: String,
    val runTimeMs: Long?,
    val resumePositionMs: Long?,
    val serverName: String,
)

/**
 * Stateless lookup of Emby items by IMDb / TMDB provider IDs.
 *
 * For movies: searches `IncludeItemTypes=Movie` with `AnyProviderIdEquals=imdb.tt...`.
 * For series episodes: first finds the matching `IncludeItemTypes=Series`, then queries
 * `Shows/{seriesId}/Episodes?Season=N` and matches by `IndexNumber`.
 *
 * Direct play only: stream URL is `${serverUrl}/Videos/${itemId}/stream?static=true`
 * with the access token provided as the `X-Emby-Token` header (NOT a query param) so
 * the token is never logged or leaked through URL history.
 */
internal object EmbyMediaService {
    private const val EMBY_PROVIDER = "emby"
    private const val TICKS_PER_MS = 10_000L

    private val log = Logger.withTag("EmbyMediaService")

    fun providerName(): String = EMBY_PROVIDER

    suspend fun findEmbyStream(
        videoId: String?,
        contentType: String?,
        season: Int?,
        episode: Int?,
    ): EmbyResolvedStream? {
        if (videoId.isNullOrBlank()) return null
        val payload = EmbyAuthRepository.currentPayload() ?: return null

        val providerFilter = buildProviderIdFilter(videoId)
        if (providerFilter.isBlank()) {
            log.d { "No usable provider IDs in '$videoId'; skipping Emby lookup." }
            return null
        }

        val normalizedType = contentType?.lowercase()
        val isEpisode = normalizedType in setOf("series", "show", "tv") &&
            season != null && episode != null
        val includeItemTypes = if (isEpisode) "Series" else "Movie"

        return try {
            val itemsResponse = EmbyApiClient.getUserItemsByProviderIds(
                payload = payload,
                includeItemTypes = includeItemTypes,
                providerIdFilter = providerFilter,
                limit = 1,
            )
            if (itemsResponse.status !in 200..299) {
                log.w { "Emby search failed: ${itemsResponse.status}" }
                return null
            }
            val items = EmbyApiClient.json.decodeFromString<EmbyItemsResponse>(itemsResponse.body)
            val matchedItem = items.items.firstOrNull()
            if (matchedItem == null) {
                log.d { "No Emby item found for '$videoId' ($includeItemTypes)" }
                return null
            }

            val targetItem = if (isEpisode) {
                val requestedSeason = season!!
                val requestedEpisode = episode!!
                val episodesResponse = EmbyApiClient.getEpisodes(
                    payload = payload,
                    seriesId = matchedItem.id,
                    season = requestedSeason,
                )
                if (episodesResponse.status !in 200..299) {
                    log.w {
                        "Emby episode lookup failed for series ${matchedItem.id} S${requestedSeason}E${requestedEpisode}: ${episodesResponse.status}"
                    }
                    return null
                }
                val episodes = EmbyApiClient.json
                    .decodeFromString<EmbyItemsResponse>(episodesResponse.body)
                    .items
                val episodeItem = episodes.firstOrNull { candidate ->
                    val matchesType = candidate.type?.equals("Episode", ignoreCase = true) != false
                    val matchesSeason = candidate.parentIndexNumber?.let { it == requestedSeason } != false
                    val matchesEpisode = candidate.indexNumber == requestedEpisode
                    matchesType && matchesSeason && matchesEpisode
                }
                if (episodeItem == null) {
                    log.d {
                        "Episode S${requestedSeason}E${requestedEpisode} not found in Emby for series ${matchedItem.id}"
                    }
                    return null
                }
                episodeItem
            } else {
                matchedItem
            }

            val streamUrl = "${payload.serverUrl}/Videos/${targetItem.id}/stream?static=true"
            val mediaSourceId = targetItem.mediaSources?.firstOrNull()?.id ?: targetItem.id
            val headers = mapOf("X-Emby-Token" to payload.accessToken)
            val runtimeMs = targetItem.runTimeTicks?.let { it / TICKS_PER_MS }
            val resumePositionMs = targetItem.userData?.playbackPositionTicks
                ?.takeIf { it > 0 }
                ?.let { it / TICKS_PER_MS }

            EmbyResolvedStream(
                streamUrl = streamUrl,
                streamHeaders = headers,
                displayName = buildDisplayName(matchedItem, targetItem, isEpisode, season, episode),
                itemId = targetItem.id,
                mediaSourceId = mediaSourceId,
                runTimeMs = runtimeMs,
                resumePositionMs = resumePositionMs,
                serverName = payload.serverName.ifBlank { "Emby" },
            )
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            log.w { "Error resolving Emby stream for '$videoId': ${error.message}" }
            null
        }
    }

    /**
     * Parses an OmnioMobile videoId into Emby `AnyProviderIdEquals` filters.
     *
     * Supported forms:
     *  - `tt1234567` (IMDb id) → `imdb.tt1234567`
     *  - `tt1234567:1:5` (episode) → `imdb.tt1234567` (series-level lookup; episode resolved later)
     *  - `tmdb:12345` → `tmdb.12345`
     *
     * Other shapes return blank, which causes [findEmbyStream] to skip Emby for the request.
     */
    internal fun buildProviderIdFilter(videoId: String): String {
        val baseId = videoId.substringBefore(':').trim()
        if (baseId.isBlank()) return ""
        val filters = mutableListOf<String>()
        when {
            baseId.startsWith("tt", ignoreCase = true) -> filters += "imdb.$baseId"
            baseId.equals("tmdb", ignoreCase = true) -> {
                val tmdbId = videoId.substringAfter(':', "").substringBefore(':').trim()
                if (tmdbId.isNotBlank()) filters += "tmdb.$tmdbId"
            }
        }
        return filters.joinToString(",")
    }

    private fun buildDisplayName(
        series: EmbyItemDto,
        target: EmbyItemDto,
        isEpisode: Boolean,
        season: Int?,
        episode: Int?,
    ): String {
        return if (isEpisode) {
            val showName = series.name ?: "Unknown Show"
            val episodeName = target.name
            "Emby: $showName S${season}E${episode}${if (!episodeName.isNullOrBlank()) " - $episodeName" else ""}"
        } else {
            "Emby: ${target.name ?: "Unknown"}"
        }
    }
}
