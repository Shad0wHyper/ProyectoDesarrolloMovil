package com.developers.admin

import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR
import com.developers.core.text.attendanceLabel

import androidx.compose.ui.res.stringResource
import com.developers.admin.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AttendanceRecord(
    val id: String = "",
    val employeeName: String = "",
    val type: String = "",
    val timestamp: Long = 0L,
    val dateFormatted: String = "",
    val timeFormatted: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit, isInsideTab: Boolean = false) {
    val isDarkMode = isSystemInDarkTheme()
    var records by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // ✨ Estados de Filtrado
    var textBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("TODOS") } // "TODOS", "ENTRADA", "SALIDA"

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // Obtenemos todos los usuarios y sus subcolecciones de asistencias (igual que en el módulo de empleado)
        db.collection("usuarios").get().addOnSuccessListener { userSnap ->
            val allRecords = mutableListOf<AttendanceRecord>()
            val users = userSnap.documents
            if (users.isEmpty()) {
                isLoading = false
                return@addOnSuccessListener
            }

            var processed = 0
            for (userDoc in users) {
                val userId = userDoc.id
                val employeeName = userDoc.getString("nombre") ?: localizedText(TextR.string.pan_employee)

                db.collection("usuarios").document(userId).collection("asistencias")
                    .get()
                    .addOnSuccessListener { attendanceSnap ->
                        for (doc in attendanceSnap.documents) {
                            allRecords.add(
                                AttendanceRecord(
                                    id = doc.id,
                                    employeeName = employeeName,
                                    type = doc.getString("type") ?: "ENTRADA",
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    dateFormatted = doc.getString("dateFormatted") ?: "",
                                    timeFormatted = doc.getString("timeFormatted") ?: ""
                                )
                            )
                        }
                        processed++
                        if (processed == users.size) {
                            records = allRecords.sortedByDescending { it.timestamp }
                            isLoading = false
                        }
                    }
                    .addOnFailureListener {
                        processed++
                        if (processed == users.size) {
                            records = allRecords.sortedByDescending { it.timestamp }
                            isLoading = false
                        }
                    }
            }
        }.addOnFailureListener {
            isLoading = false
        }
    }

    // ✨ Lógica de Filtrado en Tiempo Real
    val registrosFiltrados = records.filter { record ->
        val coincideNombre = record.employeeName.contains(textBusqueda, ignoreCase = true)
        val coincideFiltro = if (filtroSeleccionado == "TODOS") true else record.type == filtroSeleccionado
        coincideNombre && coincideFiltro
    }

    if (isInsideTab) {
        AttendanceContent(
            textBusqueda = textBusqueda,
            onTextBusquedaChange = { textBusqueda = it },
            filtroSeleccionado = filtroSeleccionado,
            onFiltroChange = { filtroSeleccionado = it },
            isLoading = isLoading,
            registrosFiltrados = registrosFiltrados,
            allRecords = records,
            isDarkMode = isDarkMode,
            cardColor = cardColor,
            textColor = textColor
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(stringResource(R.string.admin_attendance_history), fontWeight = FontWeight.Bold, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.admin_back), tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = bgColor
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                AttendanceContent(
                    textBusqueda = textBusqueda,
                    onTextBusquedaChange = { textBusqueda = it },
                    filtroSeleccionado = filtroSeleccionado,
                    onFiltroChange = { filtroSeleccionado = it },
                    isLoading = isLoading,
                    registrosFiltrados = registrosFiltrados,
                    allRecords = records,
                    isDarkMode = isDarkMode,
                    cardColor = cardColor,
                    textColor = textColor
                )
            }
        }
    }
}

