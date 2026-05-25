package com.developers.client

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.developers.client.ui.theme.PanAppClientTheme
import com.stripe.android.PaymentConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializar Stripe
        PaymentConfiguration.init(
            applicationContext,
            PaymentConfig.PUBLISHABLE_KEY
        )

        setContent {
            val appViewModel: AppViewModel = viewModel()
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

    NavHost(navController = navController, startDestination = "home") {
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
