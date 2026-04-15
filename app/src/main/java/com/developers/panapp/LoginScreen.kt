package com.developers.panapp

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.panapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit, onNavigateToTerms: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo area
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF3F3FF)), 
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFFFB38E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(50.dp),
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "¡Bienvenido de nuevo!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = PanAppTextPrimary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Huele a pan recién horneado...",
                style = MaterialTheme.typography.bodyMedium,
                color = PanAppTextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Correo o Teléfono
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Correo o Teléfono",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = PanAppTextPrimary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = { Text("ejemplo@correo.com", color = PanAppTextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Email,
                            contentDescription = null,
                            tint = PanAppTextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = PanAppBorder,
                        focusedBorderColor = PanAppPrimary
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contraseña
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contraseña",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PanAppTextPrimary
                    )
                    Text(
                        text = "¿Olvidaste tu clave?",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PanAppPrimary,
                        modifier = Modifier.clickable { /* TODO: Forgot password */ }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("••••••••", color = PanAppTextSecondary) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = PanAppTextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = PanAppBorder,
                        focusedBorderColor = PanAppPrimary
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Iniciar sesión button
            Button(
                onClick = {
                    if (correo.isNotEmpty() && password.isNotEmpty()) {
                        isLoading = true
                        auth.signInWithEmailAndPassword(correo.trim(), password.trim())
                            .addOnSuccessListener { result ->
                                val uid = result.user?.uid
                                if (uid != null) {
                                    db.collection("usuarios").document(uid).get()
                                        .addOnSuccessListener { documento ->
                                            isLoading = false
                                            if (documento.exists()) {
                                                val rol = documento.getString("rol") ?: "sin_rol"
                                                when (rol) {
                                                    "admin" -> Toast.makeText(context, "🚀 BIENVENIDO ADMIN", Toast.LENGTH_LONG).show()
                                                    "cliente" -> Toast.makeText(context, "🥖 BIENVENIDO CLIENTE", Toast.LENGTH_LONG).show()
                                                    "empleado" -> Toast.makeText(context, "📷 BIENVENIDO EMPLEADO", Toast.LENGTH_LONG).show()
                                                    else -> Toast.makeText(context, "Rol desconocido", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, "Usuario sin rol en la BD", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { isLoading = false }
                                }
                            }
                            .addOnFailureListener { error ->
                                isLoading = false
                                Toast.makeText(context, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, "Llena todos los campos", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLoading) "Cargando..." else "Iniciar sesión",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (!isLoading) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = PanAppBorder)
                Text(
                    text = " O TAMBIÉN PUEDES ",
                    style = MaterialTheme.typography.labelSmall,
                    color = PanAppTextSecondary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = PanAppBorder)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Crear una cuenta nueva button
            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, PanAppTextPrimary)
            ) {
                Text(
                    text = "Crear una cuenta nueva",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = PanAppTextPrimary
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Footer text
            val annotatedString = buildAnnotatedString {
                append("Al continuar, aceptas nuestros ")
                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                withStyle(style = SpanStyle(color = PanAppPrimary, fontWeight = FontWeight.Bold)) {
                    append("Términos de Servicio")
                }
                pop()
                append(" y ")
                pushStringAnnotation(tag = "POLICY", annotation = "policy")
                withStyle(style = SpanStyle(color = PanAppPrimary, fontWeight = FontWeight.Bold)) {
                    append("Política de Privacidad")
                }
                pop()
                append(".")
            }

            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = PanAppTextSecondary
                ),
                onClick = { offset ->
                    annotatedString.getStringAnnotations(tag = "TERMS", start = offset, end = offset).firstOrNull()?.let {
                        onNavigateToTerms()
                    }
                    annotatedString.getStringAnnotations(tag = "POLICY", start = offset, end = offset).firstOrNull()?.let {
                        onNavigateToTerms()
                    }
                },
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}