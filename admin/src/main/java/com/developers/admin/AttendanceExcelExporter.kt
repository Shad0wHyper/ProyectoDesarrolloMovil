package com.developers.admin

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal const val XLSX_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

internal data class PayrollEmployeeSummary(
    val name: String,
    val days: Int,
    val entries: Int,
    val exits: Int,
    val dailyRate: Double,
    val totalPay: Double,
    val status: String
)

internal fun summarizePayroll(
    records: List<AttendanceRecord>,
    dailyRate: Double = 300.0
): List<PayrollEmployeeSummary> {
    if (records.isEmpty()) return emptyList()
    val fallbackDayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    return records
        .groupBy { it.userId.ifBlank { it.employeeName } }
        .values
        .map { empRecords ->
            val name = empRecords.firstNotNullOfOrNull { it.employeeName.takeIf(String::isNotBlank) }
                ?: "Empleado"
            val entries = empRecords.count { it.type == "ENTRADA" }
            val exits = empRecords.count { it.type == "SALIDA" }
            val days = empRecords.mapNotNull { rec ->
                rec.dateFormatted.takeIf(String::isNotBlank)
                    ?: if (rec.timestamp > 0L) fallbackDayFormat.format(Date(rec.timestamp)) else null
            }.distinct().size
            val totalPay = days * dailyRate
            val status = if (entries > 0 && exits > 0) "Activo / Completo" else "Parcial / Pendiente"
            PayrollEmployeeSummary(
                name = name,
                days = days,
                entries = entries,
                exits = exits,
                dailyRate = dailyRate,
                totalPay = totalPay,
                status = status
            )
        }
        .sortedWith(compareBy<PayrollEmployeeSummary> { it.name.lowercase(Locale.ROOT) }.thenBy { it.name })
}

internal fun exportAttendancePayrollFile(
    cacheDir: File,
    records: List<AttendanceRecord>,
    generatedAt: Date = Date(),
    dailyRate: Double = 300.0,
    locale: Locale = Locale.getDefault()
): File {
    if (!cacheDir.exists()) cacheDir.mkdirs()
    val stamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(generatedAt)
    val file = File(cacheDir, "Nomina_Asistencia_$stamp.xlsx")
    FileOutputStream(file).use { out ->
        writeAttendancePayrollXlsx(
            records = records,
            output = out,
            generatedAt = generatedAt,
            dailyRate = dailyRate,
            locale = locale
        )
    }
    return file
}

internal fun writeAttendancePayrollXlsx(
    records: List<AttendanceRecord>,
    output: OutputStream,
    generatedAt: Date = Date(),
    dailyRate: Double = 300.0,
    locale: Locale = Locale.getDefault()
) {
    val sortedRecords = records.sortedByDescending { it.timestamp }
    val summaryList = summarizePayroll(sortedRecords, dailyRate)
    val strings = SharedStringTable()

    val displayDate = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", locale).format(generatedAt)
    val isoUtcDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(generatedAt)

    val sheet1Xml = buildSummarySheetXml(
        records = sortedRecords,
        summaryList = summaryList,
        displayDate = displayDate,
        strings = strings
    )
    val sheet2Xml = buildDetailsSheetXml(
        records = sortedRecords,
        displayDate = displayDate,
        strings = strings
    )

    ZipOutputStream(output, StandardCharsets.UTF_8).use { zip ->
        writeZipEntry(zip, "[Content_Types].xml", contentTypesXml())
        writeZipEntry(zip, "_rels/.rels", rootRelsXml())
        writeZipEntry(zip, "docProps/core.xml", corePropsXml(isoUtcDate))
        writeZipEntry(zip, "docProps/app.xml", appPropsXml())
        writeZipEntry(zip, "xl/workbook.xml", workbookXml())
        writeZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml())
        writeZipEntry(zip, "xl/styles.xml", stylesXml())
        writeZipEntry(zip, "xl/sharedStrings.xml", strings.toXml())
        writeZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml)
        writeZipEntry(zip, "xl/worksheets/sheet2.xml", sheet2Xml)
    }
}

