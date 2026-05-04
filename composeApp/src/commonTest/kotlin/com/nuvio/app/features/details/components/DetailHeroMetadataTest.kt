package com.nuvio.app.features.details.components

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.details.MetaExternalRating
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_AUDIENCE
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_IMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TMDB
import com.nuvio.app.features.mdblist.MdbListMetadataService.PROVIDER_TOMATOES
import kotlin.test.Test
import kotlin.test.assertEquals

class DetailHeroMetadataTest {
    @Test
    fun heroPillsPreferAudienceScoreThenReleaseLineThenAgeRating() {
        val meta = meta(
            releaseInfo = "2024-03-14",
            ageRating = "PG-13",
            externalRatings = listOf(
                MetaExternalRating(source = PROVIDER_IMDB, value = 7.2),
                MetaExternalRating(source = PROVIDER_AUDIENCE, value = 91.2),
            ),
        )

        assertEquals(
            listOf(
                DetailHeroPill(text = "91%", isHighlighted = true),
                DetailHeroPill(text = "2024", isHighlighted = false),
                DetailHeroPill(text = "PG-13", isHighlighted = false),
            ),
            detailHeroPills(meta),
        )
    }

    @Test
    fun heroPillsFallbackToTomatoesThenScaledImdbScore() {
        val tomatoesMeta = meta(
            externalRatings = listOf(
                MetaExternalRating(source = PROVIDER_TOMATOES, value = 84.0),
            ),
        )
        val imdbMeta = meta(imdbRating = "8.4")

        assertEquals(
            listOf(DetailHeroPill(text = "84%", isHighlighted = true)),
            detailHeroPills(tomatoesMeta),
        )
        assertEquals(
            listOf(DetailHeroPill(text = "84%", isHighlighted = true)),
            detailHeroPills(imdbMeta),
        )
    }

    @Test
    fun heroPillsUseWholeTmdbPercentageWithoutRescaling() {
        val meta = meta(
            externalRatings = listOf(
                MetaExternalRating(source = PROVIDER_TMDB, value = 73.0),
            ),
        )

        assertEquals(
            listOf(DetailHeroPill(text = "73%", isHighlighted = true)),
            detailHeroPills(meta),
        )
    }

    private fun meta(
        releaseInfo: String? = null,
        imdbRating: String? = null,
        ageRating: String? = null,
        externalRatings: List<MetaExternalRating> = emptyList(),
    ) = MetaDetails(
        id = "meta-1",
        type = "movie",
        name = "Example",
        releaseInfo = releaseInfo,
        imdbRating = imdbRating,
        ageRating = ageRating,
        externalRatings = externalRatings,
    )
}
