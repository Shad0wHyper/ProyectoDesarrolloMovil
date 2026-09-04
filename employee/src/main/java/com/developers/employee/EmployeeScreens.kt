package com.developers.employee

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Datos falsos solo para proveedores, los pedidos ahora son 100% reales
data class SupplierItem(val name: String, val quantity: String)
data class SupplierOrder(val id: String, val supplierName: String, val statusBadgeRes: String, val orderDate: String, val items: List<SupplierItem>, val trackingStatus: String, val trackingDate: String)

val globalSupplierOrders = listOf(SupplierOrder("ORD-2023", "Harinas del Sol", "En Camino", "12 Oct, 2023", listOf(SupplierItem("Harina 000", "10 sacos")), "Pedido Confirmado", "12 Oct, 2023"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: EmployeeViewModel, onNavigate: (AppScreen) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.INICIO, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PrimaryBlue), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("¡Hola, ${viewModel.userName}!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Text("Turno Activo", color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
                }
            }
            Text("Accesos Rápidos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                DashboardButton(modifier = Modifier.weight(1f), icon = Icons.Outlined.CheckCircle, text = "Asistencia", onClick = { onNavigate(AppScreen.ASISTENCIA) })
                DashboardButton(modifier = Modifier.weight(1f), icon = Icons.AutoMirrored.Outlined.List, text = "Despachar", onClick = { onNavigate(AppScreen.PEDIDOS) })
            }
        }
    }
}

// ✨ PANTALLA DE PEDIDOS CONECTADA A FIREBASE
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PedidosScreen(viewModel: EmployeeViewModel, onNavigate: (AppScreen) -> Unit, onSendWhatsapp: (PedidoFirebase) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Cola de Despacho", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PEDIDOS, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (viewModel.pedidosActivos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No hay pedidos activos.", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                items(viewModel.pedidosActivos, key = { it.id }) { order ->
                    OrderCard(pedido = order, viewModel = viewModel, isLaunching = false, onSendClick = { onSendWhatsapp(order) })
                }
                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }
    }
}

