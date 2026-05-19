package com.nuvio.app.features.streams.prefs

import com.nuvio.app.features.sourcecloud.SourceCloudService
import com.nuvio.app.features.sourcecloud.SourceCloudStreamMetadata
import com.nuvio.app.features.streams.StreamItem
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamPrefFilterTest {

    private fun stream(
        name: String = "Test Stream",
        quality: String? = null,
        sizeBytes: Long? = null,
        codec: String? = null,
        audio: String? = null,
        hdr: String? = null,
        language: String? = null,
        cached: Boolean? = null,
        sourceConfidence: Double? = null,
    ): StreamItem = StreamItem(
        name = name,
        description = null,
        url = "https://example.com/stream",
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        addonName = "Source Cloud",
        addonId = "source_cloud",
        sourceProvider = "source_cloud",
        sourceCloudMetadata = SourceCloudStreamMetadata(
            quality = quality,
            sizeBytes = sizeBytes,
            codec = codec,
            audio = audio,
            hdr = hdr,
            language = language,
            cached = cached,
            sourceConfidence = sourceConfidence,
            sourceService = null
        )
    )

    @Test
    fun emptyPrefsReturnsInputUnchanged() {
        val streams = listOf(stream("S1"), stream("S2"))
        val result = StreamPrefFilter.apply(streams, StreamPreferences.DEFAULT)
        assertEquals(streams, result)
    }

    @Test
    fun disabledPrefsReturnsInputUnchanged() {
        val streams = listOf(stream("S1"))
        val prefs = StreamPreferences.DEFAULT.copy(enabled = false)
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(streams, result)
    }

    @Test
    fun enabledWithNoFiltersReturnsInput() {
        val streams = listOf(stream("S1"), stream("S2"))
        val prefs = StreamPreferences(enabled = true)
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(streams, result)
    }

    @Test
    fun minResolutionFiltersLowRes() {
        val hd = stream("HD", quality = "720p")
        val fhd = stream("FHD", quality = "1080p")
        val uhd = stream("UHD", quality = "2160p")
        val streams = listOf(hd, fhd, uhd)
        val prefs = StreamPreferences(
            enabled = true,
            minResolution = StreamPrefMinQuality.P1080,
            sortCriteria = emptyList()
        )
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(listOf(fhd, uhd), result)
    }

    @Test
    fun requiredVisualTagKeepsOnlyMatch() {
        val hdr10 = stream("HDR10", hdr = "HDR10")
        val dv = stream("DV", hdr = "Dolby Vision")
        val sdr = stream("SDR", hdr = "SDR")
        val streams = listOf(hdr10, dv, sdr)
        val prefs = StreamPreferences(
            enabled = true,
            requiredVisualTags = setOf(StreamPrefVisualTag.DOLBY_VISION)
        )
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(listOf(dv), result)
    }

    @Test
    fun excludedCodecRemovesMatch() {
        val hevc = stream("HEVC", codec = "HEVC")
        val av1 = stream("AV1", codec = "AV1")
        val streams = listOf(hevc, av1)
        val prefs = StreamPreferences(
            enabled = true,
            excludedCodecs = setOf(StreamPrefCodec.HEVC)
        )
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(listOf(av1), result)
    }

    @Test
    fun sortByResolutionDesc() {
        val uhd = stream("4K", quality = "2160p")
        val hd = stream("HD", quality = "720p")
        val fhd = stream("FHD", quality = "1080p")
        val streams = listOf(hd, uhd, fhd)
        val prefs = StreamPreferences(
            enabled = true,
            sortCriteria = listOf(
                StreamPrefSortCriterion(StreamPrefSortKey.RESOLUTION, StreamPrefSortDirection.DESC)
            )
        )
        val result = StreamPrefFilter.apply(streams, prefs)
        assertEquals(listOf(uhd, fhd, hd), result)
    }

    @Test
    fun nullMetadataDoesNotCrash() {
        val noMeta = StreamItem(
            name = "No Meta",
            description = null,
            url = "https://example.com",
            addonName = "Test",
            addonId = "test",
            sourceProvider = null,
            sourceCloudMetadata = null
        )
        val withMeta = stream("With Meta", quality = "1080p")
        val prefs = StreamPreferences(
            enabled = true,
            minResolution = StreamPrefMinQuality.P720
        )
        val result = StreamPrefFilter.apply(listOf(noMeta, withMeta), prefs)
        assertEquals(listOf(withMeta), result)
    }
}
