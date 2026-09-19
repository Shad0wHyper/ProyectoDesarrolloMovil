package com.developers.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPrimary
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Tarjeta") }
    var showPaymentSheetManual by remember { mutableStateOf(false) }

    // ✨ Estado para controlar el pop-up de error de dirección
    var showAddressErrorDialog by remember { mutableStateOf(false) }

    // ✨ Nuevo estado para la dirección editable en tiempo real
    var editableAddress by remember(appViewModel.userAddress) { mutableStateOf(appViewModel.userAddress) }

    val isDarkMode = appViewModel.isDarkMode

    val subtotal = appViewModel.cartSubtotal
    val deliveryFee = if (subtotal > 0) 2.00 else 0.0
    val taxes = subtotal * 0.08
    val finalTotal = subtotal + deliveryFee + taxes

    // ✨ LÓGICA FILTRADORA DE SEGURIDAD (Permite poner la dirección aquí mismo)
    fun handleCheckoutProcess() {
        if (editableAddress.trim().isEmpty()) {
            showAddressErrorDialog = true
        } else {
            appViewModel.userAddress = editableAddress
            val uid = appViewModel.currentUserId
            if (uid != "INVITADO" && uid.isNotEmpty()) {
                FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                    .update("direccion", editableAddress)
            }

            // Mostramos la pasarela manual para todos los métodos para asegurar funcionalidad
            appViewModel.addAddress(editableAddress) // ✨ Guardamos en la lista global del usuario
            showPaymentSheetManual = true
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
                        onClick = { handleCheckoutProcess() },
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

                Text(
                    appViewModel.getString("delivery_address"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.height(12.dp))

                // ✨ Selector de Direcciones Guardadas
                if (appViewModel.userAddressesList.isNotEmpty()) {
                    var showAddressDialog by remember { mutableStateOf(false) }
                    
                    OutlinedButton(
                        onClick = { showAddressDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, PanAppPrimary.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar de mis direcciones", fontSize = 13.sp)
                    }

                    if (showAddressDialog) {
                        AlertDialog(
                            onDismissRequest = { showAddressDialog = false },
                            title = { Text("Mis Direcciones") },
                            text = {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    appViewModel.userAddressesList.forEach { addr ->
                                        ListItem(
                                            headlineContent = { Text(addr, fontSize = 14.sp) },
                                            modifier = Modifier.clickable {
                                                editableAddress = addr
                                                showAddressDialog = false
                                            }
                                        )
                                        HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showAddressDialog = false }) { Text("Cerrar") }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = editableAddress,
                    onValueChange = { editableAddress = it },
                    label = { Text(appViewModel.getString("add_address_or_select")) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ej. Calle Principal #123, Colonia Centro") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = PanAppPrimary) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PanAppPrimary,
                        unfocusedBorderColor = Color.Gray.copy(alpha = 0.4f),
                        focusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                        unfocusedContainerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
                        focusedTextColor = if (isDarkMode) Color.White else Color.Black,
                        unfocusedTextColor = if (isDarkMode) Color.White else Color.Black
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                            appViewModel.getString("card"), "Visa / Mastercard", Icons.Outlined.CreditCard,
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
                            appViewModel.getString("cash"), "OXXO Pay", Icons.Outlined.Payments,
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

    if (showAddressErrorDialog) {
        AlertDialog(
            onDismissRequest = { showAddressErrorDialog = false },
            title = { Text(appViewModel.getString("missing_address_title"), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black) },
            text = { Text(appViewModel.getString("missing_address_desc"), color = if (isDarkMode) Color.LightGray else Color.DarkGray) },
            confirmButton = { Button(onClick = { showAddressErrorDialog = false }, colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)) { Text("Entendido", color = Color.White) } },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        )
    }

    // ✨ PASARELA DE PAGO MANUAL (CONECTADA AL BANCO INTERNO)
    var isProcessingPayment by remember { mutableStateOf(false) }
    var paymentErrorMessage by remember { mutableStateOf<String?>(null) }

    if (showPaymentSheetManual) {
        ModalBottomSheet(
            onDismissRequest = { if (!isProcessingPayment) showPaymentSheetManual = false },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDarkMode) Color.Gray else Color.LightGray) }
        ) {
            PaymentGatewayContent(
                method = selectedPaymentMethod,
                isDarkMode = isDarkMode,
                appViewModel = appViewModel,
                isProcessing = isProcessingPayment,
                onCancel = { if (!isProcessingPayment) showPaymentSheetManual = false },
                onConfirm = { last4, barcode ->
                    isProcessingPayment = true
                    paymentErrorMessage = null
                    
                    if (selectedPaymentMethod == "Efectivo") {
                        registrarPedidoEnFirebase(
                            appViewModel = appViewModel,
                            total = finalTotal,
                            metodo = "OXXO Pay",
                            cardInfo = "Ref: $barcode",
                            onSuccess = onPaymentSuccess,
                            onComplete = {
                                isProcessingPayment = false
                                showPaymentSheetManual = false
                            }
                        )
                    } else {
                        //  PROCESAR COBRO BANCARIO REAL (Saldo interno App)
                        appViewModel.procesarCobroBancario(finalTotal) { success, message ->
                            if (!success) {
                                isProcessingPayment = false
                                paymentErrorMessage = message
                            } else {
                                registrarPedidoEnFirebase(
                                    appViewModel = appViewModel,
                                    total = finalTotal,
                                    metodo = selectedPaymentMethod,
                                    cardInfo = if (selectedPaymentMethod == "Tarjeta") "**** $last4" else "",
                                    onSuccess = onPaymentSuccess,
                                    onComplete = {
                                        isProcessingPayment = false
                                        showPaymentSheetManual = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
            
            paymentErrorMessage?.let { msg ->
                Text(msg, color = Color.Red, modifier = Modifier.padding(16.dp).fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

fun registrarPedidoEnFirebase(
    appViewModel: AppViewModel,
    total: Double,
    metodo: String,
    cardInfo: String,
    onSuccess: () -> Unit,
    onComplete: () -> Unit = {}
) {
    val uid = appViewModel.currentUserId
    val db = FirebaseFirestore.getInstance()
    val batch = db.batch()

    val itemsListFirebase = appViewModel.cartItems.map { item ->
        val productRef = db.collection("productos").document(item.id)
        batch.update(productRef, "stock", FieldValue.increment(-item.quantity.toLong()))
        
        hashMapOf("nombre" to item.name, "cantidad" to item.quantity, "precio" to item.price)
    }

    val nuevoPedido = hashMapOf(
        "userId" to uid,
        "clienteNombre" to appViewModel.userName,
        "cliente" to appViewModel.userName,
        "direccion" to appViewModel.userAddress,
        "direccionEnvio" to appViewModel.userAddress,
        "total" to total,
        "estado" to if (metodo == "OXXO Pay") "ESPERANDO PAGO" else "PENDIENTE",
        "status" to if (metodo == "OXXO Pay") "ESPERANDO PAGO" else "PENDIENTE",
        "timestamp" to System.currentTimeMillis(),
        "mainItem" to (appViewModel.cartItems.firstOrNull()?.name ?: "Pedido"),
        "itemCount" to appViewModel.cartTotalQuantity,
        "items" to itemsListFirebase,
        "metodoPago" to metodo,
        "pagado" to (metodo != "OXXO Pay"),
        "cuentaDestino" to "7229 6901 3635 3659 79",
        "cardUsed" to cardInfo
    )
    if (uid != "INVITADO" && uid.isNotEmpty()) {
        val userRef = db.collection("usuarios").document(uid).collection("pedidos").document()
        batch.set(userRef, nuevoPedido)
    }
    val globalRef = db.collection("pedidos").document()
    batch.set(globalRef, nuevoPedido)

    batch.commit().addOnSuccessListener {
        appViewModel.clearCart()
        appViewModel.resetTempPaymentData() //  Limpiamos el controlador al tener éxito
        onSuccess()
        onComplete()
    }.addOnFailureListener { onComplete() }
}

@Composable
fun OrderSummaryWidget(subtotal: Double, delivery: Double, taxes: Double, total: Double, isDarkMode: Boolean, appViewModel: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF0F0F0).copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow(appViewModel.getString("subtotal"), String.format("$%.2f", subtotal), isDarkMode)
            SummaryRow(appViewModel.getString("delivery"), String.format("$%.2f", delivery), isDarkMode)
            SummaryRow(appViewModel.getString("taxes"), String.format("$%.2f", taxes), isDarkMode)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isDarkMode) Color.White else Color.Black)
                Text(String.format("$%.2f", total), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PanAppPrimary)
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isDarkMode: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.Gray, style = MaterialTheme.typography.bodyMedium)
        Text(value, color = if (isDarkMode) Color.White else Color.Black, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PaymentGatewayContent(
    method: String,
    isDarkMode: Boolean,
    appViewModel: AppViewModel,
    isProcessing: Boolean = false,
    onConfirm: (String, String) -> Unit, // (last4, barcode)
    onCancel: () -> Unit
) {
    // ✨ Usamos el Controlador del ViewModel para persistir datos entre pantallas
    val cardNumber = appViewModel.tempCardNumber
    val expiryDate = appViewModel.tempExpiryDate
    val cvc = appViewModel.tempCVC
    val saveCard = appViewModel.tempSaveCard

    // Generar un código de barras único cada vez que se abre la pasarela
    val oxxoReference = remember(method) { 
        (1..14).map { Random.nextInt(0, 10) }.joinToString("") 
    }

    // ✨ DETERMINAR SI USAR LA TARJETA GUARDADA O LA NUEVA
    val isUsingSavedCard = appViewModel.hasSavedCard && cardNumber.isEmpty()
    
    val displayCardValue = if (isUsingSavedCard) {
        "**** **** **** ${appViewModel.userCardLast4}"
    } else cardNumber

    val isCardValid = remember(cardNumber, expiryDate, cvc, appViewModel.hasSavedCard) {
        if (method != "Tarjeta") true
        else if (isUsingSavedCard && cvc.length == 3) true
        else cardNumber.length == 16 && expiryDate.length >= 4 && cvc.length == 3
    }

    Column(modifier = Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(imageVector = when(method) { "Tarjeta" -> Icons.Outlined.CreditCard; "Transferencia" -> Icons.Outlined.AccountBalance; "Efectivo" -> Icons.Outlined.Payments; else -> Icons.Outlined.Storefront }, contentDescription = null, modifier = Modifier.size(48.dp), tint = PanAppPrimary)
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = if (method == "Efectivo") "OXXO Pay" 
                   else if (isUsingSavedCard) "Confirmar Tarjeta Guardada" 
                   else "Completar Pago", 
            style = MaterialTheme.typography.headlineSmall, 
            fontWeight = FontWeight.Bold, 
            color = if (isDarkMode) Color.White else Color.Black
        )
        
        Text(text = "Método seleccionado: $method", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
        
        if (method != "Efectivo") {
            Text(text = "Tu saldo: ${String.format("$%.2f", appViewModel.userBalance)}", style = MaterialTheme.typography.labelLarge, color = if (appViewModel.userBalance < (appViewModel.cartSubtotal + 5.5)) Color.Red else Color(0xFF4CAF50), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Surface(modifier = Modifier.fillMaxWidth(), color = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5), shape = RoundedCornerShape(12.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = when(method) { 
                    "Transferencia" -> "CLABE: 7229 6901 3635 3659 79\nBanco: PanApp Official\nReferencia: #ORD-${System.currentTimeMillis().toString().takeLast(6)}"
                    "Efectivo" -> "Presenta este código en cualquier OXXO para pagar tu pedido."
                    "Tarjeta" -> if (isUsingSavedCard) "Confirma el CVC de tu tarjeta guardada para proceder." else "Introduce los datos de tu tarjeta. Los fondos se liquidarán a la cuenta terminada en ...5979.\n(Conexión SSL Segura)"
                    else -> "Ingresa los detalles de tu $method para continuar con la transacción segura." 
                }, style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Color.LightGray else Color.DarkGray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                
                if (method == "Efectivo") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .background(Color.White)
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "|| ||| || |||| ||| ||", letterSpacing = 4.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                        }
                        Text(
                            text = oxxoReference.chunked(4).joinToString(" "),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color.Black,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                if (method == "Tarjeta") {
                    Spacer(modifier = Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = displayCardValue, 
                            onValueChange = { if (it.length <= 16 && it.all { c -> c.isDigit() }) { appViewModel.tempCardNumber = it } }, 
                            modifier = Modifier.fillMaxWidth(), 
                            label = { Text("Número de Tarjeta") }, 
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                            placeholder = { Text("0000 0000 0000 0000") }, 
                            leadingIcon = { Icon(Icons.Outlined.CreditCard, contentDescription = null) }, 
                            trailingIcon = { 
                                if (isUsingSavedCard) { 
                                    TextButton(onClick = { appViewModel.resetTempPaymentData(); appViewModel.hasSavedCard = false }) { 
                                        Text("Cambiar", fontSize = 12.sp) 
                                    } 
                                } 
                            }, 
                            singleLine = true, 
                            enabled = !isProcessing && !isUsingSavedCard, 
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                        )
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = expiryDate, 
                                onValueChange = { if (it.length <= 5) appViewModel.tempExpiryDate = it }, 
                                modifier = Modifier.weight(1f), 
                                label = { Text("MM/YY") }, 
                                placeholder = { Text("12/28") }, 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                singleLine = true, 
                                enabled = !isProcessing && !isUsingSavedCard, 
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                            )
                            OutlinedTextField(
                                value = cvc, 
                                onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) appViewModel.tempCVC = it }, 
                                modifier = Modifier.weight(1f), 
                                label = { Text("CVC") }, 
                                placeholder = { Text("123") }, 
                                visualTransformation = PasswordVisualTransformation(), 
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                                singleLine = true, 
                                enabled = !isProcessing, 
                                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent)
                            )
                        }
                        
                        if (!appViewModel.hasSavedCard || !isUsingSavedCard) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(enabled = !isProcessing) { appViewModel.tempSaveCard = !saveCard }) { 
                                Checkbox(checked = saveCard, onCheckedChange = { appViewModel.tempSaveCard = it }, enabled = !isProcessing)
                                Text("Guardar tarjeta para futuras compras", style = MaterialTheme.typography.bodySmall, color = if (isDarkMode) Color.LightGray else Color.DarkGray) 
                            }
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50)); Spacer(modifier = Modifier.width(6.dp)); Text("Tus datos están encriptados y protegidos.", style = MaterialTheme.typography.labelSmall, color = Color(0xFF4CAF50)) }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = { 
                val finalLast4 = if (isUsingSavedCard) appViewModel.userCardLast4 else cardNumber.takeLast(4)
                val finalExp = if (isUsingSavedCard) appViewModel.userCardExp else expiryDate
                
                if (saveCard && cardNumber.isNotEmpty()) { 
                    appViewModel.savePaymentCard(finalLast4, finalExp) 
                }
                onConfirm(finalLast4, oxxoReference) 
            }, 
            modifier = Modifier.fillMaxWidth(), 
            shape = RoundedCornerShape(12.dp), 
            enabled = isCardValid && !isProcessing, 
            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
        ) {
            if (isProcessing) { 
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp) 
            } else { 
                Text(if (method == "Tarjeta") appViewModel.getString("pay_now") else if (method == "Efectivo") "Generar Código OXXO" else "Confirmar Pedido", modifier = Modifier.padding(vertical = 4.dp)) 
            }
        }
        TextButton(onClick = onCancel, enabled = !isProcessing) { Text(appViewModel.getString("close"), color = Color.Gray) }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CartItemWidget(item: CartItem, isDarkMode: Boolean, onIncrease: () -> Unit, onDecrease: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF8F8F8).copy(alpha = 0.5f))) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(item.imageUrl).crossfade(true).build(), contentDescription = item.name, contentScale = ContentScale.Crop, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(12.dp)).background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5)))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(item.desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(String.format("$%.2f", item.price), color = PanAppPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDecrease, modifier = Modifier.size(28.dp).background(if (isDarkMode) Color.DarkGray else Color(0xFFEEEEEE), CircleShape)) { Icon(Icons.Default.Remove, contentDescription = "Menos", modifier = Modifier.size(16.dp), tint = if (isDarkMode) Color.White else Color.Black) }
                        Text(text = "${item.quantity}", modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                        IconButton(onClick = { onIncrease() }, modifier = Modifier.size(28.dp).background(PanAppPrimary, CircleShape)) { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White) }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodItem(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, isDarkMode: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(modifier = modifier.height(100.dp), shape = RoundedCornerShape(16.dp), color = if (isSelected) Color(0xFF32324D) else if (isDarkMode) Color(0xFF1E1E1E) else Color.White, border = if (isSelected) BorderStroke(2.dp, PanAppPrimary) else BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)), onClick = onClick) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = if (isSelected) Color.White else Color.Gray)
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = if (isDarkMode || isSelected) Color.White else Color.Black)
        }
    }
}
