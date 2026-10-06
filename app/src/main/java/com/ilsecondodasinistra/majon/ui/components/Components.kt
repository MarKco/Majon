package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.ui.theme.toColor
import kotlin.math.PI
import kotlin.math.sin

val ProjectIcon.emoji: String
    get() = when (this) {
        ProjectIcon.SWEATER -> "🧥"
        ProjectIcon.SCARF -> "🧣"
        ProjectIcon.HAT -> "🧢"
        ProjectIcon.SOCKS -> "🧦"
        ProjectIcon.GLOVES -> "🧤"
        ProjectIcon.BLANKET -> "🛏️"
        ProjectIcon.BAG -> "👜"
        ProjectIcon.YARN -> "🧶"
    }

@Composable
fun ProjectIconBadge(
    icon: ProjectIcon,
    color: ProjectColor,
    size: Dp = 56.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(color.toColor().copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.82f)
                .clip(CircleShape)
                .background(color.toColor().copy(alpha = 0.35f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = icon.emoji, fontSize = (size.value * 0.42f).sp)
        }
    }
}

/** Whether progress bars animate their filled part like a shaken thread. Provided from settings. */
val LocalWavyProgress = staticCompositionLocalOf { true }

private val WaveAmplitude = 3.dp
private val WaveLength = 26.dp
private val WaveAnchorRamp = 80.dp

/**
 * Progress bar whose filled part looks like a thread held still at the tip (where the colored
 * part meets the uncolored track) and shaken from the start: the wave grows from nothing at the
 * tip to full amplitude at the left edge, and travels toward the anchored tip. Falls back to a flat bar when waves are disabled, empty or complete.
 */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "progress",
    )
    val wavy = LocalWavyProgress.current && animated in 0.01f..0.995f
    val phase = if (wavy) {
        val transition = rememberInfiniteTransition(label = "wave")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
            label = "wavePhase",
        ).value
    } else {
        0f
    }
    // Same height in both modes so toggling the setting never shifts the layout
    Canvas(modifier.fillMaxWidth().height(height + WaveAmplitude * 2)) {
        val strokeWidth = height.toPx()
        val inset = strokeWidth / 2f
        val usable = (size.width - strokeWidth).coerceAtLeast(0f)
        val centerY = size.height / 2f
        drawLine(
            color.copy(alpha = 0.18f),
            Offset(inset, centerY),
            Offset(inset + usable, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
        val length = usable * animated
        if (length <= 0f) return@Canvas
        if (!wavy) {
            drawLine(color, Offset(inset, centerY), Offset(inset + length, centerY), strokeWidth, StrokeCap.Round)
            return@Canvas
        }
        val amplitude = WaveAmplitude.toPx()
        val k = (2 * PI / WaveLength.toPx()).toFloat()
        val ramp = WaveAnchorRamp.toPx().coerceAtMost(length)
        val path = Path()
        var x = 0f
        while (true) {
            // Distance from the anchored tip: still at the tip, full wave at the free start
            val t = ((length - x) / ramp).coerceIn(0f, 1f)
            val envelope = t * t * (3f - 2f * t)
            val y = centerY + amplitude * envelope * sin(k * x - phase)
            if (x == 0f) path.moveTo(inset + x, y) else path.lineTo(inset + x, y)
            if (x >= length) break
            x = (x + 3.dp.toPx()).coerceAtMost(length)
        }
        // Slightly thinner than the track so it reads as a thread lying on it
        drawPath(path, color, style = Stroke(width = strokeWidth * 0.7f, cap = StrokeCap.Round))
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel) }
        },
    )
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val scale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(600),
            label = "emptyScale",
        )
        Text(text = emoji, fontSize = (64 * scale).sp)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(8.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
