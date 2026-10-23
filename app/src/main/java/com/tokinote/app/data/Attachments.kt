package com.tokinote.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.util.UUID

val JournalEmojis = listOf(
    "😀", "😄", "😊", "🥰", "😎", "🤩",
    "😌", "🤔", "😴", "🥲", "😤", "😭",
    "🌱", "🌞", "🌊", "🔥", "🍜", "☕",
    "🎧", "📚", "🚴", "🏔️", "✨", "💡",
)

private val documentMimes = arrayOf(
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-powerpoint",
    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
)

fun documentMimeFilter(): Array<String> = documentMimes

/** 把选中的图片 / 文档复制进应用私有目录，保证卸载前一直可用。 */
object Attachments {

    fun copyImage(context: Context, uri: Uri): String? = runCatching {
        val dir = File(context.filesDir, "media").apply { mkdirs() }
        val target = File(dir, "${UUID.randomUUID()}.img")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        target.absolutePath
    }.getOrNull()

    fun copyDocument(context: Context, uri: Uri): JournalFile? = runCatching {
        val name = displayName(context, uri) ?: "document"
        val dir = File(context.filesDir, "files").apply { mkdirs() }
        val safeName = name.replace(Regex("[^\\p{L}\\p{N}._-]"), "_")
        val target = File(dir, "${UUID.randomUUID()}_$safeName")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        JournalFile(
            name = name,
            path = target.absolutePath,
            mime = context.contentResolver.getType(uri).orEmpty(),
            size = target.length(),
        )
    }.getOrNull()

    private fun displayName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        return uri.lastPathSegment?.substringAfterLast('/')
    }
}

fun formatFileSize(bytes: Long): String = when {
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
    bytes >= 1024L -> "%d KB".format(bytes / 1024L)
    else -> "$bytes B"
}
