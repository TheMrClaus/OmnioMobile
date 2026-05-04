package com.nuvio.app.features.player

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class PlayerLayoutTest {
    @Test
    fun mobileMetricsFavorLargerPrimaryControlAndThinnerScrubber() {
        val metrics = PlayerLayoutMetrics.fromWidth(360.dp)

        assertEquals(42.dp, metrics.playIconSize)
        assertEquals(24.dp, metrics.sideIconSize)
        assertEquals(18.dp, metrics.sliderTouchHeight)
        assertEquals(0.62f, metrics.sliderScaleY)
        assertEquals(48.dp, metrics.centerGap)
    }

    @Test
    fun tabletMetricsKeepProminentCenterControl() {
        val metrics = PlayerLayoutMetrics.fromWidth(900.dp)

        assertEquals(46.dp, metrics.playIconSize)
        assertEquals(26.dp, metrics.sideIconSize)
        assertEquals(18.dp, metrics.sliderTouchHeight)
        assertEquals(0.6f, metrics.sliderScaleY)
        assertEquals(64.dp, metrics.centerGap)
    }

    @Test
    fun desktopMetricsUseLargestPlayControl() {
        val metrics = PlayerLayoutMetrics.fromWidth(1200.dp)

        assertEquals(52.dp, metrics.playIconSize)
        assertEquals(28.dp, metrics.sideIconSize)
        assertEquals(20.dp, metrics.sliderTouchHeight)
        assertEquals(0.58f, metrics.sliderScaleY)
        assertEquals(80.dp, metrics.centerGap)
    }
}
