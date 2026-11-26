package com.developers.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class AttendanceExcelExporterTest {

    @Test
    fun `summarizePayroll groups by employee, counts distinct days and computes pay`() {
        val records = listOf(
            AttendanceRecord(
                id = "1", key = "k1", userId = "u1", employeeName = "Justino",
                type = "ENTRADA", timestamp = 1_764_100_000_000L,
                dateFormatted = "25 Nov 2026", timeFormatted = "08:00 AM"
            ),
            AttendanceRecord(
                id = "2", key = "k2", userId = "u1", employeeName = "Justino",
                type = "SALIDA", timestamp = 1_764_130_000_000L,
                dateFormatted = "25 Nov 2026", timeFormatted = "05:00 PM"
            ),
            AttendanceRecord(
                id = "3", key = "k3", userId = "u2", employeeName = "Ana",
                type = "ENTRADA", timestamp = 1_764_140_000_000L,
                dateFormatted = "26 Nov 2026", timeFormatted = "08:10 AM"
            )
        )

        val summary = summarizePayroll(records, dailyRate = 300.0)
        assertEquals(2, summary.size)

        val ana = summary[0]
        assertEquals("Ana", ana.name)
        assertEquals(1, ana.days)
        assertEquals(1, ana.entries)
        assertEquals(0, ana.exits)
        assertEquals(300.0, ana.totalPay, 0.001)
        assertEquals("Parcial / Pendiente", ana.status)

        val justino = summary[1]
        assertEquals("Justino", justino.name)
        assertEquals(1, justino.days)
        assertEquals(1, justino.entries)
        assertEquals(1, justino.exits)
        assertEquals(300.0, justino.totalPay, 0.001)
        assertEquals("Activo / Completo", justino.status)
    }

    @Test
    fun `writeAttendancePayrollXlsx generates valid OOXML zip package with well-formed XML and valid indices`() {
        val records = listOf(
            AttendanceRecord(
                id = "1", key = "k1", userId = "u1",
                employeeName = "Justino & Co <Pan> \"Norte\"",
                type = "ENTRADA", timestamp = 1_764_100_000_000L,
                dateFormatted = "25 Nov 2026", timeFormatted = "08:00 AM"
            ),
            AttendanceRecord(
                id = "2", key = "k2", userId = "u1",
                employeeName = "Justino & Co <Pan> \"Norte\"",
                type = "SALIDA", timestamp = 1_764_130_000_000L,
                dateFormatted = "25 Nov 2026", timeFormatted = "05:00 PM"
            ),
            AttendanceRecord(
                id = "3", key = "k3", userId = "u2",
                employeeName = "José López",
                type = "ENTRADA", timestamp = 1_764_140_000_000L,
                dateFormatted = "26 Nov 2026", timeFormatted = "09:15 AM"
            )
        )

        val baos = ByteArrayOutputStream()
        writeAttendancePayrollXlsx(records, baos, generatedAt = Date(1_764_150_000_000L), locale = Locale.US)

        val entries = unzipEntries(baos.toByteArray())
        val expectedEntries = setOf(
            "[Content_Types].xml",
            "_rels/.rels",
            "docProps/core.xml",
            "docProps/app.xml",
            "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels",
            "xl/styles.xml",
            "xl/sharedStrings.xml",
            "xl/worksheets/sheet1.xml",
            "xl/worksheets/sheet2.xml"
        )
        assertEquals(expectedEntries, entries.keys)

        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        val docs = entries.mapValues { (_, bytes) ->
            factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
        }

        val stylesDoc = docs.getValue("xl/styles.xml")
        val fills = stylesDoc.getElementsByTagName("fill")
        assertTrue(fills.length >= 2)
        val fill0Pattern = (fills.item(0) as Element).getElementsByTagName("patternFill").item(0) as Element
        val fill1Pattern = (fills.item(1) as Element).getElementsByTagName("patternFill").item(0) as Element
        assertEquals("none", fill0Pattern.getAttribute("patternType"))
        assertEquals("gray125", fill1Pattern.getAttribute("patternType"))

        val cellXfsCount = (stylesDoc.getElementsByTagName("cellXfs").item(0) as Element)
            .getAttribute("count").toInt()
        val sharedStringCount = docs.getValue("xl/sharedStrings.xml")
            .documentElement.getAttribute("uniqueCount").toInt()

        for (sheetName in listOf("xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml")) {
            val sheetDoc = docs.getValue(sheetName)
            val cells = sheetDoc.getElementsByTagName("c")
            assertTrue(cells.length > 0)
            for (i in 0 until cells.length) {
                val cell = cells.item(i) as Element
                val styleAttr = cell.getAttribute("s")
                if (styleAttr.isNotEmpty()) {
                    val styleIdx = styleAttr.toInt()
                    assertTrue("Invalid style index $styleIdx in $sheetName", styleIdx in 0 until cellXfsCount)
                }
                if (cell.getAttribute("t") == "s") {
                    val vText = cell.getElementsByTagName("v").item(0).textContent.toInt()
                    assertTrue("Invalid sharedString index $vText in $sheetName", vText in 0 until sharedStringCount)
                }
            }
            val formulas = sheetDoc.getElementsByTagName("f")
            for (i in 0 until formulas.length) {
                val formulaText = formulas.item(i).textContent
                assertFalse("Formula must not start with '=': $formulaText", formulaText.startsWith("="))
                val parentCell = formulas.item(i).parentNode as Element
                assertTrue("Formula cell should include cached <v>", parentCell.getElementsByTagName("v").length == 1)
            }
        }
    }

    @Test
    fun `writeAttendancePayrollXlsx handles empty records cleanly`() {
        val baos = ByteArrayOutputStream()
        writeAttendancePayrollXlsx(emptyList(), baos, generatedAt = Date(1_764_150_000_000L), locale = Locale.US)

        val entries = unzipEntries(baos.toByteArray())
        assertEquals(10, entries.size)
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        for ((name, bytes) in entries) {
            factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).also {
                assertTrue("Empty root for $name", it.documentElement.tagName.isNotBlank())
            }
        }
    }

    private fun unzipEntries(zipBytes: ByteArray): Map<String, ByteArray> {
        val map = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zis ->
            while (true) {
                val entry = zis.nextEntry ?: break
                map[entry.name] = zis.readBytes()
                zis.closeEntry()
            }
        }
        return map
    }
}
