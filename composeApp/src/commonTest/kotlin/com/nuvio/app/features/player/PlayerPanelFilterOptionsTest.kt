package com.nuvio.app.features.player

import com.nuvio.app.features.streams.AddonStreamGroup
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerPanelFilterOptionsTest {
    @Test
    fun filterOptionsKeepDistinctAddonIdsWhenNamesMatch() {
        val groups = listOf(
            AddonStreamGroup(addonName = "Torrentio", addonId = "torrentio-a", streams = emptyList()),
            AddonStreamGroup(addonName = "Torrentio", addonId = "torrentio-b", streams = emptyList()),
            AddonStreamGroup(addonName = "MediaFusion", addonId = "mediafusion", streams = emptyList()),
        )

        val filterOptions = playerPanelFilterOptions(groups)

        assertEquals(listOf("torrentio-a", "torrentio-b", "mediafusion"), filterOptions.map { it.addonId })
        assertEquals(listOf("Torrentio", "Torrentio", "MediaFusion"), filterOptions.map { it.addonName })
    }

    @Test
    fun filterOptionsDropDuplicateGroupsForSameAddonId() {
        val groups = listOf(
            AddonStreamGroup(addonName = "Torrentio", addonId = "torrentio", streams = emptyList(), isLoading = true),
            AddonStreamGroup(addonName = "Torrentio", addonId = "torrentio", streams = emptyList(), error = "timeout"),
        )

        val filterOptions = playerPanelFilterOptions(groups)

        assertEquals(1, filterOptions.size)
        assertEquals("torrentio", filterOptions.single().addonId)
        assertEquals("Torrentio", filterOptions.single().addonName)
    }
}
