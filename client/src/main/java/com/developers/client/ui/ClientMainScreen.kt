package com.developers.client.ui

import androidx.compose.animation.AnimatedVisibility
import com.developers.core.ui.PanAppFloatingTab
import com.developers.core.ui.panAppPressMotion
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.developers.core.ui.panAppScreenEnter
import com.developers.core.ui.panAppScreenExit
import com.developers.core.ui.panAppRevealIn
import com.developers.core.ui.panAppRevealOut

import com.developers.core.ui.panAppNavigationColor
import com.developers.core.ui.panAppGlassBorder


import com.developers.core.ui.contrastTextColor


import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.developers.client.*
import com.developers.core.ui.tabSlideDirection

@Composable
fun ClientMainScreen(appViewModel: AppViewModel, onLogoutClick: () -> Unit) {
    val navController = rememberNavController()
    val bottomNavRoutes = remember { listOf("home", "orders", "search") }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route ?: "home"
    val showBottomBar = bottomNavRoutes.contains(currentRoute)

    // ✨ 1. SCROLL INFINITO (SUPERPOSICIÓN REAL CON BOX)
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, 1)
                    panAppScreenEnter(direction)
                },
                exitTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, 1)
                    panAppScreenExit(direction)
                },
                popEnterTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, -1)
                    panAppScreenEnter(direction)
                },
                popExitTransition = {
                    val direction = tabSlideDirection(bottomNavRoutes, initialState.destination.route, targetState.destination.route, -1)
                    panAppScreenExit(direction)
                }
            ) {
                composable("home") {
                    HomeScreen(
                        appViewModel = appViewModel,
                        onNavigateToCart = { navController.navigate("cart") },
                        onNavigateToOrders = { navController.navigate("orders") },
                        onNavigateToSettings = { navController.navigate("settings") },
                        onNavigateToProfile = { navController.navigate("profile") },
                        onLogoutClick = onLogoutClick
                    )
                }
                composable("search") {
                    SearchScreen(
                        appViewModel = appViewModel,
                        onNavigateToCart = { navController.navigate("cart") }
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
                        onNavigateToNotifications = { navController.navigate("notifications") },
                        onLogoutClick = onLogoutClick
                    )
                }
                composable("profile") {
                    ProfileScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToPayments = { navController.navigate("payment_methods") },
                        onNavigateToAddresses = { navController.navigate("manage_addresses") },
                        onLogoutClick = onLogoutClick
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
                composable("manage_addresses") {
                    ManageAddressesScreen(
                        appViewModel = appViewModel,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }

        // BARRA FLOTANTE EN CAPA SUPERIOR
        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = panAppRevealIn(),
            exit = panAppRevealOut()
        ) {
            GooglePhotosFloatingBottomBar(
                appViewModel = appViewModel,
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
            )
        }

        CartAddedFeedback(
            additions = appViewModel.cartAdditions,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 0.dp)
                .padding(bottom = if (showBottomBar) 88.dp else 16.dp)
        )
    }
}

// ✨ BARRA DE NAVEGACIÓN FLOTANTE REFACTORIZADA ESTILO GOOGLE PHOTOS & PANAPP
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GooglePhotosFloatingBottomBar(
    appViewModel: AppViewModel,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navHome = appViewModel.getString("nav_home")
    val navOrders = appViewModel.getString("nav_orders")
    
    val bottomNavItems = remember(navHome, navOrders) {
        listOf(
            Triple("home", navHome, Icons.Default.Home),
            Triple("orders", navOrders, Icons.Default.Receipt)
        )
    }

    // ✨ 4. PALETA DE COLORES: Blanco en Modo Claro, Gris Oscuro (0xFF2C2C2C) en Modo Oscuro
    val containerBackgroundColor = panAppNavigationColor()
    val colors = MaterialTheme.colorScheme
    val searchSelected = currentRoute == "search"
    val searchInteractions = remember { MutableInteractionSource() }

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
                color = Color.Transparent,
                modifier = Modifier.background(containerBackgroundColor, CircleShape),
                border = panAppGlassBorder()
            ) {
                Row(
                    modifier = Modifier.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    bottomNavItems.forEach { (route, title, icon) ->
                        val isSelected = currentRoute == route

                        PanAppFloatingTab(
                            title = title,
                            icon = icon,
                            isSelected = isSelected,
                            onClick = { onNavigate(route) }
                        )
                    }
                }
            }

            // 2. Botón Circular Independiente de Búsqueda
            Surface(
                onClick = { onNavigate("search") },
                shape = CircleShape,
                color = if (searchSelected) colors.primary else Color.Transparent,
                contentColor = if (searchSelected) colors.onPrimary else colors.onSurfaceVariant,
                interactionSource = searchInteractions,
                border = panAppGlassBorder(),
                modifier = Modifier.size(52.dp).panAppPressMotion(searchInteractions)
                    .background(containerBackgroundColor, CircleShape)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = appViewModel.getString("nav_search"),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}


