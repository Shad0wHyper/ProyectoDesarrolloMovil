package com.developers.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.developers.panapp.ui.theme.PanAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
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
    var currentScreen by rememberSaveable { mutableStateOf("splash") }
    var termsSource by rememberSaveable { mutableStateOf("login") }

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            if (targetState == "splash") {
                EnterTransition.None togetherWith ExitTransition.None
            } else {
                fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f) togetherWith
                fadeOut(animationSpec = tween(500))
            }
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            "splash" -> SplashScreen(
                onNextScreen = { currentScreen = "login" }
            )
            "login" -> LoginScreen(
                onNavigateToRegister = { currentScreen = "register" },
                onNavigateToTerms = { 
                    termsSource = "login"
                    currentScreen = "terms" 
                },
                onNavigateToForgotPassword = { currentScreen = "forgot_password" }
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
            "forgot_password" -> ForgotPasswordScreen(
                onNavigateBack = { currentScreen = "login" }
            )
        }
    }
}