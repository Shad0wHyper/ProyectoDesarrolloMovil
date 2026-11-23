package com.developers.employee

import androidx.compose.animation.AnimatedVisibility
import com.developers.core.ui.PanAppFloatingTab
import com.developers.core.ui.panAppScreenEnter
import com.developers.core.ui.panAppScreenExit
import com.developers.core.ui.panAppRevealIn
import com.developers.core.ui.panAppRevealOut

import com.developers.core.ui.PanAppRole
import com.developers.core.ui.panAppRoleBackdrop
import com.developers.core.ui.panAppNavigationColor
import com.developers.core.ui.panAppGlassBorder

import com.developers.core.ui.contrastTextColor
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
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.platform.LocalView
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
    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var selectedOrderId by rememberSaveable { mutableStateOf<String?>(null) }
    var accountOrigin by rememberSaveable { mutableStateOf(AppScreen.INICIO) }
    var profileOrigin by rememberSaveable { mutableStateOf(AppScreen.INICIO) }

    val bottomNavTabs = remember {
        listOf(AppScreen.INICIO, AppScreen.ASISTENCIA, AppScreen.PEDIDOS, AppScreen.ALMACEN)
    }
    val showBottomBar = currentScreen in bottomNavTabs || currentScreen == AppScreen.PERFIL
    val backdrop = panAppRoleBackdrop(PanAppRole.Employee)

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
        Surface(modifier = Modifier.fillMaxSize().background(backdrop), color = Color.Transparent) {
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
                    panAppScreenEnter(direction) togetherWith panAppScreenExit(direction)
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
                        role = PanAppRole.Employee,
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

        AnimatedVisibility(
            visible = showBottomBar,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = panAppRevealIn(),
            exit = panAppRevealOut()
        ) {
            EmployeeFloatingBottomBar(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it },
            )
        }
    }
}

@Composable
fun EmployeeFloatingBottomBar(
    currentScreen: AppScreen,
    onNavigate: (AppScreen) -> Unit,
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

    val containerBackgroundColor = panAppNavigationColor()

    BoxWithConstraints(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
    ) {
        val spacing = 4.dp
        val padding = 6.dp
        val available = (this.maxWidth - padding * 2 - spacing * (bottomNavItems.size - 1)).coerceAtLeast(0.dp)
        val itemWidth = minOf(52.dp, available / bottomNavItems.size)
        val selectedWidth = minOf(152.dp, available - itemWidth * (bottomNavItems.size - 1))
        Surface(
            shape = CircleShape,
            color = Color.Transparent,
            modifier = Modifier.background(containerBackgroundColor, CircleShape),
            border = panAppGlassBorder()
        ) {
            Row(
                modifier = Modifier.padding(padding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing)
            ) {
                bottomNavItems.forEach { (screen, title, icon) ->
                    val isSelected = when(screen) {
                        AppScreen.ASISTENCIA -> currentScreen == AppScreen.ASISTENCIA || currentScreen == AppScreen.HISTORIAL
                        AppScreen.PEDIDOS -> currentScreen == AppScreen.PEDIDOS || currentScreen == AppScreen.LAUNCHING_WS
                        else -> currentScreen == screen
                    }

                    PanAppFloatingTab(
                        title = title,
                        icon = icon,
                        isSelected = isSelected,
                        itemWidth = itemWidth,
                        selectedWidth = selectedWidth,
                        onClick = { onNavigate(screen) }
                    )
                }
            }
        }
    }
}


