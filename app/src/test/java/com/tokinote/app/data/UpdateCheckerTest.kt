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
    fun `beta channel picks the highest beta or stable`() {
        val stable = UpdateChecker.pickLatest(
            listOf(release("0.3.5-beta"), release("0.3.6")),
            "beta",
        )
        assertTrue(stable?.version == "0.3.6")

        val sameCore = UpdateChecker.pickLatest(
            listOf(release("0.3.5-beta"), release("0.3.5")),
            "beta",
        )
        assertTrue(sameCore?.version == "0.3.5")

        val betaAhead = UpdateChecker.pickLatest(
            listOf(release("0.3.8-beta"), release("0.3.7")),
            "beta",
        )
        assertTrue(betaAhead?.version == "0.3.8-beta")

        val releaseOnly = UpdateChecker.pickLatest(
            listOf(release("0.3.6"), release("0.3.7-beta")),
            "release",
        )
        assertTrue(releaseOnly?.version == "0.3.6")

        val installedOnBeta = UpdateChecker.pickLatest(
            listOf(release("0.3.6"), release("0.3.7-beta.4"), release("windows-1.0.0"), release("ios-1.0.0")),
            "beta",
        )
        assertTrue(installedOnBeta?.version == "0.3.7-beta.4")
        assertTrue(UpdateChecker.isNewer("0.3.7-beta.4", "0.3.6"))
        assertFalse(UpdateChecker.isNewer("0.3.6", "0.3.7-beta.4"))
    }

    @Test
    fun `0_3_7 is newer than 0_3_6 and releases without an apk are ignored`() {
        assertTrue(UpdateChecker.isNewer("0.3.7", "0.3.6"))
        assertFalse(UpdateChecker.isNewer("0.3.6", "0.3.7"))
        assertFalse(UpdateChecker.isNewer("0.3.7", "0.3.7"))
        assertTrue(UpdateChecker.isNewer("0.3.7", "0.3.7-beta.4"))

        val latest = UpdateChecker.pickLatest(
            listOf(
                release("0.3.6"),
                release("0.3.7-beta.4"),
                releaseWithoutApk("0.3.8"),
                release("0.3.7"),
            ),
            "release",
        )
        assertTrue(latest?.tag == "0.3.7")
        assertTrue(latest?.version == "0.3.7")

        val onlyZip = UpdateChecker.pickLatest(
            listOf(releaseWithoutApk("0.3.7"), release("0.3.6")),
            "release",
        )
        assertTrue(onlyZip?.version == "0.3.6")
    }

    @Test
    fun `bN sorts before the same stable version and by integer`() {
        assertFalse(UpdateChecker.isNewer("0.1.1-b1", "0.1.1"))
        assertTrue(UpdateChecker.isNewer("0.1.1", "0.1.1-b1"))
        assertTrue(UpdateChecker.isNewer("0.1.1-b1", "0.1.0"))
        assertTrue(UpdateChecker.isNewer("0.1.1-b2", "0.1.1-b1"))
        assertTrue(UpdateChecker.isNewer("0.1.1-b10", "0.1.1-b2"))
        assertFalse(UpdateChecker.isNewer("0.1.1-b1", "0.1.1-b1"))
        assertFalse(UpdateChecker.isNewer("0.3.7-b4", "0.3.7-beta.4"))
        assertFalse(UpdateChecker.isNewer("0.3.7-beta.4", "0.3.7-b4"))
    }

    @Test
    fun `channels accept bN and already published beta tags`() {
        assertTrue(UpdateChecker.matchesChannel(release("0.1.1"), "release"))
        assertFalse(UpdateChecker.matchesChannel(release("0.1.1-b1"), "release"))
        assertFalse(UpdateChecker.matchesChannel(release("0.3.7-beta.4"), "release"))
        assertTrue(UpdateChecker.matchesChannel(release("v0.1.1-b1"), "beta"))
        assertTrue(UpdateChecker.matchesChannel(release("0.1.1-b10"), "beta"))
        assertTrue(UpdateChecker.matchesChannel(release("0.3.7-beta.4"), "beta"))
        assertTrue(UpdateChecker.matchesChannel(release("0.1.1"), "beta"))
    }

    @Test
    fun `beta channel follows release order through bN`() {
        val stableWins = UpdateChecker.pickLatest(
            listOf(release("0.1.1-b2"), release("0.1.1"), release("0.1.0")),
            "beta",
        )
        assertTrue(stableWins?.version == "0.1.1")

        val betaAhead = UpdateChecker.pickLatest(
            listOf(release("0.1.1-b1"), release("0.1.0")),
            "beta",
        )
        assertTrue(betaAhead?.version == "0.1.1-b1")

        val releaseOnly = UpdateChecker.pickLatest(
            listOf(release("0.1.1-b1"), release("0.1.0")),
            "release",
        )
        assertTrue(releaseOnly?.version == "0.1.0")
    }

    private fun release(version: String): ReleaseInfo = ReleaseInfo(
        tag = version,
        version = version.removePrefix("v"),
        notes = "",
        apkUrl = "https://example.com/${version.removePrefix("v")}.apk",
        pageUrl = "",
    )

    private fun releaseWithoutApk(version: String): ReleaseInfo =
        release(version).copy(apkUrl = null)
}
