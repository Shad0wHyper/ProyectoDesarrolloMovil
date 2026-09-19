package com.developers.employee

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

val PrimaryBlue = Color(0xFF6B72E2)
val CardPink = Color(0xFFED5A85)
val OnlineGreen = Color(0xFF2ECA7F)
val OrangePrep = Color(0xFFE88A64)
val WhatsappGreen = Color(0xFF25D366)
val DarkPurpleText = Color(0xFF4A4E91)

val darkColors = darkColorScheme(
    primary = PrimaryBlue,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    onBackground = Color.White,
    onSurfaceVariant = Color(0xFFAAAAAA)
)

val lightColors = lightColorScheme(
    primary = PrimaryBlue,
    background = Color(0xFFF5F6FA),
    surface = Color.White,
    onSurface = Color.DarkGray,
    onBackground = Color.Black,
    onSurfaceVariant = Color(0xFF7A869A)
)

@Composable
fun PanAppEmployeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) darkColors else lightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}

enum class AppScreen { INICIO, ASISTENCIA, PEDIDOS, ALMACEN, PERFIL, LAUNCHING_WS, HISTORIAL }

// class MainActivity removed.

@Composable
fun EmployeeMainScreen(viewModel: EmployeeViewModel, onLogoutClick: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }

    val bottomNavScreens = remember { 
        listOf(AppScreen.INICIO, AppScreen.ASISTENCIA, AppScreen.PEDIDOS, AppScreen.ALMACEN, AppScreen.PERFIL) 
    }
    val showBottomBar = bottomNavScreens.contains(currentScreen)
    val bgColor = if (isDarkTheme) Color(0xFF121212) else Color(0xFFF5F6FA)

    BackHandler(enabled = currentScreen != AppScreen.INICIO) {
        currentScreen = when (currentScreen) {
            AppScreen.LAUNCHING_WS -> AppScreen.PEDIDOS
            AppScreen.HISTORIAL -> AppScreen.ASISTENCIA
            else -> AppScreen.INICIO
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            when (currentScreen) {
                AppScreen.INICIO -> DashboardScreen(viewModel, onNavigate = { currentScreen = it })
                AppScreen.ASISTENCIA -> AsistenciaScreen(viewModel, onNavigate = { currentScreen = it })
                AppScreen.PEDIDOS -> PedidosScreen(viewModel, onNavigate = { currentScreen = it }, onSendWhatsapp = { order -> selectedOrderId = order.id; currentScreen = AppScreen.LAUNCHING_WS })
                AppScreen.ALMACEN -> EmpleadoAlmacenScreen(viewModel)
                AppScreen.PERFIL -> PerfilScreen(viewModel, onNavigate = { currentScreen = it }, onLogoutClick = onLogoutClick)
                AppScreen.HISTORIAL -> FullHistoryScreen(viewModel, onBackClick = { currentScreen = AppScreen.ASISTENCIA })
                AppScreen.LAUNCHING_WS -> {
                    val order = viewModel.pedidosActivos.find { it.id == selectedOrderId }
                    if (order != null) LaunchingWhatsappScreen(order = order, onBackClick = { currentScreen = AppScreen.PEDIDOS })
                }
            }
        }

        if (showBottomBar) {
            EmployeeFloatingBottomBar(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
                isDarkMode = isDarkTheme,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun EmployeeFloatingBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val bottomNavItems = remember {
        listOf(
            Triple(AppScreen.INICIO, "Inicio", Icons.Default.Home),
            Triple(AppScreen.ASISTENCIA, "Asistencia", Icons.Default.QrCodeScanner),
            Triple(AppScreen.PEDIDOS, "Pedidos", Icons.Default.ListAlt),
            Triple(AppScreen.ALMACEN, "Almacén", Icons.Default.Inventory2),
            Triple(AppScreen.PERFIL, "Perfil", Icons.Default.Person)
        )
    }

    val containerBackgroundColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White

    Box(
        modifier = modifier
            .wrapContentWidth()
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 12.dp, end = 12.dp)
    ) {
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
                bottomNavItems.forEach { (screen, title, icon) ->
                    val isSelected = when(screen) {
                        AppScreen.ASISTENCIA -> currentScreen == AppScreen.ASISTENCIA || currentScreen == AppScreen.HISTORIAL
                        AppScreen.PEDIDOS -> currentScreen == AppScreen.PEDIDOS || currentScreen == AppScreen.LAUNCHING_WS
                        else -> currentScreen == screen
                    }

                    EmployeeFloatingNavItem(
                        title = title,
                        icon = icon,
                        isSelected = isSelected,
                        isDarkMode = isDarkMode,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmployeeFloatingNavItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    isDarkMode: Boolean,
    onClick: () -> Unit
) {
    val activeBackgroundColor = PrimaryBlue.copy(alpha = 0.15f)
    val contentColor = if (isSelected) PrimaryBlue else Color.Gray

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) activeBackgroundColor else Color.Transparent,
        contentColor = contentColor
    ) {
        Row(
            modifier = Modifier
                .animateContentSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = contentColor,
                modifier = Modifier.size(20.dp)
            )
            
            if (isSelected) {
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    maxLines = 1
                )
            }
        }
    }
}
