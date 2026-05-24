package com.nuvio.app.features.profiles

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem

object ProfileContentFilter {
    fun filter(meta: MetaDetails, activeProfile: NuvioProfile?): MetaDetails? {
        val threshold = kidsAgeThreshold(activeProfile) ?: return meta
        return meta.takeIf { allows(it.ageRating, threshold) }
    }

    /**
     * Multi-item entry point used by catalog rows, TMDB credits/rails, and
     * other discovery surfaces. Returns the list unchanged when the active
     * profile is not a kids profile (so non-kids contexts skip the cost of
     * iterating).
     */
    fun filterPreviews(items: List<MetaPreview>, activeProfile: NuvioProfile?): List<MetaPreview> {
        val threshold = kidsAgeThreshold(activeProfile) ?: return items
        return items.filter { allows(it.ageRating, threshold) }
    }

    /** Returns true when the supplied [ageRating] is allowed for the active
     *  profile. Used at sites that already iterate (e.g. continue-watching
     *  with an async age-rating lookup map). */
    fun allows(ageRating: String?, activeProfile: NuvioProfile?): Boolean {
        val threshold = kidsAgeThreshold(activeProfile) ?: return true
        return allows(ageRating, threshold)
    }

    /** Library equivalent of [filterPreviews]. */
    fun filterLibraryItems(items: List<LibraryItem>, activeProfile: NuvioProfile?): List<LibraryItem> {
        val threshold = kidsAgeThreshold(activeProfile) ?: return items
        return items.filter { allows(it.ageRating, threshold) }
    }

    private fun kidsAgeThreshold(activeProfile: NuvioProfile?): Int? =
        activeProfile
            ?.takeIf { it.isKids }
            ?.effectiveMaxAgeRating()
            ?.let(::ageRatingValue)

    private fun allows(ageRating: String?, threshold: Int): Boolean {
        val value = ageRatingValue(ageRating) ?: return true
        return value <= threshold
    }

    private fun ageRatingValue(raw: String?): Int? {
        val normalized = raw?.trim()?.lowercase().orEmpty()
        if (normalized.isBlank()) return null

        val compact = normalized
            .substringBefore('/')
            .substringBefore('(')
            .filter { char -> char.isLetterOrDigit() || char == '+' }

        symbolicAgeRatings[compact]?.let { return it }

        return ageDigits.find(compact)
            ?.value
            ?.toIntOrNull()
    }

    private val ageDigits = Regex("""\d{1,2}""")

    private val symbolicAgeRatings = mapOf(
        "all" to 0,
        "allages" to 0,
        "g" to 0,
        "u" to 0,
        "tvy" to 0,
        "tvg" to 0,
        "tvy7" to 7,
        "tvy7fv" to 7,
        "pg" to 10,
        "tvpg" to 10,
        "m" to 15,
        "r" to 17,
        "tvma" to 17,
        "nc17" to 17,
        "x" to 18,
    )
}
