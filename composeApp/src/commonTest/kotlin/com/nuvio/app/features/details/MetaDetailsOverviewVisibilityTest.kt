package com.nuvio.app.features.details

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetaDetailsOverviewVisibilityTest {
    @Test
    fun overviewIsHiddenWhenNoOverviewContentExists() {
        assertFalse(hasOverviewContent(sparseMeta()))
    }

    @Test
    fun overviewIsVisibleWhenDescriptionExists() {
        assertTrue(hasOverviewContent(sparseMeta(description = "A synopsis")))
    }

    @Test
    fun overviewIsVisibleWhenMetaLineExistsWithoutSynopsis() {
        assertTrue(hasOverviewContent(sparseMeta(releaseInfo = "2024-03-14")))
    }

    private fun sparseMeta(
        description: String? = null,
        releaseInfo: String? = null,
    ) = MetaDetails(
        id = "meta-1",
        type = "movie",
        name = "Example",
        description = description,
        releaseInfo = releaseInfo,
    )
}
