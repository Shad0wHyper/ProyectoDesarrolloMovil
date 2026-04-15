package com.developers.panapp

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.developers.panapp.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(onNavigateToLogin: () -> Unit, onNavigateToTerms: () -> Unit) {
    var nombre by rememberSaveable { mutableStateOf("") }
    var telefono by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }
    var confirmPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var acceptTerms by rememberSaveable { mutableStateOf(false) }
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
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onNavigateToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Regresar",
                        tint = PanAppTextPrimary
                    )
                }
                Text(
                    text = "PanApp",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = PanAppPrimary
                )
                Spacer(modifier = Modifier.width(48.dp)) // To center the title
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // Logo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFFFB38E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier.size(60.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Crea una cuenta",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = PanAppTextPrimary
                )

                Text(
                    text = "Introduce tus datos a continuación para unirte a la comunidad de PanApp.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PanAppTextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Campos de entrada
                RegistrationField(
                    label = "Nombre completo",
                    value = nombre,
                    onValueChange = { nombre = it },
                    placeholder = "Ej. Juan Pérez",
                    leadingIcon = Icons.Outlined.Person
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegistrationField(
                    label = "Correo electrónico",
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = "tu@ejemplo.com",
                    leadingIcon = Icons.Outlined.Email,
                    keyboardType = KeyboardType.Email
                )

                Spacer(modifier = Modifier.height(16.dp))

                RegistrationField(
                    label = "Teléfono (opcional)",
                    value = telefono,
                    onValueChange = { telefono = it },
                    placeholder = "+34 000 000 000",
                    leadingIcon = Icons.Outlined.Phone,
                    keyboardType = KeyboardType.Phone
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Password Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Contraseña",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PanAppTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("••••••••", color = PanAppTextSecondary) },
                        leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = PanAppTextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = PanAppTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = PanAppBorder,
                            focusedBorderColor = PanAppPrimary
                        ),
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true
                    )
                    Text(
                        text = "Mínimo 8 caracteres con números y símbolos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PanAppTextSecondary,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Confirm Password Field
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Confirmar contraseña",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = PanAppTextPrimary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        placeholder = { Text("••••••••", color = PanAppTextSecondary) },
                        leadingIcon = { Icon(Icons.Outlined.CheckCircle, null, tint = PanAppTextSecondary) },
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                Icon(
                                    imageVector = if (confirmPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = PanAppTextSecondary
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = PanAppBorder,
                            focusedBorderColor = PanAppPrimary
                        ),
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terms checkbox
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFF9F9F9))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = acceptTerms,
                        onCheckedChange = { acceptTerms = it },
                        colors = CheckboxDefaults.colors(checkedColor = PanAppPrimary)
                    )
                    
                    val annotatedTerms = buildAnnotatedString {
                        append("Acepto los ")
                        pushStringAnnotation(tag = "TERMS", annotation = "terms")
                        withStyle(style = SpanStyle(color = PanAppPrimary, fontWeight = FontWeight.Bold)) {
                            append("Términos de servicio")
                        }
                        pop()
                        append(" y la ")
                        pushStringAnnotation(tag = "POLICY", annotation = "policy")
                        withStyle(style = SpanStyle(color = PanAppPrimary, fontWeight = FontWeight.Bold)) {
                            append("Política de privacidad")
                        }
                        pop()
                        append(" de PanApp.")
                    }

                    ClickableText(
                        text = annotatedTerms,
                        style = MaterialTheme.typography.bodySmall.copy(color = PanAppTextSecondary),
                        onClick = { offset ->
                            annotatedTerms.getStringAnnotations(tag = "TERMS", start = offset, end = offset).firstOrNull()?.let {
                                onNavigateToTerms()
                            }
                            annotatedTerms.getStringAnnotations(tag = "POLICY", start = offset, end = offset).firstOrNull()?.let {
                                onNavigateToTerms()
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // Create Account Button
                Button(
                    onClick = {
                        if (nombre.isNotEmpty() && correo.isNotEmpty() && password.length >= 8 && password == confirmPassword && acceptTerms) {
                            isLoading = true
                            auth.createUserWithEmailAndPassword(correo.trim(), password.trim())
                                .addOnSuccessListener { result ->
                                    val uid = result.user?.uid
                                    if (uid != null) {
                                        val datosUsuario = hashMapOf(
                                            "nombre" to nombre.trim(),
                                            "telefono" to telefono.trim(),
                                            "correo" to correo.trim(),
                                            "rol" to "cliente"
                                        )
                                        db.collection("usuarios").document(uid).set(datosUsuario)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                Toast.makeText(context, "¡Cuenta creada con éxito!", Toast.LENGTH_LONG).show()
                                                onNavigateToLogin()
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                Toast.makeText(context, "Error al guardar en BD", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener { error ->
                                    isLoading = false
                                    Toast.makeText(context, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                                }
                        } else if (!acceptTerms) {
                            Toast.makeText(context, "Debes aceptar los términos", Toast.LENGTH_SHORT).show()
                        } else if (password != confirmPassword) {
                            Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Llena todo correctamente (Clave >= 8)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PanAppPrimary)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (isLoading) "Creando..." else "Crear cuenta",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isLoading) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.AddCircleOutline, contentDescription = null)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Inicia sesión link
                Row(
                    modifier = Modifier.padding(bottom = 32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "¿Ya tienes una cuenta? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = PanAppTextSecondary
                    )
                    Text(
                        text = "Inicia sesión",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PanAppPrimary,
                        modifier = Modifier.clickable { onNavigateToLogin() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrationField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = PanAppTextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = PanAppTextSecondary) },
            leadingIcon = { Icon(leadingIcon, null, tint = PanAppTextSecondary) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(15.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = PanAppBorder,
                focusedBorderColor = PanAppPrimary
            ),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true
        )
    }
}