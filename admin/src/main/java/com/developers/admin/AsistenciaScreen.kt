package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

data class AsistenciaLog(
    val id: String = "",
    val userId: String = "",
    val userName: String = "Empleado",
    val type: String = "ENTRADA",
    val dateFormatted: String = "",
    val timeFormatted: String = "",
    val timestamp: Long = 0L
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var listaAsistencias by remember { mutableStateOf<List<AsistenciaLog>>(emptyList()) }
    var userNamesMap by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Cargamos nombres de usuarios para el mapeo
        db.collection("usuarios").get().addOnSuccessListener { userDocs ->
            val mapping = userDocs.documents.associate { doc ->
                doc.id to (doc.getString("nombre") ?: "Empleado")
            }
            userNamesMap = mapping
        }

        // 2. Cargamos logs de asistencia
        db.collectionGroup("asistencias")
            .get()
            .addOnSuccessListener { result ->
                val logs = result.documents.map { doc ->
                    val userId = doc.reference.parent.parent?.id ?: ""
                    AsistenciaLog(
                        id = doc.id,
                        userId = userId,
                        userName = userNamesMap[userId] ?: "Empleado",
                        type = doc.getString("type") ?: "ENTRADA",
                        dateFormatted = doc.getString("dateFormatted") ?: "",
                        timeFormatted = doc.getString("timeFormatted") ?: "",
                        timestamp = doc.getLong("timestamp") ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                
                listaAsistencias = logs
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Error al cargar asistencias", Toast.LENGTH_SHORT).show()
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de Asistencia", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF6200EE))
            }
        } else if (listaAsistencias.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No hay registros de asistencia hoy.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(listaAsistencias) { log ->
                    val name = userNamesMap[log.userId] ?: "Cargando..."
                    AttendanceAdminCard(log.copy(userName = name))
                }
            }
        }
    }
}

@Composable
fun AttendanceAdminCard(log: AsistenciaLog) {
    val isEntrada = log.type == "ENTRADA"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
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
                    .background(if (isEntrada) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEntrada) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.userName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "ID: ${log.userId.takeLast(4).uppercase()} • ${log.dateFormatted}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Text(
                text = log.timeFormatted,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
