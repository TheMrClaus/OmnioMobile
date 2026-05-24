package com.nuvio.app.features.profiles

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileContentFilterTest {

    @Test
    fun `meta above kids threshold is blocked`() {
        val profile = kidsProfile(maxAgeRating = "13+")

        val result = ProfileContentFilter.filter(
            meta = meta(id = "blocked", ageRating = "16+"),
            activeProfile = profile,
        )

        assertNull(result)
    }

    @Test
    fun `meta at or below kids threshold passes through`() {
        val profile = kidsProfile(maxAgeRating = "13+")
        val source = meta(id = "allowed", ageRating = "PG-13")

        val result = ProfileContentFilter.filter(meta = source, activeProfile = profile)

        assertEquals(source, result)
    }

    @Test
    fun `meta with unknown rating passes through for kids profile`() {
        val profile = kidsProfile(maxAgeRating = "13+")
        val source = meta(id = "unknown", ageRating = null)

        val result = ProfileContentFilter.filter(meta = source, activeProfile = profile)

        assertEquals(source, result)
    }

    @Test
    fun `non-kids profile leaves meta untouched`() {
        val source = meta(id = "anything", ageRating = "18+")

        val result = ProfileContentFilter.filter(
            meta = source,
            activeProfile = NuvioProfile(profileIndex = 1, isKids = false),
        )

        assertEquals(source, result)
    }

    @Test
    fun `filterPreviews drops above-threshold items for kids`() {
        val profile = kidsProfile(maxAgeRating = "13+")
        val items = listOf(
            preview(id = "kid", ageRating = "G"),
            preview(id = "teen", ageRating = "PG-13"),
            preview(id = "mature", ageRating = "R"),
            preview(id = "ma", ageRating = "TV-MA"),
        )

        val result = ProfileContentFilter.filterPreviews(items, profile)

        assertEquals(listOf("kid", "teen"), result.map { it.id })
    }

    @Test
    fun `filterPreviews leaves list untouched for non-kids profile`() {
        val source = listOf(
            preview(id = "kid", ageRating = "G"),
            preview(id = "mature", ageRating = "R"),
        )

        val result = ProfileContentFilter.filterPreviews(
            items = source,
            activeProfile = NuvioProfile(profileIndex = 1, isKids = false),
        )

        // Same list (no allocation) is the expected fast path.
        assertTrue(source === result)
    }

    @Test
    fun `filterPreviews passes unrated items through for kids`() {
        val profile = kidsProfile(maxAgeRating = "13+")
        val items = listOf(
            preview(id = "unrated", ageRating = null),
            preview(id = "rated_mature", ageRating = "R"),
        )

        val result = ProfileContentFilter.filterPreviews(items, profile)

        assertEquals(listOf("unrated"), result.map { it.id })
    }

    @Test
    fun `filterLibraryItems drops above-threshold items for kids`() {
        val profile = kidsProfile(maxAgeRating = "13+")
        val items = listOf(
            libraryItem(id = "kid", ageRating = "TV-PG"),
            libraryItem(id = "teen", ageRating = "13+"),
            libraryItem(id = "mature", ageRating = "NC-17"),
        )

        val result = ProfileContentFilter.filterLibraryItems(items, profile)

        assertEquals(listOf("kid", "teen"), result.map { it.id })
    }

    @Test
    fun `filterLibraryItems leaves list untouched when active profile is null`() {
        val source = listOf(libraryItem(id = "any", ageRating = "R"))

        val result = ProfileContentFilter.filterLibraryItems(items = source, activeProfile = null)

        assertTrue(source === result)
    }

    @Test
    fun `allows raw rating returns true for unrated entries`() {
        val profile = kidsProfile(maxAgeRating = "13+")

        assertTrue(ProfileContentFilter.allows(ageRating = null, activeProfile = profile))
        assertTrue(ProfileContentFilter.allows(ageRating = "", activeProfile = profile))
    }

    @Test
    fun `allows raw rating blocks above-threshold entries for kids`() {
        val profile = kidsProfile(maxAgeRating = "13+")

        assertTrue(ProfileContentFilter.allows(ageRating = "PG-13", activeProfile = profile))
        assertEquals(
            expected = false,
            actual = ProfileContentFilter.allows(ageRating = "R", activeProfile = profile),
        )
    }

    @Test
    fun `allows raw rating always permits when active profile is not kids`() {
        assertTrue(
            ProfileContentFilter.allows(
                ageRating = "NC-17",
                activeProfile = NuvioProfile(profileIndex = 1, isKids = false),
            ),
        )
    }

    private fun kidsProfile(maxAgeRating: String): NuvioProfile = NuvioProfile(
        profileIndex = 2,
        isKids = true,
        maxAgeRating = maxAgeRating,
    )

    private fun meta(id: String, ageRating: String?): MetaDetails = MetaDetails(
        id = id,
        type = "movie",
        name = id,
        ageRating = ageRating,
    )

    private fun preview(id: String, ageRating: String?): MetaPreview = MetaPreview(
        id = id,
        type = "movie",
        name = id,
        ageRating = ageRating,
    )

    private fun libraryItem(id: String, ageRating: String?): LibraryItem = LibraryItem(
        id = id,
        type = "movie",
        name = id,
        ageRating = ageRating,
        savedAtEpochMs = 0L,
    )
}
