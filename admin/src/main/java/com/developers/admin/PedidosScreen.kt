package com.developers.admin

import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
import java.text.NumberFormat

// 1. Data Class Proveedor
data class Proveedor(
    val id: String = "",
    val nombre: String = "",
    val telefono: String = "",
    val materiaPrimaId: String = "",
    val materiaPrimaNombre: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen() {
    val isDarkMode = isSystemInDarkTheme()
    var tabIndex by remember { mutableIntStateOf(0) }
    var showProveedorDialog by remember { mutableStateOf(false) }
    var proveedorAEditar by remember { mutableStateOf<Proveedor?>(null) }

    var materiasPrimas by remember { mutableStateOf<List<MateriaPrima>>(emptyList()) }
    var proveedores by remember { mutableStateOf<List<Proveedor>>(emptyList()) }

    val bgColor = MaterialTheme.colorScheme.background
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface

    // Escucha en tiempo real de materia_prima y proveedores
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("materia_prima").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            val lista = snapshot.documents.mapNotNull { doc ->
                val insumo = doc.toObject(MateriaPrima::class.java)?.copy(id = doc.id)
                if (insumo != null) {
                    // Reset Automático: Si ya hay stock pero quedó con alertas registradas, lo limpiamos
                    if (insumo.cantidadActual > insumo.nivelCritico && insumo.alertasEnviadas > 0) {
                        db.collection("materia_prima").document(insumo.id).update("alertasEnviadas", 0)
                    }
                }
                insumo
            }
            materiasPrimas = lista
        }

        db.collection("proveedores").addSnapshotListener { snapshot, error ->
            if (error != null || snapshot == null) return@addSnapshotListener
            proveedores = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Proveedor::class.java)?.copy(id = doc.id)
            }
        }
    }

    // 2. Estructura de Pantalla y Pestañas
    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    proveedorAEditar = null
                    showProveedorDialog = true
                },
                containerColor = AdminPrimary,
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier.padding(bottom = 80.dp) // ✨ Subimos el FAB para que no tape la barra
            ) {
                Icon( Icons.Default.Add, contentDescription = stringResource(R.string.admin_add_supplier), tint = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
            }
        },
        containerColor = bgColor
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .statusBarsPadding() // ✨ Evita empalme con barra de estado
        ) {
            // ✨ 1. Cabecera Personalizada y Contador (Adiós al TabRow genérico)
            val insumosCriticos = materiasPrimas.filter { it.cantidadActual <= it.nivelCritico }
            val sugerenciasCount = insumosCriticos.size

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedText(TextR.string.pan_orders),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor
                )
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD)
                ) {
                    Text(
                        text = localizedText(TextR.string.pan_suggestions_count, sugerenciasCount),
                        color = if (isDarkMode) Color(0xFFBBDEFB) else Color(0xFF1976D2),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp
                    )
                }
            }

            // ✨ 2. Pestañas Estilo 'Pill' Personalizadas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pill 1: Sugerencias
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (tabIndex == 0) (if (isDarkMode) AdminPrimary.copy(alpha = 0.2f) else Color(0xFFF3E5F5)) else Color.Transparent,
                    border = if (tabIndex == 0) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.clickable { tabIndex = 0 }
                ) {
                    Text(
                        text = localizedText(TextR.string.pan_suggestions),
                        color = if (tabIndex == 0) (if (isDarkMode) AdminPrimary else Color(0xFF6200EE)) else contrastTextColor(Color.Gray, secondary = true),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Pill 2: Proveedores
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (tabIndex == 1) (if (isDarkMode) AdminPrimary.copy(alpha = 0.2f) else Color(0xFFF3E5F5)) else Color.Transparent,
                    border = if (tabIndex == 1) null else androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray),
                    modifier = Modifier.clickable { tabIndex = 1 }
                ) {
                    Text(
                        text = localizedText(TextR.string.pan_suppliers),
                        color = if (tabIndex == 1) (if (isDarkMode) AdminPrimary else Color(0xFF6200EE)) else contrastTextColor(Color.Gray, secondary = true),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }

            if (tabIndex == 0) {
                TabPedidosSugerencias(insumosCriticos, proveedores, isDarkMode)
            } else {
                TabProveedoresDirectorio(proveedores, isDarkMode) { prov ->
                    proveedorAEditar = prov
                    showProveedorDialog = true
                }
            }
        }
    }

    if (showProveedorDialog) {
        AddEditProveedorDialog(
            proveedor = proveedorAEditar,
            materiasPrimas = materiasPrimas,
            onDismiss = { showProveedorDialog = false },
            isDarkMode = isDarkMode
        )
    }
}

