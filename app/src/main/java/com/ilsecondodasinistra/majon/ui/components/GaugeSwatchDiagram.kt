package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ilsecondodasinistra.majon.R

/**
 * The gauge square printed on yarn labels: a dotted 10 × 10 cm square with the rows counted on its left
 * side, the stitches under it and the needle size in the middle ("34 rows / 26 sts / 3 mm").
 * [needleSize] is the already formatted size in mm, null when not known.
 */
@Composable
fun GaugeSwatchDiagram(
    stitches: Int,
    rows: Int,
    needleSize: String?,
    modifier: Modifier = Modifier,
    squareSize: Dp = 96.dp,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val description = stringResource(R.string.swatch_diagram_desc, stitches, rows)
    Column(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.diagram_size), style = MaterialTheme.typography.labelSmall, color = color)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.diagram_rows, rows),
                style = MaterialTheme.typography.labelMedium,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.vertical().rotate(-90f),
            )
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(squareSize)) {
                Canvas(Modifier.size(squareSize)) {
                    val stroke = 2.dp.toPx()
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                        // Round dots, like the dotted border on the label
                        style = Stroke(
                            width = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, stroke * 2.2f)),
                        ),
                    )
                }
                if (needleSize != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            needleSize,
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = (squareSize.value * 0.32f).sp,
                            color = accent,
                        )
                        Text("mm", style = MaterialTheme.typography.labelSmall, color = color)
                    }
                }
            }
        }
        Text(stringResource(R.string.diagram_sts, stitches), style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Lays a text out sideways: swaps width and height so the rotated text takes the room it needs. */
internal fun Modifier.vertical(): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(maxWidth = Int.MAX_VALUE))
    layout(placeable.height, placeable.width) {
        placeable.place(
            x = -(placeable.width / 2 - placeable.height / 2),
            y = -(placeable.height / 2 - placeable.width / 2),
        )
    }
}

/**
 * The gauge square as a form: the numbers are typed right where they sit on a yarn label — the rows on the
 * left side, the stitches under the square, the needle size in the middle. All values are the text as typed.
 */
@Composable
fun GaugeSwatchInput(
    stitches: String,
    rows: String,
    needleMm: String,
    onStitches: (String) -> Unit,
    onRows: (String) -> Unit,
    onNeedleMm: (String) -> Unit,
    modifier: Modifier = Modifier,
    squareSize: Dp = 200.dp,
    error: Boolean = false,
) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val slot = 76.dp
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.diagram_size), style = MaterialTheme.typography.labelLarge, color = color)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.width(slot), horizontalAlignment = Alignment.CenterHorizontally) {
                NumberSlot(rows, onRows, color = color, error = error && rows.isBlank())
                Text(stringResource(R.string.swatch_rows_caption), style = MaterialTheme.typography.labelMedium, color = color)
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(squareSize)) {
                Canvas(Modifier.size(squareSize)) {
                    val stroke = 3.dp.toPx()
                    drawRoundRect(
                        color = color,
                        topLeft = androidx.compose.ui.geometry.Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style = Stroke(
                            width = stroke,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f, stroke * 2.4f)),
                        ),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    NumberSlot(needleMm, onNeedleMm, color = accent, decimal = true, placeholder = "3")
                    Text("mm", style = MaterialTheme.typography.labelMedium, color = color)
                }
            }
            // Keeps the square centred, as the rows slot sits on its left
            Spacer(Modifier.width(slot))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NumberSlot(stitches, onStitches, color = color, error = error && stitches.isBlank())
                Text(stringResource(R.string.swatch_stitches_caption), style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
    }
}

/** A big number written in place: no box, just an underline, like writing on the label. */
@Composable
private fun NumberSlot(
    value: String,
    onValueChange: (String) -> Unit,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    decimal: Boolean = false,
    placeholder: String = "–",
    error: Boolean = false,
) {
    val line = if (error) MaterialTheme.colorScheme.error else color.copy(alpha = 0.6f)
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = { v -> onValueChange(v.filter { c -> c.isDigit() || (decimal && (c == '.' || c == ',')) }.take(5)) },
        singleLine = true,
        textStyle = MaterialTheme.typography.headlineMedium.copy(color = color, textAlign = TextAlign.Center),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(color),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = if (decimal) androidx.compose.ui.text.input.KeyboardType.Decimal else androidx.compose.ui.text.input.KeyboardType.Number,
        ),
        modifier = modifier.width(76.dp),
        decorationBox = { inner ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(48.dp), contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(placeholder, style = MaterialTheme.typography.headlineMedium, color = color.copy(alpha = 0.3f))
                    }
                    inner()
                }
                androidx.compose.material3.HorizontalDivider(color = line, thickness = 2.dp, modifier = Modifier.width(56.dp))
            }
        },
    )
}
