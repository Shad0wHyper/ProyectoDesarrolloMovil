package com.developers.panapp

import com.developers.core.ui.PanAppMotion
import com.developers.core.ui.panAppRevealIn
import com.developers.core.ui.panAppRevealOut

import com.developers.core.text.localizedText
import com.developers.core.R as TextR

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.developers.client.AppViewModel
import com.developers.client.ui.ClientMainScreen
import com.developers.employee.EmployeeViewModel
import com.developers.employee.EmployeeMainScreen
import com.developers.admin.AdminViewModel
import com.developers.admin.AdminMainScreen
import com.developers.core.ui.PanAppRole
import com.developers.core.ui.panAppStatusBarColor
import com.developers.core.ui.panAppRoleBackdrop
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val role = when (route) {
        "admin_graph" -> PanAppRole.Admin
        "client_graph" -> PanAppRole.Client
        "employee_graph" -> PanAppRole.Employee
        else -> null
    }
    val context = LocalContext.current

    // Instancia global de Auth
    val auth = remember { FirebaseAuth.getInstance() }

    // Función de Logout Global Segura (Regla 4)
    val logoutUser = {
        Toast.makeText(context, localizedText(TextR.string.pan_logout_success_alt), Toast.LENGTH_SHORT).show()
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

    val backdrop = when {
        route == "login" -> Modifier.loginAurora()
        role == PanAppRole.Client -> Modifier.background(panAppRoleBackdrop(PanAppRole.Client))
        else -> Modifier.background(panAppStatusBarColor(role))
    }

    NavHost(
        navController = navController,
        modifier = Modifier.fillMaxSize().then(backdrop).statusBarsPadding(),
        startDestination = "splash", // Regla 1: Splash es el Rey
        enterTransition = { panAppRevealIn() },
        exitTransition = { panAppRevealOut() },
        popEnterTransition = { panAppRevealIn() },
        popExitTransition = { panAppRevealOut() }
    ) {
        
        // 1. Splash Screen
        composable(
            route = "splash",
            enterTransition = { fadeIn(animationSpec = tween(0)) },
            exitTransition = { fadeOut(animationSpec = tween(PanAppMotion.ExitMillis, easing = PanAppMotion.easing)) }
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
                                    } else if (rol == "admin") {
                                        navController.navigate("admin_graph") {
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
                },
                onEmployeeLoginSuccess = {
                    navController.navigate("employee_graph") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onAdminLoginSuccess = {
                    navController.navigate("admin_graph") {
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
                    clientViewModel.setSessionUser(user.uid, user.email ?: localizedText(TextR.string.pan_no_email_lower))
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

        // 4. Ruta Employee Graph
        composable("employee_graph") {
            val employeeViewModel: EmployeeViewModel = viewModel()
            
            // Cargar los datos del empleado logueado en el ViewModel
            LaunchedEffect(Unit) {
                val user = auth.currentUser
                if (user != null) {
                    employeeViewModel.setSessionUser(user.uid, user.email ?: localizedText(TextR.string.pan_no_email_lower))
                }
            }

            EmployeeMainScreen(
                viewModel = employeeViewModel,
                onLogoutClick = { logoutUser() }
            )
        }

        // 5. Ruta Admin Graph
        composable("admin_graph") {
            val adminViewModel: AdminViewModel = viewModel()
            
            LaunchedEffect(Unit) {
                val user = auth.currentUser
                if (user != null) {
                    adminViewModel.setSessionUser(user.uid, user.email ?: localizedText(TextR.string.pan_no_email_lower))
                }
            }

            AdminMainScreen(
                viewModel = adminViewModel,
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
