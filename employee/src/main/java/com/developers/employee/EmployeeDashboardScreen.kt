package com.developers.employee

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
// ✨ AQUÍ VAN TODOS LOS IMPORTS DE MATERIAL 3 PARA QUE NO TE SALGA ROJO
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
// ✨ EL IMPORT DEL CONTEXTO QUE TE FALTABA
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val EmployeePrimary = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(viewModel: EmployeeViewModel) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Portal de Empleados", fontWeight = FontWeight.Bold) },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .padding(16.dp)
        ) {
            Text(
                text = "Hola, ${viewModel.userName}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                text = "Registra tu asistencia escaneando el código QR de la sucursal.",
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EmployeePrimary),
                onClick = { showDialog = true }
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = "Escanear QR",
                        modifier = Modifier.size(64.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "ESCANEAR CÓDIGO QR",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Tu Historial de Hoy",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (viewModel.isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EmployeePrimary)
                }
            } else if (viewModel.logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Aún no tienes registros de asistencia.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(viewModel.logs) { log ->
                        LogCard(log)
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar Asistencia", fontWeight = FontWeight.Bold) },
            text = { Text("Simulando lectura de código QR de la sucursal...") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.registerAttendance("ENTRADA") {
                            Toast.makeText(context, "Entrada registrada", Toast.LENGTH_SHORT).show()
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) { Text("Marcar Entrada") }
            },
            dismissButton = {
                Button(
                    onClick = {
                        viewModel.registerAttendance("SALIDA") {
                            Toast.makeText(context, "Salida registrada", Toast.LENGTH_SHORT).show()
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336))
                ) { Text("Marcar Salida") }
            }
        )
    }
}

@Composable
fun LogCard(log: LogData) {
    val isEntrada = log.type == "ENTRADA"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isEntrada) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEntrada) Icons.Default.Login else Icons.Default.Logout,
                    contentDescription = null,
                    tint = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(log.type, fontWeight = FontWeight.Bold)
                Text(log.dateFormatted, color = Color.Gray, fontSize = 12.sp)
            }
            Text(log.timeFormatted, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}