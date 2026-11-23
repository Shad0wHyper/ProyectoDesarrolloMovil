package com.developers.core.ui

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically

object PanAppMotion {
    const val EnterMillis = 460
    const val ExitMillis = 380
    const val ControlMillis = 320
    val easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
}

fun panAppScreenEnter(direction: Int): EnterTransition =
    slideInHorizontally(tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing)) { it * direction / 4 } +
        fadeIn(tween(PanAppMotion.ExitMillis))

fun panAppScreenExit(direction: Int): ExitTransition =
    slideOutHorizontally(tween(PanAppMotion.ExitMillis, easing = PanAppMotion.easing)) { -it * direction / 5 } +
        fadeOut(tween(PanAppMotion.ControlMillis))

fun panAppRevealIn(): EnterTransition =
    fadeIn(tween(PanAppMotion.ControlMillis)) +
        scaleIn(tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing), initialScale = 0.96f) +
        slideInVertically(tween(PanAppMotion.EnterMillis, easing = PanAppMotion.easing)) { it / 5 }

fun panAppRevealOut(): ExitTransition =
    fadeOut(tween(PanAppMotion.ExitMillis)) +
        scaleOut(tween(PanAppMotion.ExitMillis, easing = PanAppMotion.easing), targetScale = 0.98f) +
        slideOutVertically(tween(PanAppMotion.ExitMillis, easing = PanAppMotion.easing)) { it / 6 }
