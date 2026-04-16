package com.developers.employee

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MainAppNavigation()
        }
    }
}

// Colores base (se adaptan al tema a través de MaterialTheme)
val PrimaryBlue = Color(0xFF6B72E2)
val CardPink = Color(0xFFED5A85)
val OnlineGreen = Color(0xFF2ECA7F)
val OrangePrep = Color(0xFFE88A64)
val WhatsappGreen = Color(0xFF25D366)
val DarkPurpleText = Color(0xFF4A4E91)

val darkColors = darkColorScheme(
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    onBackground = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA)
)

val lightColors = lightColorScheme(
    background = Color(0xFFF5F6FA),
    surface = Color.White,
    onSurface = Color.DarkGray,
    onBackground = Color.Black,
    onSurfaceVariant = Color(0xFF7A869A)
)

// --- MODELOS DE DATOS GLOBALES ---
data class LogData(val titleRes: Int, val subtitle: String, val statusRes: Int, val isEntry: Boolean, val id: String = UUID.randomUUID().toString())
data class OrderItem(val name: String, val quantity: Int, val isReady: Boolean)
data class OrderData(val id: String, val timeWaiting: String, val customerName: String, val address: String, val items: List<OrderItem>, val total: String, val isReady: Boolean)
data class SupplierItem(val name: String, val quantity: String)
data class SupplierOrder(val id: String, val supplierName: String, val supplierDescRes: Int, val statusBadgeRes: Int, val orderDate: String, val items: List<SupplierItem>, val trackingStatus: String, val trackingDate: String)

val globalOrders = listOf(
    OrderData("PED-2841", "12 min", "Lucia Fernández", "Calle 45 #12-80", listOf(OrderItem("Croissant", 4, true), OrderItem("Baguette", 1, false)), "$24.500", false),
    OrderData("PED-2845", "8 min", "Carlos Ruiz", "Av. Principal #45-12", listOf(OrderItem("Pan de Chocolate", 3, true)), "$18.200", true)
)

val globalSupplierOrders = listOf(SupplierOrder("ORD-2023", "Harinas del Sol", R.string.premium_supplies, R.string.on_way, "12 Oct, 2023", listOf(SupplierItem("Harina 000", "10 sacos")), "Pedido Confirmado", "12 Oct, 2023"))

fun getCurrentTimeText(): String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

// --- NAVEGACIÓN PRINCIPAL ---
enum class AppScreen { INICIO, ASISTENCIA, PEDIDOS, PROVEEDORES, PERFIL, LAUNCHING_WS, QR_CHECK, HISTORIAL }

@Composable
fun MainAppNavigation() {
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var qrModeIsEntry by rememberSaveable { mutableStateOf(true) }

    val logsSaver = listSaver<MutableList<LogData>, Any>(
        save = { stateList -> stateList.map { listOf(it.titleRes, it.subtitle, it.statusRes, it.isEntry, it.id) } },
        restore = { savedList -> val list = mutableStateListOf<LogData>(); savedList.forEach { item -> val props = item as List<*>; list.add(LogData(props[0] as Int, props[1] as String, props[2] as Int, props[3] as Boolean, props[4] as String)) }; list }
    )
    val logsList = rememberSaveable(saver = logsSaver) { mutableStateListOf(LogData(R.string.check_out, "AYER • 18:10", R.string.ready, false), LogData(R.string.check_in, "AYER • 09:15", R.string.ready, true)) }

    BackHandler(enabled = currentScreen != AppScreen.INICIO) {
        currentScreen = when (currentScreen) {
            AppScreen.LAUNCHING_WS -> AppScreen.PEDIDOS
            AppScreen.QR_CHECK, AppScreen.HISTORIAL -> AppScreen.ASISTENCIA
            else -> AppScreen.INICIO
        }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColors else lightColors) {
        when (currentScreen) {
            AppScreen.INICIO -> DashboardScreen(onNavigate = { currentScreen = it })
            AppScreen.ASISTENCIA -> AsistenciaScreen(logsList, onNavigate = { currentScreen = it }, onOpenQr = { isEntry -> qrModeIsEntry = isEntry; currentScreen = AppScreen.QR_CHECK })
            AppScreen.PEDIDOS -> PedidosScreen(onNavigate = { currentScreen = it }, onSendWhatsapp = { order -> selectedOrderId = order.id; currentScreen = AppScreen.LAUNCHING_WS })
            AppScreen.PROVEEDORES -> ProveedoresScreen(onNavigate = { currentScreen = it })
            AppScreen.PERFIL -> PerfilScreen(
                isDark = isDarkTheme,
                onToggleDark = { isDarkTheme = !isDarkTheme },
                onNavigate = { currentScreen = it }
            )
            AppScreen.HISTORIAL -> FullHistoryScreen(logs = logsList, onBackClick = { currentScreen = AppScreen.ASISTENCIA })
            AppScreen.LAUNCHING_WS -> {
                val order = globalOrders.find { it.id == selectedOrderId }
                if (order != null) LaunchingWhatsappScreen(order = order, onBackClick = { currentScreen = AppScreen.PEDIDOS })
            }
            AppScreen.QR_CHECK -> {
                QrCheckScreen(
                    isEntry = qrModeIsEntry,
                    onConfirm = {
                        logsList.add(0, LogData(
                            if (qrModeIsEntry) R.string.check_in else R.string.check_out,
                            "HOY • " + getCurrentTimeText(),
                            R.string.ready,
                            qrModeIsEntry
                        ))
                        currentScreen = AppScreen.ASISTENCIA
                    },
                    onBackClick = { currentScreen = AppScreen.ASISTENCIA }
                )
            }
        }
    }
}

