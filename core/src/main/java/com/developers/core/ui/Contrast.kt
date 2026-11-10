package com.developers.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

private val DarkContentColors = darkColorScheme()

/**
 * Foreground roles for an existing dark surface. Some legacy screens paint dark
 * containers while inheriting a light MaterialTheme; do not change their backgrounds.
 */
@Composable
private fun darkContentColors(): ColorScheme {
    val current = MaterialTheme.colorScheme
    return if (current.onSurface.luminance() > current.surface.luminance()) current
    else DarkContentColors
}

/** The supplied color is returned unchanged for the existing light appearance. */
@Composable
fun contrastTextColor(
    lightColor: Color,
    secondary: Boolean = false,
    darkBackground: Boolean = isSystemInDarkTheme()
): Color {
    if (!darkBackground) return lightColor
    val colors = darkContentColors()
    return if (secondary) colors.onSurfaceVariant else colors.onSurface
}

@Composable
fun contrastAccentColor(
    lightColor: Color,
    darkBackground: Boolean = isSystemInDarkTheme()
): Color = if (darkBackground) darkContentColors().primary else lightColor

@Composable
fun contrastErrorColor(
    lightColor: Color,
    darkBackground: Boolean = isSystemInDarkTheme()
): Color = if (darkBackground) darkContentColors().error else lightColor

/**
 * Overrides only field content. Containers, borders and all light-mode colors
 * remain exactly as configured by the caller, including disabled/error containers.
 */
@Composable
fun contrastFieldColors(
    original: TextFieldColors,
    darkBackground: Boolean = isSystemInDarkTheme()
): TextFieldColors {
    return fieldColorsForBackground(original, darkContentColors(), darkBackground)
}

internal fun fieldColorsForBackground(
    original: TextFieldColors,
    colors: ColorScheme,
    darkBackground: Boolean
): TextFieldColors {
    if (!darkBackground) return original
    val disabled = colors.onSurface.copy(alpha = 0.75f)
    return original.copy(
        focusedTextColor = colors.onSurface,
        unfocusedTextColor = colors.onSurface,
        disabledTextColor = disabled,
        errorTextColor = colors.onSurface,
        focusedLeadingIconColor = colors.onSurfaceVariant,
        unfocusedLeadingIconColor = colors.onSurfaceVariant,
        disabledLeadingIconColor = disabled,
        errorLeadingIconColor = colors.onSurfaceVariant,
        focusedTrailingIconColor = colors.onSurfaceVariant,
        unfocusedTrailingIconColor = colors.onSurfaceVariant,
        disabledTrailingIconColor = disabled,
        errorTrailingIconColor = colors.error,
        focusedLabelColor = colors.onSurfaceVariant,
        unfocusedLabelColor = colors.onSurfaceVariant,
        disabledLabelColor = disabled,
        errorLabelColor = colors.error,
        focusedPlaceholderColor = colors.onSurfaceVariant,
        unfocusedPlaceholderColor = colors.onSurfaceVariant,
        disabledPlaceholderColor = disabled,
        errorPlaceholderColor = colors.onSurfaceVariant,
        focusedSupportingTextColor = colors.onSurfaceVariant,
        unfocusedSupportingTextColor = colors.onSurfaceVariant,
        disabledSupportingTextColor = disabled,
        errorSupportingTextColor = colors.error,
        focusedPrefixColor = colors.onSurfaceVariant,
        unfocusedPrefixColor = colors.onSurfaceVariant,
        disabledPrefixColor = disabled,
        errorPrefixColor = colors.onSurfaceVariant,
        focusedSuffixColor = colors.onSurfaceVariant,
        unfocusedSuffixColor = colors.onSurfaceVariant,
        disabledSuffixColor = disabled,
        errorSuffixColor = colors.onSurfaceVariant,
        cursorColor = colors.primary,
        errorCursorColor = colors.error,
        textSelectionColors = TextSelectionColors(
            handleColor = colors.primary,
            backgroundColor = original.textSelectionColors.backgroundColor
        )
    )
}
