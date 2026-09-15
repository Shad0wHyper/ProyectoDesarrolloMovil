package com.developers.admin

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.AccessTime
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

data class AdminLogData(
    val id: String = "",
    val type: String = "ENTRADA",
    val timestamp: Long = 0L,
    val dateFormatted: String = "",
    val timeFormatted: String = "",
    val employeeName: String = "Empleado",
    val employeeEmail: String = ""
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AttendanceHistoryScreen(onBack: () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
    var rawLogs by remember { mutableStateOf<List<AdminLogData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("TODOS") } // TODOS, ENTRADA, SALIDA

    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        
        // 1. Cargamos todos los usuarios primero para tener un mapa de nombres (Caché)
        db.collection("usuarios").get().addOnSuccessListener { usersResult ->
            val usersMap = usersResult.documents.associate { it.id to (it.getString("nombre") ?: "Empleado") }
            val emailsMap = usersResult.documents.associate { it.id to (it.getString("email") ?: "") }

            // 2. Cargamos todas las asistencias globalmente
            db.collectionGroup("asistencias")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    rawLogs = result.documents.mapNotNull { doc ->
                        val log = doc.toObject(AdminLogData::class.java)
                        if (log != null) {
                            val uid = doc.reference.parent.parent?.id ?: ""
                            log.copy(
                                employeeName = usersMap[uid] ?: "Empleado Desconocido",
                                employeeEmail = emailsMap[uid] ?: ""
                            )
                        } else null
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        }
    }

    // Lógica de Filtrado
    val filteredLogs = remember(rawLogs, searchQuery, filterType) {
        rawLogs.filter { log ->
            val matchSearch = log.employeeName.contains(searchQuery, ignoreCase = true) || 
                             log.employeeEmail.contains(searchQuery, ignoreCase = true)
            val matchType = if (filterType == "TODOS") true else log.type == filterType
            matchSearch && matchType
        }
    }

    // Agrupación por Fecha para la UI organizada
    val groupedLogs = remember(filteredLogs) {
        filteredLogs.groupBy { it.dateFormatted }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Registro de Asistencia", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                        if (!isLoading) {
                            Text("${filteredLogs.size} registros encontrados", fontSize = 12.sp, color = secondaryTextColor)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar", tint = textColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Buscador y Filtros
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar empleado...", color = secondaryTextColor) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = secondaryTextColor) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("TODOS", "ENTRADA", "SALIDA").forEach { type ->
                        FilterChip(
                            selected = filterType == type,
                            onClick = { filterType = type },
                            label = { Text(type, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = if (type == "ENTRADA") Color(0xFF4CAF50) else if (type == "SALIDA") Color(0xFFF44336) else MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                labelColor = secondaryTextColor
                            )
                        )
                    }
                }

                // Resumen rápido
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val entradas = rawLogs.count { it.type == "ENTRADA" }
                    val salidas = rawLogs.count { it.type == "SALIDA" }
                    
                    ResumenAsistenciaMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "Entradas",
                        count = entradas,
                        color = Color(0xFF4CAF50),
                        isDarkMode = isDarkMode
                    )
                    ResumenAsistenciaMiniCard(
                        modifier = Modifier.weight(1f),
                        label = "Salidas",
                        count = salidas,
                        color = Color(0xFFF44336),
                        isDarkMode = isDarkMode
                    )
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.LightGray)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No se encontraron registros", color = secondaryTextColor)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    groupedLogs.forEach { (date, logsInDate) ->
                        stickyHeader {
                            DateHeader(date, isDarkMode)
                        }
                        items(logsInDate) { log ->
                            AdminLogCard(log, isDarkMode)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String, isDarkMode: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isDarkMode) Color(0xFF1A1A1A) else Color(0xFFF0F0F0))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = date,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 12.sp,
            color = if (isDarkMode) Color.LightGray else Color.DarkGray
        )
    }
}

@Composable
fun AdminLogCard(log: AdminLogData, isDarkMode: Boolean) {
    val isEntrada = log.type == "ENTRADA"
    val iconColor = if (isEntrada) Color(0xFF4CAF50) else Color(0xFFF44336)
    val iconBg = iconColor.copy(alpha = 0.1f)
    val textColor = if (isDarkMode) Color.White else Color.Black
    val secondaryTextColor = if (isDarkMode) Color.LightGray else Color.Gray

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEntrada) Icons.AutoMirrored.Filled.Login else Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.employeeName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = textColor
                )
                Text(
                    text = if (isEntrada) "Entrada registrada" else "Salida registrada",
                    color = iconColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp
                )
                if (log.employeeEmail.isNotEmpty()) {
                    Text(
                        text = log.employeeEmail,
                        color = secondaryTextColor,
                        fontSize = 10.sp
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = log.timeFormatted,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = textColor
                )
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = secondaryTextColor
                )
            }
        }
    }
}

@Composable
fun ResumenAsistenciaMiniCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    color: Color,
    isDarkMode: Boolean
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = color
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
            )
        }
    }
}
