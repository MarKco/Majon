package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilsecondodasinistra.majon.domain.model.ProjectColor
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import com.ilsecondodasinistra.majon.ui.theme.toColor

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

@Composable
fun AnimatedProgressBar(
    progress: Float,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "progress",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(color.copy(alpha = 0.15f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animated)
                .height(height)
                .clip(RoundedCornerShape(height / 2))
                .background(color),
        )
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
