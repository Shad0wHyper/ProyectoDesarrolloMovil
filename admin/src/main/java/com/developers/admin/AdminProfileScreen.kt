package com.developers.admin

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.CameraAlt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminProfileScreen(viewModel: AdminViewModel, onBack: () -> Unit, onLogoutClick: () -> Unit) {
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val scrollState = rememberScrollState()

    val db = FirebaseFirestore.getInstance()
    val uid = viewModel.currentUserId

    // Variables de estado
    var phone by remember { mutableStateOf("") }
    var nombreEditable by remember { mutableStateOf(viewModel.userName) } // ✨ ESTADO PARA EL NOMBRE EDITABLE
    var isSaving by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var isUploadingImage by remember { mutableStateOf(false) }

    // Launcher para seleccionar la imagen
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null && uid != "INVITADO") {
                isUploadingImage = true
                val storageRef = FirebaseStorage.getInstance().reference
                    .child("perfiles/${uid}.jpg")

                storageRef.putFile(uri)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                            val newUrl = downloadUrl.toString()
                            db.collection("usuarios").document(uid).update("imageUrl", newUrl)
                                .addOnSuccessListener {
                                    viewModel.userImageUrl = newUrl
                                    isUploadingImage = false
                                    Toast.makeText(context, "Foto de perfil actualizada", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        isUploadingImage = false
                        Toast.makeText(context, "Error al subir la foto", Toast.LENGTH_SHORT).show()
                    }
            } else if (uid == "INVITADO") {
                Toast.makeText(context, "Debes iniciar sesión para subir fotos", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Cargar datos actuales desde Firestore
    LaunchedEffect(Unit) {
        if (uid != "INVITADO") {
            db.collection("usuarios").document(uid).get()
                .addOnSuccessListener { doc ->
                    phone = doc.getString("telefono") ?: ""
                    nombreEditable = doc.getString("nombre") ?: viewModel.userName
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkMode) Color.Black else Color.White
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
            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF6200EE))
                return@Column
            }

            // SECCIÓN FOTO DE PERFIL
            Box(
                modifier = Modifier
                    .size(130.dp)
                    .clickable {
                        if (!isUploadingImage) {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                // CÍRCULO PRINCIPAL DE LA FOTO
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(if (isDarkMode) Color(0xFF1E1E1E) else Color(0xFFEEEEEE)),
                    contentAlignment = Alignment.Center
                ) {
                    if (viewModel.userImageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(viewModel.userImageUrl)
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

                // BURBUJA DE LA CÁMARA
                if (!isUploadingImage) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6200EE))
                            .border(2.dp, if (isDarkMode) Color.Black else Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Cambiar foto",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // NOMBRE (Editable)
            OutlinedTextField(
                value = nombreEditable,
                onValueChange = { nombreEditable = it },
                label = { Text("Nombre Completo") },
                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6200EE),
                    focusedLabelColor = Color(0xFF6200EE)
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // CORREO (BLOQUEADO)
            OutlinedTextField(
                value = viewModel.userEmail.ifEmpty { "Sin Correo" },
                onValueChange = { },
                readOnly = true,
                enabled = false,
                label = { Text("Correo Electrónico") },
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
                label = { Text("Teléfono de Contacto") },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6200EE),
                    focusedLabelColor = Color(0xFF6200EE)
                )
            )

            Spacer(modifier = Modifier.height(40.dp))

            // BOTÓN GUARDAR CAMBIOS
            Button(
                onClick = {
                    if (uid != "INVITADO") {
                        isSaving = true
                        val updates = hashMapOf<String, Any>(
                            "telefono" to phone.trim(),
                            "nombre" to nombreEditable.trim()
                        )
                        db.collection("usuarios").document(uid).update(updates)
                            .addOnSuccessListener {
                                isSaving = false
                                viewModel.userName = nombreEditable.trim() // Actualiza la variable de sesión
                                Toast.makeText(context, "Perfil guardado con éxito", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                            .addOnFailureListener {
                                isSaving = false
                                Toast.makeText(context, "Error al guardar: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = !isSaving,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Guardar Cambios", modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // BOTÓN CERRAR SESIÓN
            OutlinedButton(
                onClick = {
                    viewModel.limpiarDatosDeSesion { onLogoutClick() }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cerrar Sesión", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
