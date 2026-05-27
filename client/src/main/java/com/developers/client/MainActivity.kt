package com.developers.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
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

            // ✨ 2. FORZAMOS A LA APP A RECONOCER AL USUARIO ANTES DE DIBUJAR NADA
            LaunchedEffect(Unit) {
                appViewModel.setSessionUser(userIdFromIntent, userEmailFromIntent)
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

    val bottomNavItems = listOf(
        Triple("home", "Inicio", Icons.Default.Home),
        Triple("orders", "Pedidos", Icons.Default.Receipt)
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val showBottomBar = bottomNavItems.any { it.first == currentDestination?.route }

            if (showBottomBar) {
                NavigationBar(
                    containerColor = if (appViewModel.isDarkMode) Color(0xFF1E1E1E) else Color.White,
                    contentColor = PanAppPrimary
                ) {
                    bottomNavItems.forEach { (route, title, icon) ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == route } == true

                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = title) },
                            label = { Text(title) },
                            selected = isSelected,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PanAppPrimary,
                                selectedTextColor = PanAppPrimary,
                                indicatorColor = PanAppPrimary.copy(alpha = 0.1f),
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
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
                        appViewModel.placeOrder {
                            navController.navigate("payment_success") {
                                popUpTo("cart") { inclusive = true }
                            }
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
}