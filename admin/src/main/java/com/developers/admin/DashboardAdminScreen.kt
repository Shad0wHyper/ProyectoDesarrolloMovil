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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

data class Producto(
    val id: String = "",
    val nombre: String,
    val categoria: String,
    val stock: Int,
    val statusLabel: String,
    val statusColor: Color,
    val precio: String,
    val imagenUrl: String = "",
    val calificacion: Double = 0.0,
    val isNuevo: Boolean = false // Añadido para que el Dashboard lo lea bien
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardAdminScreen(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AdminScreen.Dashboard.route

    // ✨ Variable para guardar el pan que queremos editar
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }

    var listaProductos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var textBusqueda by remember { mutableStateOf("") }
    val context = LocalContext.current

    LaunchedEffect(currentRoute) {
        if (currentRoute == AdminScreen.Dashboard.route) {
            isLoading = true
            val db = FirebaseFirestore.getInstance()
            db.collection("productos").get().addOnSuccessListener { result ->
                listaProductos = result.documents.map { doc ->
                    val stockReal = doc.getLong("stock")?.toInt() ?: 0

                    val statusL = when {
                        stockReal == 0 -> "AGOTADO"
                        stockReal < 5 -> "Crítico"
                        else -> "Óptimo"
                    }
                    val statusC = when {
                        stockReal == 0 -> Color.Red
                        stockReal < 5 -> Color(0xFFF44336)
                        else -> Color(0xFF4CAF50)
                    }

                    Producto(
                        id = doc.id,
                        nombre = doc.getString("nombre") ?: "Sin nombre",
                        categoria = doc.getString("categoria") ?: "Otros",
                        stock = stockReal,
                        statusLabel = statusL,
                        statusColor = statusC,
                        precio = String.format("%.2f", doc.getDouble("precio") ?: 0.0),
                        imagenUrl = doc.getString("imagenUrl") ?: "",
                        calificacion = doc.getDouble("calificacion") ?: 5.0,
                        isNuevo = doc.getBoolean("isNuevo") ?: false
                    )
                }
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, "Error al cargar inventario", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        bottomBar = {
            // ✨ Ocultamos la barra inferior si estamos en el generador QR o agregar producto
            if (currentRoute != AdminScreen.AddProduct.route && currentRoute != AdminScreen.QrGenerator.route) {
                AdminBottomBar(currentRoute = currentRoute, onScreenSelected = { route ->
                    navController.navigate(route) {
                        popUpTo(AdminScreen.Dashboard.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                })
            }
        },
        floatingActionButton = {
            if (currentRoute == AdminScreen.Dashboard.route) {
                AdminFAB(onAdd = {
                    productoAEditar = null // Limpia la variable si vamos a agregar uno nuevo
                    navController.navigate(AdminScreen.AddProduct.route)
                })
            }
        },
        containerColor = Color(0xFFF8F9FA)
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = AdminScreen.Dashboard.route,
            modifier = Modifier
                .padding(paddingValues)
                .background(Color(0xFFF8F9FA)), // Fondo sólido para evitar transparencias
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            composable(AdminScreen.Dashboard.route) {
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6200EE))
                    }
                } else {
                    DashboardContent(
                        listaProductos = listaProductos,
                        textBusqueda = textBusqueda,
                        onTextBusquedaChange = { textBusqueda = it },
                        onGestionarPedidosClick = { navController.navigate(AdminScreen.Pedidos.route) },
                        onAIPredictionsClick = { navController.navigate(AdminScreen.IAReport.route) },
                        onAlmacenClick = { navController.navigate(AdminScreen.Almacen.route) },
                        onAddClick = {
                            productoAEditar = null
                            navController.navigate(AdminScreen.AddProduct.route)
                        },
                        onDecreaseStock = { productoToUpdate ->
                            val newStock = (productoToUpdate.stock - 1).coerceAtLeast(0)
                            FirebaseFirestore.getInstance().collection("productos").document(productoToUpdate.id)
                                .update("stock", newStock)
                                .addOnSuccessListener {
                                    listaProductos = listaProductos.map {
                                        if (it.id == productoToUpdate.id) {
                                            it.copy(
                                                stock = newStock,
                                                statusLabel = if (newStock == 0) "AGOTADO" else if (newStock < 5) "Crítico" else "Óptimo",
                                                statusColor = if (newStock == 0) Color.Red else if (newStock < 5) Color(0xFFF44336) else Color(0xFF4CAF50)
                                            )
                                        } else it
                                    }
                                }
                        },
                        onEditClick = { productoQueQueremosEditar ->
                            productoAEditar = productoQueQueremosEditar
                            navController.navigate(AdminScreen.AddProduct.route)
                        },
                        // ✨ Conectamos el botón para abrir el Generador QR
                        onQrClick = { navController.navigate(AdminScreen.QrGenerator.route) }
                    )
                }
            }
            composable(AdminScreen.Almacen.route) { AlmacenStockScreen() }
            composable(AdminScreen.Pedidos.route) { PedidosScreen() }
            composable(AdminScreen.IAReport.route) { AIReportScreen(onBack = { navController.popBackStack() }) }
            composable(AdminScreen.AddProduct.route) {
                AddProductScreen(
                    productoAEditar = productoAEditar,
                    onBack = { navController.popBackStack() },
                    onSuccessSave = { navController.popBackStack() }
                )
            }
            composable(AdminScreen.QrGenerator.route) { QrGeneratorScreen(onBack = { navController.popBackStack() }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    listaProductos: List<Producto>,
    textBusqueda: String,
    onTextBusquedaChange: (String) -> Unit,
    onGestionarPedidosClick: () -> Unit,
    onAIPredictionsClick: () -> Unit,
    onAlmacenClick: () -> Unit,
    onAddClick: () -> Unit,
    onDecreaseStock: (Producto) -> Unit,
    onEditClick: (Producto) -> Unit,
    onQrClick: () -> Unit // ✨ Nuevo parámetro para el clic del QR
) {
    val context = LocalContext.current

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
        // ✨ Pasamos la acción al TopBar
        item { AdminTopBar(onQrClick = onQrClick) }

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
                text = "CATÁLOGO DE PRODUCTOS ACTIVOS",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (productosFiltrados.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No hay productos en inventario.", color = Color.Gray)
                }
            }
        } else {
            items(productosFiltrados) { producto ->
                ProductoCard(
                    producto = producto,
                    onInventarioClick = onAlmacenClick,
                    onDarDeBajaClick = { onDecreaseStock(producto) },
                    onEditClick = { onEditClick(producto) }
                )
            }
        }

        item { AlertaSuministrosCard(onClick = onAlmacenClick) }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(onQrClick: () -> Unit) { // ✨ Recibe el evento del QR
    val context = LocalContext.current
    TopAppBar(
        title = {
            Text(
                text = "Administración",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        actions = {
            // ✨ NUEVO BOTÓN GENERADOR DE QR
            IconButton(onClick = onQrClick) {
                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = "Generar QR Asistencia",
                    tint = Color(0xFF6200EE)
                )
            }

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
                if (producto.imagenUrl.isNotEmpty()) {
                    AsyncImage(
                        model = producto.imagenUrl,
                        contentDescription = producto.nombre,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F0))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(producto.nombre, fontWeight = FontWeight.Bold)
                        if (producto.isNuevo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("NUEVO", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(producto.categoria, color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stock: ${producto.stock} und.", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Text("${producto.calificacion}", style = MaterialTheme.typography.bodySmall)
                    }
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
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reducir Stock", color = Color.Gray)
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
fun AdminBottomBar(currentRoute: String, onScreenSelected: (String) -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = currentRoute == AdminScreen.Dashboard.route,
            onClick = { onScreenSelected(AdminScreen.Dashboard.route) },
            icon = { Icon(Icons.Default.GridView, contentDescription = null) },
            label = { Text("Dashboard") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6200EE),
                selectedTextColor = Color(0xFF6200EE),
                indicatorColor = Color(0xFFF3E5F5)
            )
        )
        NavigationBarItem(
            selected = currentRoute == AdminScreen.Almacen.route,
            onClick = { onScreenSelected(AdminScreen.Almacen.route) },
            icon = {
                BadgedBox(badge = { Badge { Text("9") } }) {
                    Icon(if (currentRoute == AdminScreen.Almacen.route) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2, contentDescription = null)
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
            selected = currentRoute == AdminScreen.Pedidos.route,
            onClick = { onScreenSelected(AdminScreen.Pedidos.route) },
            icon = { Icon(if (currentRoute == AdminScreen.Pedidos.route) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline, contentDescription = null) },
            label = { Text("Pedidos") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFF6200EE),
                selectedTextColor = Color(0xFF6200EE),
                indicatorColor = Color(0xFFF3E5F5)
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
fun DashboardAdminPreview() {
    val navController = androidx.navigation.compose.rememberNavController()
    DashboardAdminScreen(navController = navController)
}
