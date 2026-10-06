package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

private val ConfettiColors = listOf(
    Color(0xFFBF6B4F), Color(0xFF7C8B6F), Color(0xFFD9A82E),
    Color(0xFFC2548F), Color(0xFF4F7E9E), Color(0xFF8B7BB8),
)

private class ConfettiPiece(
    val x: Float,
    val speed: Float,
    val drift: Float,
    val spin: Float,
    val size: Float,
    val color: Color,
    val isThread: Boolean,
)

/** One-shot burst of confetti and short threads, restarted whenever [burstKey] changes (and is > 0). */
@Composable
fun Confetti(burstKey: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    val pieces = remember(burstKey) {
        val random = Random(burstKey)
        List(70) {
            ConfettiPiece(
                x = random.nextFloat(),
                speed = 0.6f + random.nextFloat() * 0.8f,
                drift = (random.nextFloat() - 0.5f) * 0.25f,
                spin = (random.nextFloat() - 0.5f) * 720f,
                size = 6f + random.nextFloat() * 8f,
                color = ConfettiColors[random.nextInt(ConfettiColors.size)],
                isThread = random.nextFloat() < 0.35f,
            )
        }
    }
    LaunchedEffect(burstKey) {
        if (burstKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(2600, easing = LinearEasing))
        }
    }
    val t = progress.value
    if (burstKey == 0 || t >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        pieces.forEach { p ->
            // Fall from above the top edge; fade out in the last part of the animation
            val y = (-0.1f + t * p.speed * 1.3f) * size.height
            val x = (p.x + p.drift * t + sin(t * 6f + p.x * 10f) * 0.02f) * size.width
            val alpha = (1f - ((t - 0.7f) / 0.3f).coerceIn(0f, 1f))
            val color = p.color.copy(alpha = alpha)
            rotate(p.spin * t, pivot = Offset(x, y)) {
                if (p.isThread) {
                    drawLine(
                        color,
                        Offset(x - p.size * 1.5f, y),
                        Offset(x + p.size * 1.5f, y),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                } else {
                    drawRect(
                        color,
                        topLeft = Offset(x - p.size / 2, y - p.size / 2),
                        size = Size(p.size, p.size * 0.6f),
                    )
                }
            }
        }
    }
}

/** Expanding ring that radiates from the center of its parent; replays when [pulseKey] changes. */
@Composable
fun PulseRing(pulseKey: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(pulseKey) {
        if (pulseKey > 0) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(650))
        }
    }
    val t = progress.value
    if (t >= 1f) return
    Canvas(modifier.fillMaxSize()) {
        val radius = size.minDimension / 2f * (1f + t * 0.9f)
        drawCircle(
            color = color.copy(alpha = 0.45f * (1f - t)),
            radius = radius,
            style = Stroke(width = (6f * (1f - t) + 1f).dp.toPx()),
        )
    }
}

/**
 * A thread winding across the screen, revealed up to [progress]; the free end
 * ends in a small ball of yarn. The wave drifts slowly to feel alive.
 */
@Composable
fun YarnThreadBackground(progress: Float, color: Color, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "thread")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
        label = "threadPhase",
    )
    Canvas(modifier.fillMaxSize()) {
        val baseY = size.height * 0.62f
        val amplitude = size.height * 0.035f
        val path = Path()
        val steps = 120
        for (i in 0..steps) {
            val x = size.width * i / steps
            val y = baseY + amplitude * sin(i / steps.toFloat() * 4f * PI.toFloat() + phase) +
                amplitude * 0.5f * sin(i / steps.toFloat() * 9f * PI.toFloat() - phase * 2f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val revealed = Path()
        val length = measure.length * progress.coerceIn(0f, 1f)
        measure.getSegment(0f, length, revealed, true)
        drawPath(
            revealed,
            color.copy(alpha = 0.28f),
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )
        if (progress > 0f) {
            val end = measure.getPosition(length)
            drawCircle(color.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = end)
            drawCircle(
                color.copy(alpha = 0.5f),
                radius = 11.dp.toPx(),
                center = end,
                style = Stroke(width = 1.5.dp.toPx()),
            )
        }
    }
}
