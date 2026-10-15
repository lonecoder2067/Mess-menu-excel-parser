package com.example.messmenuapp.ui

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.zip.ZipInputStream

object ExcelParser {
    data class Result(val menu: List<MenuItemUI>, val message: String? = null)

    private val weekdays = listOf("sun", "mon", "tue", "wed", "thu", "fri", "sat", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")

    fun parseExcel(context: Context, uri: Uri): Result = try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            val rows = XlsxReader.readFirstSheet(input)
            val days = parseRows(rows)
            if (days.isEmpty()) Result(emptyList(), "No dated menu sections were found. Include a date or weekday and day number in the sheet.")
            else Result(days)
        } ?: Result(emptyList(), "Could not open this file. Choose it again from your device.")
    } catch (_: Exception) {
        Result(emptyList(), "Could not read this workbook. Make sure it is a valid .xlsx file.")
    }

    internal fun parseRows(rows: List<List<String>>, today: LocalDate = LocalDate.now()): List<MenuItemUI> {
        val headers = rows.indices.filter { isHeader(rows[it]) }
        if (headers.isEmpty()) return emptyList()
        return headers.mapIndexedNotNull { index, start ->
            val date = dateFromRow(rows[start], today) ?: return@mapIndexedNotNull null
            val end = headers.getOrNull(index + 1) ?: rows.size
            val meals = Array(4) { mutableListOf<String>() }
            for (r in start + 1 until end) {
                val row = rows[r]
                // Standard layout is B:E; also accept compact A:D sheets with an empty first column.
                val offset = if (row.size <= 4) 0 else 1
                for (meal in 0..3) row.getOrNull(offset + meal)?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotEmpty() }?.let(meals[meal]::add)
            }
            MenuItemUI(date.toString(), meals[0].joinToString(", "), meals[1].joinToString(", "), meals[2].joinToString(", "), meals[3].joinToString(", "), date)
        }.distinctBy { it.date }
    }

    private fun isHeader(row: List<String>): Boolean {
        val text = row.joinToString(" ").lowercase(Locale.ENGLISH)
        val date = Regex("(?<!\\d)\\d{1,2}(?:st|nd|rd|th)?(?!\\d)|\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b|\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val day = weekdays.any { Regex("(?<![a-z])$it(?![a-z])").containsMatchIn(text) }
        return date && day || Regex("\\b\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}\\b|\\b\\d{1,2}[-/]\\d{1,2}[-/]\\d{2,4}\\b").containsMatchIn(text)
    }

    private fun dateFromRow(row: List<String>, today: LocalDate): LocalDate? {
        val text = row.joinToString(" ").trim()
        val numeric = Regex("(?<!\\d)(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})|(?<!\\d)(\\d{1,2})[-/](\\d{1,2})[-/](\\d{2,4})(?!\\d)").find(text)
        if (numeric != null) runCatching {
            val g = numeric.groupValues
            return if (g[1].isNotEmpty()) LocalDate.of(g[1].toInt(), g[2].toInt(), g[3].toInt()) else {
                var year = g[6].toInt(); if (year < 100) year += 2000
                LocalDate.of(year, g[5].toInt(), g[4].toInt())
            }
        }
        val monthDate = Regex("(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+([a-z]{3,9})(?:\\s*,?\\s*(\\d{4}))?\\b").find(text)
            ?: Regex("(?i)\\b([a-z]{3,9})\\s+(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*,?\\s*(\\d{4}))?\\b").find(text)
        if (monthDate != null) runCatching {
            val g = monthDate.groupValues
            val firstIsNumber = g[1].firstOrNull()?.isDigit() == true
            val day = (if (firstIsNumber) g[1] else g[2]).toInt()
            val monthText = if (firstIsNumber) g[2] else g[1]
            val month = Month.entries.firstOrNull { it.getDisplayName(TextStyle.FULL, Locale.ENGLISH).startsWith(monthText, true) || it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).equals(monthText, true) } ?: return null
            val year = g[3].toIntOrNull() ?: today.year
            return LocalDate.of(year, month, day)
        }
        val day = Regex("(?<!\\d)(\\d{1,2})(?:st|nd|rd|th)?(?!\\d)").find(text)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        if (day !in 1..31) return null
        val weekdayText = weekdays.firstOrNull { Regex("(?<![a-z])$it(?![a-z])", RegexOption.IGNORE_CASE).containsMatchIn(text) } ?: return null
        val target = (1..7).map { today.plusDays(it.toLong()) }.firstOrNull { it.dayOfMonth == day && it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).startsWith(weekdayText.take(3), true) }
        return target ?: runCatching { LocalDate.of(today.year, today.month, day) }.getOrNull()
    }
}

