package com.developers.admin

import com.developers.core.ui.PanAppScreenTitle

import com.developers.core.ui.panAppFilteredItem

import com.developers.core.ui.PanAppGlassFilterChip

import com.developers.core.ui.PanAppGlassCard

import android.content.ClipData
import android.content.Intent
import android.util.Log
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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit, isInsideTab: Boolean = false) {
    val isDarkMode = isSystemInDarkTheme()
    var rawRecords by remember { mutableStateOf<List<AttendanceRecord>>(emptyList()) }
    var userNames by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    // ✨ Estados de Filtrado
    var textBusqueda by remember { mutableStateOf("") }
    var filtroSeleccionado by remember { mutableStateOf("TODOS") } // "TODOS", "ENTRADA", "SALIDA"

    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface
    val fallbackEmployeeName = localizedText(TextR.string.pan_employee)

    DisposableEffect(reloadTrigger) {
        var active = true
        isLoading = true
        hasError = false
        val db = FirebaseFirestore.getInstance()
        val requestedUserLookups = mutableSetOf<String>()

        fun parseAttendanceDoc(
            doc: DocumentSnapshot,
            fallbackUserId: String = ""
        ): AttendanceRecord? = runCatching {
            val uid = (doc.get("userId") as? String)?.takeIf { it.isNotBlank() }
                ?: fallbackUserId.takeIf { it.isNotBlank() }
                ?: doc.reference.parent.parent?.id.orEmpty()
            val docEmployeeName = (doc.get("employeeName") as? String)?.takeIf { it.isNotBlank() }
                ?: (doc.get("nombreEmpleado") as? String)?.takeIf { it.isNotBlank() }
                ?: (doc.get("nombre") as? String)?.takeIf { it.isNotBlank() }
            val timestamp = parseAttendanceTimestampMillis(
                doc.get("timestamp"),
                doc.get("fecha"),
                doc.get("createdAt")
            )
            buildAttendanceRecord(
                id = doc.id,
                path = doc.reference.path,
                userId = uid,
                rawEmployeeName = docEmployeeName ?: userNames[uid],
                fallbackEmployeeName = fallbackEmployeeName,
                rawType = doc.get("type") as? String,
                rawTipo = doc.get("tipo") as? String,
                timestamp = timestamp,
                rawDateFormatted = (doc.get("dateFormatted") as? String) ?: (doc.get("fecha") as? String),
                rawTimeFormatted = (doc.get("timeFormatted") as? String) ?: (doc.get("hora") as? String)
            )
        }.onFailure {
            Log.e("AttendanceHistory", "Error parsing attendance ${doc.reference.path}", it)
        }.getOrNull()

        fun resolveMissingNames(items: List<AttendanceRecord>) {
            items.map { it.userId }
                .filter { it.isNotBlank() && !userNames.containsKey(it) && requestedUserLookups.add(it) }
                .forEach { uid ->
                    db.collection("usuarios").document(uid).get()
                        .addOnSuccessListener { userDoc ->
                            if (!active) return@addOnSuccessListener
                            val nombre = (userDoc.get("nombre") as? String)?.takeIf { it.isNotBlank() }
                            if (nombre != null) {
                                userNames = userNames + (uid to nombre)
                            }
                        }
                }
        }

        // Precargamos nombres disponibles sin bloquear ni fallar si el listado general no está permitido
        db.collection("usuarios").get().addOnSuccessListener { userSnap ->
            if (!active) return@addOnSuccessListener
            val loaded = buildMap {
                for (userDoc in userSnap.documents) {
                    val nombre = (userDoc.get("nombre") as? String)?.takeIf { it.isNotBlank() }
                    if (nombre != null) put(userDoc.id, nombre)
                }
            }
            if (loaded.isNotEmpty()) {
                userNames = userNames + loaded
            }
        }

        var fallbackStarted = false
        fun runFallbackQueries() {
            if (fallbackStarted) return
            fallbackStarted = true
            val collected = linkedMapOf<String, AttendanceRecord>()
            var rootDone = false
            var usersDone = false
            var anySuccess = false

            fun finishFallback() {
                if (!active || !rootDone || !usersDone) return
                if (anySuccess) {
                    val merged = collected.values.sortedByDescending { it.timestamp }
                    rawRecords = merged
                    resolveMissingNames(merged)
                    hasError = false
                } else {
                    hasError = true
                }
                isLoading = false
            }

            db.collection("asistencias").get()
                .addOnSuccessListener { snap ->
                    anySuccess = true
                    snap.documents.mapNotNull { parseAttendanceDoc(it) }.forEach { rec ->
                        collected[rec.key] = rec
                    }
                    rootDone = true
                    finishFallback()
                }
                .addOnFailureListener { e ->
                    Log.e("AttendanceHistory", "Fallback root asistencias failed", e)
                    rootDone = true
                    finishFallback()
                }

            db.collection("usuarios").get()
                .addOnSuccessListener { userSnap ->
                    val users = userSnap.documents
                    if (users.isEmpty()) {
                        anySuccess = true
                        usersDone = true
                        finishFallback()
                        return@addOnSuccessListener
                    }
                    val namesFromUsers = buildMap {
                        for (userDoc in users) {
                            val nombre = (userDoc.get("nombre") as? String)?.takeIf { it.isNotBlank() }
                            if (nombre != null) put(userDoc.id, nombre)
                        }
                    }
                    if (active && namesFromUsers.isNotEmpty()) {
                        userNames = userNames + namesFromUsers
                    }
                    var processed = 0
                    for (userDoc in users) {
                        val userId = userDoc.id
                        db.collection("usuarios").document(userId).collection("asistencias")
                            .get()
                            .addOnSuccessListener { attendanceSnap ->
                                anySuccess = true
                                attendanceSnap.documents.mapNotNull { parseAttendanceDoc(it, userId) }.forEach { rec ->
                                    collected[rec.key] = rec
                                }
                                processed++
                                if (processed == users.size) {
                                    usersDone = true
                                    finishFallback()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("AttendanceHistory", "Fallback user $userId asistencias failed", e)
                                processed++
                                if (processed == users.size) {
                                    usersDone = true
                                    finishFallback()
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("AttendanceHistory", "Fallback usuarios query failed", e)
                    usersDone = true
                    finishFallback()
                }
        }

        val listener: ListenerRegistration = db.collectionGroup("asistencias")
            .addSnapshotListener { snapshot, error ->
                if (!active) return@addSnapshotListener
                if (error != null) {
                    Log.e("AttendanceHistory", "collectionGroup asistencias failed", error)
                    runFallbackQueries()
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val parsed = snapshot.documents
                        .mapNotNull { parseAttendanceDoc(it) }
                        .distinctBy { it.key }
                        .sortedByDescending { it.timestamp }
                    rawRecords = parsed
                    hasError = false
                    isLoading = false
                    resolveMissingNames(parsed)
                }
            }

        onDispose {
            active = false
            listener.remove()
        }
    }

    val records = remember(rawRecords, userNames) {
        rawRecords.map { rec ->
            val resolved = userNames[rec.userId]
            if (!resolved.isNullOrBlank()) rec.copy(employeeName = resolved) else rec
        }
    }

    // ✨ Lógica de Filtrado en Tiempo Real
    val registrosFiltrados = remember(records, textBusqueda, filtroSeleccionado) {
        filterAttendanceRecords(records, textBusqueda, filtroSeleccionado)
    }

    if (isInsideTab) {
        AttendanceContent(
            textBusqueda = textBusqueda,
            onTextBusquedaChange = { textBusqueda = it },
            filtroSeleccionado = filtroSeleccionado,
            onFiltroChange = { filtroSeleccionado = it },
            isLoading = isLoading,
            hasError = hasError,
            onRetry = { reloadTrigger++ },
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
                    title = { PanAppScreenTitle(stringResource(R.string.admin_attendance_history), color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.admin_back), tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                AttendanceContent(
                    textBusqueda = textBusqueda,
                    onTextBusquedaChange = { textBusqueda = it },
                    filtroSeleccionado = filtroSeleccionado,
                    onFiltroChange = { filtroSeleccionado = it },
                    isLoading = isLoading,
                    hasError = hasError,
                    onRetry = { reloadTrigger++ },
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
    hasError: Boolean,
    onRetry: () -> Unit,
    registrosFiltrados: List<AttendanceRecord>,
    allRecords: List<AttendanceRecord>,
    isDarkMode: Boolean,
    cardColor: Color,
    textColor: Color
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize()) {
        // ✨ BOTÓN EXPORTAR NOMINA EXCEL (.xlsx OOXML real)
        if (allRecords.isNotEmpty()) {
            Button(
                onClick = {
                    try {
                        val file = exportAttendancePayrollFile(context.cacheDir, allRecords)

                        // ✨ Usamos la autoridad exacta del FileProvider del módulo admin
                        val uri = FileProvider.getUriForFile(context, "com.developers.admin.fileprovider", file)
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = XLSX_MIME_TYPE
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, file.name)
                            clipData = ClipData.newRawUri(file.name, uri)
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
                onClick = { onFiltroChange("TODOS") }
            )
            AttendanceFilterPill(
                text = localizedText(TextR.string.pan_entries),
                isSelected = filtroSeleccionado == "ENTRADA",
                onClick = { onFiltroChange("ENTRADA") }
            )
            AttendanceFilterPill(
                text = localizedText(TextR.string.pan_exits),
                isSelected = filtroSeleccionado == "SALIDA",
                onClick = { onFiltroChange("SALIDA") }
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = contrastAccentColor(AdminPrimary))
            }
        } else if (hasError && allRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f).padding(32.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.admin_attendance_load_error),
                        color = contrastTextColor(Color.Gray, secondary = true)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                    ) {
                        Text(stringResource(R.string.admin_retry), color = Color.White)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (registrosFiltrados.isEmpty()) {
                    item(key = "emptyAttendanceFilter") {
                        Box(modifier = panAppFilteredItem().fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.admin_no_records), color = contrastTextColor(Color.Gray, secondary = true))
                        }
                    }
                } else {
                    items(registrosFiltrados, key = { it.key }) { record ->
                        Box(panAppFilteredItem()) { AttendanceItem(record, isDarkMode) }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // Margen para la navbar flotante
            }
        }
    }
}

@Composable
private fun AttendanceFilterPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    PanAppGlassFilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(text) }
    )
}

@Composable
fun AttendanceItem(record: AttendanceRecord, isDarkMode: Boolean) {
    val isEntrada = record.type == "ENTRADA"
    val accentColor = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336)
    val icon = if (isEntrada) Icons.Outlined.CheckCircle else Icons.Default.AccessTime

    PanAppGlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
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
