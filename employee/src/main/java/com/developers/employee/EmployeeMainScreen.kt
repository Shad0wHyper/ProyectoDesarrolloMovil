package com.developers.employee

import com.developers.core.ui.contrastTextColor
import com.developers.core.ui.contrastAccentColor
import com.developers.core.ui.PanAppBrand
import com.developers.core.ui.PanAppDesignTheme
import com.developers.core.ui.PanAppAccountSettings
import com.developers.core.ui.PanAppProfileMenu
import com.developers.core.ui.tabSlideDirection

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel

val PrimaryBlue = PanAppBrand
val CardPink = Color(0xFFED5A85)
val OnlineGreen = Color(0xFF2ECA7F)
val OrangePrep = Color(0xFFE88A64)
val WhatsappGreen = Color(0xFF25D366)
val DarkPurpleText = Color(0xFF4A4E91)

@Composable
fun PanAppEmployeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    PanAppDesignTheme(darkTheme, content)
}

enum class AppScreen { INICIO, ASISTENCIA, PEDIDOS, ALMACEN, PERFIL, AJUSTES, LAUNCHING_WS, HISTORIAL }

// class MainActivity removed.

@Composable
fun EmployeeMainScreen(viewModel: EmployeeViewModel, onLogoutClick: () -> Unit) {
    val isDarkTheme = isSystemInDarkTheme()
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountOrigin by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var profileOrigin by rememberSaveable { mutableStateOf(AppScreen.INICIO) }

    val bottomNavTabs = remember {
        listOf(AppScreen.INICIO, AppScreen.ASISTENCIA, AppScreen.PEDIDOS, AppScreen.ALMACEN)
    }
    val showBottomBar = currentScreen in bottomNavTabs || currentScreen == AppScreen.PERFIL
    val bgColor = MaterialTheme.colorScheme.background

    BackHandler(enabled = currentScreen != AppScreen.INICIO) {
        currentScreen = when (currentScreen) {
            AppScreen.LAUNCHING_WS -> AppScreen.PEDIDOS
            AppScreen.HISTORIAL -> AppScreen.ASISTENCIA
            AppScreen.AJUSTES -> accountOrigin
            AppScreen.PERFIL -> profileOrigin
            else -> AppScreen.INICIO
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    val fallback = if (targetState == AppScreen.INICIO ||
                        initialState == AppScreen.HISTORIAL && targetState == AppScreen.ASISTENCIA ||
                        initialState == AppScreen.LAUNCHING_WS && targetState == AppScreen.PEDIDOS ||
                        initialState == AppScreen.AJUSTES && targetState == accountOrigin ||
                        initialState == AppScreen.PERFIL && targetState == profileOrigin
                    ) -1 else 1
                    val direction = tabSlideDirection(bottomNavTabs, initialState, targetState, fallback)
                    slideInHorizontally { it * direction } togetherWith
                        slideOutHorizontally { -it * direction }
                },
                label = "employeeScreen"
            ) { screen ->
                val profileMenu: @Composable () -> Unit = {
                    PanAppProfileMenu(
                        onProfile = { profileOrigin = screen; currentScreen = AppScreen.PERFIL },
                        onSettings = { accountOrigin = screen; currentScreen = AppScreen.AJUSTES },
                        onLogout = onLogoutClick,
                        avatar = if (viewModel.userImageUrl.isNotEmpty()) {
                            { modifier ->
                                coil.compose.AsyncImage(
                                    model = viewModel.userImageUrl,
                                    contentDescription = stringResource(R.string.emp_nav_profile),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = modifier
                                )
                            }
                        } else null
                    )
                }
                when (screen) {
                    AppScreen.INICIO -> DashboardScreen(viewModel, onNavigate = { currentScreen = it }, profileMenu = profileMenu)
                    AppScreen.ASISTENCIA -> AsistenciaScreen(viewModel, onNavigate = { currentScreen = it }, profileMenu = profileMenu)
                    AppScreen.PEDIDOS -> PedidosScreen(viewModel, profileMenu = profileMenu, onSendWhatsapp = { order -> selectedOrderId = order.id; currentScreen = AppScreen.LAUNCHING_WS })
                    AppScreen.ALMACEN -> EmpleadoAlmacenScreen(viewModel, profileMenu = profileMenu)
                    AppScreen.AJUSTES -> PanAppAccountSettings(
                        onBack = { currentScreen = accountOrigin },
                        onProfile = { profileOrigin = AppScreen.AJUSTES; currentScreen = AppScreen.PERFIL },
                        onLogout = onLogoutClick
                    )
                    AppScreen.PERFIL -> PerfilScreen(viewModel, onNavigate = { currentScreen = it }, onLogoutClick = onLogoutClick)
                    AppScreen.HISTORIAL -> FullHistoryScreen(viewModel, onBackClick = { currentScreen = AppScreen.ASISTENCIA })
                    AppScreen.LAUNCHING_WS -> {
                        val order = viewModel.pedidosActivos.find { it.id == selectedOrderId }
                        if (order != null) LaunchingWhatsappScreen(order = order, onBackClick = { currentScreen = AppScreen.PEDIDOS })
                    }
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
    val navHome = stringResource(R.string.emp_nav_home)
    val navAttendance = stringResource(R.string.emp_nav_attendance)
    val navOrders = stringResource(R.string.emp_nav_orders)
    val navWarehouse = stringResource(R.string.emp_nav_warehouse)

    val bottomNavItems = remember(navHome, navAttendance, navOrders, navWarehouse) {
        listOf(
            Triple(AppScreen.INICIO, navHome, Icons.Default.Home),
            Triple(AppScreen.ASISTENCIA, navAttendance, Icons.Default.QrCodeScanner),
            Triple(AppScreen.PEDIDOS, navOrders, Icons.Default.ListAlt),
            Triple(AppScreen.ALMACEN, navWarehouse, Icons.Default.Inventory2)
        )
    }

    val containerBackgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh

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
    val colors = MaterialTheme.colorScheme
    val activeBackgroundColor = colors.primaryContainer
    val contentColor = if (isSelected) colors.onPrimaryContainer else colors.onSurfaceVariant

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
