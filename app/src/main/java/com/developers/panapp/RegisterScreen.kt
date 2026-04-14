package com.developers.panapp

import android.util.Log
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
fun RegisterScreen(onNavigateToLogin: () -> Unit) {
    var nombre by remember { mutableStateOf("") }
    var telefono by remember { mutableStateOf("") }
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
        Text(text = "Crear Cuenta", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre completo") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { telefono = it },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña (mín. 6 letras)") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (nombre.isNotEmpty() && telefono.isNotEmpty() && correo.isNotEmpty() && password.length >= 6) {
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
                                        Log.e("FIREBASE_REG", "Error al guardar datos: ${e.message}")
                                        Toast.makeText(context, "Error al guardar en BD", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                        .addOnFailureListener { error ->
                            isLoading = false
                            Toast.makeText(context, "Error: ${error.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                } else {
                    Toast.makeText(context, "Llena todo correctamente (Clave > 5)", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Text(if (isLoading) "Guardando..." else "Crear Cuenta")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onNavigateToLogin, modifier = Modifier.fillMaxWidth()) {
            Text("¿Ya tienes cuenta? Inicia sesión")
        }
    }
}