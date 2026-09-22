package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.developers.admin.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenStockScreen() {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black
    
    // 1. Base de Datos Reactiva (Estado de la Lista)
    val listaMateriales = remember { 
        mutableStateListOf<MaterialStock>().apply { addAll(getMaterialesStockInicial()) } 
    }
    
    // 4. Estados para Búsqueda y Filtros
    var textBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todo") }

    // Estado para Nuevo Material
    var showAddMaterialDialog by remember { mutableStateOf(false) }
    var nuevoNombreMat by remember { mutableStateOf("") }
    var nuevoSKUMat by remember { mutableStateOf("") }
    var nuevoMinimoMat by remember { mutableStateOf("") }
    var nuevaUnidadMat by remember { mutableStateOf("kg") }

    // Filtrado de la lista
    val materialesFiltrados = listaMateriales.filter { material ->
        val coincideBusqueda = material.nombre.contains(textBusqueda, ignoreCase = true) || 
                              material.sku.contains(textBusqueda, ignoreCase = true)
        val coincideCategoria = categoriaSeleccionada == "Todo" || material.categoria == categoriaSeleccionada
        coincideBusqueda && coincideCategoria
    }

    // Contador de Alertas Dinámico
    val alertasCount = listaMateriales.count { it.existencia < it.minimo }

    var materialParaAjustar by remember { mutableStateOf<MaterialStock?>(null) }
    var nuevoStockValue by remember { mutableStateOf("") }

    if (showAddMaterialDialog) {
        AlertDialog(
            onDismissRequest = { showAddMaterialDialog = false },
            containerColor = cardColor,
            title = { Text(stringResource(R.string.admin_new_material), color = textColor) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = nuevoNombreMat,
                        onValueChange = { nuevoNombreMat = it },
                        label = { Text(stringResource(R.string.admin_name)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
                    )
                    OutlinedTextField(
                        value = nuevoSKUMat,
                        onValueChange = { nuevoSKUMat = it },
                        label = { Text(stringResource(R.string.admin_sku)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
                    )
                    OutlinedTextField(
                        value = nuevoMinimoMat,
                        onValueChange = { nuevoMinimoMat = it },
                        label = { Text(stringResource(R.string.admin_min_required)) },
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nuevoNombreMat.isNotBlank()) {
                            listaMateriales.add(MaterialStock(nuevoNombreMat, nuevoSKUMat, 0.0, nuevoMinimoMat.toDoubleOrNull() ?: 10.0, nuevaUnidadMat, "Otros"))
                            showAddMaterialDialog = false
                            nuevoNombreMat = ""
                            nuevoSKUMat = ""
                            Toast.makeText(context, context.getString(R.string.admin_material_added), Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) { Text(stringResource(R.string.admin_add_btn)) }
            },
            dismissButton = {
                TextButton(onClick = { showAddMaterialDialog = false }) { Text(stringResource(R.string.admin_cancel), color = textColor) }
            }
        )
    }

    if (materialParaAjustar != null) {
        AlertDialog(
            onDismissRequest = { materialParaAjustar = null },
            containerColor = cardColor,
            title = { Text(stringResource(R.string.admin_adjust_stock_title, materialParaAjustar?.nombre ?: ""), color = textColor) },
            text = {
                Column {
                    Text(stringResource(R.string.admin_enter_new_quantity, materialParaAjustar?.unidad ?: ""), color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nuevoStockValue,
                        onValueChange = { nuevoStockValue = it },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = textColor, focusedTextColor = textColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val nuevaCant = nuevoStockValue.toDoubleOrNull()
                        if (nuevaCant != null) {
                            val index = listaMateriales.indexOfFirst { it.sku == materialParaAjustar?.sku }
                            if (index != -1) {
                                listaMateriales[index] = materialParaAjustar!!.copy(existencia = nuevaCant)
                                Toast.makeText(context, context.getString(R.string.admin_stock_updated), Toast.LENGTH_SHORT).show()
                            }
                            materialParaAjustar = null
                            nuevoStockValue = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    Text(stringResource(R.string.admin_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { materialParaAjustar = null }) {
                    Text("Cancelar", color = textColor)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // Top Bar
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = if (isDarkMode) Color.White else Color.Black,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = if (isDarkMode) Color.Black else Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.admin_bakery_stock), fontWeight = FontWeight.Bold, color = textColor)
                }
            },
            actions = {
                IconButton(onClick = { showAddMaterialDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo Material", tint = textColor)
                }
                IconButton(onClick = { 
                    Toast.makeText(context, context.getString(R.string.admin_syncing_db), Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones", tint = textColor)
                }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Buscador
            item {
                OutlinedTextField(
                    value = textBusqueda,
                    onValueChange = { textBusqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.admin_search_name_sku), color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = cardColor,
                        focusedContainerColor = cardColor,
                        unfocusedBorderColor = Color.LightGray,
                        unfocusedTextColor = textColor,
                        focusedTextColor = textColor
                    )
                )
            }

            // Filtros
            item {
                val categories = listOf("Todo", "Harinas", "Lácteos", "Azúcares", "Huevos", "Otros")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { category ->
                        FilterChip(
                            selected = categoriaSeleccionada == category,
                            onClick = { categoriaSeleccionada = category },
                            label = { Text(category) },
                            shape = RoundedCornerShape(16.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AdminPrimary,
                                selectedLabelColor = Color.White,
                                containerColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFE0E0E0),
                                labelColor = if (isDarkMode) Color.LightGray else Color.DarkGray
                            )
                        )
                    }
                }
            }

            // Indicadores de Estado
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.admin_showing_materials, materialesFiltrados.size), color = Color.Gray, fontSize = 14.sp)
                    if (alertasCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), color = Color.Red, shape = CircleShape) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.admin_stock_alerts_count, alertasCount), color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Lista de Materiales
            items(materialesFiltrados, key = { it.sku }) { material ->
                MaterialStockCard(
                    material = material,
                    isDarkMode = isDarkMode,
                    onAjustarClick = {
                        materialParaAjustar = material
                        nuevoStockValue = material.existencia.toString()
                    },
                    onEditClick = {
                        Toast.makeText(context, context.getString(R.string.admin_modifying_material, material.nombre), Toast.LENGTH_SHORT).show()
                    }
                )
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
fun MaterialStockCard(
    material: MaterialStock,
    isDarkMode: Boolean,
    onAjustarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    // 2. Lógica Dinámica de Estado y Color
    val (estado, color) = when {
        material.existencia <= 0 -> "AGOTADO" to Color(0xFFF44336)
        material.existencia < material.minimo -> "BAJO" to Color(0xFFFFA000)
        else -> "OK" to Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(material.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = textColor)
                    Surface(color = if (isDarkMode) Color(0xFF333333) else Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)) {
                        Text(
                            "SKU: ${material.sku}",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Surface(
                    color = color.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        estado,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = color,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Existencias
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.admin_current_stock), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.admin_stock_unit, material.existencia.toString(), material.unidad), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(stringResource(R.string.admin_min_required_caps), fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.admin_stock_unit, material.minimo.toString(), material.unidad), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Barra de progreso dinámica
            val progress = (material.existencia / material.minimo).coerceIn(0.0, 1.0).toFloat()
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = if (isDarkMode) Color(0xFF333333) else Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Acciones
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onAjustarClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) Color.LightGray else Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.admin_adjust), fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = if (isDarkMode) Color.LightGray else Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.admin_edit), fontWeight = FontWeight.Bold)
                    }
                }
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

data class MaterialStock(
    val nombre: String,
    val sku: String,
    val existencia: Double,
    val minimo: Double,
    val unidad: String,
    val categoria: String
)

fun getMaterialesStockInicial() = listOf(
    MaterialStock("Harina de Trigo 000", "HRN-001", 12.5, 50.0, "kg", "Harinas"),
    MaterialStock("Levadura Seca", "LEV-042", 5.0, 2.0, "kg", "Lácteos"),
    MaterialStock("Mantequilla Sin Sal", "LAC-015", 0.0, 10.0, "kg", "Lácteos"),
    MaterialStock("Azúcar Refinada", "AZU-009", 85.0, 30.0, "kg", "Azúcares"),
    MaterialStock("Huevos Docena", "HUE-221", 4.0, 12.0, "pcs", "Otros")
)

@Preview(showBackground = true)
@Composable
fun AlmacenStockPreview() {
    AlmacenStockScreen()
}
