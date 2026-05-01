package com.nuvio.app.features.profiles

import com.nuvio.app.features.details.MetaDetails
import com.nuvio.app.features.home.MetaPreview
import com.nuvio.app.features.library.LibraryItem
import com.nuvio.app.features.watchprogress.ContinueWatchingItem

object ProfileContentFilter {
    fun filter(items: List<MetaPreview>, activeProfile: NuvioProfile?): List<MetaPreview> =
        filterByAgeRating(items, activeProfile) { item -> item.ageRating }

    fun filterLibraryItems(items: List<LibraryItem>, activeProfile: NuvioProfile?): List<LibraryItem> =
        filterByAgeRating(items, activeProfile) { item -> item.ageRating }

    fun filterContinueWatchingItems(
        items: List<ContinueWatchingItem>,
        activeProfile: NuvioProfile?,
    ): List<ContinueWatchingItem> = filterByAgeRating(items, activeProfile) { item -> item.ageRating }

    private inline fun <T> filterByAgeRating(
        items: List<T>,
        activeProfile: NuvioProfile?,
        ageRatingOf: (T) -> String?,
    ): List<T> {
        val threshold = kidsAgeThreshold(activeProfile) ?: return items
        return items.filter { item -> allows(ageRatingOf(item), threshold) }
    }

    fun filter(meta: MetaDetails, activeProfile: NuvioProfile?): MetaDetails? {
        val threshold = kidsAgeThreshold(activeProfile) ?: return meta
        if (!allows(meta.ageRating, threshold)) return null

        return meta.copy(
            moreLikeThis = filter(meta.moreLikeThis, activeProfile),
            collectionItems = filter(meta.collectionItems, activeProfile),
        )
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