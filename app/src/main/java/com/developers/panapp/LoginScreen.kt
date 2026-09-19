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
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToTerms: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit = {}
) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    val errorTemplate = stringResource(id = R.string.common_error_prefix)

    // Configuración de Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                isLoading = true
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential)
                    .addOnSuccessListener { authResult ->
                        val uid = authResult.user?.uid
                        if (uid != null) {
                            db.collection("usuarios").document(uid).get()
                                .addOnSuccessListener { documento ->
                                    isLoading = false
                                    if (documento.exists()) {
                                        val rol = documento.getString("rol") ?: "sin_rol"
                                        val targetPackage = when (rol) {
                                            "admin" -> "com.developers.admin"
                                            "cliente" -> "com.developers.client"
                                            "empleado" -> "com.developers.employee"
                                            else -> ""
                                        }

                                                if (targetPackage == "com.developers.client") {
                                                    onLoginSuccess()
                                                } else if (targetPackage.isNotEmpty()) {
                                                    val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)
                                                    if (intent != null) {
                                                        intent.putExtra("USER_ID", auth.currentUser?.uid)
                                                        intent.putExtra("USER_EMAIL", auth.currentUser?.email)
                                                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        context.startActivity(intent)
                                                    } else {
                                                        Toast.makeText(context, "La app de $rol no está instalada en este dispositivo", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Rol no reconocido", Toast.LENGTH_SHORT).show()
                                                }
                                    } else {
                                        // Si no existe el usuario en firestore, podemos crearlo o dar error
                                        // Aquí asumo que no tiene rol, o puedes guardarlo como cliente por defecto.
                                        Toast.makeText(context, "No tienes rol asignado. Contacta a soporte.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    isLoading = false
                                    Toast.makeText(context, "Error al obtener datos de Firestore", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener { error ->
                        isLoading = false
                        Toast.makeText(context, "Autenticación fallida: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                    }
            }
        } catch (e: ApiException) {
            Toast.makeText(context, "Google Sign-In falló", Toast.LENGTH_SHORT).show()
        }
    }

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

            // Logo
            val isDarkMode = androidx.compose.foundation.isSystemInDarkTheme()
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(85.dp) // ✨ Cuadro interno de color más grande
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isDarkMode) PanAppIconBackgroundDark else PanAppPeach),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.newp),
                        contentDescription = stringResource(id = R.string.common_logo),
                        modifier = Modifier.size(65.dp), // ✨ Logo 30% más grande
                        tint = Color.Unspecified
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(id = R.string.login_welcome_back),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(id = R.string.login_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Campo Correo
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.login_email_or_phone),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = correo,
                    onValueChange = { correo = it },
                    placeholder = { Text(stringResource(id = R.string.login_email_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Email, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Campo Contraseña
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(id = R.string.login_password),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(id = R.string.login_forgot_password),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToForgotPassword() }
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text(stringResource(id = R.string.login_password_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Outlined.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                contentDescription = if (isPasswordVisible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(15.dp),
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true
                )

            }

            Spacer(modifier = Modifier.height(40.dp))

            // Botón Iniciar Sesión
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

                                                val targetPackage = when (rol) {
                                                    "admin" -> "com.developers.admin"
                                                    "cliente" -> "com.developers.client"
                                                    "empleado" -> "com.developers.employee"
                                                    else -> ""
                                                }

                                                if (targetPackage == "com.developers.client") {
                                                    onLoginSuccess()
                                                } else if (targetPackage.isNotEmpty()) {
                                                    val intent = context.packageManager.getLaunchIntentForPackage(targetPackage)

                                                    if (intent != null) {
                                                        intent.putExtra("USER_ID", auth.currentUser?.uid)
                                                        intent.putExtra("USER_EMAIL", auth.currentUser?.email)
                                                        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        context.startActivity(intent)
                                                    } else {
                                                        Toast.makeText(context, "La app de $rol no está instalada en este dispositivo", Toast.LENGTH_LONG).show()
                                                    }
                                                } else {
                                                    Toast.makeText(context, R.string.login_error_unknown_rol, Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                Toast.makeText(context, R.string.login_error_no_rol, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .addOnFailureListener { isLoading = false }
                                }
                            }
                            .addOnFailureListener { error ->
                                isLoading = false
                                val errorMessage = String.format(errorTemplate, error.localizedMessage ?: "")
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                    } else {
                        Toast.makeText(context, R.string.login_error_empty_fields, Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = !isLoading,
                shape = RoundedCornerShape(15.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isLoading) stringResource(id = R.string.login_loading) else stringResource(id = R.string.login_sign_in_button),
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
            Spacer(modifier = Modifier.height(32.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
                Text(
                    text = stringResource(id = R.string.login_divider_text),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Google Sign-In
            OutlinedButton(
                onClick = {
                    val signInIntent = googleSignInClient.signInIntent
                    launcher.launch(signInIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        tint = Color.Unspecified
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(id = R.string.login_google_button),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Botón Crear Cuenta
            OutlinedButton(
                onClick = onNavigateToRegister,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(15.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onBackground)
            ) {
                Text(
                    text = stringResource(id = R.string.login_create_account_button),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(60.dp))

            // Footer con ClickableText
            val termsPrefix = stringResource(id = R.string.login_terms_prefix)
            val termsLink = stringResource(id = R.string.login_terms_link)
            val termsMiddle = stringResource(id = R.string.login_terms_middle)
            val policyLink = stringResource(id = R.string.login_policy_link)
            val termsSuffix = stringResource(id = R.string.login_terms_suffix)

            val annotatedString = buildAnnotatedString {
                append(termsPrefix)
                append(" ")
                pushStringAnnotation(tag = "TERMS", annotation = "terms")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(termsLink)
                }
                pop()
                append(" ")
                append(termsMiddle)
                append(" ")
                pushStringAnnotation(tag = "POLICY", annotation = "policy")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append(policyLink)
                }
                pop()
                append(termsSuffix)
            }

            @Suppress("DEPRECATION")
            ClickableText(
                text = annotatedString,
                style = MaterialTheme.typography.bodySmall.copy(
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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