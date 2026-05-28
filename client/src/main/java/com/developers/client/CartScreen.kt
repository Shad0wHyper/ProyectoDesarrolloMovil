package com.developers.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Storefront
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPrimary
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Tarjeta") }
    var showPaymentSheet by remember { mutableStateOf(false) }

    // ✨ Estado para controlar el pop-up de error de dirección
    var showAddressErrorDialog by remember { mutableStateOf(false) }

    val isDarkMode = appViewModel.isDarkMode

    val subtotal = appViewModel.cartSubtotal
    val deliveryFee = if (subtotal > 0) 2.00 else 0.0
    val taxes = subtotal * 0.08
    val finalTotal = subtotal + deliveryFee + taxes

    val paymentSheet = rememberPaymentSheet { paymentSheetResult ->
        when (paymentSheetResult) {
            is com.stripe.android.paymentsheet.PaymentSheetResult.Completed -> {
                // ✨ CORRECCIÓN: Leemos el ID desde nuestro ViewModel Inteligente
                val uid = appViewModel.currentUserId
                if (uid != "INVITADO" && uid.isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val itemsListFirebase = appViewModel.cartItems.map { item ->
                        hashMapOf("nombre" to item.name, "cantidad" to item.quantity, "precio" to item.price)
                    }
                    val nuevoPedido = hashMapOf(
                        "userId" to uid,
                        "clienteNombre" to appViewModel.userName,
                        "cliente" to appViewModel.userName,
                        "direccion" to appViewModel.userAddress,
                        "direccionEnvio" to appViewModel.userAddress,
                        "total" to finalTotal,
                        "estado" to "PENDIENTE",
                        "status" to "PENDIENTE",
                        "timestamp" to System.currentTimeMillis(),
                        "mainItem" to (appViewModel.cartItems.firstOrNull()?.name ?: "Pedido"),
                        "itemCount" to appViewModel.cartTotalQuantity,
                        "items" to itemsListFirebase
                    )
                    db.collection("usuarios").document(uid).collection("pedidos").add(nuevoPedido)
                        .addOnSuccessListener {
                            appViewModel.clearCart()
                            onPaymentSuccess()
                        }
                }
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Canceled -> { }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Failed -> { }
        }
    }

    fun presentPaymentSheet() {
        val googlePayConfig = PaymentSheet.GooglePayConfiguration(
            environment = PaymentSheet.GooglePayConfiguration.Environment.Test,
            countryCode = "MX"
        )
        val configuration = PaymentSheet.Configuration(
            merchantDisplayName = "PanApp Inc.",
            googlePay = googlePayConfig,
            allowsDelayedPaymentMethods = true
        )
        paymentSheet.presentWithPaymentIntent(
            "pi_example_secret_placeholder",
            configuration
        )
    }

    // ✨ LÓGICA FILTRADORA DE SEGURIDAD
    fun handleCheckoutProcess() {
        if (appViewModel.userAddress.trim().isEmpty()) {
            // Candado activado: No hay dirección, detenemos todo y mostramos Pop-Up
            showAddressErrorDialog = true
        } else {
            // Procedimiento normal de cobro
            if (selectedPaymentMethod == "Tarjeta") {
                presentPaymentSheet()
            } else {
                showPaymentSheet = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appViewModel.getString("cart"), fontWeight = FontWeight.Bold) },
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
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(appViewModel.getString("total"), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                        Text(String.format("$%.2f", finalTotal), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PanAppPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { handleCheckoutProcess() }, // ✨ Redirige al filtro de seguridad
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = appViewModel.cartItems.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                    ) {
                        Text(appViewModel.getString("confirm_order"), modifier = Modifier.padding(vertical = 8.dp))
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8))
                .padding(horizontal = 16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        appViewModel.getString("your_order"),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Surface(
                        color = PanAppPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "${appViewModel.cartTotalQuantity} ${appViewModel.getString("items")}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = PanAppPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            if (appViewModel.cartItems.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        Text(appViewModel.getString("empty_cart"), color = Color.Gray, fontSize = 16.sp)
                    }
                }
            } else {
                items(appViewModel.cartItems) { cartItem ->
                    CartItemWidget(
                        item = cartItem,
                        isDarkMode = isDarkMode,
                        onIncrease = { appViewModel.updateQuantity(cartItem.id, cartItem.quantity + 1) },
                        onDecrease = { appViewModel.updateQuantity(cartItem.id, cartItem.quantity - 1) }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))

                // ✨ MOSTRAR DIRECCIÓN EN EL RESUMEN DEL CARRITO (Si ya la tiene escrita)
                if (appViewModel.userAddress.trim().isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = PanAppPrimary.copy(alpha = 0.05f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = PanAppPrimary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Entregar en:", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                Text(appViewModel.userAddress, fontSize = 14.sp, color = if (isDarkMode) Color.White else Color.Black)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                HorizontalDivider(color = if (isDarkMode) Color.DarkGray else Color.LightGray.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    appViewModel.getString("payment_method"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PaymentMethodItem(
                            appViewModel.getString("card"), "Stripe / Apple Pay", Icons.Outlined.CreditCard,
                            selectedPaymentMethod == "Tarjeta", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Tarjeta" }
                        Spacer(modifier = Modifier.width(12.dp))
                        PaymentMethodItem(
                            appViewModel.getString("transfer"), "SPEI / Bank", Icons.Outlined.AccountBalance,
                            selectedPaymentMethod == "Transferencia", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Transferencia" }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PaymentMethodItem(
                            appViewModel.getString("cash"), "OXXO / Tienda", Icons.Outlined.Payments,
                            selectedPaymentMethod == "Efectivo", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Efectivo" }
                        Spacer(modifier = Modifier.width(12.dp))
                        PaymentMethodItem(
                            appViewModel.getString("local_pay"), "Google Pay", Icons.Outlined.Storefront,
                            selectedPaymentMethod == "Pago Local", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Pago Local" }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                OrderSummaryWidget(subtotal, deliveryFee, taxes, finalTotal, isDarkMode, appViewModel)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ✨ POP-UP FLOTANTE DE ERROR DE DIRECCIÓN (Candado del Carrito)
    if (showAddressErrorDialog) {
        AlertDialog(
            onDismissRequest = { showAddressErrorDialog = false },
            title = { Text("Falta Dirección de Envío", fontWeight = FontWeight.Bold) },
            text = { Text("Se necesita registrar una dirección de entrega en los ajustes de tu perfil para poder realizar una compra.") },
            confirmButton = {
                Button(
                    onClick = { showAddressErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                ) {
                    Text("Entendido")
                }
            }
        )
    }

    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDarkMode) Color.Gray else Color.LightGray) }
        ) {
            PaymentGatewayContent(selectedPaymentMethod, isDarkMode, appViewModel) {
                // ✨ CORRECCIÓN: Leemos el ID desde nuestro ViewModel Inteligente
                val uid = appViewModel.currentUserId
                if (uid != "INVITADO" && uid.isNotEmpty()) {
                    val db = FirebaseFirestore.getInstance()
                    val itemsListFirebase = appViewModel.cartItems.map { item ->
                        hashMapOf("nombre" to item.name, "cantidad" to item.quantity, "precio" to item.price)
                    }
                    val nuevoPedido = hashMapOf(
                        "userId" to uid,
                        "clienteNombre" to appViewModel.userName,
                        "cliente" to appViewModel.userName,
                        "direccion" to appViewModel.userAddress,
                        "direccionEnvio" to appViewModel.userAddress,
                        "total" to finalTotal,
                        "estado" to "PENDIENTE",
                        "status" to "PENDIENTE",
                        "timestamp" to System.currentTimeMillis(),
                        "mainItem" to (appViewModel.cartItems.firstOrNull()?.name ?: "Pedido"),
                        "itemCount" to appViewModel.cartTotalQuantity,
                        "items" to itemsListFirebase
                    )
                    db.collection("usuarios").document(uid).collection("pedidos").add(nuevoPedido).addOnSuccessListener {
                        showPaymentSheet = false
                        appViewModel.clearCart()
                        onPaymentSuccess()
                    }
                } else {
                    showPaymentSheet = false
                }
            }
        }
    }
}

@Composable
fun OrderSummaryWidget(subtotal: Double, delivery: Double, taxes: Double, total: Double, isDarkMode: Boolean, appViewModel: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF0F0F0).copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(appViewModel.getString("subtotal"), String.format("$%.2f", subtotal), isDarkMode)
            SummaryRow(appViewModel.getString("delivery"), String.format("$%.2f", delivery), isDarkMode)
            SummaryRow(appViewModel.getString("taxes"), String.format("$%.2f", taxes), isDarkMode)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isDarkMode) Color.White else Color.Black)
                Text(String.format("$%.2f", total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PanAppPrimary)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isDarkMode: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = if (isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PaymentGatewayContent(method: String, isDarkMode: Boolean, appViewModel: AppViewModel, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = when(method) {
                "Tarjeta" -> Icons.Outlined.CreditCard
                "Transferencia" -> Icons.Outlined.AccountBalance
                "Efectivo" -> Icons.Outlined.Payments
                else -> Icons.Outlined.Storefront
            },
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = PanAppPrimary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Completar Pago",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Text(
            text = "Método seleccionado: $method",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = when(method) {
                        "Transferencia" -> "CLABE: 0123 4567 8901 2345 67\nBanco: PanBank\nReferencia: #ORDER-772"
                        "Efectivo" -> "Presenta este código en cualquier OXXO o tienda afiliada para realizar tu pago."
                        else -> "Ingresa los detalles de tu $method para continuar con la transacción segura."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDarkMode) Color.LightGray else Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (method == "Efectivo") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(Color.White)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("|| ||| || |||| ||| ||", letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
        ) {
            Text(
                if (method == "Tarjeta") appViewModel.getString("pay_now") else "Confirmar Pedido",
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        TextButton(onClick = { /* Aquí puedes manejar cancelar */ }) {
            Text(appViewModel.getString("close"), color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CartItemWidget(
    item: CartItem,
    isDarkMode: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(item.desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(String.format("$%.2f", item.price), color = PanAppPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onDecrease,
                            modifier = Modifier.size(28.dp).background(if (isDarkMode) Color.DarkGray else Color(0xFFEEEEEE), CircleShape)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp), tint = if (isDarkMode) Color.White else Color.Black)
                        }

                        Text(
                            text = "${item.quantity}",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color.White else Color.Black
                        )

                        IconButton(
                            onClick = { onIncrease() },
                            modifier = Modifier.size(28.dp).background(PanAppPrimary, CircleShape)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Más", modifier = Modifier.size(16.dp), tint = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(
    title: String, subtitle: String, icon: ImageVector,
    isSelected: Boolean, isDarkMode: Boolean, modifier: Modifier, onClick: () -> Unit
) {
    Surface(
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFF32324D) else if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
        border = if (isSelected) BorderStroke(2.dp, PanAppPrimary) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon, contentDescription = null,
                tint = if (isSelected) Color.White else Color.Gray
            )
            Text(
                title,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodySmall,
                color = if (isDarkMode || isSelected) Color.White else Color.Black
            )
        }
    }
}