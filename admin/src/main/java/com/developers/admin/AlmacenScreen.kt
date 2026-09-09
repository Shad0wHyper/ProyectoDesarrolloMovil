package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// 1. Data Class Híbrida MateriaPrima
data class MateriaPrima(
    val id: String = "",
    val nombre: String = "",
    val cantidadActual: Double = 0.0,
    val unidadMedida: String = "g",
    val codigoBarras: String = "",
    val codigosBarras: List<String> = emptyList(), // Array para usar whereArrayContains / contains
    val cantidadesPorCodigo: Map<String, Double> = emptyMap(), // Diccionario codigo -> cantidad que aporta
    val nivelCritico: Double = 0.0,
    val colorHex: String = "#4CAF50"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenScreen() {
    val context = LocalContext.current
    var insumosList by remember { mutableStateOf<List<MateriaPrima>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Estados para diálogos
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    var unregisteredCode by remember { mutableStateOf<String?>(null) }
    var modoAprendizajeOpcion by remember { mutableStateOf("A") } // "A" = Vincular, "B" = Alta Nueva

    // Campos Opción A (Vincular)
    var insumoASeleccionar by remember { mutableStateOf<MateriaPrima?>(null) }
    var cantidadAportaTextA by remember { mutableStateOf("") }
    var expandedDropdownInsumo by remember { mutableStateOf(false) }

    // Campos Opción B (Alta Nueva)
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaUnidad by remember { mutableStateOf("g") }
    val unidadesDisponibles = remember { listOf("g", "ml") }
    var expandedDropdownUnidad by remember { mutableStateOf(false) }
    var nuevoNivelCriticoText by remember { mutableStateOf("10.0") }
    var cantidadAportaTextB by remember { mutableStateOf("") }

    // Campo auxiliar para simulación o entrada manual de prueba
    var showTestInputCodeDialog by remember { mutableStateOf(false) }
    var testCodeInputText by remember { mutableStateOf("") }

    val barcodeScanner = remember { GmsBarcodeScanning.getClient(context) }

    // 2. Lectura en Tiempo Real desde Firebase Firestore (materia_prima)
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("materia_prima").addSnapshotListener { snapshot, error ->
            if (error != null) {
                isLoading = false
                return@addSnapshotListener
            }
            if (snapshot != null) {
                insumosList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MateriaPrima::class.java)?.copy(id = doc.id)
                }
                isLoading = false
            }
        }
    }

    // Lógica del Escáner (Modo Autopiloto)
    fun procesarCodigoEscaneado(scannedCode: String) {
        val codeClean = scannedCode.trim()
        if (codeClean.isEmpty()) return

        val db = FirebaseFirestore.getInstance()

        // Buscar si el código ya existe en algún insumo
        val insumoEncontrado = insumosList.find { insumo ->
            insumo.codigosBarras.contains(codeClean) || insumo.codigoBarras == codeClean
        }

        if (insumoEncontrado != null) {
            // SI LO ENCUENTRA (Modo Autopiloto)
            val cantidadAportada = insumoEncontrado.cantidadesPorCodigo[codeClean]
                ?: if (insumoEncontrado.cantidadActual > 0) 1.0 else 0.0

            db.collection("materia_prima").document(insumoEncontrado.id)
                .update("cantidadActual", FieldValue.increment(cantidadAportada))
                .addOnSuccessListener {
                    successMessage = "¡Ingreso Exitoso!\nSe sumaron $cantidadAportada ${insumoEncontrado.unidadMedida} a ${insumoEncontrado.nombre}"
                    showSuccessDialog = true
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Error al actualizar stock en Firestore", Toast.LENGTH_SHORT).show()
                }
        } else {
            // SI NO LO ENCUENTRA (Modo Aprendizaje)
            unregisteredCode = codeClean
            modoAprendizajeOpcion = "A"
            insumoASeleccionar = null
            cantidadAportaTextA = ""
            nuevoNombre = ""
            cantidadAportaTextB = ""
        }
    }

    fun ejecutarEscaneo() {
        barcodeScanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrEmpty()) {
                    procesarCodigoEscaneado(rawValue)
                }
            }
            .addOnFailureListener {
                // Fallback para emuladores o si falla la cámara: abrir diálogo de entrada manual de prueba
                showTestInputCodeDialog = true
            }
    }

    val insumosCriticosCount = remember(insumosList) {
        insumosList.count { it.cantidadActual <= it.nivelCritico }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { ejecutarEscaneo() },
                containerColor = Color(0xFF6200EE),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Código de Barras")
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Control de Almacén",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                )

                OutlinedButton(
                    onClick = { showTestInputCodeDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Probar Código", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de Estadísticas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StockStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Insumos Críticos",
                    value = insumosCriticosCount.toString(),
                    color = Color(0xFFF44336)
                )
                StockStatCard(
                    modifier = Modifier.weight(1f),
                    label = "Total Insumos",
                    value = insumosList.size.toString(),
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Materias Primas",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFF6200EE))
                }
            } else if (insumosList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No hay materias primas registradas en Firestore.", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(insumosList, key = { it.id }) { insumo ->
                        InsumoCard(insumo)
                    }
                }
            }
        }
    }

    // ✨ ALERT DIALOG AUTOPILOTO (ÉXITO AUTOMÁTICO)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
            title = { Text("¡Ingreso Exitoso!", fontWeight = FontWeight.Bold) },
            text = { Text(successMessage, fontSize = 15.sp) },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("OK")
                }
            }
        )
    }

    // ✨ MODO APRENDIZAJE: CÓDIGO NO REGISTRADO
    if (unregisteredCode != null) {
        val code = unregisteredCode!!

        AlertDialog(
            onDismissRequest = { unregisteredCode = null },
            title = { Text("Código No Registrado", fontWeight = FontWeight.Bold) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Código leído: $code", fontWeight = FontWeight.Bold, color = Color(0xFF6200EE), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de opción
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = modoAprendizajeOpcion == "A",
                            onClick = { modoAprendizajeOpcion = "A" },
                            label = { Text("A) Vincular a Insumo", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = modoAprendizajeOpcion == "B",
                            onClick = { modoAprendizajeOpcion = "B" },
                            label = { Text("B) Dar de Alta Nuevo", fontSize = 11.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (modoAprendizajeOpcion == "A") {
                        // OPCIÓN A: VINCULAR A INSUMO EXISTENTE
                        Text("Selecciona el insumo existente:", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = insumoASeleccionar?.let { "${it.nombre} (${it.unidadMedida})" } ?: "Seleccionar Insumo...",
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedDropdownInsumo = true }) },
                                modifier = Modifier.fillMaxWidth().clickable { expandedDropdownInsumo = true },
                                shape = RoundedCornerShape(8.dp)
                            )
                            DropdownMenu(
                                expanded = expandedDropdownInsumo,
                                onDismissRequest = { expandedDropdownInsumo = false },
                                modifier = Modifier.fillMaxWidth(0.8f).background(Color.White)
                            ) {
                                insumosList.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text("${item.nombre} (${item.unidadMedida})") },
                                        onClick = {
                                            insumoASeleccionar = item
                                            expandedDropdownInsumo = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cantidadAportaTextA,
                            onValueChange = { cantidadAportaTextA = it },
                            label = { Text("Cantidad que aporta este empaque") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                    } else {
                        // OPCIÓN B: DAR DE ALTA NUEVO INSUMO
                        OutlinedTextField(
                            value = nuevoNombre,
                            onValueChange = { nuevoNombre = it },
                            label = { Text("Nombre de Insumo") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = nuevaUnidad,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Unidad") },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedDropdownUnidad = true }) },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedDropdownUnidad = true },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true
                                )
                                DropdownMenu(expanded = expandedDropdownUnidad, onDismissRequest = { expandedDropdownUnidad = false }, modifier = Modifier.background(Color.White)) {
                                    unidadesDisponibles.forEach { und ->
                                        DropdownMenuItem(text = { Text(und) }, onClick = { nuevaUnidad = und; expandedDropdownUnidad = false })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = cantidadAportaTextB,
                                onValueChange = { cantidadAportaTextB = it },
                                label = { Text("Aporte Empaque") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )
                        }

                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val db = FirebaseFirestore.getInstance()
                        if (modoAprendizajeOpcion == "A") {
                            val cantAporta = cantidadAportaTextA.toDoubleOrNull() ?: 0.0
                            if (insumoASeleccionar != null && cantAporta > 0) {
                                val targetDoc = db.collection("materia_prima").document(insumoASeleccionar!!.id)
                                val updates = hashMapOf<String, Any>(
                                    "cantidadActual" to FieldValue.increment(cantAporta),
                                    "codigosBarras" to FieldValue.arrayUnion(code),
                                    "cantidadesPorCodigo.$code" to cantAporta
                                )
                                targetDoc.update(updates).addOnSuccessListener {
                                    Toast.makeText(context, "Código $code vinculado a ${insumoASeleccionar!!.nombre}", Toast.LENGTH_SHORT).show()
                                    unregisteredCode = null
                                }
                            } else {
                                Toast.makeText(context, "Seleccione un insumo e ingrese la cantidad que aporta", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val cantAporta = cantidadAportaTextB.toDoubleOrNull() ?: 0.0
                            val critico = nuevoNivelCriticoText.toDoubleOrNull() ?: 10.0
                            if (nuevoNombre.isNotBlank() && cantAporta > 0) {
                                val nuevoMap = hashMapOf<String, Any>(
                                    "nombre" to nuevoNombre.trim(),
                                    "unidadMedida" to nuevaUnidad.trim(),
                                    "cantidadActual" to cantAporta,
                                    "nivelCritico" to critico,
                                    "codigoBarras" to code,
                                    "codigosBarras" to listOf(code),
                                    "cantidadesPorCodigo" to mapOf(code to cantAporta),
                                    "colorHex" to "#4CAF50"
                                )
                                db.collection("materia_prima").add(nuevoMap).addOnSuccessListener {
                                    Toast.makeText(context, "Nuevo insumo registrado y vinculado", Toast.LENGTH_SHORT).show()
                                    unregisteredCode = null
                                }
                            } else {
                                Toast.makeText(context, "Ingrese el nombre y la cantidad aportada", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
                ) {
                    Text("Confirmar y Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { unregisteredCode = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // DIÁLOGO AUXILIAR PARA PRUEBA DE CÓDIGO MANUAL
    if (showTestInputCodeDialog) {
        AlertDialog(
            onDismissRequest = { showTestInputCodeDialog = false },
            title = { Text("Escanear / Probar Código") },
            text = {
                Column {
                    Text("Ingresa o pega un código de barras para probar la lógica WMS:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testCodeInputText,
                        onValueChange = { testCodeInputText = it },
                        label = { Text("Código de Barras") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = testCodeInputText.trim()
                        showTestInputCodeDialog = false
                        testCodeInputText = ""
                        if (code.isNotEmpty()) {
                            procesarCodigoEscaneado(code)
                        }
                    }
                ) {
                    Text("Procesar Código")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTestInputCodeDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun StockStatCard(modifier: Modifier, label: String, value: String, color: Color) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium, color = color)
        }
    }
}

@Composable
fun InsumoCard(insumo: MateriaPrima) {
    val parsedColor = remember(insumo.colorHex) {
        try {
            Color(android.graphics.Color.parseColor(insumo.colorHex))
        } catch (e: Exception) {
            Color(0xFF4CAF50)
        }
    }

    val progress = remember(insumo.cantidadActual, insumo.nivelCritico) {
        val maxEstimado = if (insumo.nivelCritico > 0) insumo.nivelCritico * 2.0 else maxOf(insumo.cantidadActual, 1.0)
        (insumo.cantidadActual / maxEstimado).coerceIn(0.0, 1.0).toFloat()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(parsedColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = parsedColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(insumo.nombre, fontWeight = FontWeight.Bold)
                Text("Stock: ${insumo.cantidadActual} ${insumo.unidadMedida}", color = Color.Gray, fontSize = 14.sp)
                
                // Mostrar resumen de códigos asociados
                if (insumo.codigosBarras.isNotEmpty()) {
                    Text("Códigos: ${insumo.codigosBarras.joinToString(", ")}", color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                } else if (insumo.codigoBarras.isNotEmpty()) {
                    Text("Cód: ${insumo.codigoBarras}", color = Color.LightGray, fontSize = 11.sp)
                }
            }
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(32.dp),
                color = parsedColor,
                strokeWidth = 4.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}
