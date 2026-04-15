package com.developers.employee

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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

// --- COLORES ---
val BgGray = Color(0xFFF5F6FA)
val PrimaryBlue = Color(0xFF6B72E2)
val TextGray = Color(0xFF7A869A)
val CardPink = Color(0xFFED5A85)
val OnlineGreen = Color(0xFF2ECA7F)
val OrangePrep = Color(0xFFE88A64)
val LightOrangeBg = Color(0xFFFDF3F0)
val WhatsappGreen = Color(0xFF25D366)
val LightWhatsappBg = Color(0xFFF7F0EA)
val LightPurpleBadge = Color(0xFFEBEBFC)
val DarkPurpleText = Color(0xFF5A5FCE)
val SupplierBg = Color(0xFFF8F9FA)

// --- MODELOS DE DATOS ---
data class LogData(val title: String, val subtitle: String, val status: String, val isEntry: Boolean, val id: String = UUID.randomUUID().toString())
data class OrderItem(val name: String, val quantity: Int, val isReady: Boolean)
data class OrderData(val id: String, val timeWaiting: String, val customerName: String, val address: String, val items: List<OrderItem>, val total: String, val isReady: Boolean)

// Modelos para Seguimiento de Proveedores
data class SupplierItem(val name: String, val quantity: String)
data class SupplierOrder(
    val id: String, val supplierName: String, val supplierDesc: String, val statusBadge: String,
    val orderDate: String, val items: List<SupplierItem>, val trackingStatus: String, val trackingDate: String
)

// --- CONTROLADOR DE NAVEGACIÓN ---
// Quitamos SEGUIMIENTO, volvemos a tener solo las necesarias
enum class AppScreen { INICIO, HISTORIAL, PEDIDOS, LAUNCHING_WS }

@Composable
fun MainAppNavigation() {
    var currentScreen by remember { mutableStateOf(AppScreen.INICIO) }
    var selectedOrder by remember { mutableStateOf<OrderData?>(null) }

    val logsList = remember {
        mutableStateListOf(
            LogData("Salida Registrada", "AYER • 18:10", "Completado", false),
            LogData("Entrada Registrada", "AYER • 09:15", "Retraso", true)
        )
    }

    when (currentScreen) {
        AppScreen.INICIO -> EmployeeDashboardScreen(
            logsList = logsList,
            onNavigateToHistory = { currentScreen = AppScreen.HISTORIAL },
            onNavigateToPedidos = { currentScreen = AppScreen.PEDIDOS }
        )
        AppScreen.HISTORIAL -> FullHistoryScreen(logs = logsList, onBackClick = { currentScreen = AppScreen.INICIO })
        AppScreen.PEDIDOS -> PedidosScreen(
            onBackClick = { currentScreen = AppScreen.INICIO },
            onSendWhatsapp = { order -> selectedOrder = order; currentScreen = AppScreen.LAUNCHING_WS }
        )
        AppScreen.LAUNCHING_WS -> selectedOrder?.let { order -> LaunchingWhatsappScreen(order = order, onBackClick = { currentScreen = AppScreen.PEDIDOS }) }
    }
}


