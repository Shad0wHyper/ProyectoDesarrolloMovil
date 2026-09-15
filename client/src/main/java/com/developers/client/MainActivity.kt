package com.developers.client

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.developers.client.ui.theme.PanAppClientTheme
import com.developers.client.ui.theme.PanAppPrimary
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        super.onCreate(savedInstanceState)

        // ✨ 1. ATRAPAMOS LOS DATOS SECRETOS DEL INTENT
        val userIdFromIntent = intent.getStringExtra("USER_ID") ?: "INVITADO"
        val userEmailFromIntent = intent.getStringExtra("USER_EMAIL") ?: "Sin correo"

        PaymentConfiguration.init(
            applicationContext,
            PaymentConfig.PUBLISHABLE_KEY
        )

        setContent {
            val appViewModel: AppViewModel = viewModel()
            val context = LocalContext.current
            val systemDarkTheme = isSystemInDarkTheme()

            // ✨ Modo oscuro 100% automático derivado del tema del sistema Android
            LaunchedEffect(systemDarkTheme) {
                appViewModel.isDarkMode = systemDarkTheme
            }

            // Launcher para solicitar el permiso de notificaciones en Android 13+ (API 33+)
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { isGranted ->
                if (isGranted) {
                    appViewModel.registrarTokenFCM()
                }
            }

            // ✨ 2. FORZAMOS A LA APP A RECONOCER AL USUARIO Y SOLICITAR PERMISOS DE NOTIFICACIÓN
            LaunchedEffect(Unit) {
                appViewModel.setSessionUser(userIdFromIntent, userEmailFromIntent)

                // Verificación y solicitud del permiso POST_NOTIFICATIONS en Android 13+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        appViewModel.registrarTokenFCM()
                    }
                } else {
                    appViewModel.registrarTokenFCM()
                }
            }

            PanAppClientTheme(darkTheme = appViewModel.isDarkMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ClientAppNavigation(appViewModel)
                }
            }
        }
    }
}

@Composable
fun ClientAppNavigation(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val bottomNavRoutes = remember { listOf("home", "orders") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: "home"
    val showBottomBar = bottomNavRoutes.contains(currentRoute)

    // ✨ 1. SCROLL INFINITO (SUPERPOSICIÓN REAL CON BOX)
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = if (appViewModel.isDarkMode) Color(0xFF121212) else Color.White,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(if (appViewModel.isDarkMode) Color(0xFF121212) else Color.White),
                enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
                popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
                popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
            ) {
                composable("home") {
                    HomeScreen(
                        appViewModel = appViewModel,
                        onNavigateToCart = { navController.navigate("cart") },
                        onNavigateToOrders = { navController.navigate("orders") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToProfile = { navController.navigate("profile") }
                    )
                }
                composable("cart") {
                    CartScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onPaymentSuccess = {
                            navController.navigate("payment_success") {
                                popUpTo("cart") { inclusive = true }
                            }
                        }
                    )
                }
                composable("payment_success") {
                    SuccessScreen(
                        appViewModel = appViewModel,
                        onNavigateHome = {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onNavigateToOrders = {
                            navController.navigate("orders") {
                                popUpTo("home")
                            }
                        }
                    )
                }
                composable("orders") {
                    OrdersScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToCart = {
                            navController.navigate("cart") {
                                popUpTo("home")
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onNavigateToNotifications = { navController.navigate("notifications") }
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPayments = { navController.navigate("payment_methods") }
                    )
                }
                composable("payment_methods") {
                    PaymentMethodsScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddCard = { navController.navigate("add_card") }
                    )
                }
                composable("add_card") {
                    AddCardScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("notifications") {
                    NotificationsScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // BARRA FLOTANTE EN CAPA SUPERIOR
        if (showBottomBar) {
            GooglePhotosFloatingBottomBar(
                currentRoute = currentRoute,
                onNavigate = { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                isDarkMode = appViewModel.isDarkMode,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

// ✨ BARRA DE NAVEGACIÓN FLOTANTE REFACTORIZADA ESTILO GOOGLE PHOTOS & PANAPP
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GooglePhotosFloatingBottomBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = remember {
        listOf(
            Triple("home", "Inicio", Icons.Default.Home),
            Triple("orders", "Pedidos", Icons.Default.Receipt)
        )
    }

    // ✨ 4. PALETA DE COLORES: Blanco en Modo Claro, Gris Oscuro (0xFF2C2C2C) en Modo Oscuro
    val containerBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White
    val searchIconColor = if (isDarkMode) Color.White else Color(0xFF2C2C2C)

    // ✨ 2. CENTRADO Y 3. ALTURA/ESPACIADO (12.dp)
    Box(
        modifier = modifier
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Píldora Principal de Navegación (Centrada)
            Surface(
                shape = CircleShape,
                color = containerBackgroundColor,
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bottomNavItems.forEach { (route, title, icon) ->
                        val isSelected = currentRoute == route

                        FloatingNavItem(
                            title = title,
                            icon = icon,
                            isSelected = isSelected,
                            isDarkMode = isDarkMode,
                            onClick = { onNavigate(route) }
                        )
                    }
                }
            }

            // 2. Botón Circular Independiente de Búsqueda
            Surface(
                onClick = { /* TODO: Búsqueda */ },
                shape = CircleShape,
                color = containerBackgroundColor,
                shadowElevation = 8.dp,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Buscar",
                        tint = searchIconColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    // ✨ 4. PALETA DE COLORES Y ESTADOS
    val activeBackgroundColor = PanAppPrimary.copy(alpha = 0.15f)
    val contentColor = if (isSelected) PanAppPrimary else Color.Gray

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) activeBackgroundColor else Color.Transparent,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = PanAppPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
