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
import androidx.compose.ui.unit.dp
import com.developers.client.ui.theme.PanAppPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedPaymentMethod by remember { mutableStateOf("Tarjeta") }
    var showPaymentSheet by remember { mutableStateOf(false) }
    val isDarkMode = appViewModel.isDarkMode

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
                        onClick = { showPaymentSheet = true },
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
                        style = MaterialTheme.typography.titleLarge, 
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Surface(
                        color = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE8E8FF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "3 ${appViewModel.getString("items")}",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = PanAppPrimary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            items(3) { index ->
                CartItem(
                    name = when(index) {
                        0 -> "Croissant"
                        1 -> "Baguette"
                        else -> "Muffin"
                    },
                    desc = "Delicioso y artesanal.",
                    price = "$5.00",
                    quantity = 1,
                    isDarkMode = isDarkMode
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
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
                            appViewModel.getString("card"), "Crédito o Débito", Icons.Outlined.CreditCard, 
                            selectedPaymentMethod == "Tarjeta", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Tarjeta" }
                        Spacer(modifier = Modifier.width(12.dp))
                        PaymentMethodItem(
                            appViewModel.getString("transfer"), "Banca Móvil", Icons.Outlined.AccountBalance, 
                            selectedPaymentMethod == "Transferencia", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Transferencia" }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        PaymentMethodItem(
                            appViewModel.getString("cash"), "Pago en Tienda", Icons.Outlined.Payments, 
                            selectedPaymentMethod == "Efectivo", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Efectivo" }
                        Spacer(modifier = Modifier.width(12.dp))
                        PaymentMethodItem(
                            appViewModel.getString("local_pay"), "Billetera Digital", Icons.Outlined.Storefront, 
                            selectedPaymentMethod == "Pago Local", isDarkMode, Modifier.weight(1f)
                        ) { selectedPaymentMethod = "Pago Local" }
                    }
                }
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
            }
        }
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
        Text(
            text = appViewModel.getString("confirm_order"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = if (isDarkMode) Color.White else Color.Black
        )
        Text(
            text = "${appViewModel.getString("payment_method")}: $method",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(24.dp))

        Text("Formulario de pago para $method", color = if (isDarkMode) Color.White else Color.Black)

        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
        ) {
            Text(appViewModel.getString("pay_now"))
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