// 4. Pestaña 1: 'Pedidos' (Sugerencias Automáticas WMS)
@Composable
fun TabPedidosSugerencias(insumosCriticos: List<MateriaPrima>, proveedores: List<Proveedor>, isDarkMode: Boolean) {
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface
    
    var restockId by remember { mutableStateOf<String?>(null) }
    val selected = insumosCriticos.firstOrNull { it.id == restockId }
    val supplier = proveedores.firstOrNull { selected != null && it.materiaPrimaId == selected.id }
    if (selected != null && supplier != null) {
        RestockDialog(selected, supplier, onClose = { restockId = null })
    }

    if (insumosCriticos.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = localizedText(TextR.string.pan_no_pending_orders),
                color = contrastTextColor(Color.Gray, secondary = true),
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(insumosCriticos, key = { it.id }) { insumo ->
                val proveedor = proveedores.find { it.materiaPrimaId == insumo.id }
                
                if (proveedor != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = cardColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Surface(
                                color = if (isDarkMode) Color(0xFF3E2723) else Color(0xFFFFF3E0), // Naranja suave adaptive
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                // ✨ 3. Iconografía en la Tarjeta de Pedido (Etiqueta SUGERENCIA)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFFF9800),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = localizedText(TextR.string.pan_suggestion),
                                        color = if (isDarkMode) Color(0xFFFFB74D) else Color(0xFFE65100),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(proveedor.nombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = textColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                localizedText(TextR.string.pan_reorder_suggestion, insumo.nombre),
                                color = contrastTextColor(Color.DarkGray, secondary = true, darkBackground = isDarkMode),
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            val limiteAlcanzado = insumo.alertasEnviadas >= 3
                            
                            Button(
                                onClick = { restockId = insumo.id },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !limiteAlcanzado,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4CAF50),
                                    disabledContainerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                // ✨ 3. Iconografía en el botón de WhatsApp
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (!limiteAlcanzado) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        if (limiteAlcanzado) localizedText(TextR.string.pan_notice_limit) else localizedText(TextR.string.pan_send_whatsapp),
                                        color = if (limiteAlcanzado) contrastTextColor(Color.Gray, secondary = true) else Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) } // ✨ Espacio para la barra flotante
        }
    }
}

@Composable
private fun RestockDialog(material: MateriaPrima, supplier: Proveedor, onClose: () -> Unit) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val unit = material.unidadMedida
    val stock = BigDecimal.valueOf(material.cantidadActual)
    val initialTarget = BigDecimal.valueOf(material.existenciaObjetivo ?: material.nivelCritico)
    val initialPackage = material.tamanoPaquete?.takeIf { it > 0 }?.let(BigDecimal::valueOf)
    var targetInput by remember(material.id) {
        mutableStateOf(initialTarget.max(BigDecimal.ZERO).inventoryInput())
    }
    var packageInput by remember(material.id) { mutableStateOf(initialPackage?.inventoryInput().orEmpty()) }
    var quantityInput by remember(material.id) {
        mutableStateOf(suggestedRestock(stock, initialTarget, initialPackage).inventoryInput())
    }
    var saving by remember(material.id) { mutableStateOf(false) }
    val target = inputNumber(targetInput)
    val packageSize = inputNumber(packageInput)?.takeIf { it.signum() > 0 }
    val validSettings = target != null && target.toDouble() >= material.nivelCritico &&
        (packageInput.isBlank() || packageSize != null)
    val quantity = inputNumber(quantityInput)?.takeIf { it.signum() > 0 }
    val document = remember(material.id) {
        FirebaseFirestore.getInstance().collection("materia_prima").document(material.id)
    }

    AlertDialog(
        onDismissRequest = { if (!saving) onClose() },
        title = { Text(stringResource(R.string.inventory_restock)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(material.nombre, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.warehouse_stock, "${material.cantidadActual} $unit"))
                RestockNumber(targetInput, { targetInput = it }, R.string.inventory_target, unit, !saving)
                RestockNumber(packageInput, { packageInput = it }, R.string.inventory_package, unit, !saving)
                if (!validSettings) Text(
                    stringResource(R.string.inventory_restock_validation),
                    color = MaterialTheme.colorScheme.error
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(enabled = validSettings && !saving, onClick = {
                        quantityInput = suggestedRestock(stock, checkNotNull(target), packageSize).inventoryInput()
                    }) { Text(stringResource(R.string.inventory_suggest)) }
                    TextButton(enabled = validSettings && !saving, onClick = {
                        saving = true
                        document.update(mapOf(
                            "existenciaObjetivo" to checkNotNull(target).toDouble(),
                            "tamanoPaquete" to (packageSize?.toDouble() ?: FieldValue.delete())
                        )).addOnSuccessListener {
                            saving = false
                            Toast.makeText(context, R.string.inventory_restock_saved, Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            saving = false
                            Toast.makeText(context, R.string.inventory_restock_error, Toast.LENGTH_SHORT).show()
                        }
                    }) { Text(stringResource(R.string.inventory_save_target)) }
                }
                RestockNumber(quantityInput, { quantityInput = it }, R.string.inventory_order_quantity, unit, !saving)
            }
        },
        confirmButton = {
            TextButton(enabled = quantity != null && !saving && material.alertasEnviadas < 3, onClick = {
                val amount = quantity ?: return@TextButton
                val formatted = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 6 }.format(amount)
                val message = context.getString(R.string.inventory_supplier_message, material.nombre, formatted, unit)
                val uri = Uri.parse("https://api.whatsapp.com/send").buildUpon()
                    .appendQueryParameter("phone", supplier.telefono)
                    .appendQueryParameter("text", message)
                    .build()
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    document.update("alertasEnviadas", FieldValue.increment(1))
                    onClose()
                } catch (e: android.content.ActivityNotFoundException) {
                    Toast.makeText(context, R.string.admin_error_open_whatsapp, Toast.LENGTH_SHORT).show()
                }
            }) { Text(localizedText(TextR.string.pan_send_whatsapp)) }
        },
        dismissButton = {
            TextButton(enabled = !saving, onClick = onClose) { Text(stringResource(R.string.admin_cancel)) }
        }
    )
}

