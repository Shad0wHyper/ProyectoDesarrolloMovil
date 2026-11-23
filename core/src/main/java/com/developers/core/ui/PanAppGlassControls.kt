package com.developers.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.panAppPressMotion(interactions: MutableInteractionSource): Modifier {
    val pressed by interactions.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(if (pressed) 160 else PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
        label = "panAppPress"
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }
}

@Composable
private fun glassOverlayColor(): Color {
    val colors = MaterialTheme.colorScheme
    return lerp(colors.surface, colors.primaryContainer, if (isPanAppInDarkTheme()) 0.18f else 0.22f)
}

@Composable
fun PanAppGlassDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        containerColor = glassOverlayColor(),
        tonalElevation = 0.dp,
        shadowElevation = 6.dp,
        border = panAppGlassBorder()
    ) {
        PanAppOverlayEntrance(expanded, content)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PanAppGlassSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(),
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = BottomSheetDefaults.ExpandedShape,
        containerColor = glassOverlayColor(),
        tonalElevation = 0.dp,
        scrimColor = scrimColor,
        dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) },
        content = { PanAppOverlayEntrance(true, content) }
    )
}

@Composable
private fun PanAppOverlayEntrance(visible: Boolean, content: @Composable ColumnScope.() -> Unit) {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        if (visible) {
            reveal.snapTo(0f)
            reveal.animateTo(1f, tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing))
        }
    }
    Column(Modifier.graphicsLayer {
        val progress = reveal.value.coerceIn(0f, 1f)
        alpha = 0.78f + 0.22f * progress
        translationY = (1f - progress) * (-5).dp.toPx()
    }, content = content)
}

@Composable
fun PanAppGlassFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    shape: Shape = CircleShape,
    colors: SelectableChipColors? = null
) {
    val interactions = remember { MutableInteractionSource() }
    val scheme = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (selected) scheme.primaryContainer else panAppPanelColor(),
        animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
        label = "filterChipFill"
    )
    val selectionScale = animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
        label = "filterChipSelection"
    )
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = label,
        modifier = modifier.panAppPressMotion(interactions).graphicsLayer {
            scaleX = selectionScale.value
            scaleY = selectionScale.value
        },
        leadingIcon = leadingIcon,
        shape = shape,
        colors = colors ?: FilterChipDefaults.filterChipColors(
            containerColor = fill,
            selectedContainerColor = fill,
            selectedLabelColor = scheme.onPrimaryContainer
        ),
        interactionSource = interactions
    )
}
