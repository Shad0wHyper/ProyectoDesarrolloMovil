package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAdminScreen() {
    var currentScreen by remember { mutableStateOf("dashboard") }
    
    // 1. Base de Datos Simulada (Estado de la Lista)
    val listaProductos = remember { 
        mutableStateListOf<Producto>().apply { addAll(getProductosEjemplo()) } 
    }
    
    // 3. Estado del Buscador
    var textBusqueda by remember { mutableStateOf("") }
    
    // Estado para Nuevo Producto
    var showAddProductDialog by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf("") }
    var nuevaCategoria by remember { mutableStateOf("Panes") }
    var nuevoPrecio by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showAddProductDialog) {
        AlertDialog(
            onDismissRequest = { showAddProductDialog = false },
            title = { Text("Nuevo Producto") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = nuevoNombre, onValueChange = { nuevoNombre = it }, label = { Text("Nombre") })
                    OutlinedTextField(value = nuevaCategoria, onValueChange = { nuevaCategoria = it }, label = { Text("Categoría") })
                    OutlinedTextField(value = nuevoPrecio, onValueChange = { nuevoPrecio = it }, label = { Text("Precio") })
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (nuevoNombre.isNotBlank() && nuevoPrecio.isNotBlank()) {
                        listaProductos.add(Producto(nuevoNombre, nuevaCategoria, 0, "AGOTADO", Color.Red, nuevoPrecio))
                        showAddProductDialog = false
                        nuevoNombre = ""
                        nuevoPrecio = ""
                        Toast.makeText(context, "Producto agregado", Toast.LENGTH_SHORT).show()
                    }
                }) { Text("Agregar") }
            },
            dismissButton = {
                TextButton(onClick = { showAddProductDialog = false }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        bottomBar = { 
            AdminBottomBar(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            ) 
        },
        floatingActionButton = { 
            if (currentScreen == "dashboard") {
                AdminFAB(onAdd = { showAddProductDialog = true })
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (currentScreen) {
                "dashboard" -> DashboardContent(
                    listaProductos = listaProductos,
                    textBusqueda = textBusqueda,
                    onTextBusquedaChange = { textBusqueda = it },
                    onGestionarPedidosClick = { currentScreen = "pedidos" },
                    onAIPredictionsClick = { currentScreen = "ia_report" },
                    onAlmacenClick = { currentScreen = "almacen" },
                    onAddClick = { showAddProductDialog = true }
                )
                "almacen" -> AlmacenStockScreen()
                "pedidos" -> PedidosScreen()
                "ia_report" -> AIReportScreen(onBack = { currentScreen = "dashboard" })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    listaProductos: MutableList<Producto>,
    textBusqueda: String,
    onTextBusquedaChange: (String) -> Unit,
    onGestionarPedidosClick: () -> Unit,
    onAIPredictionsClick: () -> Unit,
    onAlmacenClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val context = LocalContext.current

    // 3. Filtrado en tiempo real
    val productosFiltrados = if (textBusqueda.isEmpty()) {
        listaProductos
    } else {
        listaProductos.filter { it.nombre.contains(textBusqueda, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { AdminTopBar() }
        item { 
            ResumenHoySection(
                onGestionarPedidosClick = onGestionarPedidosClick
            ) 
        }
        item { AIPredictionsSection(onClick = onAIPredictionsClick) }
        item { 
            GestionProductosSection(
                textBusqueda = textBusqueda,
                onTextBusquedaChange = onTextBusquedaChange,
                onAddClick = onAddClick
            ) 
        }
        
        item {
            Text(
                text = "CATEGORÍA: PANES & BOLLERÍA",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        items(productosFiltrados) { producto ->
            ProductoCard(
                producto = producto,
                onInventarioClick = onAlmacenClick,
                onDarDeBajaClick = {
                    // 2. Decrementar stock
                    val index = listaProductos.indexOf(producto)
                    if (index != -1) {
                        val newStock = (producto.stock - 1).coerceAtLeast(0)
                        listaProductos[index] = producto.copy(
                            stock = newStock,
                            statusLabel = when {
                                newStock == 0 -> "AGOTADO"
                                newStock < 5 -> "Crítico"
                                else -> "Bajo"
                            },
                            statusColor = when {
                                newStock == 0 -> Color.Red
                                newStock < 5 -> Color(0xFFF44336)
                                else -> Color(0xFFFFA000)
                            }
                        )
                    }
                },
                onEditClick = {
                    Toast.makeText(context, "Editando ${producto.nombre}...", Toast.LENGTH_SHORT).show()
                }
            )
        }

        item { AlertaSuministrosCard(onClick = onAlmacenClick) }
        
        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar() {
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = "Administración",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        actions = {
            Box(modifier = Modifier.padding(8.dp).clickable {
                Toast.makeText(context, "No hay notificaciones nuevas", Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Alertas",
                    modifier = Modifier.size(28.dp)
                )
                Surface(
                    modifier = Modifier
                        .size(10.dp)
                        .align(Alignment.TopEnd),
                    color = Color.Red,
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White)
                ) {}
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .size(36.dp)
                    .padding(end = 8.dp)
                    .clickable {
                        Toast.makeText(context, "Perfil de Administrador", Toast.LENGTH_SHORT).show()
                    },
                shape = CircleShape,
                color = Color.LightGray
            ) {
                // Imagen de perfil placeholder
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.padding(4.dp))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF8F9FA))
    )
}

@Composable
fun AIPredictionsSection(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF673AB7)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Predicciones Inteligentes (Beta)",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Ventas hoy, Stock y Producción",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.White
            )
        }
    }
}

@Composable
fun ResumenHoySection(onGestionarPedidosClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Actualizado 10:30 AM",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.align(Alignment.End)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ResumenCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                iconColor = Color(0xFF2196F3),
                label = "Ventas Totales",
                value = "$1,240.50",
                trendText = "+12% vs ayer",
                trendColor = Color(0xFF4CAF50)
            )
            ResumenCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.ErrorOutline,
                iconColor = Color.Red,
                label = "Stock Bajo",
                value = "8 items",
                isAlert = true
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onGestionarPedidosClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD), contentColor = Color(0xFF1976D2)),
            contentPadding = PaddingValues(16.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = null)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Gestionar Pedidos", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Envío vía WhatsApp", fontSize = 12.sp)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun ResumenCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    trendText: String? = null,
    trendColor: Color = Color.Black,
    isAlert: Boolean = false
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            if (trendText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = trendColor, modifier = Modifier.size(14.dp))
                    Text(trendText, style = MaterialTheme.typography.labelSmall, color = trendColor)
                }
            }
        }
    }
}

