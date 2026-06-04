package com.minlish.app.util

import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Lightweight XLSX reader using Android's XmlPullParser (no fastexcel-reader).
 * Supports inline strings and shared strings tables.
 */
internal object SimpleXlsxReader {

    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    fun readRows(input: InputStream): List<List<String>> {
        val entries = readZipEntries(input)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetBytes = entries["xl/worksheets/sheet1.xml"]
            ?: entries.entries
                .firstOrNull { it.key.startsWith("xl/worksheets/") && it.key.endsWith(".xml") }
                ?.value
            ?: throw IllegalArgumentException("Không tìm thấy sheet trong tệp Excel.")
        return parseSheet(sheetBytes, sharedStrings)
    }

    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return entries
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val strings = mutableListOf<String>()
        parseXml(bytes) { parser, event ->
            if (event == XmlPullParser.START_TAG && isTag(parser, "si")) {
                strings.add(readSharedStringItem(parser))
            }
        }
        return strings
    }

    private fun readSharedStringItem(parser: XmlPullParser): String {
        val builder = StringBuilder()
        var depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (isTag(parser, "t")) {
                        builder.append(parser.nextText())
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.depth <= depth && isTag(parser, "si")) break
                }
            }
        }
        return builder.toString()
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val result = mutableListOf<List<String>>()
        parseXml(bytes) { parser, event ->
            if (event == XmlPullParser.START_TAG && isTag(parser, "row")) {
                val rowCells = parseRow(parser, sharedStrings)
                if (rowCells.isNotEmpty()) {
                    result.add(rowCells)
                }
            }
        }
        return result
    }

    private fun parseRow(parser: XmlPullParser, sharedStrings: List<String>): List<String> {
        val valuesByColumn = mutableMapOf<Int, String>()
        var depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> {
                    if (isTag(parser, "c")) {
                        val ref = parser.getAttributeValue(null, "r")
                            ?: parser.getAttributeValue(NS, "r")
                        val type = parser.getAttributeValue(null, "t")
                            ?: parser.getAttributeValue(NS, "t")
                        val colIndex = ref?.let { columnIndexFromRef(it) } ?: valuesByColumn.size
                        val text = readCellValue(parser, type, sharedStrings)
                        valuesByColumn[colIndex] = text
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.depth <= depth && isTag(parser, "row")) break
                }
            }
        }
        if (valuesByColumn.isEmpty()) return emptyList()
        val maxCol = valuesByColumn.keys.max()
        return (0..maxCol).map { valuesByColumn[it].orEmpty() }
    }

    private fun readCellValue(
        parser: XmlPullParser,
        cellType: String?,
        sharedStrings: List<String>
    ): String {
        val builder = StringBuilder()
        var depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when (parser.eventType) {
                XmlPullParser.START_TAG -> when {
                    isTag(parser, "t") -> builder.append(parser.nextText())
                    isTag(parser, "v") -> {
                        val raw = parser.nextText()
                        builder.append(
                            when (cellType) {
                                "s" -> sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty()
                                else -> raw
                            }
                        )
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.depth <= depth && isTag(parser, "c")) break
                }
            }
        }
        return builder.toString()
    }

    private fun columnIndexFromRef(ref: String): Int {
        val letters = ref.takeWhile { it.isLetter() }
        var col = 0
        for (ch in letters.uppercase()) {
            col = col * 26 + (ch.code - 'A'.code + 1)
        }
        return col - 1
    }

    private fun isTag(parser: XmlPullParser, localName: String): Boolean {
        return parser.name == localName || parser.name.endsWith(":$localName")
    }

    private inline fun parseXml(bytes: ByteArray, block: (XmlPullParser, Int) -> Unit) {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val parser = factory.newPullParser()
        parser.setInput(ByteArrayInputStream(bytes), "UTF-8")
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            block(parser, event)
            event = parser.next()
        }
    }
}
