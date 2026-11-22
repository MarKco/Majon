package com.ilsecondodasinistra.majon.ui.stash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.R
import com.ilsecondodasinistra.majon.domain.model.argbToHsv
import com.ilsecondodasinistra.majon.domain.model.hsvToArgb

/** Picks any colour: three sliders over a continuum of tones (hue, saturation, brightness) with a preview. */
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
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(current))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                )
                GradientSlider(
                    label = stringResource(R.string.color_hue),
                    fraction = hue / 360f,
                    brush = Brush.horizontalGradient((0..6).map { Color(hsvToArgb(it * 60f, 1f, 1f)) }),
                    onChange = { hue = it * 360f },
                )
                GradientSlider(
                    label = stringResource(R.string.color_saturation),
                    fraction = sat,
                    brush = Brush.horizontalGradient(listOf(Color(hsvToArgb(hue, 0f, value)), Color(hsvToArgb(hue, 1f, value)))),
                    onChange = { sat = it },
                )
                GradientSlider(
                    label = stringResource(R.string.color_brightness),
                    fraction = value,
                    brush = Brush.horizontalGradient(listOf(Color.Black, Color(hsvToArgb(hue, sat, 1f)))),
                    onChange = { value = it },
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** A slider whose track is a gradient showing what moving it does; drag or tap anywhere on it. */
@Composable
private fun GradientSlider(label: String, fraction: Float, brush: Brush, onChange: (Float) -> Unit) {
    var width by remember { mutableStateOf(IntSize.Zero) }
    val thumb = 28.dp
    val density = LocalDensity.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(
            Modifier
                .fillMaxWidth()
                .height(thumb)
                .onSizeChanged { width = it }
                .pointerInput(Unit) {
                    detectTapGestures { p -> if (width.width > 0) onChange((p.x / width.width).coerceIn(0f, 1f)) }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        if (width.width > 0) onChange((change.position.x / width.width).coerceIn(0f, 1f))
                    }
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp)).background(brush))
            val x = with(density) { (fraction.coerceIn(0f, 1f) * width.width).toDp() - thumb / 2 }
            Box(
                Modifier
                    .offset(x = x)
                    .size(thumb)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
        }
    }
}