@Composable
fun GestionProductosSection(
    textBusqueda: String,
    onTextBusquedaChange: (String) -> Unit,
    onAddClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de Productos", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
            Row {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filtrar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Text("+ Nuevo")
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = textBusqueda,
            onValueChange = onTextBusquedaChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Buscar producto...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(24.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = Color.White,
                focusedContainerColor = Color.White
            )
        )
    }
}

@Composable
fun ProductoCard(
    producto: Producto,
    onInventarioClick: () -> Unit,
    onDarDeBajaClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0F0F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(producto.nombre, fontWeight = FontWeight.Bold)
                    Text(producto.categoria, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                    Text("Stock: ${producto.stock} und.", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = producto.statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = producto.statusLabel,
                            color = producto.statusColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$ ${producto.precio}", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = Color.LightGray)
            Row(
                modifier = Modifier
                    .padding(8.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onInventarioClick) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color(0xFF2196F3))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Inventario", color = Color(0xFF2196F3))
                }
                TextButton(onClick = onDarDeBajaClick) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Dar de baja", color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun AlertaSuministrosCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alerta de Suministros", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "La Harina Integral y el Azúcar glass están por debajo del 10% de su capacidad. Considere reabastecer hoy.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Ver Inventario Crítico",
                color = Color.Red,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.clickable(onClick = onClick)
            )
        }
    }
}

@Composable
fun AdminFAB(onAdd: () -> Unit) {
    FloatingActionButton(
        onClick = onAdd,
        containerColor = Color(0xFFE91E63),
        contentColor = Color.White,
        shape = CircleShape
    ) {
        Icon(Icons.Default.Add, contentDescription = "Nuevo")
    }
}

@Composable
fun AdminBottomBar(currentScreen: String, onScreenSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = currentScreen == "dashboard",
            onClick = { onScreenSelected("dashboard") },
            icon = { Icon(Icons.Default.GridView, contentDescription = null) },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6200EE),
                selectedTextColor = Color(0xFF6200EE),
                indicatorColor = Color(0xFFF3E5F5)
            )
        )
        NavigationBarItem(
            selected = currentScreen == "almacen",
            onClick = { onScreenSelected("almacen") },
            icon = {
                BadgedBox(badge = { Badge { Text("9") } }) {
                    Icon(if (currentScreen == "almacen") Icons.Filled.Inventory2 else Icons.Outlined.Inventory2, contentDescription = null)
                }
            },
            label = { Text("Almacén") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6200EE),
                selectedTextColor = Color(0xFF6200EE),
                indicatorColor = Color(0xFFF3E5F5)
            )
        )
        NavigationBarItem(
            selected = currentScreen == "pedidos",
            onClick = { onScreenSelected("pedidos") },
            icon = { Icon(if (currentScreen == "pedidos") Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
            label = { Text("Pedidos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6200EE),
                selectedTextColor = Color(0xFF6200EE),
                indicatorColor = Color(0xFFF3E5F5)
            )
        )
    }
}

@Composable
fun ExtendedFAB(onClick: () -> Unit) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = Color(0xFF6200EE),
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Nuevo Item") }
    )
}

data class Producto(
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val statusLabel: String,
    val statusColor: Color,
    val precio: String
)

fun getProductosEjemplo() = listOf(
    Producto("Croissant de Mantequilla", "Panes", 12, "Bajo", Color(0xFFFFA000), "2.50"),
    Producto("Baguette Tradicional", "Panes", 45, "Óptimo", Color(0xFF4CAF50), "1.80"),
    Producto("Muffin de Chocolate", "Bollería", 5, "Crítico", Color(0xFFF44336), "3.20")
)

@Preview(showBackground = true)
@Composable
fun DashboardAdminPreview() {
    DashboardAdminScreen()
}
