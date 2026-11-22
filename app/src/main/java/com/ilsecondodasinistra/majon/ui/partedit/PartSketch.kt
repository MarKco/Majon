package com.ilsecondodasinistra.majon.ui.partedit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.runtime.remember
import com.ilsecondodasinistra.majon.domain.model.paths
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.domain.model.PartShape
import com.ilsecondodasinistra.majon.ui.components.vertical

private fun DrawScope.arrow(from: Offset, to: Offset, color: Color, head: Float, width: Float) {
    drawLine(color, from, to, width, StrokeCap.Round)
    val dx = to.x - from.x
    val dy = to.y - from.y
    val len = kotlin.math.sqrt(dx * dx + dy * dy)
    val ux = dx / len
    val uy = dy / len
    fun barb(tip: Offset, sign: Float) {
        // two short strokes folded back from the tip
        for (side in listOf(-1f, 1f)) {
            val bx = tip.x - sign * ux * head - side * uy * head * 0.7f
            val by = tip.y - sign * uy * head + side * ux * head * 0.7f
            drawLine(color, tip, Offset(bx, by), width, StrokeCap.Round)
        }
    }
    barb(to, 1f)
    barb(from, -1f)
}

/**
 * A sketch of the part whose measures are asked, with an arrow across it (what gives the stitches) and one
 * up its side (what gives the rows), each named after the measure chosen ("Larghezza → punti").
 */
@Composable
fun PartSketch(shape: PartShape, stitchLabel: String, rowLabel: String, modifier: Modifier = Modifier) {
    val fill = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
    val line = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val accent = MaterialTheme.colorScheme.primary
    // The drawing of this shape, parsed once
    val parts = remember(shape) { shape.paths().map { PathParser().parsePathString(it.d).toPath() to it.filled } }
    // The extent of the filled outlines, in the 100 x 100 drawing
    val bounds = remember(parts) {
        parts.filter { it.second }.map { it.first.getBounds() }.let { all ->
            androidx.compose.ui.geometry.Rect(all.minOf { it.left }, all.minOf { it.top }, all.maxOf { it.right }, all.maxOf { it.bottom })
        }
    }
    // Where the rows label sits: just left of the vertical arrow, whatever the width of the piece
    val labelStart = (270f - 136f) / 2f + bounds.left * 1.36f - 14f - 24f
    Box(modifier.width(270.dp).height(200.dp)) {
        Canvas(Modifier.width(270.dp).height(200.dp)) {
            // The sketch is a 100 x 100 drawing scaled evenly into a 136 dp square, centred
            val size = 136.dp.toPx()
            val left = (this.size.width - size) / 2f
            val top = 8.dp.toPx()
            val w = size
            val h = size
            val scale = size / 100f
            withTransform({
                translate(left, top)
                scale(scale, scale, pivot = Offset.Zero)
            }) {
                parts.forEach { (path, filled) ->
                    if (filled) {
                        drawPath(path, fill)
                        drawPath(path, line, style = Stroke(width = 2.dp.toPx() / scale, join = StrokeJoin.Round))
                    } else {
                        drawPath(path, line, style = Stroke(width = 2.dp.toPx() / scale, cap = StrokeCap.Round))
                    }
                }
            }
            val head = 6.dp.toPx()
            val stroke = 2.dp.toPx()
            // The arrows run along the piece itself (its real extent), not along the whole square
            val x0 = left + bounds.left * scale
            val x1 = left + bounds.right * scale
            val y0 = top + bounds.top * scale
            val y1 = top + bounds.bottom * scale
            // across, under the piece: stitches
            arrow(Offset(x0, y1 + 14.dp.toPx()), Offset(x1, y1 + 14.dp.toPx()), accent, head, stroke)
            // up its left side: rows
            arrow(Offset(x0 - 14.dp.toPx(), y0), Offset(x0 - 14.dp.toPx(), y1), accent, head, stroke)
        }
        Text(
            stitchLabel,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        Text(
            rowLabel,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = labelStart.dp, bottom = 36.dp).vertical().rotate(-90f),
        )
    }
}
