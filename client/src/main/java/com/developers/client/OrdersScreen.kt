package com.developers.client

import com.developers.core.ui.PanAppScreenTitle

import com.developers.core.ui.panAppFilteredItem

import com.developers.core.ui.PanAppGlassFilterChip

import com.developers.core.ui.PanAppGlassCard

import com.developers.core.ui.isPanAppInDarkTheme
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR
import com.developers.core.text.paymentMethodLabel

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.res.stringResource
import java.text.Normalizer
import java.util.Locale
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

internal enum class OrderFilter(val status: String?, val label: Int?) {
    All(null, R.string.client_order_filter_all),
    AwaitingPayment("ESPERANDO PAGO", null),
    Pending("PENDIENTE", R.string.client_pendiente),
    OnTheWay("ENVIADO", R.string.client_en_camino),
    Delivered("ENTREGADO", R.string.client_entregado)
}

private val ORDER_DIACRITICS_REGEX = Regex("\\p{Mn}+")
private val ORDER_SPACE_REGEX = Regex("\\s+")

internal fun orderStatus(status: String): OrderFilter? {
    val decomposed = Normalizer.normalize(status.trim(), Normalizer.Form.NFD)
    val token = ORDER_DIACRITICS_REGEX.replace(decomposed, "")
        .replace(ORDER_SPACE_REGEX, " ")
        .uppercase(Locale.ROOT)
    return when (token) {
        "ENTREGADO", "ENTREGADOS", "DELIVERED", "COMPLETADO", "COMPLETADOS", "COMPLETED",
        "CERRADO", "CERRADO - ENTREGADO", "ENTREGUE", "ENTREGUES" -> OrderFilter.Delivered

        "ENVIADO", "ENVIADOS", "EN CAMINO", "EN_CAMINO", "ON THE WAY", "ON_THE_WAY",
        "SHIPPED", "EN TRANSITO", "IN TRANSIT", "A CAMINHO" -> OrderFilter.OnTheWay

        "PENDIENTE", "PENDIENTES", "PENDING", "ESPERANDO PAGO", "ESPERANDO_PAGO",
        "AWAITING PAYMENT", "PAGO PENDIENTE", "PROCESADO", "PROCESADOS", "PROCESANDO",
        "PROCESSED", "EN PROCESO", "EN PREPARACION", "PREPARANDO", "" -> OrderFilter.Pending

        else -> null
    }
}

