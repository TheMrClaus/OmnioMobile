package com.nuvio.app.features.profiles

import com.nuvio.app.features.home.PosterShape
import com.nuvio.app.features.library.LibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals

class ProfileContentFilterTest {

    @Test
    fun `library items above kids threshold are removed while unknown ratings remain`() {
        val profile = NuvioProfile(
            profileIndex = 2,
            isKids = true,
            maxAgeRating = "13+",
        )

        val filtered = ProfileContentFilter.filterLibraryItems(
            items = listOf(
                libraryItem(id = "allowed", ageRating = "PG-13"),
                libraryItem(id = "blocked", ageRating = "16+"),
                libraryItem(id = "unknown", ageRating = null),
            ),
            activeProfile = profile,
        )

        assertEquals(listOf("allowed", "unknown"), filtered.map(LibraryItem::id))
    }

    private fun libraryItem(
        id: String,
        ageRating: String?,
    ): LibraryItem = LibraryItem(
        id = id,
        type = "movie",
        name = id,
        ageRating = ageRating,
        genres = emptyList(),
        posterShape = PosterShape.Poster,
        savedAtEpochMs = 1L,
    )
}