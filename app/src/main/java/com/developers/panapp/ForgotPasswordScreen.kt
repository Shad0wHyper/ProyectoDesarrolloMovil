package com.developers.panapp

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.panapp.ui.theme.PanAppPeach
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ForgotPasswordScreen(onNavigateBack: () -> Unit) {
    // Manejo del botón atrás del sistema
    BackHandler {
        onNavigateBack()
    }

    var correo by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Estados para el Modal Dinámico
    var showModal by rememberSaveable { mutableStateOf(false) }
    var isSuccess by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val scrollState = rememberScrollState()

    // ✨ Capturamos la plantilla para inyectar errores (Lint safe)
    val errorNetworkTemplate = stringResource(id = R.string.common_error_prefix)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.common_back),
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.forgot_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Logo area
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(15.dp))
                            .background(PanAppPeach),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.icon),
                            contentDescription = stringResource(id = R.string.common_logo),
                            modifier = Modifier.size(50.dp),
                            tint = Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = stringResource(id = R.string.forgot_main_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = stringResource(id = R.string.forgot_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Correo Input
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 30.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.forgot_email_label),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = correo,
                        onValueChange = { correo = it },
                        placeholder = { Text(stringResource(id = R.string.forgot_email_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        enabled = !showModal
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(40.dp))

                // Restablecer Cuenta Button
                if (!showModal) {
                    Button(
                        onClick = {
                            if (correo.isNotEmpty()) {
                                isLoading = true
                                // Verificamos si existe el usuario
                                db.collection("usuarios").whereEqualTo("correo", correo.trim()).get()
                                    .addOnSuccessListener { documents ->
                                        if (documents.isEmpty) {
                                            isLoading = false
                                            isSuccess = false
                                            showModal = true
                                        } else {
                                            auth.sendPasswordResetEmail(correo.trim())
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    isSuccess = true
                                                    showModal = true
                                                }
                                                .addOnFailureListener { error ->
                                                    isLoading = false
                                                    val finalError = String.format(errorNetworkTemplate, error.localizedMessage ?: "")
                                                    Toast.makeText(context, finalError, Toast.LENGTH_LONG).show()
                                                }
                                        }
                                    }
                                    .addOnFailureListener {
                                        isLoading = false
                                        Toast.makeText(context, R.string.forgot_toast_error_db, Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(context, R.string.forgot_toast_error_empty, Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 30.dp)
                            .height(56.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isLoading) stringResource(id = R.string.forgot_loading) else stringResource(id = R.string.forgot_button),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            if (!isLoading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            }

            // MODAL DINÁMICO CON ANIMACIÓN
            AnimatedVisibility(
                visible = showModal,
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { /* Evita clics detrás del modal */ },
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.6f)
                            .animateEnterExit(
                                enter = slideInVertically(
                                    initialOffsetY = { it },
                                    animationSpec = tween(durationMillis = 400)
                                ),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(durationMillis = 400)
                                )
                            ),
                        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(30.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Modal handle
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = if (isSuccess) stringResource(id = R.string.forgot_success_title) else stringResource(id = R.string.forgot_error_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(48.dp))

                            // Success/Error Icon
                            val iconColor = if (isSuccess) Color(0xFF00C4B4) else Color(0xFFE57373)
                            Box(contentAlignment = Alignment.Center) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(CircleShape)
                                        .background(iconColor.copy(alpha = 0.1f))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(iconColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSuccess) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(45.dp),
                                        tint = if (isSuccess) Color.Black else Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = if (isSuccess) stringResource(id = R.string.forgot_success_subtitle) else stringResource(id = R.string.forgot_error_subtitle),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(32.dp))

                            Text(
                                text = if (isSuccess) stringResource(id = R.string.forgot_success_message) else stringResource(id = R.string.forgot_error_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = {
                                    if (isSuccess) {
                                        onNavigateBack()
                                    } else {
                                        showModal = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                shape = RoundedCornerShape(15.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSuccess) MaterialTheme.colorScheme.primary else iconColor
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = if (isSuccess) stringResource(id = R.string.forgot_success_button) else stringResource(id = R.string.forgot_error_button),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    if (isSuccess) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}