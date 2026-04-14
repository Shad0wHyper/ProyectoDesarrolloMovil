package com.developers.panapp

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoginScreen(onNavigateToRegister: () -> Unit) {
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Bienvenido a PanApp", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

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
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Cargando..." else "Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister, modifier = Modifier.fillMaxWidth()) {
            Text("¿No tienes cuenta? Regístrate aquí")
        }
    }
}