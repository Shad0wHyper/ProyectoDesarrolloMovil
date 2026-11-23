package com.developers.client

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.IconButton
import com.developers.core.R as TextR
import com.developers.core.ui.PanAppMotion
import com.developers.core.ui.panAppPressMotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.developers.core.ui.PanAppGlassCard
import com.developers.core.ui.panAppRevealIn
import com.developers.core.ui.panAppRevealOut
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@Composable
internal fun CartAddIconButton(onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    var taps by remember { mutableIntStateOf(0) }
    var confirmed by remember { mutableStateOf(false) }
    LaunchedEffect(taps) {
        if (taps != 0) {
            confirmed = true
            delay(1_050)
            confirmed = false
        }
    }
    val colors = MaterialTheme.colorScheme
    val addLabel = stringResource(TextR.string.pan_add_cart)
    val background by animateColorAsState(
        targetValue = if (confirmed) colors.primaryContainer else colors.primary,
        animationSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
        label = "cartAddColor"
    )
    IconButton(
        onClick = {
            onClick()
            taps++
        },
        modifier = Modifier.size(44.dp).panAppPressMotion(interactions)
            .semantics { contentDescription = addLabel },
        interactionSource = interactions
    ) {
        Box(
            Modifier.size(34.dp).background(background, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = confirmed,
                animationSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
                label = "cartAddCheck"
            ) { added ->
                Icon(
                    if (added) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = null,
                    tint = if (added) colors.onPrimaryContainer else colors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun CartAddedFeedback(additions: SharedFlow<String>, modifier: Modifier = Modifier) {
    var lastAdded by remember { mutableStateOf("") }
    var visible by remember { mutableStateOf(false) }
    val accessibilityManager = LocalAccessibilityManager.current
    val timeout = accessibilityManager?.calculateRecommendedTimeoutMillis(
        1_800, containsIcons = true, containsText = true
    ) ?: 1_800L
    LaunchedEffect(additions, timeout) {
        additions.collectLatest { name ->
            lastAdded = name
            visible = true
            delay(timeout)
            visible = false
        }
    }
    AnimatedVisibility(visible, modifier, enter = panAppRevealIn(), exit = panAppRevealOut()) {
        PanAppGlassCard(
            modifier = Modifier.widthIn(max = 420.dp).fillMaxWidth(),
            shape = MaterialTheme.shapes.large
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.client_cart_added_feedback), style = MaterialTheme.typography.titleSmall)
                    Text(
                        lastAdded,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
