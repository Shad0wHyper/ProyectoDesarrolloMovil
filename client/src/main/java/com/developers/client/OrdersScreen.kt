package com.developers.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.PanAppPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    var selectedOrder by remember { mutableStateOf<OrderData?>(null) }
    val isDarkMode = appViewModel.isDarkMode
    val orderList = appViewModel.ordersList

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appViewModel.getString("my_orders"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appViewModel.getString("close"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White,
                    titleContentColor = if (isDarkMode) Color.White else Color.Black,
                    navigationIconContentColor = if (isDarkMode) Color.White else Color.Black
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8))
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        appViewModel.getString("history"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                if (orderList.isEmpty() && appViewModel.currentUserId != "INVITADO") {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("Aún no tienes pedidos registrados.", color = Color.Gray)
                        }
                    }
                } else {
                    items(orderList, key = { it.id }) { order ->
                        OrderItemCard(
                            order = order,
                            appViewModel = appViewModel,
                            onViewDetails = { selectedOrder = order },
                            onReorder = onNavigateToCart
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // ✨ POPUP DE DETALLES DEL PEDIDO
            if (selectedOrder != null) {
                val statusKey = when(selectedOrder?.status?.uppercase()) {
                    "ENVIADO" -> "en_camino"
                    "ENTREGADO" -> "entregado"
                    else -> "pendiente"
                }
                AlertDialog(
                    onDismissRequest = { selectedOrder = null },
                    title = { Text("${appViewModel.getString("order_details")} #${selectedOrder?.id?.take(8)?.uppercase()}") },
                    text = {
                        Column {
                            Text("${appViewModel.getString("date")}: ${selectedOrder?.date}")
                            Text("Envío a: ${selectedOrder?.direccionEnvio}", fontSize = 12.sp, color = Color.Gray)
                            Text("Total: ${selectedOrder?.total}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Status: ${appViewModel.getString(statusKey)}", fontWeight = FontWeight.Bold, color = PanAppPrimary)

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(8.dp))

                            Text("${appViewModel.getString("items")}:", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            // ✨ AQUÍ MOSTRAMOS LA LISTA COMPLETA DE ARTÍCULOS CON SCROLL SI SON MUCHOS
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 150.dp) // Solo escrolea si compran muchísimo pan
                                    .verticalScroll(rememberScrollState())
                            ) {
                                selectedOrder?.itemsList?.forEach { item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("- ${item.cantidad}x ${item.nombre}", fontSize = 14.sp)
                                        Text(String.format("$%.2f", item.precio * item.cantidad), fontSize = 14.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedOrder = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                        ) {
                            Text(appViewModel.getString("close"), color = Color.White)
                        }
                    },
                    containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(
    order: OrderData,
    appViewModel: AppViewModel,
    onViewDetails: () -> Unit,
    onReorder: () -> Unit
) {
    val isDarkMode = appViewModel.isDarkMode
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ID ${order.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(order.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }

                val statusUpper = order.status.uppercase()
                val statusKey = when(statusUpper) {
                    "ENVIADO" -> "en_camino"
                    "ENTREGADO" -> "entregado"
                    else -> "pendiente"
                }

                Surface(
                    color = when(statusUpper) {
                        "ENVIADO" -> Color(0xFFE3F2FD)
                        "ENTREGADO" -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFF4E5)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = appViewModel.getString(statusKey),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = when(statusUpper) {
                            "ENVIADO" -> Color(0xFF1565C0)
                            "ENTREGADO" -> Color(0xFF2E7D32)
                            else -> Color(0xFFE65100)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = if (isDarkMode) Color.DarkGray else Color(0xFFF0F0F0)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF3F3FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = PanAppPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.mainItem,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    // En la tarjeta principal seguimos mostrando un resumen rápido
                    Text("${order.itemCount} ${appViewModel.getString("items")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    order.total,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isDarkMode) Color.White else Color.Black
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = onViewDetails,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color(0xFFF0F0F0))
                ) {
                    Text(appViewModel.getString("view_details"), color = if (isDarkMode) Color.White else Color.Black)
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = if (isDarkMode) Color.White else Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onReorder,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary),
                    enabled = order.status.uppercase() == "ENTREGADO"                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appViewModel.getString("buy_again"), fontSize = 12.sp)
                }
            }
        }
    }
}