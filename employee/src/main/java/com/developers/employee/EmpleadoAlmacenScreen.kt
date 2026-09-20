package com.developers.employee

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

data class MateriaPrima(
    val id: String = "",
    val nombre: String = "",
    val cantidadActual: Double = 0.0,
    val unidadMedida: String = "g",
    val codigoBarras: String = "",
    val codigosBarras: List<String> = emptyList(),
    val cantidadesPorCodigo: Map<String, Double> = emptyMap(),
    val nivelCritico: Double = 0.0,
    val colorHex: String = "#4CAF50",
    val alertasEnviadas: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmpleadoAlmacenScreen(viewModel: EmployeeViewModel) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    
    var insumosList by remember { mutableStateOf<List<MateriaPrima>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog states
    var scannedCodeError by remember { mutableStateOf<String?>(null) }
    var insumoToModify by remember { mutableStateOf<MateriaPrima?>(null) }
    var showModifyDialog by remember { mutableStateOf(false) }

    // Scanner
    val barcodeScanner = remember { GmsBarcodeScanning.getClient(context) }

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

    fun processScannedCode(scannedCode: String) {
        val codeClean = scannedCode.trim()
        if (codeClean.isEmpty()) return

        val insumoEncontrado = insumosList.find { insumo ->
            insumo.codigosBarras.contains(codeClean) || insumo.codigoBarras == codeClean
        }

        if (insumoEncontrado != null) {
            insumoToModify = insumoEncontrado
            showModifyDialog = true
        } else {
            scannedCodeError = codeClean
        }
    }

    val insumosCriticosCount = remember(insumosList) {
        insumosList.count { it.cantidadActual <= it.nivelCritico }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    barcodeScanner.startScan()
                        .addOnSuccessListener { barcode ->
                            val rawValue = barcode.rawValue
                            if (!rawValue.isNullOrEmpty()) {
                                processScannedCode(rawValue)
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, context.getString(R.string.emp_scanner_error), Toast.LENGTH_SHORT).show()
                        }
                },
                containerColor = PrimaryBlue,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 80.dp) // Move slightly up so it doesn't overlap BottomNav
            ) {
                Icon(Icons.Default.QrCodeScanner, contentDescription = "Escanear Producto")
            }
        },
        containerColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF5F6FA)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.emp_warehouse),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = if (isDarkMode) Color.White else Color.Black
            )
            Text(
                text = stringResource(R.string.emp_warehouse_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Tarjetas de Estadísticas (Idénticas a Admin)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EmpleadoStockStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.emp_critical_supplies),
                    value = insumosCriticosCount.toString(),
                    color = Color(0xFFF44336)
                )
                EmpleadoStockStatCard(
                    modifier = Modifier.weight(1f),
                    label = stringResource(R.string.emp_total_supplies),
                    value = insumosList.size.toString(),
                    color = Color(0xFF2196F3)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (insumosList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(stringResource(R.string.emp_no_supplies), color = Color.Gray)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 80.dp) // padding for FAB
                ) {
                    items(insumosList, key = { it.id }) { insumo ->
                        EmpleadoInsumoCard(insumo, isDarkMode)
                    }
                }
            }
        }
    }

    // MODAL ERROR CODIGO NO EXISTE
    if (scannedCodeError != null) {
        AlertDialog(
            onDismissRequest = { scannedCodeError = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(40.dp)) },
            title = { Text(stringResource(R.string.emp_code_not_found), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.emp_code_not_found_desc)) },
            confirmButton = {
                Button(
                    onClick = { scannedCodeError = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text(stringResource(R.string.emp_understood))
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            titleContentColor = if (isDarkMode) Color.White else Color.Black,
            textContentColor = if (isDarkMode) Color.LightGray else Color.DarkGray
        )
    }

    // MODAL MODIFICAR INVENTARIO (Entrada / Salida)
    if (showModifyDialog && insumoToModify != null) {
        var modifyAmount by remember { mutableStateOf("") }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isSaving) showModifyDialog = false },
            title = { Text(stringResource(R.string.emp_adjust_inventory), fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.emp_product, insumoToModify!!.nombre),
                        fontWeight = FontWeight.SemiBold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = stringResource(R.string.emp_current_stock, insumoToModify!!.cantidadActual.toString(), insumoToModify!!.unidadMedida),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = modifyAmount,
                        onValueChange = { modifyAmount = it },
                        label = { Text(stringResource(R.string.emp_amount_to_adjust)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.emp_adjust_hint),
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = modifyAmount.toDoubleOrNull()
                        if (amount != null && amount != 0.0) {
                            isSaving = true
                            val db = FirebaseFirestore.getInstance()
                            db.collection("materia_prima").document(insumoToModify!!.id)
                                .update("cantidadActual", FieldValue.increment(amount))
                                .addOnSuccessListener {
                                    isSaving = false
                                    showModifyDialog = false
                                    Toast.makeText(context, context.getString(R.string.emp_stock_updated), Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    isSaving = false
                                    Toast.makeText(context, context.getString(R.string.emp_update_error), Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(context, context.getString(R.string.emp_invalid_amount), Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    else Text(stringResource(R.string.emp_apply_adjustment))
                }
            },
            dismissButton = {
                TextButton(onClick = { showModifyDialog = false }, enabled = !isSaving) {
                    Text(stringResource(R.string.emp_cancel), color = Color.Gray)
                }
            },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            titleContentColor = if (isDarkMode) Color.White else Color.Black
        )
    }
}

@Composable
fun EmpleadoStockStatCard(modifier: Modifier, label: String, value: String, color: Color) {
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
fun EmpleadoInsumoCard(insumo: MateriaPrima, isDarkMode: Boolean) {
    val isCritico = insumo.cantidadActual <= insumo.nivelCritico
    val parsedColor = remember(insumo.colorHex, isCritico) {
        if (isCritico) {
            Color(0xFFF44336) // Rojo de alerta
        } else {
            try {
                Color(android.graphics.Color.parseColor(insumo.colorHex))
            } catch (e: Exception) {
                Color(0xFF4CAF50) // Verde por defecto
            }
        }
    }

    val progress = remember(insumo.cantidadActual, insumo.nivelCritico) {
        val maxEstimado = if (insumo.nivelCritico > 0) insumo.nivelCritico * 5.0 else maxOf(insumo.cantidadActual, 1.0)
        (insumo.cantidadActual / maxEstimado).coerceIn(0.0, 1.0).toFloat()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(parsedColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Inventory, contentDescription = null, tint = parsedColor)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insumo.nombre, 
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Text(
                    text = stringResource(R.string.emp_stock_label, insumo.cantidadActual.toString(), insumo.unidadMedida), 
                    color = if (isCritico) Color.Red else Color.Gray, 
                    fontSize = 14.sp,
                    fontWeight = if (isCritico) FontWeight.Bold else FontWeight.Normal
                )
                if (insumo.codigosBarras.isNotEmpty()) {
                    Text(stringResource(R.string.emp_codes_label, insumo.codigosBarras.joinToString(", ")), color = Color.LightGray, fontSize = 11.sp, maxLines = 1)
                } else if (insumo.codigoBarras.isNotEmpty()) {
                    Text(stringResource(R.string.emp_code_label, insumo.codigoBarras), color = Color.LightGray, fontSize = 11.sp)
                }
            }
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.size(36.dp),
                color = parsedColor,
                strokeWidth = 4.dp,
                trackColor = Color.LightGray.copy(alpha = 0.3f)
            )
        }
    }
}