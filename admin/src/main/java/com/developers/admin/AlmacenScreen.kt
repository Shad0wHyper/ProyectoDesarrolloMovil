package com.developers.admin

import com.developers.core.ui.PanAppScreenTitle
import com.developers.core.ui.isPanAppInDarkTheme
import com.developers.core.ui.panAppFilteredItem
import com.developers.core.ui.panAppPressMotion
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Tune

import com.developers.core.ui.PanAppGlassFilterChip
import com.developers.core.ui.PanAppGlassDropdown

import com.developers.core.ui.panAppGlassBorder
import com.developers.core.ui.PanAppGlassCard

import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.developers.admin.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
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
    val colorHex: String = "#4CAF50",
    val alertasEnviadas: Int = 0,
    val existenciaObjetivo: Double? = null,
    val tamanoPaquete: Double? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AlmacenScreen(
    warehouse: WarehouseLoad,
    onRetry: () -> Unit,
    selectedMaterialId: String? = null,
    onBack: (() -> Unit)? = null,
    onManageStock: () -> Unit,
    profileMenu: @Composable () -> Unit
) {
    val context = LocalContext.current
    val isDarkMode = isPanAppInDarkTheme()
    val insumosList = remember(warehouse) {
        (warehouse as? WarehouseLoad.Ready)?.items.orEmpty().map { it.material }
    }
    val latestWarehouse by rememberUpdatedState(warehouse)

    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)

    // Estados para diálogos
    var showSuccessDialog by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    var unregisteredCode by remember { mutableStateOf<String?>(null) }
    var modoAprendizajeOpcion by remember { mutableStateOf("A") } // "A" = Vincular, "B" = Alta Nueva
    var isSavingInsumo by remember { mutableStateOf(false) }

    // Campos Opción A (Vincular)
    var insumoASeleccionar by remember { mutableStateOf<MateriaPrima?>(null) }
    var cantidadAportaTextA by remember { mutableStateOf("") }
    var expandedDropdownInsumo by remember { mutableStateOf(false) }

    // Campos Opción B (Alta Nueva)
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaUnidad by remember { mutableStateOf("g") }
    val unidadesDisponibles = InventoryUnit.entries.map { it.symbol }
    var contributionUnit by remember { mutableStateOf("") }
    var expandedContributionUnit by remember { mutableStateOf(false) }
    var expandedDropdownUnidad by remember { mutableStateOf(false) }
    var nuevoNivelCriticoText by remember { mutableStateOf("10.0") }
    var cantidadAportaTextB by remember { mutableStateOf("") }

    // Campo auxiliar para simulación o entrada manual de prueba
    var showTestInputCodeDialog by rememberSaveable { mutableStateOf(false) }
    var testCodeInputText by rememberSaveable { mutableStateOf("") }
    var openSelectedEntry by rememberSaveable(selectedMaterialId) { mutableStateOf(selectedMaterialId != null) }
    var entryMaterialId by rememberSaveable { mutableStateOf<String?>(null) }

    val barcodeScanner = remember { GmsBarcodeScanning.getClient(context) }

    LaunchedEffect(insumosList, openSelectedEntry) {
        if (openSelectedEntry) {
            val material = insumosList.firstOrNull { it.id == selectedMaterialId }
            if (material != null) {
                openSelectedEntry = false
                entryMaterialId = material.id
                testCodeInputText = material.codigoBarras.ifBlank { material.codigosBarras.firstOrNull().orEmpty() }
                showTestInputCodeDialog = true
            }
        }
    }

    // Lógica del Escáner (Modo Autopiloto)
    fun procesarCodigoEscaneado(scannedCode: String, targetMaterialId: String? = null) {
        val codeClean = scannedCode.trim()
        if (codeClean.isEmpty()) return

        val db = FirebaseFirestore.getInstance()

        val materials = (latestWarehouse as? WarehouseLoad.Ready)?.items ?: return
        val currentMaterials = materials.map { it.material }
        val insumoEncontrado = currentMaterials.find { insumo ->
            insumo.codigosBarras.contains(codeClean) || insumo.codigoBarras == codeClean
        }

        if (targetMaterialId != null && (
                currentMaterials.none { it.id == targetMaterialId } ||
                    insumoEncontrado?.let { it.id != targetMaterialId } == true
                )
        ) {
            Toast.makeText(context, context.getString(R.string.warehouse_code_mismatch), Toast.LENGTH_SHORT).show()
            return
        }

        if (insumoEncontrado != null) {
            // SI LO ENCUENTRA (Modo Autopiloto)
            val cantidadAportada = insumoEncontrado.cantidadesPorCodigo[codeClean]
                ?: if (insumoEncontrado.cantidadActual > 0) 1.0 else 0.0

            db.collection("materia_prima").document(insumoEncontrado.id)
                .update("cantidadActual", FieldValue.increment(cantidadAportada))
                .addOnSuccessListener {
                    successMessage = localizedText(TextR.string.pan_stock_added, cantidadAportada, insumoEncontrado.unidadMedida, insumoEncontrado.nombre)
                    showSuccessDialog = true
                }
                .addOnFailureListener {
                    Toast.makeText(context, context.getString(R.string.admin_error_updating_stock_firestore), Toast.LENGTH_SHORT).show()
                }
        } else {
            // SI NO LO ENCUENTRA (Modo Aprendizaje)
            unregisteredCode = codeClean
            modoAprendizajeOpcion = "A"
            insumoASeleccionar = currentMaterials.firstOrNull { it.id == targetMaterialId }
            contributionUnit = insumoASeleccionar?.unidadMedida.orEmpty()
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
                entryMaterialId = null
                showTestInputCodeDialog = true
            }
    }

    val insumosCriticosCount = remember(insumosList) { insumosList.count { it.hasStockAlert() } }

    val colors = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(warehouseBackdrop()).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 108.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", contentType = "header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.admin_back))
                    }
                }
                Column(Modifier.weight(1f)) {
                    PanAppScreenTitle(localizedText(TextR.string.pan_warehouse_control), maxLines = 2)
                    Text(
                        stringResource(R.string.warehouse_scan_subtitle),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
                profileMenu()
            }
        }
        item(key = "stats", contentType = "stats") {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StockStatCard(Modifier.weight(1f), localizedText(TextR.string.pan_critical_supplies), insumosCriticosCount, colors.error)
                StockStatCard(Modifier.weight(1f), localizedText(TextR.string.pan_total_supplies), insumosList.size, colors.primary)
            }
        }
        item(key = "actions", contentType = "actions") {
            PanAppGlassCard(shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val interactions = remember { MutableInteractionSource() }
                    Button(
                        onClick = ::ejecutarEscaneo,
                        enabled = warehouse is WarehouseLoad.Ready,
                        modifier = Modifier.fillMaxWidth().panAppPressMotion(interactions),
                        interactionSource = interactions,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.QrCodeScanner, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.admin_scan_barcode))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        WarehouseAction(
                            text = stringResource(R.string.warehouse_manual_entry),
                            icon = Icons.Default.Keyboard,
                            onClick = { entryMaterialId = null; showTestInputCodeDialog = true },
                            modifier = Modifier.weight(1f),
                            enabled = warehouse is WarehouseLoad.Ready
                        )
                        WarehouseAction(
                            text = stringResource(R.string.warehouse_manage_stock),
                            icon = Icons.Default.Tune,
                            onClick = onManageStock,
                            modifier = Modifier.weight(1f),
                            highlighted = true
                        )
                    }
                }
            }
        }
        item(key = "materials", contentType = "heading") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    localizedText(TextR.string.pan_raw_materials),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium
                )
                Badge(containerColor = colors.primaryContainer, contentColor = colors.onPrimaryContainer) {
                    Text(insumosList.size.toString())
                }
            }
        }
        if (warehouse == WarehouseLoad.Loading) {
            item(key = "loading") {
                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        } else if (warehouse == WarehouseLoad.Error) {
            item(key = "error") {
                Column {
                    Text(stringResource(R.string.warehouse_stock_load_error), color = colors.error)
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.warehouse_stock_retry)) }
                }
            }
        } else if (insumosList.isEmpty()) {
            item(key = "empty") {
                Text(stringResource(R.string.admin_no_raw_materials_firestore), color = colors.onSurfaceVariant)
            }
        }
        items(insumosList, key = { it.id }, contentType = { "material" }) { insumo ->
            InsumoCard(insumo, modifier = panAppFilteredItem())
        }
    }

    // ✨ ALERT DIALOG AUTOPILOTO (ÉXITO AUTOMÁTICO)
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            icon = { Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(48.dp)) },
            title = { Text( stringResource(R.string.admin_success_entry), fontWeight = FontWeight.Bold, color = contrastTextColor(androidx.compose.material3.LocalContentColor.current)) },
            text = { Text( successMessage, fontSize = 15.sp, color = contrastTextColor(androidx.compose.material3.LocalContentColor.current)) },
            confirmButton = {
                Button(
                    onClick = { showSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text( stringResource(R.string.admin_ok), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                }
            }
        )
    }

    // ✨ MODO APRENDIZAJE: CÓDIGO NO REGISTRADO
    if (unregisteredCode != null) {
        val code = unregisteredCode!!

        AlertDialog(
            onDismissRequest = { unregisteredCode = null },
            containerColor = cardColor,
            title = { Text(stringResource(R.string.admin_code_not_registered), fontWeight = FontWeight.Bold, color = textColor) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.admin_code_read, code), fontWeight = FontWeight.Bold, color = contrastAccentColor(AdminPrimary), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de opción
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanAppGlassFilterChip(
                            selected = modoAprendizajeOpcion == "A",
                            onClick = { modoAprendizajeOpcion = "A" },
                            label = { Text( stringResource(R.string.admin_link_to_supply), fontSize = 11.sp, color = contrastTextColor(androidx.compose.material3.LocalContentColor.current)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = textColor,
                                selectedLabelColor = Color.White,
                                selectedContainerColor = AdminPrimary
                            )
                        )
                        PanAppGlassFilterChip(
                            selected = modoAprendizajeOpcion == "B",
                            onClick = { modoAprendizajeOpcion = "B" },
                            label = { Text( stringResource(R.string.admin_register_new), fontSize = 11.sp, color = contrastTextColor(androidx.compose.material3.LocalContentColor.current)) },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                labelColor = textColor,
                                selectedLabelColor = Color.White,
                                selectedContainerColor = AdminPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (modoAprendizajeOpcion == "A") {
                        // OPCIÓN A: VINCULAR A INSUMO EXISTENTE
                        Text(stringResource(R.string.admin_select_existing_supply), fontSize = 12.sp, color = contrastTextColor(Color.Gray, secondary = true))
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = insumoASeleccionar?.let { "${it.nombre} (${it.unidadMedida})" } ?: localizedText(TextR.string.pan_select_supply),
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedDropdownInsumo = true }) },
                                modifier = Modifier.fillMaxWidth().clickable { expandedDropdownInsumo = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    unfocusedBorderColor = Color.Gray
                                ))
                            )
                            PanAppGlassDropdown(
                                expanded = expandedDropdownInsumo,
                                onDismissRequest = { expandedDropdownInsumo = false },
                                modifier = Modifier.fillMaxWidth(0.8f)
                            ) {
                                insumosList.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(R.string.admin_supply_format, item.nombre, item.unidadMedida), color = textColor) },
                                        onClick = {
                                            insumoASeleccionar = item
                                            contributionUnit = item.unidadMedida
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
                            label = { Text(stringResource(R.string.admin_package_contribution_quantity)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                unfocusedBorderColor = Color.Gray
                            ))
                        )
                        insumoASeleccionar?.let { material ->
                            val baseUnit = InventoryUnit.from(material.unidadMedida)
                            val compatible = InventoryUnit.entries.filter {
                                baseUnit != null && it.group == baseUnit.group
                            }.map { it.symbol }
                            if (compatible.size > 1) {
                                Box {
                                    TextButton(onClick = { expandedContributionUnit = true }) {
                                        Text(stringResource(R.string.inventory_entry_unit, contributionUnit))
                                    }
                                    PanAppGlassDropdown(expandedContributionUnit, { expandedContributionUnit = false }) {
                                        compatible.forEach { unit ->
                                            DropdownMenuItem(text = { Text(unit) }, onClick = {
                                                contributionUnit = unit
                                                expandedContributionUnit = false
                                            })
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // OPCIÓN B: DAR DE ALTA NUEVO INSUMO
                        OutlinedTextField(
                            value = nuevoNombre,
                            onValueChange = { nuevoNombre = it },
                            label = { Text(stringResource(R.string.admin_supply_name)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                unfocusedBorderColor = Color.Gray
                            ))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = nuevoNivelCriticoText,
                            onValueChange = { nuevoNivelCriticoText = it },
                            label = { Text(stringResource(R.string.admin_low_stock_when), fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                Text(
                                    text = nuevaUnidad,
                                    modifier = Modifier.padding(end = 16.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = contrastTextColor(Color.Gray, secondary = true)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                unfocusedTextColor = textColor,
                                focusedTextColor = textColor,
                                unfocusedBorderColor = Color.Gray
                            ))
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = nuevaUnidad,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text(stringResource(R.string.admin_unit)) },
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedDropdownUnidad = true }) },
                                    modifier = Modifier.fillMaxWidth().clickable { expandedDropdownUnidad = true },
                                    shape = RoundedCornerShape(8.dp),
                                    singleLine = true,
                                    colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                        unfocusedTextColor = textColor,
                                        focusedTextColor = textColor,
                                        unfocusedBorderColor = Color.Gray
                                    ))
                                )
                                PanAppGlassDropdown(expanded = expandedDropdownUnidad, onDismissRequest = { expandedDropdownUnidad = false }, modifier = Modifier) {
                                    unidadesDisponibles.forEach { und ->
                                        DropdownMenuItem(text = { Text(und, color = textColor) }, onClick = { nuevaUnidad = und; expandedDropdownUnidad = false })
                                    }
                                }
                            }
                            OutlinedTextField(
                                value = cantidadAportaTextB,
                                onValueChange = { cantidadAportaTextB = it },
                                label = { Text(stringResource(R.string.admin_package_contribution)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                                    unfocusedTextColor = textColor,
                                    focusedTextColor = textColor,
                                    unfocusedBorderColor = Color.Gray
                                ))
                            )
                        }

                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isSavingInsumo) return@Button
                        
                        val db = FirebaseFirestore.getInstance()
                        if (modoAprendizajeOpcion == "A") {
                            val material = insumoASeleccionar
                            val cantAporta = inputNumber(cantidadAportaTextA)?.let { value ->
                                material?.let { convertInventoryAmount(value, contributionUnit, it.unidadMedida) }
                            }
                            if (material != null && cantAporta != null) {
                                isSavingInsumo = true
                                val targetDoc = db.collection("materia_prima").document(material.id)
                                targetDoc.update(
                                    com.google.firebase.firestore.FieldPath.of("cantidadActual"),
                                    FieldValue.increment(cantAporta),
                                    com.google.firebase.firestore.FieldPath.of("codigosBarras"),
                                    FieldValue.arrayUnion(code),
                                    com.google.firebase.firestore.FieldPath.of("cantidadesPorCodigo", code),
                                    cantAporta
                                ).addOnSuccessListener {
                                    isSavingInsumo = false
                                    Toast.makeText(context, context.getString(R.string.admin_code_linked, code, material.nombre), Toast.LENGTH_SHORT).show()
                                    unregisteredCode = null
                                }.addOnFailureListener { e ->
                                    isSavingInsumo = false
                                    Toast.makeText(context, context.getString(R.string.admin_error_message, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.admin_select_supply_contribution), Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val cantAporta = inputNumber(cantidadAportaTextB)?.toDouble() ?: 0.0
                            val critico = inputNumber(nuevoNivelCriticoText)?.toDouble()
                            if (nuevoNombre.isNotBlank() && cantAporta > 0 && critico != null) {
                                isSavingInsumo = true
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
                                    isSavingInsumo = false
                                    Toast.makeText(context, context.getString(R.string.admin_new_supply_linked), Toast.LENGTH_SHORT).show()
                                    unregisteredCode = null
                                }.addOnFailureListener { e ->
                                    isSavingInsumo = false
                                    Toast.makeText(context, context.getString(R.string.admin_error_message, e.message ?: ""), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, context.getString(R.string.admin_enter_name_contribution), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isSavingInsumo,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    if (isSavingInsumo) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text( stringResource(R.string.admin_confirm_and_save), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { unregisteredCode = null }) {
                    Text(stringResource(R.string.admin_cancel), color = textColor)
                }
            }
        )
    }

    // DIÁLOGO AUXILIAR PARA PRUEBA DE CÓDIGO MANUAL
    if (showTestInputCodeDialog) {
        val entryMaterial = insumosList.firstOrNull { it.id == entryMaterialId }
        val dismissEntry = {
            showTestInputCodeDialog = false
            entryMaterialId = null
            testCodeInputText = ""
        }
        AlertDialog(
            onDismissRequest = dismissEntry,
            containerColor = cardColor,
            title = { Text(entryMaterial?.nombre ?: stringResource(R.string.inventory_manual_code), color = textColor) },
            text = {
                Column {
                    Text(stringResource(R.string.inventory_code_hint), fontSize = 12.sp, color = contrastTextColor(Color.Gray, secondary = true))
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testCodeInputText,
                        onValueChange = { testCodeInputText = it },
                        label = { Text(stringResource(R.string.admin_barcode)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor,
                            unfocusedBorderColor = Color.Gray
                        ))
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val code = testCodeInputText.trim()
                        val target = entryMaterialId
                        dismissEntry()
                        if (code.isNotEmpty()) {
                            procesarCodigoEscaneado(code, target)
                        }
                    },
                    enabled = testCodeInputText.isNotBlank() && warehouse is WarehouseLoad.Ready,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    Text( stringResource(R.string.admin_process_code), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                }
            },
            dismissButton = {
                TextButton(onClick = dismissEntry) {
                    Text(stringResource(R.string.admin_cancel), color = textColor)
                }
            }
        )
    }
}

@Composable
private fun StockStatCard(modifier: Modifier, label: String, value: Int, color: Color) {
    PanAppGlassCard(modifier, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsumoCard(insumo: MateriaPrima, modifier: Modifier = Modifier) {
    val color = warehouseStockColor(insumo)
    val codes = remember(insumo.codigoBarras, insumo.codigosBarras) { insumo.warehouseBarcodes().size }
    PanAppGlassCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Column(Modifier.weight(1f)) {
                Text(insumo.nombre, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.warehouse_material_meta, warehouseQuantity(insumo.nivelCritico), insumo.unidadMedida, codes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(warehouseQuantity(insumo.cantidadActual), style = MaterialTheme.typography.titleMedium, color = color)
                Text(insumo.unidadMedida, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
