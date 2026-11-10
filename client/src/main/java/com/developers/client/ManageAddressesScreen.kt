package com.developers.client

import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
fun ManageAddressesScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
) {
    val isDarkMode = appViewModel.isDarkMode
    var showAddDialog by remember { mutableStateOf(false) }
    var newAddress by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(localizedText(TextR.string.pan_my_addresses), fontWeight = FontWeight.Bold) },
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
                onClick = { showAddDialog = true },
                containerColor = PanAppPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = localizedText(TextR.string.pan_add_address))
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
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    localizedText(TextR.string.pan_manage_addresses),
                    style = MaterialTheme.typography.bodyMedium,
                    color = contrastTextColor(Color.Gray, secondary = true)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            if (appViewModel.userAddressesList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 64.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(64.dp), tint = contrastTextColor(Color.Gray.copy(alpha = 0.3f), secondary = true))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(localizedText(TextR.string.pan_no_addresses), color = contrastTextColor(Color.Gray, secondary = true))
                        }
                    }
                }
            } else {
                items(appViewModel.userAddressesList) { address ->
                    AddressItem(
                        address = address,
                        isSelected = address == appViewModel.userAddress,
                        isDarkMode = isDarkMode,
                        onSelect = { appViewModel.userAddress = address },
                        onDelete = {
                            val newList = appViewModel.userAddressesList.filter { it != address }
                            val uid = appViewModel.currentUserId
                            if (uid != "INVITADO") {
                                FirebaseFirestore.getInstance().collection("usuarios").document(uid)
                                    .update("listaDirecciones", newList)
                                    .addOnSuccessListener {
                                        appViewModel.userAddressesList = newList
                                        if (appViewModel.userAddress == address) {
                                            appViewModel.userAddress = newList.firstOrNull() ?: ""
                                        }
                                    }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(localizedText(TextR.string.pan_new_address)) },
            text = {
                OutlinedTextField(
                    colors = contrastFieldColors(androidx.compose.material3.OutlinedTextFieldDefaults.colors()),
                    value = newAddress,
                    onValueChange = { newAddress = it },
                    placeholder = { Text(localizedText(TextR.string.pan_address_example_alt)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newAddress.isNotBlank()) {
                            appViewModel.addAddress(newAddress)
                            newAddress = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                ) {
                    Text(localizedText(TextR.string.pan_save))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text(localizedText(TextR.string.pan_cancel)) }
            }
        )
    }
}

@Composable
fun AddressItem(
    address: String,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, PanAppPrimary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(8.dp),
                color = if (isSelected) PanAppPrimary.copy(alpha = 0.1f) else Color.Gray.copy(alpha = 0.1f)
            ) {
                Icon(
                    Icons.Default.Home,
                    contentDescription = null,
                    tint = if (isSelected) PanAppPrimary else contrastTextColor(Color.Gray, secondary = true),
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
                if (isSelected) {
                    Text(localizedText(TextR.string.pan_main_address), fontSize = 11.sp, color = contrastAccentColor(PanAppPrimary), fontWeight = FontWeight.Bold)
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = localizedText(TextR.string.pan_delete), tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
            }
        }
    }
}
