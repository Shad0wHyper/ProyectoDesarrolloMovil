package com.developers.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

enum class PanAppRole { Admin, Client, Employee }

@Composable
fun panAppStatusBarColor(role: PanAppRole?): Color {
    val dark = isPanAppInDarkTheme()
    return when (role) {
        PanAppRole.Admin -> if (dark) Color(0xFF312044) else Color(0xFFECE0FF)
        PanAppRole.Client -> if (dark) Color(0xFF272042) else Color(0xFFEAE4FF)
        PanAppRole.Employee -> if (dark) Color(0xFF29203D) else Color(0xFFF0E6FC)
        null -> MaterialTheme.colorScheme.background
    }
}

@Composable
fun panAppRoleBackdrop(role: PanAppRole): Brush {
    val dark = isPanAppInDarkTheme()
    val top = panAppStatusBarColor(role)
    val background = MaterialTheme.colorScheme.background
    return when (role) {
        PanAppRole.Admin -> Brush.verticalGradient(
            0f to top,
            0.3f to if (dark) Color(0xFF3E2255) else Color(0xFFF1DFFA),
            0.68f to if (dark) Color(0xFF2E2450) else Color(0xFFDCCCF9),
            1f to background
        )
        PanAppRole.Client -> Brush.linearGradient(
            0f to top,
            0.38f to if (dark) Color(0xFF372658) else Color(0xFFDFD2FB),
            0.75f to if (dark) Color(0xFF292E50) else Color(0xFFDFE5FF),
            1f to background
        )
        PanAppRole.Employee -> Brush.radialGradient(
            0f to if (dark) Color(0xFF3E2753) else Color(0xFFDCC9F7),
            0.52f to if (dark) Color(0xFF302744) else Color(0xFFE5DDF8),
            0.8f to if (dark) Color(0xFF272D3F) else Color(0xFFE3E8F6),
            1f to top
        )
    }
}

@Composable
fun panAppPanelColor(): Color {
    val dark = isPanAppInDarkTheme()
    val colors = MaterialTheme.colorScheme
    return lerp(colors.surface, colors.primaryContainer, if (dark) 0.10f else 0.18f)
        .copy(alpha = if (dark) 0.88f else 0.80f)
}

@Composable
fun panAppMatteGlassBrush(): Brush {
    val colors = MaterialTheme.colorScheme
    val panel = panAppPanelColor()
    val dark = isPanAppInDarkTheme()
    val tint = if (dark) 0.15f else 0.34f
    return Brush.verticalGradient(
        0f to panel,
        1f to lerp(panel, colors.primaryContainer.copy(alpha = panel.alpha), tint)
    )
}

@Composable
fun panAppNavigationColor(): Color {
    val opacity = if (isPanAppInDarkTheme()) 0.95f else 0.92f
    return MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = opacity)
}

@Composable
fun panAppGlassBorder(): BorderStroke {
    val dark = isPanAppInDarkTheme()
    val colors = MaterialTheme.colorScheme
    return BorderStroke(
        1.dp,
        Brush.linearGradient(
            0f to Color.White.copy(alpha = if (dark) 0.27f else 0.96f),
            0.5f to colors.outlineVariant.copy(alpha = if (dark) 0.42f else 0.32f),
            1f to colors.primary.copy(alpha = if (dark) 0.33f else 0.22f)
        )
    )
}
