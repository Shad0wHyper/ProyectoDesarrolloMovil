package com.developers.core.ui

import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.ui.Modifier

fun LazyItemScope.panAppFilteredItem(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
    placementSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
    fadeOutSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing)
)

fun LazyGridItemScope.panAppFilteredItem(): Modifier = Modifier.animateItem(
    fadeInSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing),
    placementSpec = tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing),
    fadeOutSpec = tween(PanAppMotion.ControlMillis, easing = PanAppMotion.easing)
)
