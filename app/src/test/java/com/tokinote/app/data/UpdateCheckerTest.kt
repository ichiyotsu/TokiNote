package com.tokinote.app.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    @Test
    fun `patch and minor bumps are newer`() {
        assertTrue(UpdateChecker.isNewer("1.0.1", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.1.0", "1.0.9"))
        assertTrue(UpdateChecker.isNewer("v2.0.0", "1.9.9"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(UpdateChecker.isNewer("1.0.0", "1.0.0"))
        assertFalse(UpdateChecker.isNewer("1.0", "1.0.0"))
    }

    @Test
    fun `stable beats its own pre-release`() {
        assertFalse(UpdateChecker.isNewer("1.0.0-beta.2", "1.0.0"))
        assertTrue(UpdateChecker.isNewer("1.0.0-beta.3", "1.0.0-beta.2"))
        assertTrue(UpdateChecker.isNewer("1.0.1-beta", "1.0.0"))
    }
}
