package com.tokinote.app.data

import android.content.Context
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.zip.ZipInputStream

/**
 * 可选的「专注」组件。
 *
 * 组件包从 GitHub Release 单独下载，安装到应用私有目录。
 * 主程序只负责入口与组件状态，未安装或未启用时不显示专注底部入口。
 */
object FocusComponent {

    const val ID = "focus"
    const val VERSION = "1.0.0"
    const val DOWNLOAD_URL =
        "https://github.com/ichiyotsu/TokiNote/releases/download/0.3.4/focus-component-1.0.zip"

    /** 组件包校验值；与 Release 中的 zip 保持一致。 */
    const val SHA256 = "B9D2D21E998503C647DFF2AB64AAE122A1EF8DEBF08B146571EB0AC3A5652F84"

    private const val MANIFEST = "manifest.json"

    fun directory(context: Context): File = File(context.filesDir, "components/focus")

    fun installed(context: Context): Boolean = installedVersion(context) != null

    fun installedVersion(context: Context): String? = runCatching {
        val file = File(directory(context), MANIFEST)
        if (!file.isFile) return null
        val json = JSONObject(file.readText())
        if (json.optString("id") != ID) return null
        json.optString("version").takeIf { it.isNotBlank() }
    }.getOrNull()

    fun matchesChecksum(file: File, expected: String): Boolean = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count <= 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }
            .equals(expected, ignoreCase = true)
    }.getOrDefault(false)

    fun install(context: Context, zip: File): Boolean {
        val targetDir = directory(context)
        return runCatching {
            targetDir.deleteRecursively()
            targetDir.mkdirs()
            ZipInputStream(zip.inputStream().buffered()).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    val target = File(targetDir, entry.name).canonicalFile
                    require(target.toPath().startsWith(targetDir.canonicalFile.toPath()))
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    }
                    input.closeEntry()
                }
            }

            val manifest = File(targetDir, MANIFEST)
            require(manifest.isFile) { "missing manifest" }
            val json = JSONObject(manifest.readText())
            require(json.optString("id") == ID) { "wrong component" }
            require(json.optString("version").isNotBlank()) { "missing version" }
            true
        }.onFailure {
            android.util.Log.e("FocusComponent", "install failed", it)
            targetDir.deleteRecursively()
        }.getOrDefault(false)
    }

    fun delete(context: Context): Boolean = runCatching {
        directory(context).deleteRecursively()
    }.getOrDefault(false)
}
