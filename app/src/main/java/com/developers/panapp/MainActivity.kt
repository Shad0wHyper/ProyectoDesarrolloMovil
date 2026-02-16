package com.developers.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

// El controlador de navegación se queda aquí (o podría ir en otro archivo también)
@Composable
fun AppNavigation() {
    var currentScreen by remember { mutableStateOf("login") }

    if (currentScreen == "login") {
        // Llama a la función que ahora vive en LoginScreen.kt
        LoginScreen(onNavigateToRegister = { currentScreen = "register" })
    } else if (currentScreen == "register") {
        // Llama a la función que ahora vive en RegisterScreen.kt
        RegisterScreen(onNavigateToLogin = { currentScreen = "login" })
    }
}