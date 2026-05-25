package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlmacenStockScreen() {
    val context = LocalContext.current
    
    // 1. Base de Datos Reactiva (Estado de la Lista)
    val listaMateriales = remember { 
        mutableStateListOf<MaterialStock>().apply { addAll(getMaterialesStockInicial()) } 
    }
    
    // 4. Estados para Búsqueda y Filtros
    var textBusqueda by remember { mutableStateOf("") }
    var categoriaSeleccionada by remember { mutableStateOf("Todo") }

    // Filtrado de la lista
    val materialesFiltrados = listaMateriales.filter { material ->
        val coincideBusqueda = material.nombre.contains(textBusqueda, ignoreCase = true) || 
                              material.sku.contains(textBusqueda, ignoreCase = true)
        val coincideCategoria = categoriaSeleccionada == "Todo" || material.categoria == categoriaSeleccionada
        coincideBusqueda && coincideCategoria
    }

    // Contador de Alertas Dinámico
    val alertasCount = listaMateriales.count { it.existencia < it.minimo }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
    ) {
        // Top Bar
        CenterAlignedTopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.Black,
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panaderia Stock", fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "Notificaciones")
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
                    placeholder = { Text("Buscar por nombre o SKU...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.White,
                        focusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.LightGray
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
                                selectedContainerColor = Color(0xFF6200EE),
                                selectedLabelColor = Color.White,
                                containerColor = Color(0xFFE0E0E0),
                                labelColor = Color.DarkGray
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
                    Text("Mostrando ${materialesFiltrados.size} materiales", color = Color.Gray, fontSize = 14.sp)
                    if (alertasCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(modifier = Modifier.size(8.dp), color = Color.Red, shape = CircleShape) {}
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("$alertasCount Alertas de stock", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // Lista de Materiales
            items(materialesFiltrados, key = { it.sku }) { material ->
                MaterialStockCard(
                    material = material,
                    onAjustarClick = {
                        val index = listaMateriales.indexOfFirst { it.sku == material.sku }
                        if (index != -1) {
                            listaMateriales[index] = material.copy(existencia = material.existencia + 5.0)
                        }
                    },
                    onEditClick = {
                        Toast.makeText(context, "Modificando ${material.nombre}...", Toast.LENGTH_SHORT).show()
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
    onAjustarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    // 2. Lógica Dinámica de Estado y Color
    val (estado, color) = when {
        material.existencia <= 0 -> "AGOTADO" to Color(0xFFF44336)
        material.existencia < material.minimo -> "BAJO" to Color(0xFFFFA000)
        else -> "OK" to Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Fila superior
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(material.nombre, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Surface(color = Color(0xFFF5F5F5), shape = RoundedCornerShape(4.dp)) {
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
                    Text("EXISTENCIA ACTUAL", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${material.existencia} ${material.unidad}", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("MÍNIMO REQUERIDO", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${material.minimo} ${material.unidad}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
                trackColor = Color(0xFFEEEEEE)
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            // Acciones
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = onAjustarClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajustar", fontWeight = FontWeight.Bold)
                    }
                    TextButton(
                        onClick = onEditClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.DarkGray)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Editar", fontWeight = FontWeight.Bold)
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
