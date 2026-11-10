package com.developers.client

import com.developers.core.ui.contrastFieldColors
import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor
import com.developers.core.ui.contrastErrorColor

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.developers.client.ui.theme.PanAppPrimary
import com.google.firebase.storage.FirebaseStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    appViewModel: AppViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onNavigateToAddresses: () -> Unit,
    onLogoutClick: () -> Unit = {}
) {
    var name by remember { mutableStateOf(appViewModel.userName) }
    var phone by remember { mutableStateOf(appViewModel.userPhone) }
    var address by remember { mutableStateOf(appViewModel.userAddress) } // ✨ NUEVO: Campo de dirección
    val email = appViewModel.userEmail

    var isSaving by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val isDarkMode = appViewModel.isDarkMode
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                isUploadingImage = true
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("perfiles/${appViewModel.currentUserId}.jpg")

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            appViewModel.updateProfileImage(downloadUrl.toString())
                            isUploadingImage = false
                            Toast.makeText(context, localizedText(TextR.string.pan_photo_updated), Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        isUploadingImage = false
                        Toast.makeText(context, localizedText(TextR.string.pan_photo_error), Toast.LENGTH_SHORT).show()
                    }
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(appViewModel.getString("profile"), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = appViewModel.getString("close"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White,
                    titleContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode),
                    navigationIconContentColor = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(if (isDarkMode) Color(0xFF121212) else Color(0xFFF8F8F8))
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // SECCIÓN FOTO DE PERFIL
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clickable {
                        if (!isUploadingImage && appViewModel.currentUserId != "INVITADO") {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } else if (appViewModel.currentUserId == "INVITADO") {
                            Toast.makeText(context, localizedText(TextR.string.pan_photo_login), Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // CÍRCULO PRINCIPAL DE LA FOTO (Clipped estricto solo a la imagen)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (appViewModel.userImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(appViewModel.userImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = localizedText(TextR.string.pan_photo_profile),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(65.dp), tint = contrastTextColor(Color.Gray, secondary = true))
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

                // BURBUJA DE LA CÁMARA (Superpuesta fuera del clip principal para dibujarse completa)
                if (!isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(PanAppPrimary)
                            .border(2.dp, if (isDarkMode) Color.Black else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = localizedText(TextR.string.pan_photo_change),
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

            // NOMBRE (Editable)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(appViewModel.getString("full_name")) },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                ))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CORREO (BLOQUEADO)
            OutlinedTextField(
                colors = contrastFieldColors(androidx.compose.material3.OutlinedTextFieldDefaults.colors()),
                value = email,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text(appViewModel.getString("email_label")) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.Lock, contentDescription = localizedText(TextR.string.pan_blocked)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // TELÉFONO (Editable)
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text(appViewModel.getString("phone_label")) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                ))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✨ NUEVO: Campo Dirección de Envío
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text(appViewModel.getString("delivery_address")) },
                placeholder = { Text(appViewModel.getString("address_placeholder")) },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = contrastFieldColors(OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                ))
            )

            // ✨ NUEVO: Gestión de Direcciones Guardadas
            if (appViewModel.userAddressesList.isNotEmpty()) {
                Text(
                    appViewModel.getString("saved_addresses"),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                appViewModel.userAddressesList.forEach { addr ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF1E1E1E) else Color.White),
                        border = BorderStroke(1.dp, if (addr == address) PanAppPrimary else Color.Transparent)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).clickable { address = addr },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Home, contentDescription = null, tint = if (addr == address) PanAppPrimary else contrastTextColor(Color.Gray, secondary = true))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(addr, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = contrastTextColor(Color.Black, darkBackground = isDarkMode))
                            if (addr == address) {
                                Icon(Icons.Default.Check, contentDescription = localizedText(TextR.string.pan_selected), tint = contrastAccentColor(PanAppPrimary), modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateToAddresses,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = contrastAccentColor(PanAppPrimary))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    appViewModel.getString("manage_addresses"),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = contrastTextColor(Color.Gray, secondary = true))
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onNavigateToPayments,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = contrastAccentColor(PanAppPrimary))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    appViewModel.getString("payment_methods"),
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = contrastTextColor(Color.Black, darkBackground = isDarkMode)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = contrastTextColor(Color.Gray, secondary = true))
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN GUARDAR CAMBIOS
            val msgPleaseFill = appViewModel.getString("please_fill_address")
            Button(
                onClick = {
                    if (name.trim().isEmpty() || address.trim().isEmpty()) {
                        Toast.makeText(context, msgPleaseFill, Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        appViewModel.updateProfileData(name, phone, address) {
                            isSaving = false
                            Toast.makeText(context, localizedText(TextR.string.pan_profile_saved), Toast.LENGTH_SHORT).show()
                            onNavigateBack()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(appViewModel.getString("save_changes"), modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÓN CERRAR SESIÓN SEGURO
            OutlinedButton(
                onClick = {
                    appViewModel.limpiarDatosDeSesion {
                        Toast.makeText(context, localizedText(TextR.string.pan_logout_success), Toast.LENGTH_SHORT).show()
                        onLogoutClick()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = contrastErrorColor(Color.Red))
                Spacer(modifier = Modifier.width(8.dp))
                Text(appViewModel.getString("logout"), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
