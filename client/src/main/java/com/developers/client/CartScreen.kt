package com.developers.client

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.PanAppPrimary
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.rememberPaymentSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onPaymentSuccess: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Tarjeta") }
    var showPaymentSheet by remember { mutableStateOf(false) }
    val isDarkMode = appViewModel.isDarkMode

    // Stripe PaymentSheet integration
    val paymentSheet = rememberPaymentSheet { paymentSheetResult ->
        when (paymentSheetResult) {
            is com.stripe.android.paymentsheet.PaymentSheetResult.Completed -> {
                onPaymentSuccess()
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Canceled -> {
                // Handle cancellation
            }
            is com.stripe.android.paymentsheet.PaymentSheetResult.Failed -> {
                // Handle error
            }
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
                        Text("$17.45", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = PanAppPrimary)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { 
                            if (selectedPaymentMethod == "Tarjeta") {
                                presentPaymentSheet()
                            } else {
                                showPaymentSheet = true 
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                            "3 ${appViewModel.getString("items")}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            color = PanAppPrimary,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(3) { index ->
                val name = when(index) {
                    0 -> "Croissant de Mantequilla"
                    1 -> "Baguette Rústica"
                    else -> "Muffin de Arándanos"
                }
                val price = when(index) {
                    0 -> "$2.50"
                    1 -> "$4.00"
                    else -> "$3.25"
                }
                CartItem(
                    name = name,
                    desc = "Artesanal, recién horneado",
                    price = price,
                    quantity = 1,
                    isDarkMode = isDarkMode
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
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
                OrderSummary(isDarkMode, appViewModel)
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    if (showPaymentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showPaymentSheet = false },
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = if (isDarkMode) Color.Gray else Color.LightGray) }
        ) {
            PaymentGatewayContent(selectedPaymentMethod, isDarkMode, appViewModel) {
                showPaymentSheet = false
                onPaymentSuccess() 
            }
        }
    }
}

@Composable
fun OrderSummary(isDarkMode: Boolean, appViewModel: AppViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFF0F0F0).copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SummaryRow("Subtotal", "$9.75", isDarkMode)
            SummaryRow("Envío", "$2.00", isDarkMode)
            SummaryRow("Impuestos", "$0.80", isDarkMode)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Gray.copy(alpha = 0.2f))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = if (isDarkMode) Color.White else Color.Black)
                Text("$12.55", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = PanAppPrimary)
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
        
        TextButton(onClick = onDismiss) {
            Text(appViewModel.getString("close"), color = Color.Gray)
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun CartItem(name: String, desc: String, price: String, quantity: Int, isDarkMode: Boolean) {
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
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFF5F5F5))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = if (isDarkMode) Color.White else Color.Black)
                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(price, color = PanAppPrimary, fontWeight = FontWeight.Bold)
                    Text("Qty: $quantity", color = if (isDarkMode) Color.White else Color.Black)
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
