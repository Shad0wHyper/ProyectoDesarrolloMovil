package com.developers.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val PanAppBrand = Color(0xFF5B50D8)
val PanAppBrandDark = Color(0xFFBEB8FF)

private val lightColors = lightColorScheme(
    primary = PanAppBrand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE7FF),
    onPrimaryContainer = Color(0xFF271C70),
    secondary = Color(0xFF665A90),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE6FA),
    onSecondaryContainer = Color(0xFF33294D),
    tertiary = Color(0xFF456761),
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF1B1A22),
    surface = Color.White,
    onSurface = Color(0xFF1B1A22),
    surfaceVariant = Color(0xFFEFEDF7),
    onSurfaceVariant = Color(0xFF565262),
    surfaceContainerLow = Color(0xFFF5F4FA),
    surfaceContainer = Color(0xFFF0EFF7),
    surfaceContainerHigh = Color(0xFFEBE9F4),
    surfaceContainerHighest = Color(0xFFE6E3F0),
    outline = Color(0xFF777283),
    outlineVariant = Color(0xFFD5D1E1)
)

private val darkColors = darkColorScheme(
    primary = PanAppBrandDark,
    onPrimary = Color(0xFF2C216E),
    primaryContainer = Color(0xFF392E72),
    onPrimaryContainer = Color(0xFFF0ECFF),
    secondary = Color(0xFFCEC1F1),
    onSecondary = Color(0xFF362B51),
    secondaryContainer = Color(0xFF453958),
    onSecondaryContainer = Color(0xFFF2E9FF),
    tertiary = Color(0xFFA9D0C8),
    background = Color(0xFF121216),
    onBackground = Color(0xFFF4F1FA),
    surface = Color(0xFF1D1C23),
    onSurface = Color(0xFFF4F1FA),
    surfaceVariant = Color(0xFF35323F),
    onSurfaceVariant = Color(0xFFCCC7D8),
    surfaceContainerLow = Color(0xFF191820),
    surfaceContainer = Color(0xFF22212A),
    surfaceContainerHigh = Color(0xFF2C2A35),
    surfaceContainerHighest = Color(0xFF34323E),
    outline = Color(0xFF9A95A9),
    outlineVariant = Color(0xFF494553)
)

private val defaultTypography = Typography()
private val panAppTypography = Typography(
    headlineLarge = defaultTypography.headlineLarge.copy(fontWeight = FontWeight.Bold),
    headlineMedium = defaultTypography.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = defaultTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = defaultTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = defaultTypography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
)

private val panAppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LocalPanAppDarkTheme = staticCompositionLocalOf<Boolean?> { null }

@Composable
fun isPanAppInDarkTheme(): Boolean = LocalPanAppDarkTheme.current ?: isSystemInDarkTheme()

@Composable
fun PanAppDesignTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPanAppDarkTheme provides darkTheme) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkColors else lightColors,
            typography = panAppTypography,
            shapes = panAppShapes,
            content = content
        )
    }
}