private enum class XlsxStyle(val index: Int) {
    Default(0),
    MainHeader(1),
    SubHeader(2),
    MetaLabel(3),
    MetaValue(4),
    KpiLabel(5),
    KpiValueNavy(6),
    KpiValueCurrency(7),
    KpiValueInt(8),
    SectionBanner(9),
    HeaderSheet1(10),
    HeaderSheet1Left(11),
    HeaderSheet1Right(12),
    HeaderSheet2(13),
    HeaderSheet2Left(14),
    DataCenterEven(15),
    DataCenterOdd(16),
    DataCenterBoldEven(17),
    DataCenterBoldOdd(18),
    DataLeftBoldEven(19),
    DataLeftBoldOdd(20),
    DataIntEven(21),
    DataIntOdd(22),
    DataCurrencyEven(23),
    DataCurrencyOdd(24),
    DataPayEven(25),
    DataPayOdd(26),
    BadgeGreen(27),
    BadgeAmber(28),
    BadgeRed(29),
    BadgeBlue(30),
    TotalLabel(31),
    TotalCenter(32),
    TotalInt(33),
    TotalCurrency(34),
    FooterNote(35)
}

private class SharedStringTable {
    private val items = mutableListOf<String>()
    private val indexByText = HashMap<String, Int>()

    fun intern(raw: String): Int {
        val sanitized = sanitizeXmlText(raw)
        return indexByText.getOrPut(sanitized) {
            val idx = items.size
            items.add(sanitized)
            idx
        }
    }

    fun toXml(): String {
        val sb = StringBuilder(items.size * 48 + 160)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<sst xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" count=\"")
            .append(items.size)
            .append("\" uniqueCount=\"")
            .append(items.size)
            .append("\">")
        for (text in items) {
            val preserve = text.startsWith(' ') || text.endsWith(' ')
            sb.append("<si><t")
            if (preserve) sb.append(" xml:space=\"preserve\"")
            sb.append(">").append(escapeXml(text)).append("</t></si>")
        }
        sb.append("</sst>")
        return sb.toString()
    }
}

private class SheetBuilder(private val strings: SharedStringTable) {
    private val rows = StringBuilder()
    private val merges = mutableListOf<String>()

    fun row(rowNum: Int, heightPt: Double? = null, block: RowBuilder.() -> Unit) {
        rows.append("<row r=\"").append(rowNum).append("\"")
        if (heightPt != null) {
            rows.append(" ht=\"").append(formatDouble(heightPt)).append("\" customHeight=\"1\"")
        }
        rows.append(">")
        RowBuilder(rowNum, rows, strings).block()
        rows.append("</row>")
    }

    fun merge(ref: String) {
        merges.add(ref)
    }

    fun build(
        colWidths: List<Double>,
        freezeSplitRow: Int? = null,
        autoFilterRef: String? = null
    ): String {
        val sb = StringBuilder(rows.length + 1024)
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetPr><pageSetUpPr fitToPage=\"1\"/></sheetPr>")
        sb.append("<sheetViews><sheetView workbookViewId=\"0\">")
        if (freezeSplitRow != null) {
            val topLeftRow = freezeSplitRow + 1
            sb.append("<pane ySplit=\"").append(freezeSplitRow)
                .append("\" topLeftCell=\"A").append(topLeftRow)
                .append("\" activePane=\"bottomLeft\" state=\"frozen\"/>")
        }
        sb.append("</sheetView></sheetViews>")
        sb.append("<sheetFormatPr defaultRowHeight=\"20\"/>")
        sb.append("<cols>")
        colWidths.forEachIndexed { idx, width ->
            val col = idx + 1
            sb.append("<col min=\"").append(col).append("\" max=\"").append(col)
                .append("\" width=\"").append(formatDouble(width)).append("\" customWidth=\"1\"/>")
        }
        sb.append("</cols>")
        sb.append("<sheetData>").append(rows).append("</sheetData>")
        if (autoFilterRef != null) {
            sb.append("<autoFilter ref=\"").append(autoFilterRef).append("\"/>")
        }
        if (merges.isNotEmpty()) {
            sb.append("<mergeCells count=\"").append(merges.size).append("\">")
            for (ref in merges) {
                sb.append("<mergeCell ref=\"").append(ref).append("\"/>")
            }
            sb.append("</mergeCells>")
        }
        sb.append("<pageMargins left=\"0.4\" right=\"0.4\" top=\"0.5\" bottom=\"0.5\" header=\"0.2\" footer=\"0.2\"/>")
        sb.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\" paperSize=\"1\"/>")
        sb.append("</worksheet>")
        return sb.toString()
    }
}