// ============================================================================
// PANTALLAS
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(onNavigate: (AppScreen) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.dashboard_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.INICIO, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(stringResource(R.string.welcome_user, "Marco"), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text(stringResource(R.string.active_shift), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
            Text(stringResource(R.string.quick_access), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.CheckCircle, text = stringResource(R.string.record_attendance), onClick = { onNavigate(AppScreen.ASISTENCIA) })
                DashboardButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Outlined.List, text = stringResource(R.string.dispatch_orders), onClick = { onNavigate(AppScreen.PEDIDOS) })
            }
        }
    }
}

@Composable
fun DashboardButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(logsList: List<LogData>, onNavigate: (AppScreen) -> Unit, onOpenQr: (Boolean) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.attendance_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.ASISTENCIA, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            DigitalIdCard()
            AttendanceControl(onEntryClick = { onOpenQr(true) }, onExitClick = { onOpenQr(false) })
            RecentLogs(logsList, onViewHistoryClick = { onNavigate(AppScreen.HISTORIAL) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(onNavigate: (AppScreen) -> Unit, onSendWhatsapp: (OrderData) -> Unit) {
    val context = LocalContext.current
    val queueRefreshedMsg = stringResource(R.string.queue_refreshed)
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.send_orders_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PEDIDOS, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.dispatch_queue), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.refresh), color = PrimaryBlue, fontSize = 12.sp, modifier = Modifier.clickable { Toast.makeText(context, queueRefreshedMsg, Toast.LENGTH_SHORT).show() })
                }
            }
            items(globalOrders) { order -> OrderCard(order, isLaunching = false, onSendClick = { onSendWhatsapp(order) }) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(onNavigate: (AppScreen) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSupplierOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.suppliers_title), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PROVEEDORES, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text(stringResource(R.string.shipments_in_transit), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
            items(globalSupplierOrders) { order ->
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedSupplierOrderId = order.id; showBottomSheet = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFDF3F0)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocalShipping, null, tint = OrangePrep) }; Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) { Text(order.supplierName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface); Text(order.id, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEBEBFC)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(stringResource(order.statusBadgeRes), color = DarkPurpleText, fontWeight = FontWeight.Bold, fontSize = 10.sp) } } }
                }
            }
        }
    }

    if (showBottomSheet && selectedSupplierOrderId != null) {
        val order = globalSupplierOrders.find { it.id == selectedSupplierOrderId }
        if (order != null) {
            val contactMsg = stringResource(R.string.opening_chat)
            val reportSentMsg = stringResource(R.string.report_sent)
            val receiptConfirmedMsg = stringResource(R.string.receipt_confirmed)

            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text(stringResource(R.string.order_details), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface); Text("ID: ${order.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; IconButton(onClick = { showBottomSheet = false }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Spacer(modifier = Modifier.height(24.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))) { Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEBEBFC)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WbSunny, null, tint = OrangePrep) }; Column(modifier = Modifier.padding(start = 12.dp)) { Text(order.supplierName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface); Text(stringResource(order.supplierDescRes), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp); Text(stringResource(R.string.contact_supplier), color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp).clickable { Toast.makeText(context, contactMsg, Toast.LENGTH_SHORT).show() }) } } }
                    Spacer(modifier = Modifier.height(24.dp)); Text(stringResource(R.string.items_in_order), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp))
                    order.items.forEach { item -> Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEBEBFC)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Inventory2, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp)) }; Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface) }; Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(item.quantity, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) } } } }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = { Toast.makeText(context, reportSentMsg, Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(stringResource(R.string.report), color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) }; Button(onClick = { showBottomSheet = false; Toast.makeText(context, receiptConfirmedMsg, Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text(stringResource(R.string.confirm), color = Color.White, fontSize = 12.sp) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(isDark: Boolean, onToggleDark: () -> Unit, onNavigate: (AppScreen) -> Unit) {
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.my_profile), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PERFIL, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFFFD1DC)), contentAlignment = Alignment.BottomCenter) { Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = Color.DarkGray.copy(alpha = 0.5f)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Marco", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(stringResource(R.string.senior_baker), color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(stringResource(R.string.account_settings), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            context.startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS))
                        }.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.language), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp))
                        }
                        Text(Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.background)

                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text(stringResource(R.string.dark_mode), color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp)) }; Switch(checked = isDark, onCheckedChange = { onToggleDark() })
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTES REUTILIZABLES
// ============================================================================
@Composable
fun BottomNav(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(icon = { Icon(Icons.Outlined.Home, null) }, label = { Text(stringResource(R.string.home_nav)) }, selected = currentScreen == AppScreen.INICIO, onClick = { onNavigate(AppScreen.INICIO) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.AccessTime, null) }, label = { Text(stringResource(R.string.attendance_nav)) }, selected = currentScreen == AppScreen.ASISTENCIA || currentScreen == AppScreen.QR_CHECK, onClick = { onNavigate(AppScreen.ASISTENCIA) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Outlined.List, null) }, label = { Text(stringResource(R.string.orders_nav)) }, selected = currentScreen == AppScreen.PEDIDOS || currentScreen == AppScreen.LAUNCHING_WS, onClick = { onNavigate(AppScreen.PEDIDOS) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.LocalShipping, null) }, label = { Text(stringResource(R.string.supplier_nav)) }, selected = currentScreen == AppScreen.PROVEEDORES, onClick = { onNavigate(AppScreen.PROVEEDORES) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.Person, null) }, label = { Text(stringResource(R.string.profile_nav)) }, selected = currentScreen == AppScreen.PERFIL, onClick = { onNavigate(AppScreen.PERFIL) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
    }
}

