package com.developers.panapp

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import com.developers.core.ui.isPanAppInDarkTheme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun Modifier.loginAurora(): Modifier {
    val dark = isPanAppInDarkTheme()
    val base = MaterialTheme.colorScheme.background
    val transition = rememberInfiniteTransition(label = "loginAurora")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "auroraPhase"
    )
    return drawWithCache {
        val radius = (maxOf(size.width, size.height) * 0.58f).coerceAtLeast(1f)
        val violetCenter = Offset(size.width * 0.12f, size.height * 0.18f)
        val blueCenter = Offset(size.width * 0.87f, size.height * 0.67f)
        val violet = Brush.radialGradient(
            colors = listOf(if (dark) Color(0xFF41245F) else Color(0xFFDCC5FF), Color.Transparent),
            center = violetCenter,
            radius = radius
        )
        val blue = Brush.radialGradient(
            colors = listOf(if (dark) Color(0xFF292D58) else Color(0xFFD6DCFF), Color.Transparent),
            center = blueCenter,
            radius = radius
        )
        onDrawBehind {
            val angle = phase.value
            val x = cos(angle) * size.width * 0.11f
            val y = sin(angle) * size.height * 0.07f
            drawRect(base)
            clipRect {
                translate(x, y) { drawCircle(violet, radius, violetCenter) }
                translate(-x, -y) { drawCircle(blue, radius, blueCenter) }
            }
        }
    }
}