// ============================================================================
// PANTALLA: PEDIDOS (AHORA INCLUYE EL SEGUIMIENTO)
// ============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(onBackClick: () -> Unit, onSendWhatsapp: (OrderData) -> Unit) {
    // --- ESTADOS PARA EL BOTTOM SHEET ---
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedSupplierOrder by remember { mutableStateOf<SupplierOrder?>(null) }

    // --- DATOS DE EJEMPLO ---
    val orders = listOf(
        OrderData("PED-2841", "12 min en espera", "Lucia Fernández", "Calle 45 #12-80, Apto 402", listOf(OrderItem("Croissant de Mantequilla", 4, true), OrderItem("Baguette Artesanal", 1, false), OrderItem("Café Latte Grande", 2, true)), "$24.500", false),
        OrderData("PED-2845", "8 min en espera", "Carlos Ruiz", "Av. Principal #45-12, Local 3", listOf(OrderItem("Pan de Chocolate", 3, true), OrderItem("Jugo de Naranja Natural", 1, true)), "$18.200", true)
    )

    val supplierOrders = listOf(
        SupplierOrder(
            id = "ORD-2023-001", supplierName = "Harinas del Sol", supplierDesc = "Suministros de Panadería Premium",
            statusBadge = "En Camino", orderDate = "12 Oct, 2023",
            items = listOf(SupplierItem("Harina 000", "10 sacos"), SupplierItem("Levadura Seca", "2 kg")),
            trackingStatus = "Pedido Confirmado", trackingDate = "12 Oct, 2023"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pedidos Activos", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") } },
                actions = {
                    Box(modifier = Modifier.padding(end = 16.dp).clip(RoundedCornerShape(12.dp)).background(LightPurpleBadge).padding(horizontal = 12.dp, vertical = 6.dp)) { Text("3 ACTIVOS", color = DarkPurpleText, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = BgGray
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {

            // --- SECCIÓN 1: FILTROS ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FAF7)), border = BorderStroke(1.dp, Color(0xFFB9E8D9))) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = OnlineGreen, modifier = Modifier.size(20.dp))
                            Text("1 Listos", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                    Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = LightOrangeBg), border = BorderStroke(1.dp, Color(0xFFF6D9CE))) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Info, null, tint = OrangePrep, modifier = Modifier.size(20.dp))
                            Text("2 En Prep.", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                }
            }

            // --- SECCIÓN 2: COLA DE DESPACHO (CLIENTES) ---
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Cola de Despacho", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Actualizar", color = PrimaryBlue, fontSize = 12.sp, modifier = Modifier.clickable { })
                }
            }
            items(orders) { order -> OrderCard(order, isLaunching = false, onSendClick = { onSendWhatsapp(order) }) }

            // --- SECCIÓN 3: SEGUIMIENTO (PROVEEDORES) ---
            item {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("PEDIDOS RECIENTES", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("Ver Historial", color = PrimaryBlue, fontSize = 12.sp, modifier = Modifier.clickable { })
                }
            }

            items(supplierOrders) { order ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable {
                        selectedSupplierOrder = order
                        showBottomSheet = true
                    },
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(LightOrangeBg), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Storefront, contentDescription = null, tint = OrangePrep) }
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(order.supplierName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text(order.id, color = TextGray, fontSize = 12.sp)
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LightPurpleBadge).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(order.statusBadge, color = DarkPurpleText, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                        }
                    }
                }
            }

            // --- SECCIÓN 4: FOOTER ---
            item {
                Row(modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(8.dp)).padding(16.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Outlined.Chat, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Al hacer clic en Enviar por WhatsApp, se abrirá la aplicación con los detalles del pedido.", fontSize = 10.sp, color = TextGray)
                }
            }

            item {
                OutlinedButton(onClick = onBackClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 32.dp), border = BorderStroke(1.dp, LightPurpleBadge)) {
                    Icon(Icons.Default.ArrowBack, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Panel Inicio", color = PrimaryBlue)
                }
            }
        }
    }

    // --- MODAL BOTTOM SHEET (Fuera del Scaffold para que superponga toda la pantalla) ---
    if (showBottomSheet && selectedSupplierOrder != null) {
        val order = selectedSupplierOrder!!
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Detalle de Pedido", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("ID: ${order.id}", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(LightPurpleBadge).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(order.statusBadge, color = DarkPurpleText, fontWeight = FontWeight.Bold, fontSize = 10.sp) }
                        IconButton(onClick = { showBottomSheet = false }) { Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = TextGray, modifier = Modifier.size(20.dp)) }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = SupplierBg), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFEBEBEB))) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(LightPurpleBadge), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.WbSunny, contentDescription = null, tint = OrangePrep) }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(order.supplierName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(order.supplierDesc, color = TextGray, fontSize = 12.sp)
                            Text("Contactar Proveedor", color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp).clickable { })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("ARTÍCULOS EN PEDIDO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))

                order.items.forEach { item ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFEBEBEB))) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(LightPurpleBadge), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp)) }
                                Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
                            }
                            Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(BgGray).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(item.quantity, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.DarkGray) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Text("ESTADO DE SEGUIMIENTO", color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.Top) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(OnlineGreen.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = OnlineGreen, modifier = Modifier.size(16.dp)) }
                        Box(modifier = Modifier.width(2.dp).height(30.dp).background(OnlineGreen.copy(alpha = 0.5f)))
                    }
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(order.trackingStatus, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(order.trackingDate, color = TextGray, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Color(0xFFEBEBEB))) { Text("Reportar Problema", color = Color.DarkGray, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    Button(onClick = { showBottomSheet = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Confirmar Recepción", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}


// ============================================================================
// COMPONENTES REUTILIZABLES DE NAVEGACIÓN Y DASHBOARD
// ============================================================================
@Composable
fun BottomNav(currentScreen: AppScreen, onHomeClick: () -> Unit, onPedidosClick: () -> Unit) {
    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(icon = { Icon(Icons.Outlined.Home, "Inicio") }, label = { Text("Inicio") }, selected = currentScreen == AppScreen.INICIO, onClick = onHomeClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.List, "Pedidos") }, label = { Text("Pedidos") }, selected = currentScreen == AppScreen.PEDIDOS, onClick = onPedidosClick, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchingWhatsappScreen(order: OrderData, onBackClick: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(1500)
        val itemsText = order.items.joinToString("\n") { "- ${it.quantity}x ${it.name}" }
        val message = "Hola ${order.customerName},\n¡Tu pedido ${order.id} está listo para ser enviado a ${order.address}!\n\nDetalles del pedido:\n$itemsText\n\nTotal a pagar: ${order.total}\n\nGracias por tu preferencia."
        try { val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}") }; context.startActivity(intent) } catch (e: Exception) { Toast.makeText(context, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show() }
    }
    Scaffold(topBar = { TopAppBar(title = { Text("Launching Whatsapp.....", fontWeight = FontWeight.Bold, fontSize = 18.sp) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Volver") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }, containerColor = BgGray) { paddingValues -> Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) { Card(modifier = Modifier.fillMaxWidth().height(200.dp), colors = CardDefaults.cardColors(containerColor = LightWhatsappBg), shape = RoundedCornerShape(16.dp)) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(WhatsappGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(50.dp)) } } }; OrderCard(order = order, isLaunching = true, onSendClick = {}) } }
}

@Composable
fun OrderCard(order: OrderData, isLaunching: Boolean, onSendClick: () -> Unit) { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) { Column(modifier = Modifier.padding(16.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Column { Text("ID: ${order.id}", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp); Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Icon(Icons.Default.AccessTime, null, tint = TextGray, modifier = Modifier.size(12.dp)); Text(" ${order.timeWaiting}", color = TextGray, fontSize = 10.sp) } }; if (order.isReady) { Text("LISTO PARA ENVIAR", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.DarkGray) } else { Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(OrangePrep).padding(horizontal = 10.dp, vertical = 4.dp)) { Text("PREPARANDO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp) } } }; HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = BgGray); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Person, null, tint = TextGray, modifier = Modifier.size(16.dp)); Text(" ${order.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp)) }; Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) { Icon(Icons.Outlined.LocationOn, null, tint = TextGray, modifier = Modifier.size(16.dp)); Text(" ${order.address}", color = TextGray, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp)) }; Text("DETALLE DEL PEDIDO", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextGray, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)); order.items.forEach { item -> Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(if (item.isReady) LightPurpleBadge else Color.White, RoundedCornerShape(8.dp)).border(1.dp, if (item.isReady) LightPurpleBadge else BgGray, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(if (item.isReady) Icons.Outlined.CheckCircle else Icons.Outlined.Info, null, tint = if (item.isReady) PrimaryBlue else TextGray, modifier = Modifier.size(16.dp)); Column(modifier = Modifier.padding(start = 8.dp)) { Text(item.name, fontWeight = FontWeight.Medium, fontSize = 12.sp); Text("Cantidad: ${item.quantity}", color = TextGray, fontSize = 10.sp) } }; Box(modifier = Modifier.border(1.dp, BgGray, RoundedCornerShape(12.dp)).padding(horizontal = 10.dp, vertical = 4.dp).background(Color.White, RoundedCornerShape(12.dp))) { Text(if (item.isReady) "LISTO" else "PRND.", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (item.isReady) Color.DarkGray else TextGray) } } }; Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("Total Pedido:", color = TextGray, fontSize = 12.sp); Text(order.total, fontWeight = FontWeight.Bold, fontSize = 16.sp) }; if (isLaunching) { OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, Color(0xFFE0E0E0)), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Outlined.Chat, null, modifier = Modifier.size(16.dp), tint = TextGray); Spacer(modifier = Modifier.width(8.dp)); Text("Enviando WhatsApp...", color = TextGray, fontWeight = FontWeight.Bold) } } else { Button(onClick = onSendClick, modifier = Modifier.fillMaxWidth(), enabled = order.isReady, colors = ButtonDefaults.buttonColors(containerColor = if (order.isReady) WhatsappGreen else BgGray, contentColor = if (order.isReady) Color.White else TextGray), shape = RoundedCornerShape(8.dp)) { Icon(Icons.Outlined.Send, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Enviar por WhatsApp", fontWeight = FontWeight.Bold) } } } } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(logsList: MutableList<LogData>, onNavigateToHistory: () -> Unit, onNavigateToPedidos: () -> Unit) {
    fun getCurrentTimeText(): String = "HOY • " + SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    Scaffold(topBar = { TopAppBar(title = { Text("Panel Empleado", fontWeight = FontWeight.Bold, fontSize = 20.sp) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }, bottomBar = { BottomNav(AppScreen.INICIO, { }, onNavigateToPedidos) }, containerColor = BgGray) { paddingValues -> Column(modifier = Modifier.fillMaxSize().padding(paddingValues).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(24.dp)) { ProfileCard(); DigitalIdCard(); AttendanceControl(onEntryClick = { logsList.add(0, LogData("Entrada Registrada", getCurrentTimeText(), "A tiempo", true)) }, onExitClick = { logsList.add(0, LogData("Salida Registrada", getCurrentTimeText(), "Completado", false)) }); RecentLogs(logs = logsList, onViewHistoryClick = onNavigateToHistory) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullHistoryScreen(logs: List<LogData>, onBackClick: () -> Unit) { Scaffold(topBar = { TopAppBar(title = { Text("Historial Completo", fontWeight = FontWeight.Bold, fontSize = 18.sp) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.Default.ArrowBack, "Volver") } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)) }, containerColor = BgGray) { paddingValues -> LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(logs, key = { it.id }) { log -> RenderLogItem(log) } } } }
@Composable
fun ProfileCard() { Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Box { Icon(Icons.Default.Person, null, modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.LightGray).padding(8.dp)); Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(OnlineGreen).border(2.dp, Color.White, CircleShape).align(Alignment.BottomEnd)) }; Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text("PANADERO SENIOR", color = PrimaryBlue, fontSize = 10.sp, fontWeight = FontWeight.Bold); Text("Sofía Martínez", fontWeight = FontWeight.Bold, fontSize = 16.sp) } } } }
@Composable
fun DigitalIdCard() { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("IDENTIFICACIÓN DIGITAL", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold); Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(16.dp)) { Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("Código QR de Acceso", fontWeight = FontWeight.Bold, fontSize = 16.sp); val dashPathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f), 0f); Box(modifier = Modifier.size(150.dp).padding(16.dp), contentAlignment = Alignment.Center) { Canvas(modifier = Modifier.fillMaxSize()) { drawRoundRect(color = PrimaryBlue.copy(alpha = 0.3f), style = Stroke(width = 4.dp.toPx(), pathEffect = dashPathEffect), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())) }; Icon(Icons.Default.QrCodeScanner, null, modifier = Modifier.size(80.dp), tint = Color.DarkGray) } } } } }
@Composable
fun AttendanceControl(onEntryClick: () -> Unit, onExitClick: () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("CONTROL DE ASISTENCIA", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) { Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), onClick = onEntryClick) { Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ArrowOutward, null, tint = TextGray); Text("ENTRADA", fontWeight = FontWeight.Bold, color = TextGray, fontSize = 14.sp) } }; Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = CardPink), onClick = onExitClick) { Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Default.ArrowDownward, null, tint = Color.White); Text("SALIDA", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp) } } } } }
@Composable
fun RecentLogs(logs: List<LogData>, onViewHistoryClick: () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Text("REGISTRO RECIENTE", color = TextGray, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Icon(Icons.Default.DateRange, null, tint = TextGray, modifier = Modifier.size(16.dp)) }; logs.take(3).forEach { log -> RenderLogItem(log) }; TextButton(onClick = onViewHistoryClick, modifier = Modifier.fillMaxWidth()) { Text("Ver historial completo", color = TextGray) } } }
@Composable
fun RenderLogItem(log: LogData) { val iconBg = if (log.isEntry) Color(0xFFE8EAF6) else Color(0xFFF3E8EC); val iconColor = if (log.isEntry) PrimaryBlue else CardPink; val icon = if (log.isEntry) Icons.Outlined.CheckCircle else Icons.Default.AccessTime; val badgeColor = if (log.status == "Retraso") Color(0xFFE53935) else BgGray; val badgeTextColor = if (log.status == "Retraso") Color.White else Color.DarkGray; Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) { Row(modifier = Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(iconBg), contentAlignment = Alignment.Center) { Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp)) }; Spacer(modifier = Modifier.width(12.dp)); Column(modifier = Modifier.weight(1f)) { Text(log.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp); Text(log.subtitle, color = TextGray, fontSize = 12.sp) }; Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(badgeColor).padding(horizontal = 12.dp, vertical = 4.dp)) { Text(log.status, color = badgeTextColor, fontSize = 10.sp, fontWeight = FontWeight.Bold) } } } }