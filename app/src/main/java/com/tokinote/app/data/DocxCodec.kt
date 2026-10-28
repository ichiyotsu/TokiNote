package com.tokinote.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 极简 DOCX 读写：只处理正文段落，足够整篇笔记的导入/导出。 */
object DocxCodec {

    fun displayName(context: Context, uri: Uri): String =
        runCatching {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        }.getOrNull()?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "导入的笔记"

    fun read(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.name == "word/document.xml") {
                        val xml = zip.readBytes().toString(Charsets.UTF_8)
                        return@runCatching parseDocumentXml(xml)
                    }
                    zip.closeEntry()
                }
            }
        }
        null
    }.getOrNull()

    fun write(notes: List<Note>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry("[Content_Types].xml"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                <Default Extension="xml" ContentType="application/xml"/>
                <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
                </Types>""".trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("_rels/.rels"))
            zip.write(
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
                </Relationships>""".trimIndent().toByteArray(),
            )
            zip.closeEntry()
            zip.putNextEntry(ZipEntry("word/document.xml"))
            zip.write(documentXml(notes).toByteArray())
            zip.closeEntry()
        }
        return output.toByteArray()
    }

    private fun documentXml(notes: List<Note>): String {
        val body = buildString {
            notes.forEach { note ->
                append("<w:p><w:r><w:rPr><w:b/></w:rPr><w:t xml:space=\"preserve\">")
                append(escape(note.title.ifBlank { note.journalDate.toString() }))
                append("</w:t></w:r></w:p>")
                note.body.lines().forEach { line ->
                    append("<w:p><w:r><w:t xml:space=\"preserve\">")
                    append(escape(line))
                    append("</w:t></w:r></w:p>")
                }
                append("<w:p/>")
            }
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body>$body<w:sectPr/></w:body></w:document>""".trimIndent()
    }

    private fun parseDocumentXml(xml: String): String {
        val paragraphs = Regex("<w:p(?:\\s[^>]*)?>(.*?)</w:p>", RegexOption.DOT_MATCHES_ALL)
            .findAll(xml)
            .map { paragraph ->
                Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                    .findAll(paragraph.groupValues[1])
                    .joinToString("") { unescape(it.groupValues[1]) }
            }
            .joinToString("\n")
            .trim()
        return paragraphs
    }

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private fun unescape(value: String): String = value
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace("&apos;", "'")
        .replace("&amp;", "&")
}