@Composable
private fun RestockNumber(value: String, onChange: (String) -> Unit, label: Int, unit: String, enabled: Boolean) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        suffix = { Text(unit) },
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        colors = contrastFieldColors(OutlinedTextFieldDefaults.colors())
    )
}

// 5. Pestaña 2: 'Proveedores' (Directorio)
@Composable
fun TabProveedoresDirectorio(proveedores: List<Proveedor>, isDarkMode: Boolean, onEditClick: (Proveedor) -> Unit) {
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface

    if (proveedores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = localizedText(TextR.string.pan_no_suppliers),
                color = contrastTextColor(Color.Gray, secondary = true),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(proveedores, key = { it.id }) { prov ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = cardColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(prov.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(stringResource(R.string.admin_supplier_tel, prov.telefono), color = contrastTextColor(Color.Gray, secondary = true), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                color = if (isDarkMode) AdminPrimary.copy(alpha = 0.2f) else Color(0xFFF3E5F5),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = prov.materiaPrimaNombre,
                                    color = if (isDarkMode) AdminPrimary else Color(0xFF6200EE),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { onEditClick(prov) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.admin_edit), tint = contrastTextColor(Color.Gray, secondary = true))
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(100.dp)) } // ✨ Espacio para la barra flotante
        }
    }
}