@Composable
private fun orderStatusLabel(status: String): String {
    val knownStatus = orderStatus(status)
    return knownStatus?.label?.let { stringResource(it) }
        ?: status.trim().ifEmpty { stringResource(R.string.client_pendiente) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCart: () -> Unit
) {
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var ratingLine by rememberSaveable(selectedOrderId) { mutableStateOf<Int?>(null) }
    val isDarkMode = isPanAppInDarkTheme()
    var savedFilter by rememberSaveable { mutableStateOf(OrderFilter.All) }
    // A previous version could save the awaiting-payment filter.
    val filter = savedFilter.takeIf { it.label != null } ?: OrderFilter.All
    val orderList = appViewModel.ordersList
    val selectedOrder = orderList.firstOrNull { it.id == selectedOrderId }
    val filteredOrders = orderList.filter { filter == OrderFilter.All || orderStatus(it.status) == filter }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { PanAppScreenTitle(appViewModel.getString("my_orders")) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appViewModel.getString("close"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    navigationIconContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                ),
                windowInsets = WindowInsets.statusBars
            )
        }
    ) { padding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = 760.dp)
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        appViewModel.getString("history"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OrderFilter.entries.forEach { option ->
                            val label = option.label ?: return@forEach
                            PanAppGlassFilterChip(
                                selected = filter == option,
                                onClick = { savedFilter = option },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (filteredOrders.isEmpty()) {
                    item(key = "emptyOrderFilter") {
                        Box(modifier = panAppFilteredItem().fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text(
                                if (orderList.isEmpty()) localizedText(TextR.string.pan_no_orders)
                                else stringResource(R.string.client_order_filter_empty),
                                color = contrastTextColor(Color.Gray, secondary = true)
                            )
                        }
                    }
                } else {
                    items(filteredOrders, key = { it.id }) { order ->
                        OrderItemCard(
                            order = order,
                            modifier = panAppFilteredItem(),
                            appViewModel = appViewModel,
                            onViewDetails = { selectedOrderId = order.id },
                            onReorder = onNavigateToCart
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }

            // ✨ POPUP DE DETALLES DEL PEDIDO
            val canRate = selectedOrder != null && orderStatus(selectedOrder.status) == OrderFilter.Delivered &&
                appViewModel.currentUserId != "INVITADO" && selectedOrder.userId == appViewModel.currentUserId
            val selectedRatingItem = ratingLine?.let { selectedOrder?.itemsList?.getOrNull(it) }
            if (canRate && selectedRatingItem != null && selectedOrder != null) {
                AlertDialog(
                    onDismissRequest = { ratingLine = null },
                    title = { Text(selectedRatingItem.nombre) },
                    text = {
                        Column(Modifier.verticalScroll(rememberScrollState())) {
                            ProductRating(selectedRatingItem.productoId, selectedOrder.id, checkNotNull(ratingLine)) {
                                appViewModel.ratingRevision++
                            }
                        }
                    },
                    confirmButton = { TextButton(onClick = { ratingLine = null }) { Text(appViewModel.getString("close")) } }
                )
            } else if (selectedOrder != null) {
                val statusLabel = orderStatusLabel(selectedOrder?.status.orEmpty())
                AlertDialog(
                    onDismissRequest = { selectedOrderId = null },
                    title = { Text("${appViewModel.getString("order_details")} #${selectedOrder?.id?.take(8)?.uppercase()}") },
                    text = {
                        Column {
                            Text("${appViewModel.getString("date")}: ${selectedOrder?.date}")
                            Text(localizedText(TextR.string.pan_shipping_to, selectedOrder?.direccionEnvio.toString()), fontSize = 12.sp, color = contrastTextColor(Color.Gray, secondary = true))
                            Text(localizedText(TextR.string.pan_order_total, selectedOrder?.total.toString()))
                            Text(localizedText(TextR.string.pan_order_method, paymentMethodLabel(selectedOrder?.metodoPago.orEmpty())), fontSize = 12.sp, color = contrastTextColor(Color.Gray, secondary = true))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(localizedText(TextR.string.pan_order_status, statusLabel), fontWeight = FontWeight.Bold, color = contrastAccentColor(PanAppPrimary))

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
                                if (canRate && selectedOrder.itemsList.any { it.productoId.isBlank() }) {
                                    Text(stringResource(R.string.client_catalog_legacy_order))
                                }
                                selectedOrder.itemsList.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("- ${item.cantidad}x ${item.nombre}", modifier = Modifier.weight(1f).padding(end = 8.dp), fontSize = 14.sp)
                                        Text(String.format("$%.2f", item.precio * item.cantidad), fontSize = 14.sp, color = contrastTextColor(Color.Gray, secondary = true))
                                    }
                                    if (canRate && item.productoId.isNotBlank()) {
                                        TextButton(onClick = { ratingLine = index }) {
                                            Text(stringResource(R.string.client_catalog_rate_product))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { selectedOrderId = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                        ) {
                            Text(appViewModel.getString("close"), color = Color.White)
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.surface
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(
    order: OrderData,
    modifier: Modifier = Modifier,
    appViewModel: AppViewModel,
    onViewDetails: () -> Unit,
    onReorder: () -> Unit
) {
    val isDarkMode = isPanAppInDarkTheme()
    PanAppGlassCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val knownStatus = orderStatus(order.status)
            val statusLabel = orderStatusLabel(order.status)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "ID ${order.id.take(8).uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = contrastTextColor(Color.Gray, secondary = true),
                        fontWeight = FontWeight.Bold
                    )
                    Text(order.date, style = MaterialTheme.typography.labelSmall, color = contrastTextColor(Color.Gray, secondary = true))
                }

                // Badge de Estado de Envío
                Surface(
                    color = when (knownStatus) {
                        OrderFilter.OnTheWay -> Color(0xFFE3F2FD)
                        OrderFilter.Delivered -> Color(0xFFE8F5E9)
                        else -> Color(0xFFFFF4E5)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = when (knownStatus) {
                            OrderFilter.OnTheWay -> Color(0xFF1565C0)
                            OrderFilter.Delivered -> Color(0xFF2E7D32)
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
                    Icon(Icons.Outlined.Inventory2, contentDescription = null, tint = contrastAccentColor(PanAppPrimary))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        order.mainItem,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                    )
                    // En la tarjeta principal seguimos mostrando un resumen rápido
                    Text("${order.itemCount} ${appViewModel.getString("items")}", style = MaterialTheme.typography.labelSmall, color = contrastTextColor(Color.Gray, secondary = true))
                }
                Text(
                    order.total,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
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
                    Text(appViewModel.getString("view_details"), color = contrastTextColor(Color.Black, darkBackground = isDarkMode))
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = contrastTextColor(Color.Black, darkBackground = isDarkMode))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onReorder,
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary),
                    enabled = knownStatus == OrderFilter.Delivered                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(appViewModel.getString("buy_again"), fontSize = 12.sp)
                }
            }
        }
    }
}