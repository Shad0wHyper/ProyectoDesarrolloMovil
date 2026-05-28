package com.developers.employee

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(viewModel: EmployeeViewModel, onNavigate: (AppScreen) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Asistencia", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = { BottomNav(AppScreen.ASISTENCIA, onNavigate) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            DigitalIdCard(viewModel.userName)
            AttendanceScannerControl(viewModel)
            RecentLogs(viewModel.logs, onViewHistoryClick = { onNavigate(AppScreen.HISTORIAL) })
        }
    }
}

@Composable
fun AttendanceScannerControl(viewModel: EmployeeViewModel) {
    val context = LocalContext.current
    val options = remember { GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).enableAutoZoom().build() }
    val scanner = remember { GmsBarcodeScanning.getClient(context, options) }

    fun procesarEscaneo(tipoAsistencia: String) {
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val codigoLeido = barcode.rawValue ?: ""

                // ✨ VALIDACIÓN CON LA BASE DE DATOS
                val claveCorrecta = if (tipoAsistencia == "ENTRADA") viewModel.claveEntrada else viewModel.claveSalida

                if (codigoLeido == claveCorrecta && claveCorrecta.isNotEmpty()) {
                    viewModel.registerAttendance(tipoAsistencia) {
                        Toast.makeText(context, "$tipoAsistencia Registrada Exitosamente", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(context, "❌ Código QR Inválido o Caducado.", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { Toast.makeText(context, "Error al abrir cámara", Toast.LENGTH_SHORT).show() }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Escáner de Sucursal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), onClick = { procesarEscaneo("ENTRADA") }) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowOutward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Marcar Entrada", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = CardPink), onClick = { procesarEscaneo("SALIDA") }) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color.White)
                    Text("Marcar Salida", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
        if (viewModel.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
    }
}

@Composable
fun DigitalIdCard(userName: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Identificación Digital", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gafete Autorizado", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                Box(modifier = Modifier.size(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(color = PrimaryBlue.copy(alpha = 0.3f), style = Stroke(width = 4.dp.toPx(), pathEffect = dashPathEffect), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
                    }
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(userName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun RecentLogs(logs: List<LogData>, onViewHistoryClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Registros Recientes", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        logs.take(3).forEach { log -> RenderLogItem(log) }
        TextButton(onClick = onViewHistoryClick, modifier = Modifier.fillMaxWidth()) {
            Text("Ver Historial Completo", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RenderLogItem(log: LogData) {
    val isEntrada = log.type == "ENTRADA"
    val iconBg = if (isEntrada) Color(0xFFE8EAF6) else Color(0xFFF3E8EC)
    val iconColor = if (isEntrada) PrimaryBlue else CardPink
    val icon = if (isEntrada) Icons.Outlined.CheckCircle else Icons.Default.AccessTime

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.type, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(log.dateFormatted, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(log.timeFormatted, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHistoryScreen(viewModel: EmployeeViewModel, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial Completo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.logs, key = { it.id }) { log -> RenderLogItem(log) }
        }
    }
}