// 3. Diálogo de Alta/Edición de Proveedor
@Composable
fun AddEditProveedorDialog(
    proveedor: Proveedor?,
    materiasPrimas: List<MateriaPrima>,
    onDismiss: () -> Unit,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf(proveedor?.nombre ?: "") }
    var telefono by remember { mutableStateOf(proveedor?.telefono ?: "") }
    
    var selectedInsumo by remember { 
        mutableStateOf(materiasPrimas.find { it.id == proveedor?.materiaPrimaId }) 
    }
    var expandedDropdown by remember { mutableStateOf(false) }

    var isSaving by remember { mutableStateOf(false) }

    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val cardColor = MaterialTheme.colorScheme.surface

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = cardColor,
        title = { Text(if (proveedor == null) stringResource(R.string.admin_new_supplier) else stringResource(R.string.admin_edit_supplier), fontWeight = FontWeight.Bold, color = textColor) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text(stringResource(R.string.admin_supplier_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    ))
                )
                OutlinedTextField(
                    value = telefono,
                    onValueChange = { telefono = it },
                    label = { Text(stringResource(R.string.admin_phone_whatsapp)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    ))
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedInsumo?.nombre ?: localizedText(TextR.string.pan_select_material),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.admin_supplied_material)) },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, Modifier.clickable { expandedDropdown = true }) },
                        modifier = Modifier.fillMaxWidth().clickable { expandedDropdown = true },
                        colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                            unfocusedTextColor = textColor,
                            focusedTextColor = textColor
                        ))
                    )
                    DropdownMenu(
                        expanded = expandedDropdown,
                        onDismissRequest = { expandedDropdown = false },
                        modifier = Modifier.fillMaxWidth(0.8f).background(cardColor)
                    ) {
                        materiasPrimas.forEach { insumo ->
                            DropdownMenuItem(
                                text = { Text(insumo.nombre, color = textColor) },
                                onClick = {
                                    selectedInsumo = insumo
                                    expandedDropdown = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank() && telefono.isNotBlank() && selectedInsumo != null) {
                        isSaving = true
                        val db = FirebaseFirestore.getInstance()
                        val dataMap = hashMapOf(
                            "nombre" to nombre.trim(),
                            "telefono" to telefono.trim(),
                            "materiaPrimaId" to selectedInsumo!!.id,
                            "materiaPrimaNombre" to selectedInsumo!!.nombre
                        )

                        if (proveedor == null) {
                            db.collection("proveedores").add(dataMap)
                                .addOnSuccessListener { 
                                    Toast.makeText(context, context.getString(R.string.admin_supplier_created), Toast.LENGTH_SHORT).show()
                                    onDismiss() 
                                }
                        } else {
                            db.collection("proveedores").document(proveedor.id).update(dataMap as Map<String, Any>)
                                .addOnSuccessListener { 
                                    Toast.makeText(context, context.getString(R.string.admin_supplier_updated), Toast.LENGTH_SHORT).show()
                                    onDismiss() 
                                }
                        }
                    } else {
                        Toast.makeText(context, context.getString(R.string.admin_fill_all_fields), Toast.LENGTH_SHORT).show()
                    }
                },
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
            ) {
                Text( if (isSaving) stringResource(R.string.admin_saving) else stringResource(R.string.admin_save), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSaving) {
                Text(stringResource(R.string.admin_cancel), color = textColor)
            }
        }
    )
}
