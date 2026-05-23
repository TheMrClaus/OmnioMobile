package com.nuvio.app.features.profiles

import com.nuvio.app.features.details.MetaDetails
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
