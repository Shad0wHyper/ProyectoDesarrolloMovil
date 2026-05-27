package com.developers.client

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
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
    onNavigateToPayments: () -> Unit
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
                            Toast.makeText(context, "Foto actualizada", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener {
                        isUploadingImage = false
                        Toast.makeText(context, "Error al subir la foto", Toast.LENGTH_SHORT).show()
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
                    titleContentColor = if (isDarkMode) Color.White else Color.Black,
                    navigationIconContentColor = if (isDarkMode) Color.White else Color.Black
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
                    .size(130.dp) // ✨ Incrementamos ligeramente para evitar recortes
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFEEEEEE))
                    .clickable {
                        if (!isUploadingImage && appViewModel.currentUserId != "INVITADO") {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } else if (appViewModel.currentUserId == "INVITADO") {
                            Toast.makeText(context, "Inicia sesión para subir una foto", Toast.LENGTH_SHORT).show()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (appViewModel.userImageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(appViewModel.userImageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto de perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(65.dp), tint = Color.Gray)
                }

                if (isUploadingImage) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp)
                    }
                } else {
                    // ✨ CORRECCIÓN ÍCONO CÁMARA: Ajustamos posición y agregamos padding para evitar cortes
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-4).dp, y = (-4).dp)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PanAppPrimary)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CORREO (BLOQUEADO)
            OutlinedTextField(
                value = email,
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text(appViewModel.getString("email_label")) },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                trailingIcon = { Icon(Icons.Default.Lock, contentDescription = "Bloqueado") },
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
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ✨ NUEVO: Campo Dirección de Envío
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Dirección de Envío") },
                placeholder = { Text("Calle, Número, Colonia...") },
                leadingIcon = { Icon(Icons.Default.Home, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PanAppPrimary,
                    focusedLabelColor = PanAppPrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedButton(
                onClick = onNavigateToPayments,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (isDarkMode) Color.DarkGray else Color.LightGray)
            ) {
                Icon(Icons.Default.Payment, contentDescription = null, tint = PanAppPrimary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Métodos de Pago",
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = if (isDarkMode) Color.White else Color.Black
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.Gray)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN GUARDAR CAMBIOS
            Button(
                onClick = {
                    if (name.trim().isEmpty() || address.trim().isEmpty()) {
                        Toast.makeText(context, "Por favor llena tu nombre y dirección", Toast.LENGTH_SHORT).show()
                    } else {
                        isSaving = true
                        appViewModel.updateProfileData(name, phone, address) {
                            isSaving = false
                            Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
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
        }
    }
}