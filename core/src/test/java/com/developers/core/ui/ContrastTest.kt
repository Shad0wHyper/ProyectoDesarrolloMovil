package com.developers.core.ui

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ContrastTest {
    private val original = TextFieldColors(
        focusedTextColor = Color.Black,
        unfocusedTextColor = Color.Black,
        disabledTextColor = Color.Black,
        errorTextColor = Color.Black,
        focusedLeadingIconColor = Color.Black,
        unfocusedLeadingIconColor = Color.Black,
        disabledLeadingIconColor = Color.Black,
        errorLeadingIconColor = Color.Black,
        focusedTrailingIconColor = Color.Black,
        unfocusedTrailingIconColor = Color.Black,
        disabledTrailingIconColor = Color.Black,
        errorTrailingIconColor = Color.Black,
        focusedLabelColor = Color.Black,
        unfocusedLabelColor = Color.Black,
        disabledLabelColor = Color.Black,
        errorLabelColor = Color.Black,
        focusedPlaceholderColor = Color.Black,
        unfocusedPlaceholderColor = Color.Black,
        disabledPlaceholderColor = Color.Black,
        errorPlaceholderColor = Color.Black,
        focusedSupportingTextColor = Color.Black,
        unfocusedSupportingTextColor = Color.Black,
        disabledSupportingTextColor = Color.Black,
        errorSupportingTextColor = Color.Black,
        focusedPrefixColor = Color.Black,
        unfocusedPrefixColor = Color.Black,
        disabledPrefixColor = Color.Black,
        errorPrefixColor = Color.Black,
        focusedSuffixColor = Color.Black,
        unfocusedSuffixColor = Color.Black,
        disabledSuffixColor = Color.Black,
        errorSuffixColor = Color.Black,
        focusedContainerColor = Color.Magenta,
        unfocusedContainerColor = Color.Magenta,
        disabledContainerColor = Color.Magenta,
        errorContainerColor = Color.Magenta,
        focusedIndicatorColor = Color.Cyan,
        unfocusedIndicatorColor = Color.Cyan,
        disabledIndicatorColor = Color.Cyan,
        errorIndicatorColor = Color.Cyan,
        cursorColor = Color.Black,
        errorCursorColor = Color.Black,
        textSelectionColors = TextSelectionColors(Color.Blue, Color.Blue.copy(alpha = 0.3f))
    )

    @Test
    fun lightAppearanceIsReturnedUnchanged() {
        assertSame(original, fieldColorsForBackground(original, darkColorScheme(), false))
    }

    @Test
    fun darkContentDoesNotChangeContainersOrBorders() {
        val actual = fieldColorsForBackground(original, darkColorScheme(), true)
        assertEquals(original.focusedContainerColor, actual.focusedContainerColor)
        assertEquals(original.focusedIndicatorColor, actual.focusedIndicatorColor)
        assertEquals(original.unfocusedContainerColor, actual.unfocusedContainerColor)
        assertEquals(original.unfocusedIndicatorColor, actual.unfocusedIndicatorColor)
        assertEquals(original.disabledContainerColor, actual.disabledContainerColor)
        assertEquals(original.disabledIndicatorColor, actual.disabledIndicatorColor)
        assertEquals(original.errorContainerColor, actual.errorContainerColor)
        assertEquals(original.errorIndicatorColor, actual.errorIndicatorColor)
        assertEquals(original.textSelectionColors.backgroundColor, actual.textSelectionColors.backgroundColor)
    }

    @Test
    fun allFieldContentStatesAreLegibleOnDarkSurfaces() {
        val colors = darkColorScheme()
        val actual = fieldColorsForBackground(original, colors, true)
        val foregrounds = listOf(
            actual.focusedTextColor,
            actual.unfocusedTextColor,
            actual.disabledTextColor,
            actual.errorTextColor,
            actual.focusedLeadingIconColor,
            actual.unfocusedLeadingIconColor,
            actual.disabledLeadingIconColor,
            actual.errorLeadingIconColor,
            actual.focusedTrailingIconColor,
            actual.unfocusedTrailingIconColor,
            actual.disabledTrailingIconColor,
            actual.errorTrailingIconColor,
            actual.focusedLabelColor,
            actual.unfocusedLabelColor,
            actual.disabledLabelColor,
            actual.errorLabelColor,
            actual.focusedPlaceholderColor,
            actual.unfocusedPlaceholderColor,
            actual.disabledPlaceholderColor,
            actual.errorPlaceholderColor,
            actual.focusedSupportingTextColor,
            actual.unfocusedSupportingTextColor,
            actual.disabledSupportingTextColor,
            actual.errorSupportingTextColor,
            actual.focusedPrefixColor,
            actual.unfocusedPrefixColor,
            actual.disabledPrefixColor,
            actual.errorPrefixColor,
            actual.focusedSuffixColor,
            actual.unfocusedSuffixColor,
            actual.disabledSuffixColor,
            actual.errorSuffixColor
        )
        for (surface in listOf(colors.background, colors.surface, colors.surfaceVariant)) {
            for (foreground in foregrounds) {
                val visible = foreground.compositeOver(surface)
                val ratio = (visible.luminance() + 0.05f) / (surface.luminance() + 0.05f)
                assertTrue("Contrast $ratio for $foreground on $surface", ratio >= 4.5f)
            }
        }
    }
}
