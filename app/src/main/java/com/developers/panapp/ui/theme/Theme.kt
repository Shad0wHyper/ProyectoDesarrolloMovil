package com.developers.panapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import com.developers.core.ui.PanAppDesignTheme

@Composable
fun PanAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = PanAppDesignTheme(darkTheme, content)
