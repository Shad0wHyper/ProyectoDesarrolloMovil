package com.developers.admin

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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // Obtenemos todos los usuarios para mapear sus IDs a nombres
        db.collection("usuarios").get().addOnSuccessListener { userSnap ->
            val userMap = userSnap.documents.associate { it.id to (it.getString("nombre") ?: "Empleado") }
            
            // Usamos collectionGroup para traer todas las asistencias de todos los empleados
            db.collectionGroup("asistencias")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    records = result.documents.mapNotNull { doc ->
                        val userId = doc.reference.parent.parent?.id ?: ""
                        val name = userMap[userId] ?: "Desconocido"
                        
                        AttendanceRecord(
                            id = doc.id,
                            employeeName = name,
                            type = doc.getString("type") ?: "ENTRADA",
                            timestamp = doc.getLong("timestamp") ?: 0L,
                            dateFormatted = doc.getString("dateFormatted") ?: "",
                            timeFormatted = doc.getString("timeFormatted") ?: ""
                        )
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
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
            isDarkMode = isDarkMode,
            cardColor = cardColor,
            textColor = textColor
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Historial de Asistencias", fontWeight = FontWeight.Bold, color = textColor) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = textColor)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
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
    isDarkMode: Boolean,
    cardColor: Color,
    textColor: Color
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ✨ BUSCADOR
        OutlinedTextField(
            value = textBusqueda,
            onValueChange = onTextBusquedaChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Buscar empleado...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = cardColor,
                focusedContainerColor = cardColor,
                unfocusedTextColor = textColor,
                focusedTextColor = textColor
            )
        )

        // ✨ FILTROS (PILLS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AttendanceFilterPill(
                text = "Todos",
                isSelected = filtroSeleccionado == "TODOS",
                onClick = { onFiltroChange("TODOS") },
                isDarkMode = isDarkMode
            )
            AttendanceFilterPill(
                text = "Entradas",
                isSelected = filtroSeleccionado == "ENTRADA",
                onClick = { onFiltroChange("ENTRADA") },
                isDarkMode = isDarkMode
            )
            AttendanceFilterPill(
                text = "Salidas",
                isSelected = filtroSeleccionado == "SALIDA",
                onClick = { onFiltroChange("SALIDA") },
                isDarkMode = isDarkMode
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AdminPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (registrosFiltrados.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No se encontraron registros.", color = Color.Gray)
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
    val contentColor = if (isSelected) Color.White else (if (isDarkMode) Color.LightGray else Color.Gray)

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
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
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
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = "${record.type} • ${record.dateFormatted}",
                    fontSize = 12.sp,
                    color = Color.Gray
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
