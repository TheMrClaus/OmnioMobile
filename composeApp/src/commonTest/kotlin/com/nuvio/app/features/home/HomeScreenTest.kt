package com.nuvio.app.features.home

import com.nuvio.app.features.profiles.NuvioProfile
import com.nuvio.app.features.watchprogress.ContinueWatchingItem
import com.nuvio.app.features.watchprogress.WatchProgressEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class HomeScreenTest {

    @Test
    fun `build home continue watching items removes duplicate video ids`() {
        val inProgress = progressEntry(
            videoId = "tt0944947:1:4",
            title = "Game of Thrones",
            episodeTitle = "Cripples, Bastards, and Broken Things",
            lastUpdatedEpochMs = 250L,
        )
        val nextUp = continueWatchingItem(
            videoId = "tt0944947:1:4",
            subtitle = "Up Next • S1E4 • Cripples, Bastards, and Broken Things",
        )
        val movie = progressEntry(
            videoId = "movie-1",
            title = "Movie",
            lastUpdatedEpochMs = 100L,
            seasonNumber = null,
            episodeNumber = null,
            episodeTitle = null,
        )

        val result = buildHomeContinueWatchingItems(
            visibleEntries = listOf(inProgress, movie),
            nextUpItemsBySeries = mapOf("tt0944947" to (200L to nextUp)),
        )

        assertEquals(listOf("tt0944947:1:4", "movie-1"), result.map(ContinueWatchingItem::videoId))
        assertEquals("S1E4 • Cripples, Bastards, and Broken Things", result.first().subtitle)
    }

    @Test
    fun `build home continue watching items prefers progress entry on timestamp tie`() {
        val inProgress = progressEntry(
            videoId = "show:1:5",
            title = "Show",
            episodeNumber = 5,
            episodeTitle = "The Wolf and the Lion",
            lastUpdatedEpochMs = 500L,
        )
        val nextUp = continueWatchingItem(
            videoId = "show:1:5",
            subtitle = "Up Next • S1E5 • The Wolf and the Lion",
        )

        val result = buildHomeContinueWatchingItems(
            visibleEntries = listOf(inProgress),
            nextUpItemsBySeries = mapOf("show" to (500L to nextUp)),
        )

        assertEquals(1, result.size)
        assertEquals("S1E5 • The Wolf and the Lion", result.single().subtitle)
    }

    @Test
    fun `build home continue watching items filters over limit kids content`() {
        val allowed = progressEntry(
            videoId = "movie-1",
            title = "Allowed",
            lastUpdatedEpochMs = 200L,
            seasonNumber = null,
            episodeNumber = null,
            episodeTitle = null,
        )
        val blocked = progressEntry(
            videoId = "movie-2",
            title = "Blocked",
            lastUpdatedEpochMs = 100L,
            seasonNumber = null,
            episodeNumber = null,
            episodeTitle = null,
        )

        val result = buildHomeContinueWatchingItems(
            visibleEntries = listOf(allowed, blocked),
            nextUpItemsBySeries = emptyMap(),
            liveAgeRatingsByContent = mapOf(
                "movie-1" to "PG-13",
                "movie-2" to "18+",
            ),
            activeProfile = NuvioProfile(
                profileIndex = 2,
                isKids = true,
                maxAgeRating = "13+",
            ),
        )

        assertEquals(listOf("movie-1"), result.map(ContinueWatchingItem::videoId))
    }

    private fun progressEntry(
        videoId: String,
        title: String,
        lastUpdatedEpochMs: Long,
        seasonNumber: Int? = 1,
        episodeNumber: Int? = 4,
        episodeTitle: String? = "Episode",
    ): WatchProgressEntry =
        WatchProgressEntry(
            contentType = if (seasonNumber != null && episodeNumber != null) "series" else "movie",
            parentMetaId = videoId.substringBefore(':'),
            parentMetaType = if (seasonNumber != null && episodeNumber != null) "series" else "movie",
            videoId = videoId,
            title = title,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeTitle = episodeTitle,
            lastPositionMs = if (seasonNumber != null && episodeNumber != null) 120_000L else 60_000L,
            durationMs = 1_000_000L,
            lastUpdatedEpochMs = lastUpdatedEpochMs,
        )

    private fun continueWatchingItem(
        videoId: String,
        subtitle: String,
    ): ContinueWatchingItem =
        ContinueWatchingItem(
            parentMetaId = videoId.substringBefore(':'),
            parentMetaType = "series",
            videoId = videoId,
            title = "Show",
            subtitle = subtitle,
            imageUrl = null,
            seasonNumber = 1,
            episodeNumber = 4,
            episodeTitle = subtitle.substringAfterLast(" • ", "Episode"),
            resumePositionMs = 0L,
            durationMs = 0L,
            progressFraction = 0f,
        )
}