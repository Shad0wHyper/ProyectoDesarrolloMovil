package com.developers.client

import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.client.ui.theme.PanAppPrimary
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddCard: () -> Unit
) {
    val isDarkMode = appViewModel.isDarkMode

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText(TextR.string.pan_payment_methods), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = localizedText(TextR.string.pan_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White,
                    titleContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    navigationIconContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAddCard,
                containerColor = PanAppPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = localizedText(TextR.string.pan_add_card))
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
                Text(
                    localizedText(TextR.string.pan_saved_cards),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }

            // ✨ MOSTRAR TARJETA REAL GUARDADA (Si existe en el ViewModel)
            if (appViewModel.hasSavedCard) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = PanAppPrimary.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    Icons.Default.CreditCard, 
                                    contentDescription = null, 
                                    tint = contrastAccentColor(PanAppPrimary),
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    localizedText(TextR.string.pan_card_last_four, appViewModel.userCardLast4),
                                    fontWeight = FontWeight.Bold, 
                                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                                )
                                Text(
                                    localizedText(TextR.string.pan_card_expiry, appViewModel.userCardExp),
                                    fontSize = 12.sp, 
                                    color = contrastTextColor(Color.Gray, secondary = true)
                                )
                            }
                            IconButton(onClick = {
                                // Opción para eliminar la tarjeta de Firestore
                                val uid = appViewModel.currentUserId
                                if (uid != "INVITADO") {
                                    FirebaseFirestore.getInstance().collection("usuarios")
                                        .document(uid)
                                        .update(mapOf("cardLast4" to "", "cardExp" to ""))
                                        .addOnSuccessListener {
                                            appViewModel.userCardLast4 = ""
                                            appViewModel.userCardExp = ""
                                            appViewModel.hasSavedCard = false
                                        }
                                }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = localizedText(TextR.string.pan_delete), tint = Color.Red.copy(alpha = 0.6f))
                            }
                        }
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            localizedText(TextR.string.pan_no_cards),
                            color = contrastTextColor(Color.Gray, secondary = true),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    localizedText(TextR.string.pan_other_methods),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    modifier = Modifier.padding(vertical = 16.dp)
                )
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    border = BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray.copy(alpha = 0.5f))
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text(localizedText(TextR.string.pan_oxxo_cash), color = contrastTextColor(Color.Black, darkBackground = isDarkMode)) },
                            supportingContent = { Text(localizedText(TextR.string.pan_oxxo_description), color = contrastTextColor(Color.Gray, secondary = true)) },
                            leadingContent = { 
                                Icon(Icons.Outlined.Payments, contentDescription = null, tint = Color(0xFF4CAF50)) 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = if (isDarkMode) Color.DarkGray else Color.LightGray.copy(alpha = 0.3f))
                        ListItem(
                            headlineContent = { Text(localizedText(TextR.string.pan_spei), color = contrastTextColor(Color.Black, darkBackground = isDarkMode)) },
                            supportingContent = { Text(localizedText(TextR.string.pan_spei_description), color = contrastTextColor(Color.Gray, secondary = true)) },
                            leadingContent = { 
                                Icon(Icons.Outlined.AccountBalance, contentDescription = null, tint = Color(0xFF2196F3)) 
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }
        }
    }
}
