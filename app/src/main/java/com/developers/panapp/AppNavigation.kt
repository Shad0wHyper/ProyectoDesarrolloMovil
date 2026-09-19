package com.developers.panapp

import androidx.compose.animation.core.tween
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.developers.client.AppViewModel
import com.developers.client.ui.ClientMainScreen
import com.developers.employee.EmployeeViewModel
import com.developers.employee.EmployeeMainScreen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current

    // Instancia global de Auth
    val auth = remember { FirebaseAuth.getInstance() }

    // Función de Logout Global Segura (Regla 4)
    val logoutUser = {
        Toast.makeText(context, "Sesión cerrada con éxito", Toast.LENGTH_SHORT).show()
        // 1. Cierra sesión en Firebase
        auth.signOut()

        // 2. Cierra sesión en Google (si aplica)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)
        googleSignInClient.signOut()

        // 3. Navega a Login limpiando absolutamente todo el backstack
        navController.navigate("login") {
            popUpTo(navController.graph.id) { inclusive = true } // Destruye todo el backstack
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "splash", // Regla 1: Splash es el Rey
        enterTransition = { fadeIn(animationSpec = tween(500)) + scaleIn(initialScale = 0.9f) },
        exitTransition = { fadeOut(animationSpec = tween(500)) },
        popEnterTransition = { fadeIn(animationSpec = tween(500)) },
        popExitTransition = { fadeOut(animationSpec = tween(500)) }
    ) {
        
        // 1. Splash Screen
        composable(
            route = "splash",
            enterTransition = { fadeIn(animationSpec = tween(0)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            SplashScreen(
                onNextScreen = {
                    val currentUser = auth.currentUser
                    if (currentUser == null) {
                        navController.navigate("login") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        // Verificar el rol del usuario en Firestore antes de enviarlo al cliente
                        val db = FirebaseFirestore.getInstance()
                        db.collection("usuarios").document(currentUser.uid).get()
                            .addOnSuccessListener { doc ->
                                if (doc.exists()) {
                                    val rol = doc.getString("rol") ?: "sin_rol"
                                    if (rol == "cliente") {
                                        navController.navigate("client_graph") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else if (rol == "empleado") {
                                        navController.navigate("employee_graph") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    } else {
                                        // Si es de otro rol, lo mandamos al login para que la lógica de LoginScreen lo derive
                                        navController.navigate("login") {
                                            popUpTo("splash") { inclusive = true }
                                        }
                                    }
                                } else {
                                    auth.signOut()
                                    navController.navigate("login") {
                                        popUpTo("splash") { inclusive = true }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                auth.signOut()
                                navController.navigate("login") {
                                    popUpTo("splash") { inclusive = true }
                                }
                            }
                    }
                }
            )
        }

        // 2. Pantalla Login
        composable("login") {
            LoginScreen(
                onNavigateToRegister = { navController.navigate("register") },
                onNavigateToTerms = { navController.navigate("terms") },
                onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                onLoginSuccess = {
                    navController.navigate("client_graph") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        // 3. Ruta Client Graph
        composable("client_graph") {
            // Instanciamos el ViewModel requerido por el módulo cliente
            val clientViewModel: AppViewModel = viewModel()
            val context = LocalContext.current

            // ✨ 1. Aplicar el tema oscuro automáticamente
            val systemDarkTheme = isSystemInDarkTheme()
            LaunchedEffect(systemDarkTheme) {
                clientViewModel.isDarkMode = systemDarkTheme
            }
            
            // ✨ 1.5 Launcher para permisos de notificación de Android 13+
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    clientViewModel.registrarTokenFCM()
                }
            }

            // ✨ 2. Cargar los datos del usuario logueado en el ViewModel y pedir permisos
            LaunchedEffect(Unit) {
                val user = auth.currentUser
                if (user != null) {
                    clientViewModel.setSessionUser(user.uid, user.email ?: "Sin correo")
                }
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        clientViewModel.registrarTokenFCM()
                    }
                } else {
                    clientViewModel.registrarTokenFCM()
                }
            }

            ClientMainScreen(
                appViewModel = clientViewModel,
                onLogoutClick = { logoutUser() }
            )
        }

        // Pantallas Secundarias de Autenticación
        composable("register") {
            RegisterScreen(
                onNavigateToLogin = { navController.popBackStack() },
                onNavigateToTerms = { navController.navigate("terms") }
            )
        }

        composable("terms") {
            TermsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable("forgot_password") {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
