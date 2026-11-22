package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.argbToHsv
import com.ilsecondodasinistra.majon.domain.model.hsvToArgb

/**
 * Picks any colour straight from a picture: a square where saturation grows to the right and brightness upwards
 * (at the chosen hue), and a strip below for the hue. Tap or drag; the preview on top shows the result.
 */
@Composable
fun ColorPickerDialog(initialArgb: Int?, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val start = remember { argbToHsv(initialArgb ?: 0xFFE0A800.toInt()) }
    var hue by remember { mutableFloatStateOf(start.first) }
    var sat by remember { mutableFloatStateOf(start.second) }
    var value by remember { mutableFloatStateOf(start.third) }
    val current = hsvToArgb(hue, sat, value)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.color_pick_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(current))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                )
                SaturationBrightnessSquare(
                    hue = hue,
                    saturation = sat,
                    brightness = value,
                    onChange = { s, v ->
                        sat = s
                        value = v
                    },
                )
                HueStrip(hue = hue, onChange = { hue = it })
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val ThumbSize = 28.dp

/** The marker that follows the finger: a white ring that stays readable on any colour. */
@Composable
private fun Thumb(modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(ThumbSize)
            .clip(CircleShape)
            .border(3.dp, Color.White, CircleShape)
            .border(1.dp, Color.Black.copy(alpha = 0.35f), CircleShape),
    )
}

/** Calls [onPoint] with the position (in px) on the first touch and on every move of the drag. */
private fun Modifier.touchAndDrag(onPoint: (Offset) -> Unit): Modifier = pointerInput(Unit) {
    awaitEachGesture {
        val down = awaitFirstDown()
        down.consume()
        onPoint(down.position)
        drag(down.id) { change ->
            change.consume()
            onPoint(change.position)
        }
    }
}

@Composable
private fun SaturationBrightnessSquare(
    hue: Float,
    saturation: Float,
    brightness: Float,
    onChange: (saturation: Float, brightness: Float) -> Unit,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val description = stringResource(R.string.color_sv_area)
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .onSizeChanged { size = it }
            .semantics { contentDescription = description }
            .touchAndDrag { p ->
                if (size.width > 0 && size.height > 0) {
                    onChange((p.x / size.width).coerceIn(0f, 1f), 1f - (p.y / size.height).coerceIn(0f, 1f))
                }
            },
    ) {
        Canvas(Modifier.fillMaxWidth().aspectRatio(1f)) {
            // White to the pure hue from left to right, then transparent to black from top to bottom
            drawRect(Brush.horizontalGradient(listOf(Color.White, Color(hsvToArgb(hue, 1f, 1f)))))
            drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
        }
        val x = with(density) { (saturation.coerceIn(0f, 1f) * size.width).toDp() - ThumbSize / 2 }
        val y = with(density) { ((1f - brightness.coerceIn(0f, 1f)) * size.height).toDp() - ThumbSize / 2 }
        Thumb(Modifier.offset(x = x, y = y))
    }
}

@Composable
private fun HueStrip(hue: Float, onChange: (Float) -> Unit) {
    var width by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current
    val description = stringResource(R.string.color_hue)
    Box(
        Modifier
            .fillMaxWidth()
            .height(ThumbSize)
            .onSizeChanged { width = it }
            .semantics { contentDescription = description }
            .touchAndDrag { p -> if (width.width > 0) onChange((p.x / width.width).coerceIn(0f, 1f) * 360f) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient((0..6).map { Color(hsvToArgb(it * 60f, 1f, 1f)) })),
        )
        val x = with(density) { (hue / 360f * width.width).toDp() - ThumbSize / 2 }
        Thumb(Modifier.offset(x = x))
    }
}
