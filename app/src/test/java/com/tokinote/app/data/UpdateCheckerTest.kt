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

    @Test
    fun `release channel only accepts standard versions`() {
        assertTrue(UpdateChecker.matchesChannel(release("0.2.7"), "release"))
        assertTrue(UpdateChecker.matchesChannel(release("v0.2.8"), "release"))
        assertFalse(UpdateChecker.matchesChannel(release("0.2.9-beta"), "release"))
        assertFalse(UpdateChecker.matchesChannel(release("0.2.9-beta.1"), "release"))
        assertFalse(UpdateChecker.matchesChannel(release("0.2.9-rc1"), "release"))
    }

    @Test
    fun `beta channel accepts beta and stable versions`() {
        assertTrue(UpdateChecker.matchesChannel(release("0.2.9-beta"), "beta"))
        assertTrue(UpdateChecker.matchesChannel(release("v0.2.9-BETA.2"), "beta"))
        assertTrue(UpdateChecker.matchesChannel(release("0.3.6"), "beta"))
        assertFalse(UpdateChecker.matchesChannel(release("0.2.9-rc1"), "beta"))
    }

    @Test
    fun `beta channel prefers a higher stable but keeps beta on same core`() {
        val stable = UpdateChecker.pickLatest(
            listOf(release("0.3.5-beta"), release("0.3.6")),
            "beta",
        )
        assertTrue(stable?.version == "0.3.6")

        val sameCore = UpdateChecker.pickLatest(
            listOf(release("0.3.5-beta"), release("0.3.5")),
            "beta",
        )
        assertTrue(sameCore?.version == "0.3.5-beta")

        val betaAhead = UpdateChecker.pickLatest(
            listOf(release("0.3.5-beta"), release("0.3.4")),
            "beta",
        )
        assertTrue(betaAhead?.version == "0.3.5-beta")

        val releaseOnly = UpdateChecker.pickLatest(
            listOf(release("0.3.6"), release("0.3.7-beta")),
            "release",
        )
        assertTrue(releaseOnly?.version == "0.3.6")
    }

    private fun release(version: String): ReleaseInfo = ReleaseInfo(
        tag = version,
        version = version.removePrefix("v"),
        notes = "",
        apkUrl = null,
        pageUrl = "",
    )
}
