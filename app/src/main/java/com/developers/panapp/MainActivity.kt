package com.developers.panapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
    // El "Motor" que controla los viajes entre pantallas
    val navController = rememberNavController()

    // NavHost es el mapa de nuestra aplicación
    NavHost(
        navController = navController,
        startDestination = "splash",
        enterTransition = { fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) }, // Animación al regresar
        popExitTransition = { fadeOut(animationSpec = tween(500)) }  // Animación al regresar
    ) {

        // 1. Pantalla Splash
        composable(
            route = "splash",
            // El splash no necesita animación de entrada porque es lo primero que carga
            enterTransition = { fadeIn(animationSpec = tween(0)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            SplashScreen(
                onNextScreen = {
                    // Navega al login Y destruye el splash para no poder volver a él
                    navController.navigate("login") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            )
        }

        // 2. Pantalla Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToTerms = { navController.navigate("terms") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") }
            )
        }

        // 3. Pantalla Registro
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() }, // Solo destruye esta vista y vuelve al login
                onNavigateToTerms = { navController.navigate("terms") }
            )
        }

        // 4. Pantalla Términos y Condiciones
        composable("terms") {
            TermsScreen(
                // popBackStack() vuelve automáticamente a la pantalla anterior (Login o Registro)
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // 5. Pantalla Olvidé mi Contraseña
        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}