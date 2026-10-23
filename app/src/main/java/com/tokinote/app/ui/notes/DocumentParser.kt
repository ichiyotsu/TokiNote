package com.tokinote.app.ui.notes

import java.io.File
import java.util.zip.ZipFile
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

sealed interface DocContent {
    data class Pages(val pages: List<String>) : DocContent
    data class Sheets(val sheets: List<SheetData>) : DocContent
    data object Unsupported : DocContent
}

data class SheetData(val name: String, val rows: List<List<String>>)

/**
 * 轻量文档文本解析：docx / pptx / xlsx 直接读 OOXML 里的文本，
 * 不引入 Apache POI 这类重依赖。PDF 由 PdfRenderer 单独渲染。
 */
object DocumentParser {

    fun parse(file: File): DocContent = runCatching {
        val name = file.name.lowercase()
        when {
            name.endsWith(".docx") -> DocContent.Pages(listOf(parseDocx(file)))
            name.endsWith(".pptx") -> DocContent.Pages(parsePptx(file))
            name.endsWith(".xlsx") -> DocContent.Sheets(parseXlsx(file))
            else -> DocContent.Unsupported
        }
    }.getOrDefault(DocContent.Unsupported)

    private fun parseDocx(file: File): String {
        val xml = readEntry(file, "word/document.xml") ?: return ""
        val parser = xmlParser(xml)
        val builder = StringBuilder()
        collectText(parser, builder, textTag = "t", paragraphTag = "p")
        return builder.toString().trim()
    }

    private fun parsePptx(file: File): List<String> {
        val slides = ZipFile(file).use { zip ->
            zip.entries().asSequence()
                .filter { it.name.matches(Regex("ppt/slides/slide\\d+\\.xml")) }
                .sortedBy { it.name.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
                .mapNotNull { entry ->
                    val text = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                    buildString {
                        val parser = xmlParser(text)
                        val builder = StringBuilder()
                        collectText(parser, builder, textTag = "t", paragraphTag = "p")
                        append(builder.toString().trim())
                    }
                }
                .toList()
        }
        return slides
    }

    private fun parseXlsx(file: File): List<SheetData> {
        val shared = mutableListOf<String>()
        readEntry(file, "xl/sharedStrings.xml")?.let { xml ->
            val parser = xmlParser(xml)
            var inString = false
            val current = StringBuilder()
            while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                when (parser.eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "si") {
                            current.clear()
                            inString = true
                        }
                    }

                    XmlPullParser.TEXT -> if (inString) current.append(parser.text)
                    XmlPullParser.END_TAG -> {
                        if (parser.name == "si") {
                            shared.add(current.toString())
                            inString = false
                        }
                    }
                }
                parser.next()
            }
        }

        val sheets = mutableListOf<SheetData>()
        ZipFile(file).use { zip ->
            val entries = zip.entries().asSequence()
                .filter { it.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) }
                .sortedBy { it.name.filter { ch -> ch.isDigit() }.toIntOrNull() ?: 0 }
                .toList()
            entries.forEachIndexed { index, entry ->
                val xml = zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
                val parser = xmlParser(xml)
                val rows = mutableListOf<List<String>>()
                var row = mutableListOf<String>()
                var cellType: String? = null
                var value = StringBuilder()

                while (parser.eventType != XmlPullParser.END_DOCUMENT) {
                    when (parser.eventType) {
                        XmlPullParser.START_TAG -> when (parser.name) {
                            "row" -> row = mutableListOf()
                            "c" -> {
                                cellType = parser.getAttributeValue(null, "t")
                                value = StringBuilder()
                            }
                        }

                        XmlPullParser.TEXT -> value.append(parser.text)
                        XmlPullParser.END_TAG -> when (parser.name) {
                            "v" -> {
                                val raw = value.toString().trim()
                                row.add(
                                    if (cellType == "s") {
                                        shared.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                                    } else {
                                        raw
                                    },
                                )
                            }

                            "row" -> if (row.isNotEmpty()) rows.add(row)
                        }
                    }
                    parser.next()
                }
                sheets.add(SheetData("Sheet${index + 1}", rows))
            }
        }
        return sheets
    }

    private fun readEntry(file: File, path: String): String? = runCatching {
        ZipFile(file).use { zip ->
            val entry = zip.getEntry(path) ?: return null
            zip.getInputStream(entry).use { it.readBytes().toString(Charsets.UTF_8) }
        }
    }.getOrNull()

    private fun xmlParser(xml: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = false
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())
        parser.next()
        return parser
    }

    private fun collectText(
        parser: XmlPullParser,
        builder: StringBuilder,
        textTag: String,
        paragraphTag: String,
    ) {
        var inText = false
        while (parser.eventType != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> if (parser.name == textTag) inText = true
                XmlPullParser.TEXT -> if (inText) builder.append(parser.text)
                XmlPullParser.END_TAG -> {
                    if (parser.name == textTag) inText = false
                    if (parser.name == paragraphTag) builder.append('\n')
                }
            }
            parser.next()
        }
    }
}