@Composable
fun AttendanceContent(
    textBusqueda: String,
    onTextBusquedaChange: (String) -> Unit,
    filtroSeleccionado: String,
    onFiltroChange: (String) -> Unit,
    isLoading: Boolean,
    registrosFiltrados: List<AttendanceRecord>,
    allRecords: List<AttendanceRecord>,
    isDarkMode: Boolean,
    cardColor: Color,
    textColor: Color
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // ✨ BOTÓN EXPORTAR NOMINA EXCEL
        if (allRecords.isNotEmpty()) {
            Button(
                onClick = {
                    try {
                        val file = File(context.cacheDir, "Nomina_Asistencia_${System.currentTimeMillis()}.xls")
                        FileOutputStream(file).use { fos ->
                            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
                                // BOM para compatibilidad UTF-8 en Excel
                                writer.write("\uFEFF")
                                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
                                val dateStr = dateFormat.format(Date())

                                // Cálculos y métricas generales
                                val grouped = allRecords.groupBy { it.employeeName }
                                val totalEmpleados = grouped.size
                                val totalRegistros = allRecords.size
                                val totalEntradasGlobal = allRecords.count { it.type == "ENTRADA" }
                                val totalSalidasGlobal = allRecords.count { it.type == "SALIDA" }

                                var granTotalNomina = 0.0
                                var granTotalDias = 0
                                var granTotalEntradas = 0
                                var granTotalSalidas = 0

                                data class EmpSummary(
                                    val name: String,
                                    val days: Int,
                                    val entries: Int,
                                    val exits: Int,
                                    val dailyRate: Double,
                                    val totalPay: Double,
                                    val estatus: String
                                )

                                val summaryList = mutableListOf<EmpSummary>()
                                for ((employeeName, empRecords) in grouped) {
                                    val entries = empRecords.count { it.type == "ENTRADA" }
                                    val exits = empRecords.count { it.type == "SALIDA" }
                                    val days = empRecords.map { it.dateFormatted }.distinct().size
                                    val dailyRate = 300.0 // Sueldo base diario estándar
                                    val totalPay = days * dailyRate
                                    val estatus = if (entries > 0 && exits > 0) "Activo / Completo" else "Parcial / Pendiente"

                                    granTotalNomina += totalPay
                                    granTotalDias += days
                                    granTotalEntradas += entries
                                    granTotalSalidas += exits

                                    summaryList.add(
                                        EmpSummary(
                                            name = employeeName,
                                            days = days,
                                            entries = entries,
                                            exits = exits,
                                            dailyRate = dailyRate,
                                            totalPay = totalPay,
                                            estatus = estatus
                                        )
                                    )
                                }

                                val formattedGranTotalNomina = String.format(Locale.US, "$%,.2f", granTotalNomina)

                                // Estructura HTML profesional con estilos CSS e interoperabilidad total para Excel
                                writer.write("""
                                    <html xmlns:o="urn:schemas-microsoft-com:office:office"
                                          xmlns:x="urn:schemas-microsoft-com:office:excel"
                                          xmlns="http://www.w3.org/TR/REC-html40">
                                    <head>
                                    <meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>
                                    <!--[if gte mso 9]>
                                    <xml>
                                     <x:ExcelWorkbook>
                                      <x:ExcelWorksheets>
                                       <x:ExcelWorksheet>
                                        <x:Name>Nómina y Asistencia</x:Name>
                                        <x:WorksheetOptions>
                                         <x:DisplayGridlines/>
                                        </x:WorksheetOptions>
                                       </x:ExcelWorksheet>
                                      </x:ExcelWorksheets>
                                     </x:ExcelWorkbook>
                                    </xml>
                                    <![endif]-->
                                    <style>
                                        body {
                                            font-family: 'Segoe UI', Arial, sans-serif;
                                            background-color: #FFFFFF;
                                            color: #1E293B;
                                            margin: 0;
                                            padding: 20px;
                                        }
                                        table {
                                            border-collapse: collapse;
                                            width: 100%;
                                            margin-bottom: 24px;
                                        }
                                        .main-header {
                                            background-color: #1E3A8A;
                                            color: #FFFFFF;
                                            font-size: 16pt;
                                            font-weight: bold;
                                            text-align: center;
                                            padding: 14px;
                                            border: 1px solid #1E3A8A;
                                        }
                                        .sub-header {
                                            background-color: #2563EB;
                                            color: #FFFFFF;
                                            font-size: 10pt;
                                            font-weight: bold;
                                            text-align: center;
                                            padding: 6px;
                                            border: 1px solid #1D4ED8;
                                        }
                                        .meta-table td {
                                            border: 1px solid #CBD5E1;
                                            padding: 8px 12px;
                                            font-size: 10pt;
                                        }
                                        .meta-label {
                                            background-color: #F1F5F9;
                                            font-weight: bold;
                                            color: #334155;
                                        }
                                        .kpi-card-title {
                                            background-color: #F8FAFC;
                                            font-weight: bold;
                                            font-size: 9pt;
                                            color: #64748B;
                                            text-align: center;
                                            padding: 6px;
                                            border: 1px solid #CBD5E1;
                                        }
                                        .kpi-card-value {
                                            background-color: #FFFFFF;
                                            font-weight: bold;
                                            font-size: 13pt;
                                            color: #1E3A8A;
                                            text-align: center;
                                            padding: 8px;
                                            border: 1px solid #CBD5E1;
                                        }
                                        .section-banner {
                                            background-color: #0F172A;
                                            color: #FFFFFF;
                                            font-size: 11pt;
                                            font-weight: bold;
                                            padding: 10px 14px;
                                            text-align: left;
                                            border: 1px solid #0F172A;
                                        }
                                        th {
                                            background-color: #1E40AF;
                                            color: #FFFFFF;
                                            font-size: 10pt;
                                            font-weight: bold;
                                            text-align: center;
                                            padding: 10px 8px;
                                            border: 1px solid #1E3A8A;
                                            vertical-align: middle;
                                        }
                                        th.th-sec {
                                            background-color: #334155;
                                            border: 1px solid #1E293B;
                                        }
                                        td {
                                            font-size: 10pt;
                                            padding: 8px 10px;
                                            border: 1px solid #E2E8F0;
                                            vertical-align: middle;
                                        }
                                        .tr-even { background-color: #FFFFFF; }
                                        .tr-odd { background-color: #F8FAFC; }
                                        .t-left { text-align: left; }
                                        .t-center { text-align: center; }
                                        .t-right { text-align: right; }
                                        .bold { font-weight: bold; }

                                        .badge-green {
                                            background-color: #DCFCE7;
                                            color: #15803D;
                                            font-weight: bold;
                                            padding: 4px 10px;
                                            border-radius: 12px;
                                            border: 1px solid #BBF7D0;
                                        }
                                        .badge-yellow {
                                            background-color: #FEF3C7;
                                            color: #B45309;
                                            font-weight: bold;
                                            padding: 4px 10px;
                                            border-radius: 12px;
                                            border: 1px solid #FDE68A;
                                        }
                                        .badge-red {
                                            background-color: #FEE2E2;
                                            color: #B91C1C;
                                            font-weight: bold;
                                            padding: 4px 10px;
                                            border-radius: 12px;
                                            border: 1px solid #FCA5A5;
                                        }
                                        .badge-blue {
                                            background-color: #DBEAFE;
                                            color: #1E40AF;
                                            font-weight: bold;
                                            padding: 4px 10px;
                                            border-radius: 12px;
                                            border: 1px solid #BFDBFE;
                                        }
                                        .tr-total {
                                            background-color: #E2E8F0;
                                            font-weight: bold;
                                            font-size: 10.5pt;
                                            border-top: 2px solid #1E3A8A;
                                            border-bottom: 3px double #1E3A8A;
                                        }
                                        .footer {
                                            margin-top: 20px;
                                            text-align: center;
                                            font-size: 9pt;
                                            color: #94A3B8;
                                            font-style: italic;
                                        }
                                    </style>
                                    </head>
                                    <body>
                                """.trimIndent())

                                // Encabezado Principal
                                writer.write("""
                                    <table>
                                        <tr>
                                            <td colspan="8" class="main-header">PANAPP - CONTROL DE ASISTENCIA Y NÓMINA EMPRESARIAL</td>
                                        </tr>
                                        <tr>
                                            <td colspan="8" class="sub-header">REPORTE OFICIAL DE PERSONAL, ASISTENCIAS Y ESTIMACIÓN DE PAGOS</td>
                                        </tr>
                                    </table>
                                """.trimIndent())

                                // Metadatos del Reporte
                                writer.write("""
                                    <table class="meta-table">
                                        <tr>
                                            <td class="meta-label" style="width: 150px;">Fecha de Generación:</td>
                                            <td style="width: 200px;">$dateStr</td>
                                            <td class="meta-label" style="width: 150px;">Sistema Origen:</td>
                                            <td>PanApp ERP - Control de Personal</td>
                                        </tr>
                                    </table>
                                """.trimIndent())

                                // Tarjetas KPI de Resumen
                                writer.write("""
                                    <table>
                                        <tr>
                                            <td class="kpi-card-title" style="width: 25%;">TOTAL EMPLEADOS</td>
                                            <td class="kpi-card-title" style="width: 25%;">TOTAL REGISTROS</td>
                                            <td class="kpi-card-title" style="width: 25%;">ENTRADAS / SALIDAS</td>
                                            <td class="kpi-card-title" style="width: 25%;">NÓMINA TOTAL ESTIMADA</td>
                                        </tr>
                                        <tr>
                                            <td class="kpi-card-value">$totalEmpleados</td>
                                            <td class="kpi-card-value">$totalRegistros</td>
                                            <td class="kpi-card-value">$totalEntradasGlobal / $totalSalidasGlobal</td>
                                            <td class="kpi-card-value" style="color: #059669;">$formattedGranTotalNomina</td>
                                        </tr>
                                    </table>
                                """.trimIndent())

                                // Tabla 1: Resumen General de Nómina
                                writer.write("""
                                    <table>
                                        <tr>
                                            <td colspan="8" class="section-banner">TABLA 1: RESUMEN GENERAL DE NÓMINA Y PAGOS POR EMPLEADO</td>
                                        </tr>
                                        <tr>
                                            <th style="width: 45px;">N°</th>
                                            <th style="width: 220px;" class="t-left">Empleado</th>
                                            <th style="width: 120px;">Días Asistidos</th>
                                            <th style="width: 120px;">Total Entradas</th>
                                            <th style="width: 120px;">Total Salidas</th>
                                            <th style="width: 150px;" class="t-right">Sueldo Base Diario</th>
                                            <th style="width: 170px;" class="t-right">Pago Estimado Periodo</th>
                                            <th style="width: 160px;">Estatus Nómina</th>
                                        </tr>
                                """.trimIndent())

                                summaryList.forEachIndexed { index, emp ->
                                    val trClass = if (index % 2 == 0) "tr-even" else "tr-odd"
                                    val formattedDaily = String.format(Locale.US, "$%,.2f", emp.dailyRate)
                                    val formattedPay = String.format(Locale.US, "$%,.2f", emp.totalPay)
                                    val badgeClass = if (emp.estatus == "Activo / Completo") "badge-green" else "badge-yellow"

                                    writer.write("""
                                        <tr class="$trClass">
                                            <td class="t-center">${index + 1}</td>
                                            <td class="t-left bold">${emp.name}</td>
                                            <td class="t-center">${emp.days}</td>
                                            <td class="t-center">${emp.entries}</td>
                                            <td class="t-center">${emp.exits}</td>
                                            <td class="t-right">$formattedDaily</td>
                                            <td class="t-right bold" style="color: #059669;">$formattedPay</td>
                                            <td class="t-center"><span class="$badgeClass">${emp.estatus}</span></td>
                                        </tr>
                                    """.trimIndent())
                                }

                                // Fila de Totales Generales de Tabla 1
                                writer.write("""
                                    <tr class="tr-total">
                                        <td colspan="2" class="t-right">TOTALES GENERALES:</td>
                                        <td class="t-center">$granTotalDias</td>
                                        <td class="t-center">$granTotalEntradas</td>
                                        <td class="t-center">$granTotalSalidas</td>
                                        <td class="t-right">-</td>
                                        <td class="t-right" style="color: #047857;">$formattedGranTotalNomina</td>
                                        <td class="t-center">-</td>
                                    </tr>
                                    </table>
                                """.trimIndent())

                                // Tabla 2: Control Diario de Asistencia y Puntualidad
                                writer.write("""
                                    <table>
                                        <tr>
                                            <td colspan="7" class="section-banner" style="background-color: #334155;">TABLA 2: CONTROL DIARIO DE ASISTENCIA Y PUNTUALIDAD</td>
                                        </tr>
                                        <tr>
                                            <th class="th-sec" style="width: 45px;">N°</th>
                                            <th class="th-sec" style="width: 220px;" class="t-left">Empleado</th>
                                            <th class="th-sec" style="width: 120px;">Fecha</th>
                                            <th class="th-sec" style="width: 100px;">Hora</th>
                                            <th class="th-sec" style="width: 140px;">Tipo de Registro</th>
                                            <th class="th-sec" style="width: 150px;">Puntualidad</th>
                                            <th class="th-sec" style="width: 120px;">Estado</th>
                                        </tr>
                                """.trimIndent())

                                allRecords.forEachIndexed { index, rec ->
                                    val trClass = if (index % 2 == 0) "tr-even" else "tr-odd"
                                    val isEntrada = rec.type == "ENTRADA"
                                    val tipoBadge = if (isEntrada) "<span class=\"badge-green\">ENTRADA</span>" else "<span class=\"badge-red\">SALIDA</span>"
                                    val puntBadge = if (isEntrada) "<span class=\"badge-blue\">A tiempo (A)</span>" else "<span class=\"badge-yellow\">Salida de Turno</span>"

                                    writer.write("""
                                        <tr class="$trClass">
                                            <td class="t-center">${index + 1}</td>
                                            <td class="t-left bold">${rec.employeeName}</td>
                                            <td class="t-center">${rec.dateFormatted}</td>
                                            <td class="t-center bold">${rec.timeFormatted}</td>
                                            <td class="t-center">$tipoBadge</td>
                                            <td class="t-center">$puntBadge</td>
                                            <td class="t-center">Registrado</td>
                                        </tr>
                                    """.trimIndent())
                                }

                                writer.write("""
                                    </table>
                                    <div class="footer">
                                        PanApp Enterprise Payroll System &bull; Documento oficial exportado el $dateStr &bull; Información Confidencial
                                    </div>
                                    </body>
                                    </html>
                                """.trimIndent())
                            }
                        }

                        // ✨ Usamos la autoridad exacta del FileProvider del módulo admin
                        val uri = FileProvider.getUriForFile(context, "com.developers.admin.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "application/vnd.ms-excel"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(intent, context.getString(R.string.admin_share_payroll)))
                        Toast.makeText(context, context.getString(R.string.admin_payroll_generated), Toast.LENGTH_LONG).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, context.getString(R.string.admin_error_generating_payroll, e.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.TableChart, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.admin_export_payroll_excel), color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // ✨ BUSCADOR
        OutlinedTextField(
            value = textBusqueda,
            onValueChange = onTextBusquedaChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text(stringResource(R.string.admin_search_employee), color = contrastTextColor(Color.Gray, secondary = true)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = contrastTextColor(Color.Gray, secondary = true)) },
            shape = RoundedCornerShape(24.dp),
            colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cardColor,
                focusedContainerColor = cardColor,
                unfocusedTextColor = textColor,
                focusedTextColor = textColor
            ))
        )

        // ✨ FILTROS (PILLS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceFilterPill(
                text = localizedText(TextR.string.pan_all),
                isSelected = filtroSeleccionado == "TODOS",
                onClick = { onFiltroChange("TODOS") },
                isDarkMode = isDarkMode
            )
            AttendanceFilterPill(
                text = localizedText(TextR.string.pan_entries),
                isSelected = filtroSeleccionado == "ENTRADA",
                onClick = { onFiltroChange("ENTRADA") },
                isDarkMode = isDarkMode
            )
            AttendanceFilterPill(
                text = localizedText(TextR.string.pan_exits),
                isSelected = filtroSeleccionado == "SALIDA",
                onClick = { onFiltroChange("SALIDA") },
                isDarkMode = isDarkMode
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = contrastAccentColor(AdminPrimary))
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (registrosFiltrados.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.admin_no_records), color = contrastTextColor(Color.Gray, secondary = true))
                        }
                    }
                } else {
                    items(registrosFiltrados, key = { it.id }) { record ->
                        AttendanceItem(record, isDarkMode)
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Margen para la navbar flotante
            }
        }
    }
}

@Composable
fun AttendanceFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDarkMode: Boolean
) {
    val selectedColor = AdminPrimary
    val unselectedColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE)
    val contentColor = if (isSelected) Color.White else (contrastTextColor(Color.Gray, secondary = true, darkBackground = isDarkMode))

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) selectedColor else unselectedColor,
        modifier = Modifier.height(36.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

@Composable
fun AttendanceItem(record: AttendanceRecord, isDarkMode: Boolean) {
    val isEntrada = record.type == "ENTRADA"
    val cardColor = MaterialTheme.colorScheme.surface
    val accentColor = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icon = if (isEntrada) Icons.Outlined.CheckCircle else Icons.Default.AccessTime

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.employeeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
                Text(
                    text = localizedText(TextR.string.pan_attendance_date, attendanceLabel(record.type), record.dateFormatted),
                    fontSize = 12.sp,
                    color = contrastTextColor(Color.Gray, secondary = true)
                )
            }
            
            Surface(
                color = accentColor.copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = record.timeFormatted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = accentColor
                )
            }
        }
    }
}
