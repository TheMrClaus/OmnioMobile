package com.nuvio.app.features.updater

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppUpdateChannelMatcherTest {
    @Test
    fun `matches exact target commitish`() {
        assertTrue(
            AppUpdateChannelMatcher.matches(
                targetCommitish = "main",
                tagName = null,
                releaseName = null,
            )
        )
    }

    @Test
    fun `matches channel token in release tag`() {
        assertTrue(
            AppUpdateChannelMatcher.matches(
                targetCommitish = null,
                tagName = "v0.1.1-beta.20260502-1-main",
                releaseName = null,
            )
        )
    }

    @Test
    fun `does not match unrelated words containing main`() {
        assertFalse(
            AppUpdateChannelMatcher.matches(
                targetCommitish = null,
                tagName = "domain-fix",
                releaseName = "Remaining work",
            )
        )
    }

    @Test
    fun `does not match release title alone`() {
        assertFalse(
            AppUpdateChannelMatcher.matches(
                targetCommitish = null,
                tagName = null,
                releaseName = "Main release",
            )
        )
    }
}
