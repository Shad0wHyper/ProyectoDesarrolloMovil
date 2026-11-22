package com.developers.admin

import com.developers.core.ui.PanAppScreenTitle

import com.developers.core.ui.panAppFilteredItem

import androidx.compose.animation.AnimatedVisibility
import com.developers.core.ui.PanAppFloatingTab
import com.developers.core.ui.panAppScreenEnter
import com.developers.core.ui.panAppScreenExit
import com.developers.core.ui.panAppRevealIn
import com.developers.core.ui.panAppRevealOut

import com.developers.core.ui.PanAppGlassSheet
import com.developers.core.ui.PanAppGlassDropdown

import com.developers.core.ui.panAppGlassBorder
import com.developers.core.ui.PanAppGlassCard

import com.developers.core.ui.PanAppLogo
import com.developers.core.ui.PanAppRole
import com.developers.core.ui.panAppRoleBackdrop
import com.developers.core.ui.panAppNavigationColor
import com.developers.core.ui.PanAppProfileMenu
import com.developers.core.ui.PanAppAccountSettings

import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.tabSlideDirection
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor
import com.developers.core.ui.contrastErrorColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR
import com.developers.core.text.stockStatusLabel

import android.widget.Toast
import androidx.compose.ui.res.stringResource
import com.developers.admin.R
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalConfiguration
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
                        nombre = doc.getString("nombre") ?: localizedText(TextR.string.pan_no_name),
                        categoria = doc.getString("categoria") ?: "Otros",
                        stock = stockReal,
                        statusLabel = statusL,
                        statusColor = statusC,
                        precio = String.format("%.2f", doc.getDouble("precio") ?: 0.0),
                        imagenUrl = doc.getString("imagenUrl") ?: "",
                        isNuevo = doc.getBoolean("isNuevo") ?: false
                    )
                }
                isLoading = false
            }.addOnFailureListener {
                isLoading = false
                Toast.makeText(context, context.getString(R.string.admin_error_loading_inventory), Toast.LENGTH_SHORT).show()
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
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = AdminScreen.Dashboard.route,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(panAppRoleBackdrop(PanAppRole.Admin)),
                enterTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, 1)
                    panAppScreenEnter(direction)
                },
                exitTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, 1)
                    panAppScreenExit(direction)
                },
                popEnterTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, -1)
                    panAppScreenEnter(direction)
                },
                popExitTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, -1)
                    panAppScreenExit(direction)
                }
            ) {
                composable(AdminScreen.Dashboard.route) {
                    if (isLoading) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = contrastAccentColor(AdminPrimary))
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
                            onWarehouseStatusClick = { navController.navigate(AdminScreen.WarehouseStatus.route) { launchSingleTop = true } },
                            onSalesClick = { navController.navigate(AdminScreen.Sales.route) },
                            onWeeklySalesClick = { navController.navigate(AdminScreen.WeeklySales.route) { launchSingleTop = true } },
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
                            onProfileClick = { navController.navigate(AdminScreen.Profile.route) { launchSingleTop = true } },
                            onSettingsClick = { navController.navigate(AdminScreen.Settings.route) { launchSingleTop = true } },
                            onLogoutClick = onLogoutClick,
                            hasNotifications = globalCriticosCount > 0
                        )
                    }
                }
                composable(AdminScreen.Almacen.route) {
                    val previous = navController.previousBackStackEntry
                        ?.takeIf { it.destination.route == AdminScreen.WarehouseStatus.route }
                    AlmacenScreen(
                        warehouse = viewModel.warehouse,
                        onRetry = viewModel::retryWarehouse,
                        selectedMaterialId = previous?.savedStateHandle?.get<String>("warehouseMaterial"),
                        onBack = previous?.let { { navController.popBackStack(); Unit } },
                        onManageStock = { navController.navigate(AdminScreen.WarehouseStock.route) { launchSingleTop = true } },
                        profileMenu = {
                            PanAppProfileMenu(
                                onProfile = { navController.navigate(AdminScreen.Profile.route) { launchSingleTop = true } },
                                onSettings = { navController.navigate(AdminScreen.Settings.route) { launchSingleTop = true } },
                                onLogout = onLogoutClick,
                                avatar = viewModel.userImageUrl.takeIf { it.isNotBlank() }?.let { url ->
                                    { modifier -> AsyncImage(model = url, contentDescription = stringResource(TextR.string.pan_menu_profile), modifier = modifier, contentScale = ContentScale.Crop) }
                                }
                            )
                        }
                    )
                }
                composable(AdminScreen.WarehouseStock.route) {
                    AlmacenStockScreen(viewModel, onBack = { navController.popBackStack() })
                }
                composable(AdminScreen.WarehouseStatus.route) { entry ->
                    WarehouseStatusScreen(
                        state = viewModel.warehouse,
                        onRetry = viewModel::retryWarehouse,
                        onBack = { navController.popBackStack() },
                        onControl = { materialId ->
                            if (navController.currentBackStackEntry == entry) {
                                entry.savedStateHandle["warehouseMaterial"] = materialId
                                navController.navigate(AdminScreen.Almacen.route) { launchSingleTop = true }
                            }
                        }
                    )
                }
                composable(AdminScreen.Pedidos.route) { PedidosScreen() }
                composable(AdminScreen.Sales.route) { SalesAnalysisScreen(onBack = { navController.popBackStack() }) }
                composable(AdminScreen.WeeklySales.route) { WeeklySalesScreen(onBack = { navController.popBackStack() }) }
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
                composable(AdminScreen.Settings.route) {
                    PanAppAccountSettings(
                        role = PanAppRole.Admin,
                        onBack = { navController.popBackStack() },
                        onProfile = { navController.navigate(AdminScreen.Profile.route) { launchSingleTop = true } },
                        onLogout = onLogoutClick
                    )
                }
                composable(AdminScreen.Profile.route) { AdminProfileScreen(viewModel = viewModel, onBack = { navController.popBackStack() }, onLogoutClick = onLogoutClick) }
            }
        }

        // ✨ HOJA DE NOTIFICACIONES
        if (showNotificationsSheet) {
            PanAppGlassSheet(
                onDismissRequest = { showNotificationsSheet = false }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = localizedText(TextR.string.pan_notifications),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (globalCriticosCount > 0) {
                            item {
                                NotificationItem(
                                    icon = Icons.Default.Warning,
                                    iconColor = Color.Red,
                                    text = localizedText(TextR.string.pan_low_stock_alert, globalCriticosCount),
                                    textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                                )
                            }
                            item {
                                NotificationItem(
                                    icon = Icons.Default.Info,
                                    iconColor = Color(0xFF2196F3),
                                    text = localizedText(TextR.string.pan_pending_orders_alert, globalCriticosCount),
                                    textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                                )
                            }
                        } else {
                            item {
                                Text(
                                    text = localizedText(TextR.string.pan_no_alerts),
                                    color = contrastTextColor(Color.Gray, secondary = true),
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
        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = panAppRevealIn(),
            exit = panAppRevealOut()
        ) {
            AdminFloatingBottomBar(
                currentRoute = currentRoute,
                globalCriticosCount = globalCriticosCount,
                onNavigate = { route ->
                    val activeRoute = navController.currentDestination?.route
                    if (activeRoute != null && activeRoute != route) {
                        val openedFromWarehouseStatus = activeRoute == AdminScreen.Almacen.route &&
                            navController.previousBackStackEntry?.destination?.route == AdminScreen.WarehouseStatus.route
                        navController.navigate(route) {
                            popUpTo(AdminScreen.Dashboard.route) {
                                saveState = !openedFromWarehouseStatus
                            }
                            launchSingleTop = true
                            restoreState = route != AdminScreen.Dashboard.route
                        }
                    }
                },
                isDarkMode = isDarkMode,
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
    onSalesClick: () -> Unit,
    onWeeklySalesClick: () -> Unit,
    onWarehouseStatusClick: () -> Unit,
    onDecreaseStock: (Producto) -> Unit,
    onEditClick: (Producto) -> Unit,
    onQrClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onProduccionClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { 
            AdminTopBar(
                viewModel = viewModel,
                onQrClick = onQrClick, 
                onHistoryClick = onHistoryClick,
                onNotificationClick = onNotificationClick,
                onProfileClick = onProfileClick,
                onSettingsClick = onSettingsClick,
                onLogoutClick = onLogoutClick,
                hasNotifications = hasNotifications
            ) 
        }

        item {
            ResumenHoySection(
                warehouse = viewModel.warehouse,
                onGestionarPedidosClick = onGestionarPedidosClick,
                onSalesClick = onSalesClick,
                onWeeklySalesClick = onWeeklySalesClick,
                onWarehouseStatusClick = onWarehouseStatusClick
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
                text = localizedText(TextR.string.pan_catalog),
                style = MaterialTheme.typography.labelMedium,
                color = contrastTextColor(Color.Gray, secondary = true),
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        if (productosFiltrados.isEmpty()) {
            item(key = "emptyProductFilter") {
                Box(modifier = panAppFilteredItem().fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text(stringResource(R.string.admin_no_products), color = contrastTextColor(Color.Gray, secondary = true))
                }
            }
        } else {
            items(productosFiltrados, key = { it.id }) { producto ->
                ProductoCard(
                    producto = producto,
                    modifier = panAppFilteredItem(),
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
    onSettingsClick: () -> Unit,
    onLogoutClick: () -> Unit,
    hasNotifications: Boolean
) {
    val colors = MaterialTheme.colorScheme
    val photoUrl = viewModel.userImageUrl
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PanAppLogo(Modifier.size(40.dp))
        Column(Modifier.weight(1f)) {
            PanAppScreenTitle(
                localizedText(TextR.string.pan_administration),
            )
            if (viewModel.userName.isNotBlank()) {
                Text(
                    viewModel.userName,
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Surface(
            onClick = onNotificationClick,
            shape = CircleShape,
            color = colors.surface.copy(alpha = 0.72f),
            modifier = Modifier.size(44.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Notifications, stringResource(R.string.admin_notifications_desc))
                if (hasNotifications) {
                    Box(
                        Modifier.align(Alignment.TopEnd).padding(6.dp).size(8.dp)
                            .background(colors.error, CircleShape)
                    )
                }
            }
        }
        PanAppProfileMenu(
            onProfile = onProfileClick,
            onSettings = onSettingsClick,
            onLogout = onLogoutClick,
            avatar = if (photoUrl.isNotEmpty()) {
                { modifier ->
                    coil.compose.AsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(R.string.admin_profile_photo),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = modifier
                    )
                }
            } else null
        )
    }
}


@Composable
fun AIPredictionsSection(onClick: () -> Unit) {
    DashboardShortcut(
        localizedText(TextR.string.pan_predictions),
        localizedText(TextR.string.pan_predictions_description),
        Icons.Default.AutoAwesome, Color(0xFF6539AD), Color.White, onClick
    )
}

@Composable
fun ProduccionDiariaCard(onClick: () -> Unit) {
    DashboardShortcut(
        localizedText(TextR.string.pan_production_manager),
        localizedText(TextR.string.pan_production_description),
        Icons.Default.Factory, Color(0xFF414EA4), Color.White, onClick
    )
}

@Composable
private fun DashboardShortcut(
    title: String,
    subtitle: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    badge: Int = 0
) {
    Card(
        border = panAppGlassBorder(),
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = background, contentColor = foreground),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).background(foreground.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = foreground.copy(alpha = 0.84f))
            }
            if (badge > 0) Badge(containerColor = foreground, contentColor = background) { Text(badge.toString()) }
            Icon(Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
internal fun ResumenHoySection(
    warehouse: WarehouseLoad,
    onGestionarPedidosClick: () -> Unit,
    onSalesClick: () -> Unit,
    onWeeklySalesClick: () -> Unit,
    onWarehouseStatusClick: () -> Unit
) {
    val sales = rememberSales(0)
    val currentMonth = YearMonth.now(salesZone)
    var monthChoice by rememberSaveable { mutableStateOf<String?>(null) }
    var showMonths by remember { mutableStateOf(false) }
    val selectedMonth = monthChoice?.let(YearMonth::parse) ?: currentMonth
    val ready = sales as? SalesLoad.Ready
    val months = remember(ready, currentMonth) {
        (ready?.sales.orEmpty().map { YearMonth.from(it.date) } + currentMonth).distinct().sortedDescending()
    }
    val locale = LocalConfiguration.current.locales[0]
    val monthFormat = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.align(Alignment.End)) {
            Surface(
                onClick = { showMonths = true },
                color = colors.surface.copy(alpha = 0.76f),
                shape = CircleShape
            ) {
                Row(
                    Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.DateRange, null, Modifier.size(16.dp), tint = colors.primary)
                    Text(selectedMonth.format(monthFormat), style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.ArrowDropDown, null, Modifier.size(16.dp))
                }
            }
            PanAppGlassDropdown(expanded = showMonths, onDismissRequest = { showMonths = false }) {
                months.forEach { month ->
                    DropdownMenuItem(
                        text = { Text(month.format(monthFormat)) },
                        onClick = {
                            monthChoice = month.takeUnless { it == currentMonth }?.toString()
                            showMonths = false
                        },
                        trailingIcon = if (month == selectedMonth) {
                            { Icon(Icons.Default.Check, contentDescription = null) }
                        } else null
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SalesSummaryCard(sales, selectedMonth, Modifier.weight(1f).fillMaxHeight(), onSalesClick)
            WarehouseSummaryCard(warehouse, Modifier.weight(1f).fillMaxHeight(), onWarehouseStatusClick)
        }
        WeeklySalesCard(sales, onWeeklySalesClick)
        val dark = isSystemInDarkTheme()
        DashboardShortcut(
            stringResource(R.string.admin_manage_orders),
            stringResource(R.string.admin_send_whatsapp),
            Icons.AutoMirrored.Filled.Chat,
            if (dark) Color(0xFF21374E) else Color(0xFFDCECF9),
            if (dark) Color(0xFFC4E5FF) else Color(0xFF174972),
            onGestionarPedidosClick,
            badge = ready?.orders?.count { it.status == "PENDIENTE" } ?: 0
        )
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
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)

    PanAppGlassCard(
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = iconColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.Gray, secondary = true))
            Text(value, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), color = textColor)
            if (trendText != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = if (trendColor == Color.Black) contrastTextColor(trendColor) else trendColor, modifier = Modifier.size(14.dp))
                    Text(trendText, style = MaterialTheme.typography.labelSmall, color = if (trendColor == Color.Black) contrastTextColor(trendColor) else trendColor)
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
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
    val fieldColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.admin_prod_mgmt), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
            Row {
                OutlinedButton(
                    onClick = { /* TODO */ },
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = if (isDarkMode) Color.LightGray else Color.Gray)
                ) {
                    Icon( Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp), tint = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text( stringResource(R.string.admin_filter), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onAddClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminPrimary)
                ) {
                    Text( stringResource(R.string.admin_new_btn), color = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = textBusqueda,
            onValueChange = onTextBusquedaChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.admin_search_prod)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(24.dp),
            colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = fieldColor,
                focusedContainerColor = fieldColor,
                unfocusedTextColor = textColor,
                focusedTextColor = textColor
            ))
        )
    }
}

@Composable
fun ProductoCard(
    producto: Producto,
    modifier: Modifier = Modifier,
    onInventarioClick: () -> Unit,
    onDarDeBajaClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val isDarkMode = isSystemInDarkTheme()
    val textColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)

    PanAppGlassCard(
        modifier = modifier.fillMaxWidth()
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
                        Icon(Icons.Default.Restaurant, contentDescription = null, tint = contrastTextColor(Color.Gray, secondary = true))
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(producto.nombre, fontWeight = FontWeight.Bold, color = textColor)
                        if (producto.isNuevo) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(color = Color.Red.copy(alpha = 0.1f), shape = RoundedCornerShape(4.dp)) {
                                Text(stringResource(R.string.admin_new_badge), fontSize = 9.sp, color = contrastErrorColor(Color.Red), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
                            }
                        }
                    }
                    Text(producto.categoria, color = contrastTextColor(Color.Gray, secondary = true), style = MaterialTheme.typography.bodySmall)

                    Text(stringResource(R.string.admin_stock_label, producto.stock), style = MaterialTheme.typography.bodySmall, color = contrastTextColor(Color.DarkGray, secondary = true, darkBackground = isDarkMode))
                }
                Column(horizontalAlignment = Alignment.End) {
                    Surface(
                        color = producto.statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = stockStatusLabel(producto.statusLabel),
                            color = contrastTextColor(producto.statusColor),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.admin_price_format, producto.precio), fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onEditClick, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp), tint = contrastTextColor(Color.Gray, secondary = true))
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
                    Text(stringResource(R.string.admin_inventory_btn), color = Color(0xFF2196F3))
                }
                TextButton(onClick = onDarDeBajaClick) {
                    Icon(Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp), tint = contrastTextColor(Color.Gray, secondary = true))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.admin_reduce_stock_btn), color = contrastTextColor(Color.Gray, secondary = true))
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { /* TODO */ }) {
                    Icon(Icons.Default.MoreVert, contentDescription = null, tint = contrastTextColor(Color.Gray, secondary = true))
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
        localizedText(TextR.string.pan_list_and, insumosCriticosNombres.dropLast(1).joinToString(", "), insumosCriticosNombres.last())
    }
    
    val verbo = if (insumosCriticosNombres.size == 1) localizedText(TextR.string.pan_is) else localizedText(TextR.string.pan_are)

    Card(
        border = panAppGlassBorder(),
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFFB71C1C).copy(alpha = 0.2f) else Color(0xFFFFEBEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = contrastErrorColor(Color.Red), modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.admin_supply_alert), fontWeight = FontWeight.Bold, color = contrastErrorColor(Color.Red))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                localizedText(TextR.string.pan_critical_alert, nombresTexto, verbo),
                style = MaterialTheme.typography.bodyMedium,
                color = contrastTextColor(Color.DarkGray, secondary = true, darkBackground = isDarkMode)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                localizedText(TextR.string.pan_critical_inventory),
                color = contrastErrorColor(Color.Red),
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
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        shape = CircleShape,
        modifier = modifier
    ) {
        Icon( Icons.Default.Add, contentDescription = stringResource(R.string.admin_new_btn), tint = contrastTextColor(androidx.compose.material3.LocalContentColor.current))
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
    val bottomNavItems = listOf(
        Triple(AdminScreen.Dashboard.route, stringResource(TextR.string.pan_dashboard), Icons.Default.GridView),
        Triple(AdminScreen.Almacen.route, stringResource(TextR.string.pan_warehouse), Icons.Default.Inventory2),
        Triple(AdminScreen.Pedidos.route, stringResource(TextR.string.pan_orders), Icons.Default.ChatBubble),
        Triple(AdminScreen.Asistencia.route, stringResource(TextR.string.pan_attendance), Icons.Default.HowToReg)
    )
    val containerBackgroundColor = panAppNavigationColor()

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        val spacing = 4.dp
        val surfacePadding = 6.dp
        val available = (maxWidth - surfacePadding * 2 - spacing * (bottomNavItems.size - 1))
            .coerceAtLeast(0.dp)
        val itemWidth = minOf(52.dp, available / bottomNavItems.size)
        val selectedWidth = minOf(152.dp, available - itemWidth * (bottomNavItems.size - 1))

        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.background(containerBackgroundColor, CircleShape),
            border = panAppGlassBorder()
        ) {
            Row(
                modifier = Modifier.padding(surfacePadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                bottomNavItems.forEach { (route, title, icon) ->
                    val isSelected = currentRoute == route
                    val badgeCount = if (route == AdminScreen.Almacen.route || route == AdminScreen.Pedidos.route) {
                        globalCriticosCount
                    } else 0

                    PanAppFloatingTab(
                        title = title,
                        icon = icon,
                        isSelected = isSelected,
                        badgeCount = badgeCount,
                        itemWidth = itemWidth,
                        selectedWidth = selectedWidth,
                        onClick = { onNavigate(route) }
                    )
                }
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