@Composable
fun QrCheckScreen(isEntry: Boolean, onConfirm: () -> Unit, onBackClick: () -> Unit) {
    val modeText = if (isEntry) stringResource(R.string.check_in) else stringResource(R.string.check_out)
    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
        Box(modifier = Modifier.fillMaxWidth().weight(0.35f).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp).padding(top = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = MaterialTheme.colorScheme.onSurface) }; Icon(Icons.Outlined.QrCodeScanner, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(12.dp)) }
            Column(modifier = Modifier.align(Alignment.Center).padding(top = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Box(modifier = Modifier.size(90.dp).border(1.dp, PrimaryBlue.copy(alpha = 0.5f), RoundedCornerShape(4.dp)).padding(4.dp), contentAlignment = Alignment.Center) { Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color(0xFFFFD1DC)), contentAlignment = Alignment.BottomCenter) { Icon(Icons.Default.Person, null, modifier = Modifier.size(70.dp), tint = Color.DarkGray.copy(alpha = 0.5f)) } }; Spacer(modifier = Modifier.height(12.dp)); Text("Marco", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface); Text(stringResource(R.string.dashboard_title), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp) }
        }
        Column(modifier = Modifier.fillMaxWidth().weight(0.65f).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) { Spacer(modifier = Modifier.height(16.dp)); Icon(imageVector = Icons.Default.QrCode2, null, modifier = Modifier.size(250.dp), tint = MaterialTheme.colorScheme.onSurface); Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth().height(56.dp), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background, contentColor = MaterialTheme.colorScheme.onSurface), shape = RoundedCornerShape(12.dp)) { Text(modeText, fontWeight = FontWeight.Medium, fontSize = 16.sp) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchingWhatsappScreen(order: OrderData, onBackClick: () -> Unit) {
    val context = LocalContext.current
    val whatsappErrorMsg = stringResource(R.string.whatsapp_error)

    LaunchedEffect(Unit) {
        delay(1500)
        val message = "Hola ${order.customerName},\n¡Tu pedido ${order.id} está listo!\nTotal: ${order.total}"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, whatsappErrorMsg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.launching_whatsapp), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(modifier = Modifier.fillMaxWidth().height(200.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F0EA))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(WhatsappGreen), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(50.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            OrderCard(order = order, isLaunching = true, onSendClick = {})
        }
    }
}

