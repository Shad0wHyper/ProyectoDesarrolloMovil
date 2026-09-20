package com.developers.admin

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth

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
fun DashboardAdminScreen(navController: NavHostController, viewModel: AdminViewModel, onLogoutClick: () -> Unit) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AdminScreen.Dashboard.route
    val isDarkMode = isSystemInDarkTheme()

    // ✨ Rutas que muestran la barra inferior
    val bottomNavRoutes = remember { 
        listOf(
            AdminScreen.Dashboard.route, 
            AdminScreen.Almacen.route, 
            AdminScreen.Pedidos.route,
            AdminScreen.Asistencia.route
        ) 
    }
    val showBottomBar = bottomNavRoutes.contains(currentRoute)

    // ✨ Colores adaptativos para el fondo
    val bgColor = if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F9FA)

    // ✨ Variable para guardar el pan que queremos editar
    var productoAEditar by remember { mutableStateOf<Producto?>(null) }

    var listaProductos by remember { mutableStateOf<List<Producto>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var textBusqueda by remember { mutableStateOf("") }
    var showNotificationsSheet by remember { mutableStateOf(false) } // ✨ ESTADO DEL BOTTOM SHEET
    val context = LocalContext.current

    // ✨ 2. BADGES DINÁMICOS: ESTADO GLOBAL
    // Usamos el viewmodel
    val globalCriticosCount = viewModel.globalCriticosCount
    val insumosCriticosNombres = viewModel.insumosCriticosNombres

    LaunchedEffect(currentRoute) {
        val db = FirebaseFirestore.getInstance()

        if (currentRoute == AdminScreen.Dashboard.route) {
            isLoading = true
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

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            floatingActionButton = {
                if (currentRoute == AdminScreen.Dashboard.route) {
                    AdminFAB(
                        modifier = Modifier.padding(bottom = 80.dp), // ✨ Subimos el FAB para que no tape la barra
                        onAdd = {
                            productoAEditar = null
                            navController.navigate(AdminScreen.AddProduct.route)
                        }
                    )
                }
            },
            containerColor = bgColor,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = AdminScreen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(bgColor),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                composable(AdminScreen.Dashboard.route) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AdminPrimary)
                        }
                    } else {
                        DashboardContent(
                            viewModel = viewModel,
                            listaProductos = listaProductos,
                            insumosCriticosNombres = insumosCriticosNombres,
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
                            onQrClick = { navController.navigate(AdminScreen.QrGenerator.route) },
                            onHistoryClick = { navController.navigate(AdminScreen.AttendanceHistory.route) },
                            onProduccionClick = { navController.navigate(AdminScreen.Produccion.route) },
                            onNotificationClick = { showNotificationsSheet = true },
                            onProfileClick = { navController.navigate(AdminScreen.Profile.route) },
                            hasNotifications = globalCriticosCount > 0
                        )
                    }
                }
                composable(AdminScreen.Almacen.route) { AlmacenScreen() }
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
                composable(AdminScreen.Produccion.route) { ProduccionScreen(onBack = { navController.popBackStack() }) }
                composable(AdminScreen.AttendanceHistory.route) { AttendanceHistoryScreen(onBack = { navController.popBackStack() }) }
                composable(AdminScreen.Asistencia.route) { AsistenciaMainScreen() }
                composable(AdminScreen.Profile.route) { AdminProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onLogoutClick = onLogoutClick) }
            }
        }

        // ✨ HOJA DE NOTIFICACIONES
        if (showNotificationsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showNotificationsSheet = false },
                containerColor = bgColor
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Notificaciones",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (globalCriticosCount > 0) {
                            item {
                                NotificationItem(
                                    icon = Icons.Default.Warning,
                                    iconColor = Color.Red,
                                    text = "Stock Bajo: Tienes $globalCriticosCount materias primas por agotarse. Recomendación: Pedir a proveedores.",
                                    textColor = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                            item {
                                NotificationItem(
                                    icon = Icons.Default.Info,
                                    iconColor = Color(0xFF2196F3),
                                    text = "Pedidos Pendientes: Tienes $globalCriticosCount órdenes de WhatsApp por gestionar.",
                                    textColor = if (isDarkMode) Color.White else Color.Black
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = "Todo al día, no hay alertas.",
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // ✨ BARRA FLOTANTE ESTILO CLIENTE
        if (showBottomBar) {
            AdminFloatingBottomBar(
                currentRoute = currentRoute,
                globalCriticosCount = globalCriticosCount,
                onNavigate = { route ->
                    if (currentRoute != route) { // ✨ Solo navegamos si no estamos ya en esa ruta
                        navController.navigate(route) {
                            popUpTo(AdminScreen.Dashboard.route) { // ✨ Forzamos popUpTo al Dashboard
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                isDarkMode = isDarkMode,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardContent(
    viewModel: AdminViewModel,
    listaProductos: List<Producto>,
    insumosCriticosNombres: List<String>,
    textBusqueda: String,
    onTextBusquedaChange: (String) -> Unit,
    onGestionarPedidosClick: () -> Unit,
    onAIPredictionsClick: () -> Unit,
    onAlmacenClick: () -> Unit,
    onAddClick: () -> Unit,
    onDecreaseStock: (Producto) -> Unit,
    onEditClick: (Producto) -> Unit,
    onQrClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProduccionClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasNotifications: Boolean
) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

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
        item { 
            AdminTopBar(
                viewModel = viewModel,
                onQrClick = onQrClick, 
                onHistoryClick = onHistoryClick,
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick,
                hasNotifications = hasNotifications
            ) 
        }

        item {
            ResumenHoySection(
                insumosCriticosNombres = insumosCriticosNombres,
                onGestionarPedidosClick = onGestionarPedidosClick
            )
        }
        item { AIPredictionsSection(onClick = onAIPredictionsClick) }
        item { ProduccionDiariaCard(onClick = onProduccionClick) }
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

        if (insumosCriticosNombres.isNotEmpty()) {
            item { 
                AlertaSuministrosCard(
                    insumosCriticosNombres = insumosCriticosNombres,
                    onClick = onAlmacenClick
                ) 
            }
        }

        item { Spacer(modifier = Modifier.height(100.dp)) } // ✨ Espacio extra para que la barra flotante no tape nada
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminTopBar(
    viewModel: AdminViewModel,
    onQrClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    hasNotifications: Boolean
) { // ✨ Recibe el evento del QR, Historial, Notificaciones y Perfil
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val textColor = if (isDarkMode) Color.White else Color.Black
    val topBarColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF8F9FA)

    val photoUrl = viewModel.userImageUrl

    TopAppBar(
        modifier = Modifier.statusBarsPadding(), // ✨ Añadido para evitar empalme en el Dashboard
        title = {
            Text(
                text = "Administración",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
        },
        actions = {
            Box(modifier = Modifier.padding(8.dp).clickable(onClick = onNotificationClick)) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Alertas",
                    modifier = Modifier.size(28.dp),
                    tint = textColor
                )
                if (hasNotifications) {
                    Surface(
                        modifier = Modifier
                            .size(10.dp)
                            .align(Alignment.TopEnd),
                        color = Color.Red,
                        shape = CircleShape,
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF1E1E1E) else Color.White)
                    ) {}
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotEmpty()) {
                    coil.compose.AsyncImage(
                        model = photoUrl,
                        contentDescription = "Foto de perfil",
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isDarkMode) Color(0xFF333333) else Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(24.dp), tint = textColor)
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarColor)
    )
}

@Composable
fun AIPredictionsSection(onClick: () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF4527A0) else Color(0xFF673AB7)),
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
fun ProduccionDiariaCard(onClick: () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF283593) else Color(0xFF3F51B5)),
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
                    imageVector = Icons.Default.Factory,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Gestor de Producción ERP",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Registrar horneados y produccion del dia",
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
fun ResumenHoySection(insumosCriticosNombres: List<String>, onGestionarPedidosClick: () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
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
                trendColor = Color(0xFF4CAF50),
                isDarkMode = isDarkMode
            )
            
            // ✨ Tarjeta de Alertas Dinámica
            val insumosCriticosCount = insumosCriticosNombres.size
            val icon = if (insumosCriticosCount > 0) Icons.Default.ErrorOutline else Icons.Default.CheckCircle
            val color = if (insumosCriticosCount > 0) Color.Red else Color(0xFF4CAF50)
            val valueText = if (insumosCriticosCount > 0) "$insumosCriticosCount items" else "Óptimo"
            
            ResumenCard(
                modifier = Modifier.weight(1f),
                icon = icon,
                iconColor = color,
                label = "Estado de Almacén",
                value = valueText,
                isAlert = insumosCriticosCount > 0,
                isDarkMode = isDarkMode
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onGestionarPedidosClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isDarkMode) Color(0xFF1E3A5F) else Color(0xFFE3F2FD),
                contentColor = if (isDarkMode) Color(0xFFBBDEFB) else Color(0xFF1976D2)
            ),
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
    isAlert: Boolean = false,
    isDarkMode: Boolean
) {
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textColor)
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
    val isDarkMode = isSystemInDarkTheme()
    val textColor = if (isDarkMode) Color.White else Color.Black
    val fieldColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gestión de Productos", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
            Row {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color.LightGray else Color.Gray)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Filtrar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
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
                unfocusedContainerColor = fieldColor,
                focusedContainerColor = fieldColor,
                unfocusedTextColor = textColor,
                focusedTextColor = textColor
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
    val isDarkMode = isSystemInDarkTheme()
    val cardColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
    val textColor = if (isDarkMode) Color.White else Color.Black

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
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
                            .background(if (isDarkMode) Color(0xFF333333) else Color(0xFFF0F0F0))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isDarkMode) Color(0xFF333333) else Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(producto.nombre, fontWeight = FontWeight.Bold, color = textColor)
                        if (producto.isNuevo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text("NUEVO", fontSize = 9.sp, color = Color.Red, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(producto.categoria, color = Color.Gray, style = MaterialTheme.typography.bodySmall)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Stock: ${producto.stock} und.", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107), modifier = Modifier.size(12.dp))
                        Text("${producto.calificacion}", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Color.LightGray else Color.DarkGray)
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
                        Text("$ ${producto.precio}", fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), thickness = 0.5.dp, color = if (isDarkMode) Color.DarkGray else Color.LightGray)
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
fun AlertaSuministrosCard(insumosCriticosNombres: List<String>, onClick: () -> Unit) {
    val isDarkMode = isSystemInDarkTheme()
    val nombresTexto = if (insumosCriticosNombres.size == 1) {
        insumosCriticosNombres.first()
    } else {
        "${insumosCriticosNombres.dropLast(1).joinToString(", ")} y ${insumosCriticosNombres.last()}"
    }
    
    val verbo = if (insumosCriticosNombres.size == 1) "está" else "están"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alerta de Suministros", fontWeight = FontWeight.Bold, color = Color.Red)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "$nombresTexto $verbo en nivel crítico. Considere reabastecer hoy.",
                style = MaterialTheme.typography.bodyMedium,
                color = if (isDarkMode) Color.LightGray else Color.DarkGray
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
fun AdminFAB(modifier: Modifier = Modifier, onAdd: () -> Unit) {
    FloatingActionButton(
        onClick = onAdd,
        containerColor = Color(0xFFE91E63),
        contentColor = Color.White,
        shape = CircleShape,
        modifier = modifier
    ) {
        Icon(Icons.Default.Add, contentDescription = "Nuevo")
    }
}

@Composable
fun AdminFloatingBottomBar(
    currentRoute: String,
    globalCriticosCount: Int,
    onNavigate: (String) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = remember {
        listOf(
            Triple(AdminScreen.Dashboard.route, "Dashboard", Icons.Default.GridView),
            Triple(AdminScreen.Almacen.route, "Almacén", Icons.Default.Inventory2),
            Triple(AdminScreen.Pedidos.route, "Pedidos", Icons.Default.ChatBubble),
            Triple(AdminScreen.Asistencia.route, "Asistencia", Icons.Default.HowToReg)
        )
    }

    val containerBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White

    Box(
        modifier = modifier
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = containerBackgroundColor,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                bottomNavItems.forEach { (route, title, icon) ->
                    val isSelected = currentRoute == route
                    val badgeCount = if (route == AdminScreen.Almacen.route || route == AdminScreen.Pedidos.route) globalCriticosCount else 0

                    AdminFloatingNavItem(
                        title = title,
                        icon = icon,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        isDarkMode = isDarkMode,
                        onClick = { onNavigate(route) }
                    )
                }
            }
        }
    }
}

@Composable
fun AdminFloatingNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    badgeCount: Int,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val activeBackgroundColor = AdminPrimary.copy(alpha = 0.15f)
    val contentColor = if (isSelected) AdminPrimary else Color.Gray

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) activeBackgroundColor else Color.Transparent,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BadgedBox(
                badge = {
                    if (badgeCount > 0) {
                        Badge(containerColor = Color.Red, contentColor = Color.White) {
                            Text(badgeCount.toString())
                        }
                    }
                }
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            if (isSelected) {
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
            }
        }
    }
}

@Composable
fun NotificationItem(icon: androidx.compose.ui.graphics.vector.ImageVector, iconColor: Color, text: String, textColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodyMedium)
    }
}

// @Preview(showBackground = true)
// @Composable
// fun DashboardAdminPreview() {
//     val navController = androidx.navigation.compose.rememberNavController()
//     DashboardAdminScreen(navController = navController, viewModel = AdminViewModel(), onLogoutClick = {})
// }
