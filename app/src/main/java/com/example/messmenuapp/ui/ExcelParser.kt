package com.example.messmenuapp.ui

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipInputStream

/**
 * Locked parsing rules (do not change without explicit request):
 *  - Finds the row for TODAY by matching the weekday name (short or full) together with
 *    the day-of-month, allowing an optional ordinal suffix (17th, 31st, etc.).
 *  - Reads every row after that header, until the next "day header" row, taking
 *    column B/C/D/E (index 1-4) as Breakfast/Lunch/Snacks/Dinner respectively.
 *  - Items are joined with ", " and bracketed text is preserved as-is.
 *  - Today's date is displayed as dd-MONTH-yyyy (day).
 */
object ExcelParser {

    private val dayNames = listOf(
        "sun", "mon", "tue", "wed", "thu", "fri", "sat",
        "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday"
    )

    fun parseExcel(context: Context, uri: Uri): List<MenuItemUI> {
        val today = LocalDate.now()
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                val rows = XlsxReader.readFirstSheet(input)
                if (rows.isEmpty()) return emptyList()

                val start = findToday(rows, today)
                if (start < 0) return emptyList()
                val end = findNextHeader(rows, start + 1)

                val breakfast = mutableListOf<String>()
                val lunch = mutableListOf<String>()
                val snacks = mutableListOf<String>()
                val dinner = mutableListOf<String>()

                for (i in start + 1 until end) {
                    val row = rows.getOrNull(i) ?: continue
                    add(row.getOrNull(1), breakfast)
                    add(row.getOrNull(2), lunch)
                    add(row.getOrNull(3), snacks)
                    add(row.getOrNull(4), dinner)
                }

                listOf(
                    MenuItemUI(
                        formatDate(today),
                        breakfast.joinToString(", "),
                        lunch.joinToString(", "),
                        snacks.joinToString(", "),
                        dinner.joinToString(", ")
                    )
                )
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun findToday(rows: List<List<String>>, date: LocalDate): Int {
        val n = date.dayOfMonth.toString()
        val short = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()
        val full = date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH).lowercase()
        val ordinalRegex = Regex("(?<!\\d)(?:0?$n)(?:st|nd|rd|th)?(?!\\d)")
        rows.forEachIndexed { i, row ->
            val text = row.joinToString(" ") { it.trim().lowercase() }
            if ((text.contains(short) || text.contains(full)) && ordinalRegex.containsMatchIn(text)) return i
        }
        return -1
    }

    private fun findNextHeader(rows: List<List<String>>, start: Int): Int {
        val dateRegex = Regex("(?<!\\d)\\d{1,2}(?:st|nd|rd|th)?(?!\\d)", RegexOption.IGNORE_CASE)
        for (i in start until rows.size) {
            val row = rows.getOrNull(i) ?: continue
            val text = row.joinToString(" ") { it.trim().lowercase() }
            val hasDay = dayNames.any { Regex("(?<![a-z])$it(?![a-z])").containsMatchIn(text) }
            val hasDate = dateRegex.containsMatchIn(text)
            if (hasDay && hasDate) return i
        }
        return rows.size
    }

    private fun add(value: String?, out: MutableList<String>) {
        if (value == null) return
        val v = value.replace(Regex("\\s+"), " ").trim()
        if (v.isNotEmpty()) out.add(v)
    }

    private fun formatDate(d: LocalDate): String {
        val month = d.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH).uppercase(Locale.ENGLISH)
        val day = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.ENGLISH)
        return String.format(Locale.ENGLISH, "%02d-%s-%04d (%s)", d.dayOfMonth, month, d.year, day)
    }
}

/**
 * Minimal, dependency-free XLSX reader.
 *
 * Why not Apache POI: poi-ooxml's runtime relies on javax.xml.stream (StAX), which is not
 * part of the Android platform. It compiles fine but commonly throws NoClassDefFoundError
 * on a real device (a well-documented issue: github.com/centic9/poi-on-android). It also
 * pulls in several large transitive libraries (xmlbeans, commons-compress, curvesapi) that
 * bloat the APK for no benefit here.
 *
 * An .xlsx file is just a zip archive of small XML files, so this reads it with only
 * java.util.zip and Android's built-in XmlPull parser (android.util.Xml) — both ship with
 * every Android version, so there's nothing extra to bundle and no missing-class risk.
 * It returns the same shape the parsing rules above expect: one List<String> per row,
 * cell text indexed by column (0 = A, 1 = B, ...).
 */