private class RowBuilder(
    private val rowNum: Int,
    private val out: StringBuilder,
    private val strings: SharedStringTable
) {
    fun text(colIndex: Int, value: String, style: XlsxStyle) {
        val ref = cellRef(colIndex, rowNum)
        val sid = strings.intern(value)
        out.append("<c r=\"").append(ref).append("\" t=\"s\" s=\"").append(style.index)
            .append("\"><v>").append(sid).append("</v></c>")
    }

    fun blank(colIndex: Int, style: XlsxStyle) {
        val ref = cellRef(colIndex, rowNum)
        out.append("<c r=\"").append(ref).append("\" s=\"").append(style.index).append("\"/>")
    }

    fun mergedText(startCol: Int, endCol: Int, value: String, style: XlsxStyle) {
        text(startCol, value, style)
        for (col in (startCol + 1)..endCol) {
            blank(col, style)
        }
    }

    fun number(colIndex: Int, value: Number, style: XlsxStyle) {
        val ref = cellRef(colIndex, rowNum)
        val formatted = when (value) {
            is Int, is Long -> value.toString()
            else -> formatDouble(value.toDouble())
        }
        out.append("<c r=\"").append(ref).append("\" s=\"").append(style.index)
            .append("\"><v>").append(formatted).append("</v></c>")
    }

    fun mergedNumber(startCol: Int, endCol: Int, value: Number, style: XlsxStyle) {
        number(startCol, value, style)
        for (col in (startCol + 1)..endCol) {
            blank(col, style)
        }
    }

    fun formula(colIndex: Int, formulaExpr: String, cachedValue: Double, style: XlsxStyle, isInteger: Boolean = false) {
        val ref = cellRef(colIndex, rowNum)
        val cachedStr = if (isInteger) cachedValue.toLong().toString() else formatDouble(cachedValue)
        out.append("<c r=\"").append(ref).append("\" s=\"").append(style.index)
            .append("\"><f>").append(escapeXml(formulaExpr)).append("</f><v>")
            .append(cachedStr).append("</v></c>")
    }

    fun mergedFormula(startCol: Int, endCol: Int, formulaExpr: String, cachedValue: Double, style: XlsxStyle) {
        formula(startCol, formulaExpr, cachedValue, style)
        for (col in (startCol + 1)..endCol) {
            blank(col, style)
        }
    }
}

