package com.developers.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.developers.panapp.ui.theme.PanAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PanAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    var currentScreen by rememberSaveable { mutableStateOf("login") }
    var termsSource by rememberSaveable { mutableStateOf("login") }

    when (currentScreen) {
        "login" -> LoginScreen(
            onNavigateToRegister = { currentScreen = "register" },
            onNavigateToTerms = { 
                termsSource = "login"
                currentScreen = "terms" 
            }
        )
        "register" -> RegisterScreen(
            onNavigateToLogin = { currentScreen = "login" },
            onNavigateToTerms = { 
                termsSource = "register"
                currentScreen = "terms" 
            }
        )
        "terms" -> TermsScreen(
            onNavigateBack = { currentScreen = termsSource }
        )
    }
}