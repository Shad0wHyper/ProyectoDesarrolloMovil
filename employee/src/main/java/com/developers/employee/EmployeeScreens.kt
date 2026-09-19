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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.storage.FirebaseStorage

// Datos falsos eliminados

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(viewModel: EmployeeViewModel, onNavigate: (AppScreen) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Dashboard", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
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
                        opcionesEstado.forEach { estadoItem ->
                            DropdownMenuItem(
                                text = { Text(estadoItem, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedDropdown = false
                                    viewModel.actualizarEstadoPedido(pedido.path, estadoItem) // Guarda en Firestore
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
// ProveedoresScreen fue movido y reemplazado por EmpleadoAlmacenScreen
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerfilScreen(viewModel: EmployeeViewModel, onNavigate: (AppScreen) -> Unit, onLogoutClick: () -> Unit = {}) {
    val context = LocalContext.current
    var isUploadingImage by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isUploadingImage = true
                val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                    .child("perfiles/${viewModel.currentUserId}.jpg")

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            viewModel.updateProfileImage(downloadUrl.toString())
                            isUploadingImage = false
                            Toast.makeText(context, "Foto actualizada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        isUploadingImage = false
                        Toast.makeText(context, "Error al subir la foto", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("Mi Perfil", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    
                    // SECCIÓN FOTO DE PERFIL
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clickable {
                                if (!isUploadingImage && viewModel.currentUserId != "INVITADO") {
                                    photoPickerLauncher.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                } else if (viewModel.currentUserId == "INVITADO") {
                                    Toast.makeText(context, "Inicia sesión para subir una foto", Toast.LENGTH_SHORT).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        // Círculo principal de la foto
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFEEEEEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (viewModel.userImageUrl.isNotEmpty()) {
                                coil.compose.AsyncImage(
                                    model = viewModel.userImageUrl,
                                    contentDescription = "Foto de perfil",
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(60.dp), tint = Color.Gray)
                            }

                            if (isUploadingImage) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                                }
                            }
                        }

                        // Burbuja de la cámara
                        if (!isUploadingImage) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-2).dp, y = (-2).dp)
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Cambiar foto",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

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
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÓN CERRAR SESIÓN SEGURO
            OutlinedButton(
                onClick = {
                    viewModel.limpiarDatosDeSesion {
                        onLogoutClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
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