private fun buildSummarySheetXml(
    records: List<AttendanceRecord>,
    summaryList: List<PayrollEmployeeSummary>,
    displayDate: String,
    strings: SharedStringTable
): String {
    val sheet = SheetBuilder(strings)

    val totalEmpleados = summaryList.size
    val totalRegistros = records.size
    val totalEntradasGlobal = records.count { it.type == "ENTRADA" }
    val totalSalidasGlobal = records.count { it.type == "SALIDA" }
    val granTotalDias = summaryList.sumOf { it.days }
    val granTotalEntradas = summaryList.sumOf { it.entries }
    val granTotalSalidas = summaryList.sumOf { it.exits }
    val granTotalNomina = summaryList.sumOf { it.totalPay }

    val firstDataRow = 10
    val lastDataRow = if (summaryList.isEmpty()) firstDataRow else firstDataRow + summaryList.size - 1

    // Banner principal
    sheet.row(1, heightPt = 30.0) {
        mergedText(1, 8, "PANAPP - CONTROL DE ASISTENCIA Y NÓMINA EMPRESARIAL", XlsxStyle.MainHeader)
    }
    sheet.merge("A1:H1")

    sheet.row(2, heightPt = 22.0) {
        mergedText(1, 8, "REPORTE OFICIAL DE PERSONAL, ASISTENCIAS Y ESTIMACIÓN DE PAGOS", XlsxStyle.SubHeader)
    }
    sheet.merge("A2:H2")

    // Metadatos
    sheet.row(3, heightPt = 22.0) {
        text(1, "Fecha:", XlsxStyle.MetaLabel)
        mergedText(2, 4, displayDate, XlsxStyle.MetaValue)
        text(5, "Sistema Origen:", XlsxStyle.MetaLabel)
        mergedText(6, 8, "PanApp ERP - Control de Personal", XlsxStyle.MetaValue)
    }
    sheet.merge("B3:D3")
    sheet.merge("F3:H3")

    sheet.row(4, heightPt = 10.0) {}

    // Tarjetas KPI (2 columnas por tarjeta)
    sheet.row(5, heightPt = 20.0) {
        mergedText(1, 2, "TOTAL EMPLEADOS", XlsxStyle.KpiLabel)
        mergedText(3, 4, "TOTAL REGISTROS", XlsxStyle.KpiLabel)
        mergedText(5, 6, "ENTRADAS / SALIDAS", XlsxStyle.KpiLabel)
        mergedText(7, 8, "NÓMINA TOTAL ESTIMADA", XlsxStyle.KpiLabel)
    }
    sheet.merge("A5:B5")
    sheet.merge("C5:D5")
    sheet.merge("E5:F5")
    sheet.merge("G5:H5")

    sheet.row(6, heightPt = 26.0) {
        mergedNumber(1, 2, totalEmpleados, XlsxStyle.KpiValueInt)
        mergedNumber(3, 4, totalRegistros, XlsxStyle.KpiValueInt)
        mergedText(5, 6, "$totalEntradasGlobal / $totalSalidasGlobal", XlsxStyle.KpiValueNavy)
        if (summaryList.isNotEmpty()) {
            mergedFormula(7, 8, "SUM(G$firstDataRow:G$lastDataRow)", granTotalNomina, XlsxStyle.KpiValueCurrency)
        } else {
            mergedNumber(7, 8, 0.0, XlsxStyle.KpiValueCurrency)
        }
    }
    sheet.merge("A6:B6")
    sheet.merge("C6:D6")
    sheet.merge("E6:F6")
    sheet.merge("G6:H6")

    sheet.row(7, heightPt = 12.0) {}

    // Sección Tabla 1
    sheet.row(8, heightPt = 24.0) {
        mergedText(1, 8, "TABLA 1: RESUMEN GENERAL DE NÓMINA Y PAGOS POR EMPLEADO", XlsxStyle.SectionBanner)
    }
    sheet.merge("A8:H8")

    sheet.row(9, heightPt = 28.0) {
        text(1, "N°", XlsxStyle.HeaderSheet1)
        text(2, "Empleado", XlsxStyle.HeaderSheet1Left)
        text(3, "Días Asistidos", XlsxStyle.HeaderSheet1)
        text(4, "Total Entradas", XlsxStyle.HeaderSheet1)
        text(5, "Total Salidas", XlsxStyle.HeaderSheet1)
        text(6, "Sueldo Base Diario", XlsxStyle.HeaderSheet1Right)
        text(7, "Pago Estimado Periodo", XlsxStyle.HeaderSheet1Right)
        text(8, "Estatus Nómina", XlsxStyle.HeaderSheet1)
    }

    summaryList.forEachIndexed { idx, emp ->
        val rowNum = firstDataRow + idx
        val even = idx % 2 == 0
        val centerStyle = if (even) XlsxStyle.DataCenterEven else XlsxStyle.DataCenterOdd
        val nameStyle = if (even) XlsxStyle.DataLeftBoldEven else XlsxStyle.DataLeftBoldOdd
        val intStyle = if (even) XlsxStyle.DataIntEven else XlsxStyle.DataIntOdd
        val currencyStyle = if (even) XlsxStyle.DataCurrencyEven else XlsxStyle.DataCurrencyOdd
        val payStyle = if (even) XlsxStyle.DataPayEven else XlsxStyle.DataPayOdd
        val badgeStyle = if (emp.status == "Activo / Completo") XlsxStyle.BadgeGreen else XlsxStyle.BadgeAmber

        sheet.row(rowNum, heightPt = 22.0) {
            number(1, idx + 1, centerStyle)
            text(2, emp.name, nameStyle)
            number(3, emp.days, intStyle)
            number(4, emp.entries, intStyle)
            number(5, emp.exits, intStyle)
            number(6, emp.dailyRate, currencyStyle)
            formula(7, "C$rowNum*F$rowNum", emp.totalPay, payStyle)
            text(8, emp.status, badgeStyle)
        }
    }

    val totalsRow = if (summaryList.isEmpty()) firstDataRow else lastDataRow + 1
    sheet.row(totalsRow, heightPt = 24.0) {
        mergedText(1, 2, "TOTALES GENERALES:", XlsxStyle.TotalLabel)
        if (summaryList.isNotEmpty()) {
            formula(3, "SUM(C$firstDataRow:C$lastDataRow)", granTotalDias.toDouble(), XlsxStyle.TotalInt, isInteger = true)
            formula(4, "SUM(D$firstDataRow:D$lastDataRow)", granTotalEntradas.toDouble(), XlsxStyle.TotalInt, isInteger = true)
            formula(5, "SUM(E$firstDataRow:E$lastDataRow)", granTotalSalidas.toDouble(), XlsxStyle.TotalInt, isInteger = true)
        } else {
            number(3, 0, XlsxStyle.TotalInt)
            number(4, 0, XlsxStyle.TotalInt)
            number(5, 0, XlsxStyle.TotalInt)
        }
        text(6, "-", XlsxStyle.TotalCenter)
        if (summaryList.isNotEmpty()) {
            formula(7, "SUM(G$firstDataRow:G$lastDataRow)", granTotalNomina, XlsxStyle.TotalCurrency)
        } else {
            number(7, 0.0, XlsxStyle.TotalCurrency)
        }
        text(8, "-", XlsxStyle.TotalCenter)
    }
    sheet.merge("A$totalsRow:B$totalsRow")

    val footerRow = totalsRow + 2
    sheet.row(footerRow, heightPt = 18.0) {
        mergedText(
            1,
            8,
            "PanApp Enterprise Payroll System • Documento oficial exportado el $displayDate • Información Confidencial",
            XlsxStyle.FooterNote
        )
    }
    sheet.merge("A$footerRow:H$footerRow")

    return sheet.build(
        colWidths = listOf(10.0, 28.0, 16.0, 16.0, 16.0, 20.0, 22.0, 22.0)
    )
}

