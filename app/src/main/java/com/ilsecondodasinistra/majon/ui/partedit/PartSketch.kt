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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.domain.model.PartShape
import com.ilsecondodasinistra.majon.ui.components.vertical

/** The outline of a part in a unit box (0..1, y down); stitches run across, rows run up. */
private fun outline(shape: PartShape, w: Float, h: Float, x0: Float, y0: Float): List<Path> {
    // Small DSL: every shape is written with unit coordinates and converted here
    fun path(block: PathBuilder.() -> Unit): Path = Path().also { block(PathBuilder(it, w, h, x0, y0)) }
    return when (shape) {
        PartShape.FRONT -> listOf(path { m(0.05f, 0.06f); l(0.32f, 0f); q(0.5f, 0.30f, 0.68f, 0f); l(0.95f, 0.06f); l(0.95f, 1f); l(0.05f, 1f); close() })
        PartShape.BACK -> listOf(path { m(0.05f, 0.06f); l(0.32f, 0f); q(0.5f, 0.08f, 0.68f, 0f); l(0.95f, 0.06f); l(0.95f, 1f); l(0.05f, 1f); close() })
        PartShape.LEFT_FRONT -> listOf(path { m(0.1f, 0.06f); l(0.55f, 0f); l(0.9f, 0.3f); l(0.9f, 1f); l(0.1f, 1f); close() })
        PartShape.RIGHT_FRONT -> listOf(path { m(0.9f, 0.06f); l(0.45f, 0f); l(0.1f, 0.3f); l(0.1f, 1f); l(0.9f, 1f); close() })
        PartShape.SLEEVE -> listOf(path { m(0.02f, 0.1f); q(0.5f, -0.1f, 0.98f, 0.1f); l(0.78f, 1f); l(0.22f, 1f); close() })
        PartShape.RECTANGLE -> listOf(path { m(0.05f, 0.05f); l(0.95f, 0.05f); l(0.95f, 0.95f); l(0.05f, 0.95f); close() })
        PartShape.LONG_RECTANGLE -> listOf(path { m(0.3f, 0f); l(0.7f, 0f); l(0.7f, 1f); l(0.3f, 1f); close() })
        PartShape.SQUARE -> listOf(path { m(0.2f, 0.08f); l(0.8f, 0.08f); l(0.8f, 0.92f); l(0.2f, 0.92f); close() })
        PartShape.TRIANGLE -> listOf(path { m(0.03f, 0.05f); l(0.97f, 0.05f); l(0.5f, 1f); close() })
        PartShape.DOME -> listOf(path { m(0.1f, 1f); l(0.1f, 0.45f); q(0.1f, 0f, 0.5f, 0f); q(0.9f, 0f, 0.9f, 0.45f); l(0.9f, 1f); close() })
        PartShape.BAND -> listOf(path { m(0.05f, 0.28f); l(0.95f, 0.28f); l(0.95f, 0.72f); l(0.05f, 0.72f); close() })
        PartShape.TUBE -> listOf(path { m(0.25f, 0f); l(0.75f, 0f); l(0.75f, 1f); l(0.25f, 1f); close() })
        PartShape.FOOT -> listOf(path { m(0.25f, 0f); l(0.75f, 0f); l(0.75f, 0.72f); q(0.75f, 1f, 0.5f, 1f); q(0.25f, 1f, 0.25f, 0.72f); close() })
        PartShape.MITTEN -> listOf(
            path { m(0.22f, 1f); l(0.22f, 0.35f); q(0.22f, 0f, 0.52f, 0f); q(0.82f, 0f, 0.82f, 0.35f); l(0.82f, 1f); close() },
            path { m(0.22f, 0.72f); l(0.04f, 0.56f); q(0f, 0.42f, 0.12f, 0.44f); l(0.22f, 0.52f); close() },
        )
        PartShape.BAG -> listOf(path { m(0.1f, 0.3f); l(0.9f, 0.3f); l(0.9f, 1f); l(0.1f, 1f); close() })
        PartShape.STRAP -> listOf(path { m(0.42f, 0f); l(0.58f, 0f); l(0.58f, 1f); l(0.42f, 1f); close() })
        PartShape.SOLE -> listOf(path { m(0.36f, 0f); q(0.82f, 0f, 0.78f, 0.3f); q(0.72f, 0.65f, 0.62f, 1f); q(0.36f, 1.02f, 0.3f, 0.72f); q(0.18f, 0.3f, 0.36f, 0f); close() })
        PartShape.UPPER -> listOf(path { m(0.12f, 0.95f); l(0.12f, 0.35f); q(0.12f, 0.02f, 0.5f, 0.02f); q(0.88f, 0.02f, 0.88f, 0.35f); l(0.88f, 0.95f); close() })
    }
}

/** Builds a path from unit coordinates inside the box (x0, y0, w, h). */
private class PathBuilder(private val path: Path, private val w: Float, private val h: Float, private val x0: Float, private val y0: Float) {
    private fun x(v: Float) = x0 + v * w
    private fun y(v: Float) = y0 + v * h
    fun m(a: Float, b: Float) = path.moveTo(x(a), y(b))
    fun l(a: Float, b: Float) = path.lineTo(x(a), y(b))
    fun q(cx: Float, cy: Float, a: Float, b: Float) = path.quadraticTo(x(cx), y(cy), x(a), y(b))
    fun close() = path.close()
}

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
    Box(modifier.width(270.dp).height(200.dp)) {
        Canvas(Modifier.width(270.dp).height(200.dp)) {
            val left = 46.dp.toPx()
            val top = 8.dp.toPx()
            val w = 210.dp.toPx()
            val h = 136.dp.toPx()
            outline(shape, w, h, left, top).forEach { path ->
                drawPath(path, fill)
                drawPath(path, line, style = Stroke(width = 2.dp.toPx()))
            }
            if (shape == PartShape.BAG) {
                // The handle of the bag, drawn as an arc over the top
                val handle = Path().apply {
                    moveTo(left + 0.3f * w, top + 0.3f * h)
                    quadraticTo(left + 0.5f * w, top - 0.2f * h, left + 0.7f * w, top + 0.3f * h)
                }
                drawPath(handle, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
            }
            val head = 6.dp.toPx()
            val stroke = 2.dp.toPx()
            // across, under the piece: stitches
            arrow(Offset(left, top + h + 14.dp.toPx()), Offset(left + w, top + h + 14.dp.toPx()), accent, head, stroke)
            // up its left side: rows
            arrow(Offset(left - 14.dp.toPx(), top), Offset(left - 14.dp.toPx(), top + h), accent, head, stroke)
        }
        Text(
            stitchLabel,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.align(Alignment.BottomCenter).padding(start = 40.dp),
        )
        Text(
            rowLabel,
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            maxLines = 1,
            modifier = Modifier.align(Alignment.CenterStart).padding(bottom = 36.dp).vertical().rotate(-90f),
        )
    }
}