@Composable
fun OrderCard(order: OrderData, isLaunching: Boolean, onSendClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("ID: ${order.id}", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Text(" ${order.timeWaiting}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }
                if (order.isReady) {
                    Text(stringResource(R.string.ready_to_send), fontWeight = FontWeight.Bold, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface)
                } else {
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(OrangePrep).padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(stringResource(R.string.preparing), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.background)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(" ${order.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(" ${order.address}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }
            Text(stringResource(R.string.order_details), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            order.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(if (item.isReady) Color(0xFFEBEBFC) else MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp)).border(1.dp, if (item.isReady) Color(0xFFEBEBFC) else MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (item.isReady) Icons.Outlined.CheckCircle else Icons.Outlined.Info, null, tint = if (item.isReady) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(item.name, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text(stringResource(R.string.qty, item.quantity), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Box(modifier = Modifier.border(1.dp, MaterialTheme.colorScheme.background, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))) {
                        Text(if (item.isReady) stringResource(R.string.ready) else stringResource(R.string.prep), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (item.isReady) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.order_total), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(order.total, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            if (isLaunching) {
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.sending_whatsapp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(onClick = onSendClick, modifier = Modifier.fillMaxWidth(), enabled = order.isReady, colors = ButtonDefaults.buttonColors(containerColor = if (order.isReady) WhatsappGreen else MaterialTheme.colorScheme.background, contentColor = if (order.isReady) Color.White else MaterialTheme.colorScheme.onSurfaceVariant), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.send_via_whatsapp), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DigitalIdCard() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.digital_id), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.access_qr_code), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f)
                Box(modifier = Modifier.size(150.dp).padding(16.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(color = PrimaryBlue.copy(alpha = 0.3f), style = Stroke(width = 4.dp.toPx(), pathEffect = dashPathEffect), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()))
                    }
                    Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

@Composable
fun AttendanceControl(onEntryClick: () -> Unit, onExitClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.attendance_control), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), onClick = onEntryClick) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = Icons.Default.ArrowOutward, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.check_in), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                }
            }
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = CardPink), onClick = onExitClick) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.ArrowDownward, null, tint = Color.White)
                    Text(stringResource(R.string.check_out), fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun RecentLogs(logs: List<LogData>, onViewHistoryClick: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.recent_logs), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.DateRange, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
        logs.take(3).forEach { log -> RenderLogItem(log) }
        TextButton(onClick = onViewHistoryClick, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.view_full_history), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun RenderLogItem(log: LogData) {
    val iconBg = if (log.isEntry) Color(0xFFE8EAF6) else Color(0xFFF3E8EC)
    val iconColor = if (log.isEntry) PrimaryBlue else CardPink
    val icon = if (log.isEntry) Icons.Outlined.CheckCircle else Icons.Default.AccessTime

    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) {
                Icon(imageVector = icon, null, tint = iconColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(log.titleRes), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(log.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 12.dp, vertical = 4.dp)) {
                Text(stringResource(log.statusRes), color = MaterialTheme.colorScheme.onSurface, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHistoryScreen(logs: List<LogData>, onBackClick: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.full_history), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(logs, key = { it.id }) { log -> RenderLogItem(log) }
        }
    }
}