private object XlsxReader {
    fun readFirstSheet(input: InputStream): List<List<String>> {
        val entries = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) { val out = ByteArrayOutputStream(); val buf = ByteArray(8192); while (true) { val n = zip.read(buf); if (n < 0) break; out.write(buf, 0, n) }; entries[entry.name] = out.toByteArray() }
            }
        }
        val strings = entries["xl/sharedStrings.xml"]?.let(::parseStrings) ?: emptyList()
        val path = resolveSheet(entries) ?: "xl/worksheets/sheet1.xml"
        return entries[path]?.let { parseSheet(it, strings) } ?: emptyList()
    }
    private fun parser(bytes: ByteArray) = Xml.newPullParser().apply { setInput(bytes.inputStream(), "UTF-8") }
    private fun resolveSheet(entries: Map<String, ByteArray>): String? {
        val wb = entries["xl/workbook.xml"] ?: return null
        val p = parser(wb); var rid: String? = null
        while (p.eventType != XmlPullParser.END_DOCUMENT) { if (p.eventType == XmlPullParser.START_TAG && p.name == "sheet") { rid = p.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id"); break }; p.next() }
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: return "xl/worksheets/sheet1.xml"
        val rp = parser(rels); var target: String? = null
        while (rp.eventType != XmlPullParser.END_DOCUMENT) { if (rp.eventType == XmlPullParser.START_TAG && rp.name == "Relationship" && rp.getAttributeValue(null, "Id") == rid) { target = rp.getAttributeValue(null, "Target"); break }; rp.next() }
        return target?.let { if (it.startsWith("/")) it.drop(1) else "xl/$it" }?.split('/')?.fold(mutableListOf<String>()) { acc, part -> when (part) { ".." -> if (acc.isNotEmpty()) acc.removeAt(acc.lastIndex); ".", "" -> Unit; else -> acc.add(part) }; acc }?.joinToString("/")
    }
    private fun parseStrings(bytes: ByteArray): List<String> {
        val out = mutableListOf<String>(); val p = parser(bytes); val current = StringBuilder(); var inside = false
        while (p.eventType != XmlPullParser.END_DOCUMENT) { when (p.eventType) { XmlPullParser.START_TAG -> if (p.name == "si") { inside = true; current.setLength(0) }; XmlPullParser.TEXT -> if (inside) current.append(p.text); XmlPullParser.END_TAG -> if (p.name == "si") { out.add(current.toString()); inside = false } }; p.next() }
        return out
    }
    private fun parseSheet(bytes: ByteArray, strings: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>(); val p = parser(bytes); var row = -1; var col = -1; var type = ""; var value = StringBuilder(); var capture = false
        while (p.eventType != XmlPullParser.END_DOCUMENT) { when (p.eventType) {
            XmlPullParser.START_TAG -> when (p.name) { "row" -> { row = (p.getAttributeValue(null, "r")?.toIntOrNull()?.minus(1) ?: row + 1); while (rows.size <= row) rows.add(mutableListOf()) }; "c" -> { type = p.getAttributeValue(null, "t") ?: ""; col = p.getAttributeValue(null, "r")?.let(::colIndex) ?: col + 1; value = StringBuilder() }; "v" -> capture = true; "t" -> capture = true }
            XmlPullParser.TEXT -> if (capture) value.append(p.text)
            XmlPullParser.END_TAG -> when (p.name) { "v", "t" -> capture = false; "c" -> { if (row >= 0 && col >= 0) { val s = value.toString(); val text = if (type == "s") strings.getOrNull(s.toIntOrNull() ?: -1).orEmpty() else s; while (rows[row].size <= col) rows[row].add(""); rows[row][col] = text } } }
        }; p.next() }
        return rows
    }
    private fun colIndex(ref: String): Int { var col = 0; for (c in ref) { if (!c.isLetter()) break; col = col * 26 + (c.uppercaseChar() - 'A' + 1) }; return col - 1 }
}
