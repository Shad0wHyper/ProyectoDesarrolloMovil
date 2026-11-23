package com.developers.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

@Composable
fun PanAppGlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val brush = panAppMatteGlassBrush()
    val colors = CardDefaults.cardColors(
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val border = panAppGlassBorder()
    val elevation = CardDefaults.cardElevation(
        defaultElevation = androidx.compose.ui.unit.Dp.Hairline,
        pressedElevation = androidx.compose.ui.unit.Dp.Hairline,
        focusedElevation = androidx.compose.ui.unit.Dp.Hairline,
        hoveredElevation = androidx.compose.ui.unit.Dp.Hairline
    )
    if (onClick == null) {
        Card(modifier = modifier.background(brush, shape), shape = shape, colors = colors, border = border, elevation = elevation, content = content)
    } else {
        val interactions = remember { MutableInteractionSource() }
        Card(
            onClick = onClick,
            modifier = modifier.panAppPressMotion(interactions).background(brush, shape),
            shape = shape,
            colors = colors,
            border = border,
            elevation = elevation,
            interactionSource = interactions,
            content = content
        )
    }
}
