package com.tokinote.app.data

import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** 从 GitHub Releases 拿到的一条版本信息。 */
data class ReleaseInfo(
    val tag: String,
    val version: String,
    val notes: String,
    val apkUrl: String?,
    val pageUrl: String,
    val isPrerelease: Boolean = false,
)

/**
 * 应用内更新：直接读 GitHub Releases API，不引入任何网络库，
 * 这样既能保持离线构建，也避免额外依赖。
 */
object UpdateChecker {

    private const val RELEASES_URL =
        "https://api.github.com/repos/ichiyotsu/TokiNote/releases?per_page=10"
    private const val USER_AGENT = "TokiNote-Android"

    /** 标准版：0.2.7 / v0.2.7，不带任何 -beta 后缀。 */
    private val stableVersion = Regex("""^\d+(\.\d+)*$""")

    /** 测试版：0.2.9-beta / 0.2.9-beta.1（允许 v 前缀与大小写）。 */
    private val betaVersion = Regex("""^\d+(\.\d+)*-beta(\.\d+)?$""", RegexOption.IGNORE_CASE)

    /**
     * 最新版本：
     * - release 只看 tag 形如 0.2.7 的标准版；
     * - beta 只看 tag 形如 0.2.9-beta 的测试版。
     * 两个通道都只从 GitHub Releases 读取，不会混用。
     */
    fun latest(channel: String = "release"): ReleaseInfo? {
        val array = JSONArray(httpGet(RELEASES_URL))
        for (index in 0 until array.length()) {
            val json = array.optJSONObject(index) ?: continue
            if (json.optBoolean("draft", false)) continue
            val info = json.toReleaseInfo()
            if (matchesChannel(info, channel)) return info
        }
        return null
    }

    /** 通道匹配规则，单独提出来便于测试。 */
    internal fun matchesChannel(info: ReleaseInfo, channel: String): Boolean {
        val version = info.version.trim().removePrefix("v").removePrefix("V")
        return if (channel.equals("beta", ignoreCase = true)) {
            betaVersion.matches(version)
        } else {
            stableVersion.matches(version)
        }
    }

    /** 比较版本号：只看核心数字，稳定版优先于同一核心的最新预发布。 */
    fun isNewer(remote: String, current: String): Boolean {
        val r = ParsedVersion.parse(remote)
        val c = ParsedVersion.parse(current)
        val core = compareCore(r.core, c.core)
        if (core > 0) return true
        if (core < 0) return false
        if (r.preRelease.isEmpty()) return c.preRelease.isNotEmpty()
        if (c.preRelease.isEmpty()) return false
        return r.preRelease > c.preRelease
    }

    private fun compareCore(a: List<Int>, b: List<Int>): Int {
        for (index in 0 until maxOf(a.size, b.size)) {
            val left = a.getOrElse(index) { 0 }
            val right = b.getOrElse(index) { 0 }
            if (left != right) return left.compareTo(right)
        }
        return 0
    }

    /** 下载 APK 到 [destination]，进度回调用 0–100。 */
    fun download(url: String, destination: File, onProgress: (Int) -> Unit): Boolean {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 30_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/octet-stream")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            val total = connection.contentLength.toLong()
            var read = 0L
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count <= 0) break
                        output.write(buffer, 0, count)
                        read += count
                        if (total > 0) {
                            onProgress(((read * 100) / total).toInt().coerceIn(0, 100))
                        }
                    }
                }
            }
            return destination.length() > 0
        } finally {
            connection.disconnect()
        }
    }

    private fun httpGet(url: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15_000
        connection.readTimeout = 20_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IOException("HTTP $code")
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun org.json.JSONObject.toReleaseInfo(): ReleaseInfo {
        var apkUrl: String? = null
        val assets = optJSONArray("assets")
        if (assets != null) {
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = asset.optString("browser_download_url")
                    break
                }
            }
        }
        val tag = optString("tag_name")
        val version = tag.removePrefix("v").removePrefix("V")
        return ReleaseInfo(
            tag = tag,
            version = version,
            notes = optString("body"),
            apkUrl = apkUrl,
            pageUrl = optString("html_url"),
            isPrerelease = optBoolean("prerelease", false) || betaVersion.matches(version),
        )
    }

    private data class ParsedVersion(val core: List<Int>, val preRelease: String) {
        companion object {
            fun parse(raw: String): ParsedVersion {
                val trimmed = raw.trim().removePrefix("v").removePrefix("V")
                val corePart = trimmed.substringBefore('-')
                val pre = trimmed.substringAfter('-', "")
                val numbers = corePart.split('.').map { it.toIntOrNull() ?: 0 }
                return ParsedVersion(
                    core = List(3) { numbers.getOrElse(it) { 0 } },
                    preRelease = pre,
                )
            }
        }
    }
}
