package com.developers.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    var selectedOrder by remember { mutableStateOf<OrderData?>(null) }
    val isDarkMode = appViewModel.isDarkMode

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

                items(orderList) { order ->
                    OrderItemCard(
                        order = order, 
                        appViewModel = appViewModel,
                        onViewDetails = { selectedOrder = order },
                        onReorder = onNavigateToCart
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            if (selectedOrder != null) {
                AlertDialog(
                    onDismissRequest = { selectedOrder = null },
                    title = { Text("${appViewModel.getString("order_details")} #${selectedOrder?.id}") },
                    text = {
                        Column {
                            Text("${appViewModel.getString("date")}: ${selectedOrder?.date}")
                            Text("Total: $${selectedOrder?.total}")
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("${appViewModel.getString("items")}:", fontWeight = FontWeight.Bold)
                            Text("- ${selectedOrder?.mainItem}")
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { selectedOrder = null }) {
                            Text(appViewModel.getString("close"))
                        }
                    }
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
                        text = "ID #${order.id}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold
                    )
                    Text(order.date, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                
                Surface(
                    color = when(order.status) {
                        "Pendiente" -> Color(0xFFFFF4E5)
                        "En Camino" -> Color(0xFFE3F2FD)
                        else -> Color(0xFFE8F5E9)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = order.status,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = when(order.status) {
                            "Pendiente" -> Color(0xFFE65100)
                            "En Camino" -> Color(0xFF1565C0)
                            else -> Color(0xFF2E7D32)
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
                    Text("${order.itemCount} ${appViewModel.getString("items")}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
                Text(
                    "$${order.total}", 
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
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appViewModel.getString("buy_again"), fontSize = 12.sp)
                }
            }
        }
    }
}

data class OrderData(
    val id: String,
    val status: String,
    val date: String,
    val mainItem: String,
    val itemCount: Int,
    val total: String
)

val orderList = listOf(
    OrderData("BK-9120", "Pendiente", "Hoy, 14 Oct • 09:30 AM", "Pan de Masa Madre", 2, "12.50"),
    OrderData("BK-8955", "En Camino", "Ayer, 13 Oct • 04:15 PM", "Croissants Chocolate", 5, "35.20"),
    OrderData("BK-8842", "Entregado", "12 Oct 2023 • 11:00 AM", "Muffin Arándanos", 3, "24.50")
)