private fun buildDetailsSheetXml(
    records: List<AttendanceRecord>,
    displayDate: String,
    strings: SharedStringTable
): String {
    val sheet = SheetBuilder(strings)

    sheet.row(1, heightPt = 28.0) {
        mergedText(1, 7, "PANAPP - CONTROL DIARIO DE ASISTENCIA Y PUNTUALIDAD", XlsxStyle.SectionBanner)
    }
    sheet.merge("A1:G1")

    sheet.row(2, heightPt = 20.0) {
        mergedText(1, 7, "Generado el $displayDate • Registros ordenados del más reciente al más antiguo", XlsxStyle.MetaLabel)
    }
    sheet.merge("A2:G2")

    sheet.row(3, heightPt = 10.0) {}

    sheet.row(4, heightPt = 26.0) {
        text(1, "N°", XlsxStyle.HeaderSheet2)
        text(2, "Empleado", XlsxStyle.HeaderSheet2Left)
        text(3, "Fecha", XlsxStyle.HeaderSheet2)
        text(4, "Hora", XlsxStyle.HeaderSheet2)
        text(5, "Tipo de Registro", XlsxStyle.HeaderSheet2)
        text(6, "Puntualidad", XlsxStyle.HeaderSheet2)
        text(7, "Estado", XlsxStyle.HeaderSheet2)
    }

    val firstDataRow = 5
    records.forEachIndexed { idx, rec ->
        val rowNum = firstDataRow + idx
        val even = idx % 2 == 0
        val centerStyle = if (even) XlsxStyle.DataCenterEven else XlsxStyle.DataCenterOdd
        val nameStyle = if (even) XlsxStyle.DataLeftBoldEven else XlsxStyle.DataLeftBoldOdd
        val timeStyle = if (even) XlsxStyle.DataCenterBoldEven else XlsxStyle.DataCenterBoldOdd
        val isEntrada = rec.type == "ENTRADA"
        val tipoLabel = if (isEntrada) "ENTRADA" else "SALIDA"
        val tipoStyle = if (isEntrada) XlsxStyle.BadgeGreen else XlsxStyle.BadgeRed
        val puntLabel = if (isEntrada) "A tiempo (A)" else "Salida de Turno"
        val puntStyle = if (isEntrada) XlsxStyle.BadgeBlue else XlsxStyle.BadgeAmber

        sheet.row(rowNum, heightPt = 22.0) {
            number(1, idx + 1, centerStyle)
            text(2, rec.employeeName, nameStyle)
            text(3, rec.dateFormatted, centerStyle)
            text(4, rec.timeFormatted, timeStyle)
            text(5, tipoLabel, tipoStyle)
            text(6, puntLabel, puntStyle)
            text(7, "Registrado", centerStyle)
        }
    }

    val lastFilterRow = if (records.isEmpty()) 4 else firstDataRow + records.size - 1

    return sheet.build(
        colWidths = listOf(8.0, 28.0, 16.0, 14.0, 18.0, 18.0, 15.0),
        freezeSplitRow = 4,
        autoFilterRef = "A4:G$lastFilterRow"
    )
}

