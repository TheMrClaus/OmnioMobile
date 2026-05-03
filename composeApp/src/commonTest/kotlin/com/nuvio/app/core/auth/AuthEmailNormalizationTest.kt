package com.nuvio.app.core.auth

import kotlin.test.Test
import kotlin.test.assertEquals

class AuthEmailNormalizationTest {
    @Test
    fun `trims surrounding whitespace before auth`() {
        assertEquals(
            "user@example.com",
            normalizeAuthEmail("  user@example.com  "),
        )
    }
}