// ✨ TARJETA DE PEDIDO DINÁMICA CON DROPDOWN Y BLOQUEO DE SEGURIDAD
@Composable
fun OrderCard(pedido: PedidoFirebase, viewModel: EmployeeViewModel, isLaunching: Boolean, onSendClick: () -> Unit) {
    val isEntregado = pedido.estado == "ENTREGADO"
    var expandedDropdown by remember { mutableStateOf(false) }
    val opcionesEstado = listOf("PENDIENTE", "ENVIADO", "ENTREGADO")

    // Colores dinámicos solicitados
    val statusColor = when (pedido.estado) {
        "PENDIENTE" -> CardPink // Rojo/Rosa
        "ENVIADO" -> Color(0xFFFFA000) // Amarillo/Naranja
        "ENTREGADO" -> Color(0xFF4CAF50) // Verde
        else -> Color.Gray
    }

    val dateFormateada = if (pedido.fecha > 0) SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(pedido.fecha)) else "Reciente"

    Card(
        modifier = Modifier.fillMaxWidth().border(if (isEntregado) 2.dp else 0.dp, if (isEntregado) statusColor else Color.Transparent, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = if (isEntregado) statusColor.copy(alpha = 0.05f) else MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("ID: ${pedido.id.take(8).uppercase()}", color = PrimaryBlue, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.AccessTime, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(12.dp))
                        Text(" $dateFormateada", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                    }
                }

                // ✨ SELECTOR DE ESTADO (DROPDOWN MENU)
                Box {
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = !isEntregado) { expandedDropdown = true } // Se bloquea si ya se entregó
                            .border(1.dp, statusColor, RoundedCornerShape(12.dp)),
                        color = if (isEntregado) statusColor.copy(alpha = 0.2f) else statusColor
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(if (isEntregado) "CERRADO - ENTREGADO" else pedido.estado, color = if (isEntregado) statusColor else Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp)
                            if (!isEntregado) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp).padding(start = 4.dp))
                            }
                        }
                    }

                    DropdownMenu(expanded = expandedDropdown, onDismissRequest = { expandedDropdown = false }, modifier = Modifier.background(MaterialTheme.colorScheme.surface)) {
                        val context = LocalContext.current
                        opcionesEstado.forEach { estadoItem ->
                            DropdownMenuItem(
                                text = { Text(estadoItem, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedDropdown = false
                                    viewModel.actualizarEstadoPedido(context, pedido, estadoItem) // Guarda en la nube y dispara notificación
                                }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.background)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Person, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(" ${pedido.clienteNombre}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 4.dp), color = MaterialTheme.colorScheme.onSurface)
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                Icon(Icons.Outlined.LocationOn, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Text(" ${pedido.direccion}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
            }

            Text("Detalles del Pedido", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
            pedido.items.forEach { item ->
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp)).padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(item.nombre, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("Cantidad: ${item.cantidad}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                    }
                    Text(String.format("$%.2f", item.precio), fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total a Cobrar", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                Text(String.format("$%.2f", pedido.total), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = statusColor)
            }

            if (isLaunching) {
                OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) { Icon(Icons.AutoMirrored.Outlined.Chat, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(modifier = Modifier.width(8.dp)); Text("Iniciando WhatsApp...", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold) }
            } else if (!isEntregado) {
                Button(onClick = onSendClick, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = WhatsappGreen, contentColor = Color.White), shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.AutoMirrored.Outlined.Send, null, modifier = Modifier.size(16.dp)); Spacer(modifier = Modifier.width(8.dp)); Text("Avisar por WhatsApp", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaunchingWhatsappScreen(order: PedidoFirebase, onBackClick: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        delay(1500)
        val mensajeEstado = if (order.estado == "ENVIADO") "¡Tu pedido está en camino a tu domicilio!" else "¡Tu pedido está siendo preparado y pronto saldrá!"
        val message = "Hola ${order.clienteNombre},\n$mensajeEstado\nTotal a pagar: $${order.total}\nAtte: Panadería"
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply { data = Uri.parse("https://api.whatsapp.com/send?text=${URLEncoder.encode(message, "UTF-8")}") }
            context.startActivity(intent)
        } catch (e: Exception) { Toast.makeText(context, "Error abriendo WhatsApp", Toast.LENGTH_SHORT).show() }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("Abriendo WhatsApp...", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface) }, navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Card(modifier = Modifier.fillMaxWidth().height(200.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F0EA))) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(WhatsappGreen), contentAlignment = Alignment.Center) { Icon(Icons.Default.Call, null, tint = Color.White, modifier = Modifier.size(50.dp)) } }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Pasamos un viewModel nulo o creamos un estado estático, solo para mostrarlo
            OrderCard(pedido = order, viewModel = viewModel(), isLaunching = true, onSendClick = {})
        }
    }
}

// ==========================================
// EL RESTO DEL CÓDIGO (Proveedores, Perfil, etc) QUEDA INTACTO
// ==========================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProveedoresScreen(onNavigate: (AppScreen) -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var showBottomSheet by rememberSaveable { mutableStateOf(false) }
    var selectedSupplierOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = { TopAppBar(title = { Text("Proveedores", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PROVEEDORES, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("Envíos en Tránsito", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface) }
            items(globalSupplierOrders) { order ->
                Card(modifier = Modifier.fillMaxWidth().clickable { selectedSupplierOrderId = order.id; showBottomSheet = true }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFDF3F0)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.LocalShipping, null, tint = OrangePrep) }; Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) { Text(order.supplierName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface); Text(order.id, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFEBEBFC)).padding(horizontal = 10.dp, vertical = 4.dp)) { Text(order.statusBadgeRes, color = DarkPurpleText, fontWeight = FontWeight.Bold, fontSize = 10.sp) } } }
                }
            }
        }
    }

    if (showBottomSheet && selectedSupplierOrderId != null) {
        val order = globalSupplierOrders.find { it.id == selectedSupplierOrderId }
        if (order != null) {
            ModalBottomSheet(onDismissRequest = { showBottomSheet = false }, sheetState = sheetState, containerColor = MaterialTheme.colorScheme.surface) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp).padding(bottom = 32.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column { Text("Detalles del Pedido", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface); Text("ID: ${order.id}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) }; IconButton(onClick = { showBottomSheet = false }) { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) } }
                    Spacer(modifier = Modifier.height(24.dp)); Text("Items del Pedido", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.Bold); Spacer(modifier = Modifier.height(8.dp))
                    order.items.forEach { item -> Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))) { Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEBEBFC)), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Inventory2, null, tint = PrimaryBlue, modifier = Modifier.size(16.dp)) }; Text(item.name, fontWeight = FontWeight.Medium, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp), color = MaterialTheme.colorScheme.onSurface) }; Box(modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.background).padding(horizontal = 8.dp, vertical = 4.dp)) { Text(item.quantity, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface) } } } }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) { OutlinedButton(onClick = { Toast.makeText(context, "Reporte Enviado", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text("Reportar", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) }; Button(onClick = { showBottomSheet = false; Toast.makeText(context, "Recepción confirmada", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) { Text("Confirmar", color = Color.White, fontSize = 12.sp) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(viewModel: EmployeeViewModel, isDark: Boolean, onToggleDark: () -> Unit, onNavigate: (AppScreen) -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = { TopAppBar(title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        bottomBar = { BottomNav(AppScreen.PERFIL, onNavigate) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(100.dp).clip(CircleShape).background(Color(0xFFFFD1DC)), contentAlignment = Alignment.BottomCenter) { Icon(Icons.Default.Person, null, modifier = Modifier.size(80.dp), tint = Color.DarkGray.copy(alpha = 0.5f)) }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(viewModel.userName, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(viewModel.userEmail, color = PrimaryBlue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
            Text("Ajustes de Cuenta", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().clickable { context.startActivity(Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS)) }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Language, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Idioma", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp)) }
                        Text(Locale.getDefault().displayLanguage.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.background)
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.DarkMode, null, tint = MaterialTheme.colorScheme.onSurfaceVariant); Text("Modo Oscuro", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(start = 8.dp)) }; Switch(checked = isDark, onCheckedChange = { onToggleDark() })
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardButton(modifier: Modifier, icon: ImageVector, text: String, onClick: () -> Unit) {
    Card(modifier = modifier.clickable { onClick() }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = PrimaryBlue, modifier = Modifier.size(32.dp))
            Text(text, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(top = 8.dp), textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun BottomNav(currentScreen: AppScreen, onNavigate: (AppScreen) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Inicio") }, selected = currentScreen == AppScreen.INICIO, onClick = { onNavigate(AppScreen.INICIO) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.AccessTime, null) }, label = { Text("Asistencia") }, selected = currentScreen == AppScreen.ASISTENCIA || currentScreen == AppScreen.HISTORIAL, onClick = { onNavigate(AppScreen.ASISTENCIA) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.AutoMirrored.Outlined.List, null) }, label = { Text("Pedidos") }, selected = currentScreen == AppScreen.PEDIDOS || currentScreen == AppScreen.LAUNCHING_WS, onClick = { onNavigate(AppScreen.PEDIDOS) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.LocalShipping, null) }, label = { Text("Proveedores") }, selected = currentScreen == AppScreen.PROVEEDORES, onClick = { onNavigate(AppScreen.PROVEEDORES) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
        NavigationBarItem(icon = { Icon(Icons.Outlined.Person, null) }, label = { Text("Perfil") }, selected = currentScreen == AppScreen.PERFIL, onClick = { onNavigate(AppScreen.PERFIL) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = PrimaryBlue, selectedTextColor = PrimaryBlue))
    }
}