private fun cellRef(colIndex1Based: Int, rowNum: Int): String {
    var col = colIndex1Based
    val letters = StringBuilder()
    while (col > 0) {
        val rem = (col - 1) % 26
        letters.append(('A'.code + rem).toChar())
        col = (col - 1) / 26
    }
    return letters.reverse().append(rowNum).toString()
}

private fun formatDouble(value: Double): String {
    val clean = if (value == -0.0 || !value.isFinite()) 0.0 else value
    return String.format(Locale.US, "%.2f", clean)
        .let { if (it.endsWith(".00")) it.dropLast(3) else it.trimEnd('0').trimEnd('.') }
}

private fun sanitizeXmlText(input: String): String {
    val sb = StringBuilder(input.length)
    for (ch in input) {
        val code = ch.code
        val valid = code == 0x9 || code == 0xA || code == 0xD || (code >= 0x20 && code <= 0xD7FF) || (code >= 0xE000 && code <= 0xFFFD)
        if (valid) sb.append(ch)
    }
    return sb.toString()
}

private fun escapeXml(input: String): String {
    val sb = StringBuilder(input.length + 8)
    for (ch in input) {
        when (ch) {
            '&' -> sb.append("&amp;")
            '<' -> sb.append("&lt;")
            '>' -> sb.append("&gt;")
            '"' -> sb.append("&quot;")
            '\'' -> sb.append("&apos;")
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

private fun writeZipEntry(zip: ZipOutputStream, name: String, content: String) {
    zip.putNextEntry(ZipEntry(name))
    zip.write(content.toByteArray(StandardCharsets.UTF_8))
    zip.closeEntry()
}

private fun contentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
    <Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
    <Override PartName="/docProps/core.xml" ContentType="application/vnd.openxmlformats-package.core-properties+xml"/>
    <Override PartName="/docProps/app.xml" ContentType="application/vnd.openxmlformats-officedocument.extended-properties+xml"/>
</Types>"""

private fun rootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/package/2006/relationships/metadata/core-properties" Target="docProps/core.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/extended-properties" Target="docProps/app.xml"/>
</Relationships>"""

private fun corePropsXml(isoUtcDate: String): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<cp:coreProperties xmlns:cp="http://schemas.openxmlformats.org/package/2006/metadata/core-properties" xmlns:dc="http://purl.org/dc/elements/1.1/" xmlns:dcterms="http://purl.org/dc/terms/" xmlns:dcmitype="http://purl.org/dc/dcmitype/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <dc:title>PanApp - Nómina y Control de Asistencia</dc:title>
    <dc:creator>PanApp ERP</dc:creator>
    <cp:lastModifiedBy>PanApp ERP</cp:lastModifiedBy>
    <dcterms:created xsi:type="dcterms:W3CDTF">$isoUtcDate</dcterms:created>
    <dcterms:modified xsi:type="dcterms:W3CDTF">$isoUtcDate</dcterms:modified>
</cp:coreProperties>"""

private fun appPropsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Properties xmlns="http://schemas.openxmlformats.org/officeDocument/2006/extended-properties" xmlns:vt="http://schemas.openxmlformats.org/package/2006/docPropsVTypes">
    <Application>PanApp ERP</Application>
</Properties>"""

private fun workbookXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="Resumen Nomina" sheetId="1" r:id="rId1"/>
        <sheet name="Detalle Asistencias" sheetId="2" r:id="rId2"/>
    </sheets>
</workbook>"""

private fun workbookRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/sharedStrings" Target="sharedStrings.xml"/>
</Relationships>"""

private fun stylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <numFmts count="2">
        <numFmt numFmtId="164" formatCode="&quot;$&quot;#,##0.00"/>
        <numFmt numFmtId="165" formatCode="#,##0"/>
    </numFmts>
    <fonts count="16">
        <font><sz val="10"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
        <font><b/><sz val="14"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FF334155"/><name val="Calibri"/></font>
        <font><b/><sz val="9"/><color rgb="FF64748B"/><name val="Calibri"/></font>
        <font><b/><sz val="13"/><color rgb="FF1E3A8A"/><name val="Calibri"/></font>
        <font><b/><sz val="13"/><color rgb="FF059669"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FF1E293B"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FF059669"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FF15803D"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FFB45309"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FFB91C1C"/><name val="Calibri"/></font>
        <font><b/><sz val="10"/><color rgb="FF1E40AF"/><name val="Calibri"/></font>
        <font><b/><sz val="10.5"/><color rgb="FF0F172A"/><name val="Calibri"/></font>
        <font><b/><sz val="10.5"/><color rgb="FF047857"/><name val="Calibri"/></font>
        <font><i/><sz val="9"/><color rgb="FF64748B"/><name val="Calibri"/></font>
    </fonts>
    <fills count="15">
        <fill><patternFill patternType="none"/></fill>
        <fill><patternFill patternType="gray125"/></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF2563EB"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFF1F5F9"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFFFFFFF"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFF8FAFC"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF0F172A"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF1E40AF"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FF334155"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFFEF3C7"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFDBEAFE"/></patternFill></fill>
        <fill><patternFill patternType="solid"><fgColor rgb="FFE2E8F0"/></patternFill></fill>
    </fills>
    <borders count="11">
        <border><left/><right/><top/><bottom/><diagonal/></border>
        <border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right><top style="thin"><color rgb="FFCBD5E1"/></top><bottom style="thin"><color rgb="FFCBD5E1"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FF1E3A8A"/></left><right style="thin"><color rgb="FF1E3A8A"/></right><top style="thin"><color rgb="FF1E3A8A"/></top><bottom style="thin"><color rgb="FF1E3A8A"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FF0F172A"/></left><right style="thin"><color rgb="FF0F172A"/></right><top style="thin"><color rgb="FF0F172A"/></top><bottom style="thin"><color rgb="FF0F172A"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FF1E293B"/></left><right style="thin"><color rgb="FF1E293B"/></right><top style="thin"><color rgb="FF1E293B"/></top><bottom style="thin"><color rgb="FF1E293B"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFE2E8F0"/></left><right style="thin"><color rgb="FFE2E8F0"/></right><top style="thin"><color rgb="FFE2E8F0"/></top><bottom style="thin"><color rgb="FFE2E8F0"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFCBD5E1"/></left><right style="thin"><color rgb="FFCBD5E1"/></right><top style="medium"><color rgb="FF1E3A8A"/></top><bottom style="double"><color rgb="FF1E3A8A"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFBBF7D0"/></left><right style="thin"><color rgb="FFBBF7D0"/></right><top style="thin"><color rgb="FFBBF7D0"/></top><bottom style="thin"><color rgb="FFBBF7D0"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFFDE68A"/></left><right style="thin"><color rgb="FFFDE68A"/></right><top style="thin"><color rgb="FFFDE68A"/></top><bottom style="thin"><color rgb="FFFDE68A"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFFCA5A5"/></left><right style="thin"><color rgb="FFFCA5A5"/></right><top style="thin"><color rgb="FFFCA5A5"/></top><bottom style="thin"><color rgb="FFFCA5A5"/></bottom><diagonal/></border>
        <border><left style="thin"><color rgb="FFBFDBFE"/></left><right style="thin"><color rgb="FFBFDBFE"/></right><top style="thin"><color rgb="FFBFDBFE"/></top><bottom style="thin"><color rgb="FFBFDBFE"/></bottom><diagonal/></border>
    </borders>
    <cellStyleXfs count="1">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
    </cellStyleXfs>
    <cellXfs count="36">
        <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
        <xf numFmtId="0" fontId="1" fillId="2" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="2" fillId="3" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="3" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
        <xf numFmtId="0" fontId="0" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
        <xf numFmtId="0" fontId="4" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="5" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="164" fontId="6" fillId="5" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="165" fontId="5" fillId="5" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="2" fillId="7" borderId="3" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
        <xf numFmtId="0" fontId="2" fillId="8" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="2" fillId="8" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="2" fillId="8" borderId="2" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="2" fillId="9" borderId="4" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="2" fillId="9" borderId="4" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center" wrapText="1"/></xf>
        <xf numFmtId="0" fontId="0" fillId="5" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="0" fillId="6" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="7" fillId="5" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="7" fillId="6" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="7" fillId="5" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
        <xf numFmtId="0" fontId="7" fillId="6" borderId="5" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="left" vertical="center"/></xf>
        <xf numFmtId="165" fontId="0" fillId="5" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="165" fontId="0" fillId="6" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="164" fontId="0" fillId="5" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="164" fontId="0" fillId="6" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="164" fontId="8" fillId="5" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="164" fontId="8" fillId="6" borderId="5" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="0" fontId="9" fillId="10" borderId="7" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="10" fillId="11" borderId="8" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="11" fillId="12" borderId="9" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="12" fillId="13" borderId="10" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="0" fontId="13" fillId="14" borderId="6" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="0" fontId="13" fillId="14" borderId="6" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="165" fontId="13" fillId="14" borderId="6" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
        <xf numFmtId="164" fontId="14" fillId="14" borderId="6" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="right" vertical="center"/></xf>
        <xf numFmtId="0" fontId="15" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment horizontal="center" vertical="center"/></xf>
    </cellXfs>
    <cellStyles count="1">
        <cellStyle name="Normal" xfId="0" builtinId="0"/>
    </cellStyles>
</styleSheet>"""
