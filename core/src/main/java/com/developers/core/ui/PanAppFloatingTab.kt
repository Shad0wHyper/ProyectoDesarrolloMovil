package com.developers.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PanAppFloatingTab(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    itemWidth: Dp = 52.dp,
    selectedWidth: Dp = 122.dp,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    val interactions = remember { MutableInteractionSource() }
    val width by animateDpAsState(
        if (isSelected) selectedWidth else itemWidth,
        animationSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
        label = "floatingTabWidth"
    )
    Surface(
        selected = isSelected,
        onClick = onClick,
        interactionSource = interactions,
        modifier = Modifier.width(width).heightIn(min = 48.dp).panAppPressMotion(interactions),
        shape = CircleShape,
        color = if (isSelected) colors.primary else Color.Transparent,
        contentColor = if (isSelected) colors.onPrimary else colors.onSurfaceVariant
    ) {
        Row(
            Modifier.padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            BadgedBox(badge = {
                if (badgeCount > 0) {
                    Badge(containerColor = colors.error, contentColor = colors.onError) {
                        Text(badgeCount.toString())
                    }
                }
            }) {
                Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp))
            }
            AnimatedVisibility(
                visible = isSelected && width >= itemWidth + 18.dp,
                enter = fadeIn(tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing)),
                exit = fadeOut(tween(140))
            ) {
                Text(
                    title,
                    modifier = Modifier.padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