private object XlsxReader {

    fun readFirstSheet(input: InputStream): List<List<String>> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val buffer = ByteArrayOutputStream()
                    val chunk = ByteArray(8192)
                    var read = zis.read(chunk)
                    while (read >= 0) {
                        buffer.write(chunk, 0, read)
                        read = zis.read(chunk)
                    }
                    entries[entry.name] = buffer.toByteArray()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedStrings.xml"]?.let { parseSharedStrings(it) } ?: emptyList()
        val sheetPath = resolveFirstSheetPath(entries) ?: "xl/worksheets/sheet1.xml"
        val sheetBytes = entries[sheetPath] ?: entries["xl/worksheets/sheet1.xml"] ?: return emptyList()
        return parseSheet(sheetBytes, sharedStrings)
    }

    private fun resolveFirstSheetPath(entries: Map<String, ByteArray>): String? {
        val workbookXml = entries["xl/workbook.xml"] ?: return null
        val relsXml = entries["xl/_rels/workbook.xml.rels"]

        var firstRid: String? = null
        val wp = parser(workbookXml)
        var event = wp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && wp.name == "sheet") {
                firstRid = wp.getAttributeValue(null, "r:id")
                break
            }
            event = wp.next()
        }

        if (relsXml == null || firstRid == null) {
            return firstRid?.let { "xl/worksheets/$it.xml" }
        }

        var target: String? = null
        val rp = parser(relsXml)
        event = rp.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && rp.name == "Relationship" &&
                rp.getAttributeValue(null, "Id") == firstRid
            ) {
                target = rp.getAttributeValue(null, "Target")
                break
            }
            event = rp.next()
        }

        val t = target ?: return "xl/worksheets/$firstRid.xml"
        return if (t.startsWith("/")) t.removePrefix("/") else "xl/$t"
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val result = mutableListOf<String>()
        val p = parser(bytes)
        var event = p.eventType
        val current = StringBuilder()
        var inSi = false
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> if (p.name == "si") {
                    inSi = true
                    current.setLength(0)
                }
                XmlPullParser.TEXT -> if (inSi) current.append(p.text)
                XmlPullParser.END_TAG -> if (p.name == "si") {
                    result.add(current.toString())
                    inSi = false
                }
            }
            event = p.next()
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        val p = parser(bytes)
        var event = p.eventType

        var rowIndex = -1
        var colIndex = -1
        var cellType: String? = null
        val cellValue = StringBuilder()
        var inValue = false
        var inInlineText = false

        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> {
                        val r = p.getAttributeValue(null, "r")?.toIntOrNull()
                        rowIndex = (r?.minus(1)) ?: (rowIndex + 1)
                        while (rows.size <= rowIndex) rows.add(mutableListOf())
                    }
                    "c" -> {
                        cellType = p.getAttributeValue(null, "t")
                        val ref = p.getAttributeValue(null, "r")
                        colIndex = ref?.let { colIndexFromRef(it) } ?: (colIndex + 1)
                        cellValue.setLength(0)
                    }
                    "v" -> inValue = true
                    "t" -> inInlineText = true
                }
                XmlPullParser.TEXT -> if (inValue || inInlineText) cellValue.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inValue = false
                    "t" -> inInlineText = false
                    "c" -> {
                        if (rowIndex >= 0 && colIndex >= 0) {
                            val text = if (cellType == "s") {
                                cellValue.toString().trim().toIntOrNull()?.let { sharedStrings.getOrNull(it) } ?: ""
                            } else {
                                cellValue.toString()
                            }
                            val row = rows[rowIndex]
                            while (row.size <= colIndex) row.add("")
                            row[colIndex] = text
                        }
                    }
                }
            }
            event = p.next()
        }
        return rows
    }

    private fun colIndexFromRef(ref: String): Int {
        var col = 0
        for (ch in ref) {
            if (ch.isLetter()) col = col * 26 + (ch.uppercaseChar() - 'A' + 1) else break
        }
        return col - 1
    }

    private fun parser(bytes: ByteArray): XmlPullParser {
        val p = Xml.newPullParser()
        p.setInput(bytes.inputStream(), "UTF-8")
        return p
    }
}
