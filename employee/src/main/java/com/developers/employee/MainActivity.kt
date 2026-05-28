package com.developers.employee

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

val PrimaryBlue = Color(0xFF6B72E2)
val CardPink = Color(0xFFED5A85)
val OnlineGreen = Color(0xFF2ECA7F)
val OrangePrep = Color(0xFFE88A64)
val WhatsappGreen = Color(0xFF25D366)
val DarkPurpleText = Color(0xFF4A4E91)

val darkColors = darkColorScheme(background = Color(0xFF121212), surface = Color(0xFF1E1E1E), onSurface = Color.White, onBackground = Color.White, onSurfaceVariant = Color(0xFFAAAAAA))
val lightColors = lightColorScheme(background = Color(0xFFF5F6FA), surface = Color.White, onSurface = Color.DarkGray, onBackground = Color.Black, onSurfaceVariant = Color(0xFF7A869A))

enum class AppScreen { INICIO, ASISTENCIA, PEDIDOS, PROVEEDORES, PERFIL, LAUNCHING_WS, HISTORIAL }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userIdFromIntent = intent.getStringExtra("USER_ID") ?: "INVITADO"
        val userEmailFromIntent = intent.getStringExtra("USER_EMAIL") ?: "Sin correo"

        setContent {
            val viewModel: EmployeeViewModel = viewModel()
            LaunchedEffect(Unit) { viewModel.setSessionUser(userIdFromIntent, userEmailFromIntent) }
            MainAppNavigation(viewModel)
        }
    }
}

@Composable
fun MainAppNavigation(viewModel: EmployeeViewModel) {
    var isDarkTheme by rememberSaveable { mutableStateOf(false) }
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }

    BackHandler(enabled = currentScreen != AppScreen.INICIO) {
        currentScreen = when (currentScreen) {
            AppScreen.LAUNCHING_WS -> AppScreen.PEDIDOS
            AppScreen.HISTORIAL -> AppScreen.ASISTENCIA
            else -> AppScreen.INICIO
        }
    }

    MaterialTheme(colorScheme = if (isDarkTheme) darkColors else lightColors) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (currentScreen) {
                AppScreen.INICIO -> DashboardScreen(viewModel, onNavigate = { currentScreen = it })
                AppScreen.ASISTENCIA -> AsistenciaScreen(viewModel, onNavigate = { currentScreen = it })
                // ✨ PASAMOS EL VIEWMODEL A PEDIDOS SCREEN
                AppScreen.PEDIDOS -> PedidosScreen(viewModel, onNavigate = { currentScreen = it }, onSendWhatsapp = { order -> selectedOrderId = order.id; currentScreen = AppScreen.LAUNCHING_WS })
                AppScreen.PROVEEDORES -> ProveedoresScreen(onNavigate = { currentScreen = it })
                AppScreen.PERFIL -> PerfilScreen(viewModel, isDark = isDarkTheme, onToggleDark = { isDarkTheme = !isDarkTheme }, onNavigate = { currentScreen = it })
                AppScreen.HISTORIAL -> FullHistoryScreen(viewModel, onBackClick = { currentScreen = AppScreen.ASISTENCIA })
                AppScreen.LAUNCHING_WS -> {
                    // ✨ BUSCAMOS EL PEDIDO EN LA LISTA REAL DE FIREBASE
                    val order = viewModel.pedidosActivos.find { it.id == selectedOrderId }
                    if (order != null) LaunchingWhatsappScreen(order = order, onBackClick = { currentScreen = AppScreen.PEDIDOS })
                }
            }
        }
    }
}