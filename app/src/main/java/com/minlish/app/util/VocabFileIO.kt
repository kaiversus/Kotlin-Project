package com.minlish.app.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.minlish.app.data.model.Word
import org.dhatim.fastexcel.Workbook
import org.dhatim.fastexcel.Worksheet
import java.io.InputStream
import java.io.OutputStream

enum class VocabFileFormat {
    CSV,
    EXCEL
}

data class VocabImportRow(
    val word: String,
    val pronunciation: String? = null,
    val meaning: String = "",
    val description: String? = null,
    val exampleSentence: String? = null
)

object VocabFileIO {

    private val HEADER_ALIASES = setOf(
        "word", "từ", "tu", "english", "vocab", "vocabulary"
    )

    fun detectFormat(fileName: String?, mimeType: String?): VocabFileFormat {
        val lowerName = fileName?.lowercase().orEmpty()
        if (lowerName.endsWith(".xlsx") || lowerName.endsWith(".xls")) return VocabFileFormat.EXCEL
        if (lowerName.endsWith(".csv")) return VocabFileFormat.CSV

        val lowerMime = mimeType?.lowercase().orEmpty()
        if (lowerMime.contains("spreadsheet") || lowerMime.contains("excel")) {
            return VocabFileFormat.EXCEL
        }
        return VocabFileFormat.CSV
    }

    fun mimeType(format: VocabFileFormat): String = when (format) {
        VocabFileFormat.CSV -> "text/csv"
        VocabFileFormat.EXCEL -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    }

    fun fileExtension(format: VocabFileFormat): String = when (format) {
        VocabFileFormat.CSV -> "csv"
        VocabFileFormat.EXCEL -> "xlsx"
    }

    fun suggestedExportFileName(setName: String, format: VocabFileFormat): String {
        val safe = setName.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "vocab_set" }
        return "${safe}_vocab.${fileExtension(format)}"
    }

    fun importMimeTypes(): Array<String> = arrayOf(
        "text/csv",
        "text/comma-separated-values",
        "application/csv",
        "text/plain",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )

    fun parseFromUri(context: Context, uri: Uri): List<VocabImportRow> {
        val resolver = context.contentResolver
        val fileName = queryDisplayName(resolver, uri)
        val mimeType = resolver.getType(uri)
        resolver.openInputStream(uri)?.use { input ->
            return when (detectFormat(fileName, mimeType)) {
                VocabFileFormat.CSV -> parseCsv(input)
                VocabFileFormat.EXCEL -> parseExcel(input)
            }
        } ?: throw IllegalArgumentException("Không thể đọc tệp đã chọn.")
    }

    fun writeToUri(context: Context, uri: Uri, words: List<Word>, format: VocabFileFormat) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            export(words, format, output)
        } ?: throw IllegalArgumentException("Không thể ghi tệp. Hãy thử chọn vị trí lưu khác.")
    }

    fun export(words: List<Word>, format: VocabFileFormat, output: OutputStream) {
        when (format) {
            VocabFileFormat.CSV -> writeCsv(words, output)
            VocabFileFormat.EXCEL -> writeExcel(words, output)
        }
    }

    private fun parseCsv(input: InputStream): List<VocabImportRow> {
        val text = input.bufferedReader(Charsets.UTF_8).readText()
        val lines = text.lines().map { it.trimEnd() }.filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            throw IllegalArgumentException("Tệp CSV trống hoặc không có dữ liệu.")
        }

        val rows = mutableListOf<VocabImportRow>()
        var skippedHeader = false
        for (line in lines) {
            val cells = parseCsvLine(line)
            if (cells.isEmpty() || cells[0].isBlank()) continue
            if (!skippedHeader && isHeaderRow(cells)) {
                skippedHeader = true
                continue
            }
            rows.add(cells.toImportRow())
        }
        return validateRows(rows, "CSV")
    }

    private fun parseExcel(input: InputStream): List<VocabImportRow> {
        val sheetRows = SimpleXlsxReader.readRows(input)
        val rows = mutableListOf<VocabImportRow>()
        var skippedHeader = false
        for (rawCells in sheetRows) {
            val cells = (0 until 5).map { col -> rawCells.getOrNull(col)?.trim().orEmpty() }
            if (cells.all { it.isBlank() }) continue
            if (!skippedHeader && isHeaderRow(cells)) {
                skippedHeader = true
                continue
            }
            if (cells[0].isBlank()) continue
            rows.add(cells.toImportRow())
        }
        return validateRows(rows, "Excel")
    }

    private fun writeCsv(words: List<Word>, output: OutputStream) {
        output.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("Word,Pronunciation,Meaning,Description,ExampleSentence")
            words.forEach { w ->
                writer.appendLine(
                    listOf(
                        w.word,
                        w.pronunciation.orEmpty(),
                        w.meaning,
                        w.description.orEmpty(),
                        w.exampleSentence.orEmpty()
                    ).joinToString(",") { escapeCsvField(it) }
                )
            }
        }
    }

    private fun writeExcel(words: List<Word>, output: OutputStream) {
        Workbook(output, "MinLish", "1.0").use { workbook ->
            val sheet: Worksheet = workbook.newWorksheet("Vocabulary")
            val headers = listOf("Word", "Pronunciation", "Meaning", "Description", "ExampleSentence")
            headers.forEachIndexed { col, header -> sheet.value(0, col, header) }
            words.forEachIndexed { index, w ->
                val row = index + 1
                sheet.value(row, 0, w.word)
                sheet.value(row, 1, w.pronunciation.orEmpty())
                sheet.value(row, 2, w.meaning)
                sheet.value(row, 3, w.description.orEmpty())
                sheet.value(row, 4, w.exampleSentence.orEmpty())
            }
            workbook.finish()
        }
    }

    private fun List<String>.toImportRow(): VocabImportRow = VocabImportRow(
        word = getOrNull(0)?.trim().orEmpty(),
        pronunciation = getOrNull(1)?.trim()?.takeIf { it.isNotBlank() },
        meaning = getOrNull(2)?.trim().orEmpty(),
        description = getOrNull(3)?.trim()?.takeIf { it.isNotBlank() },
        exampleSentence = getOrNull(4)?.trim()?.takeIf { it.isNotBlank() }
    )

    private fun isHeaderRow(cells: List<String>): Boolean {
        val first = cells.firstOrNull()?.trim()?.lowercase().orEmpty()
        return first in HEADER_ALIASES || first.contains("pronunciation") || first.contains("nghĩa")
    }

    private fun validateRows(rows: List<VocabImportRow>, label: String): List<VocabImportRow> {
        if (rows.isEmpty()) {
            throw IllegalArgumentException(
                "Không tìm thấy từ vựng hợp lệ trong tệp $label. " +
                    "Cột: Từ, Phiên âm, Nghĩa, Giải nghĩa, Ví dụ."
            )
        }
        return rows
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            when (val c = line[i]) {
                '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                ',' -> {
                    if (inQuotes) current.append(c) else {
                        result.add(current.toString())
                        current.clear()
                    }
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result.map { it.trim() }
    }

    private fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') || value.contains('"') || value.contains('\n')
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? {
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0) return cursor.getString(index)
            }
        }
        return uri.lastPathSegment
    }
}
