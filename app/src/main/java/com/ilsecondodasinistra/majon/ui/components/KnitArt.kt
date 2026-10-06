package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import kotlin.math.max

/** Number of knit stitches across (and rows down) the unit square the silhouettes are drawn in. */
internal const val KNIT_GRID = 22

/** A stitch position in the grid; row 0 is the bottom row, as in real knitting. */
internal data class KnitCell(val col: Int, val row: Int)

private sealed interface Primitive {
    fun contains(x: Float, y: Float): Boolean
}

private data class Box(val l: Float, val t: Float, val r: Float, val b: Float) : Primitive {
    override fun contains(x: Float, y: Float) = x in l..r && y in t..b
}

private data class Oval(val cx: Float, val cy: Float, val rx: Float, val ry: Float) : Primitive {
    override fun contains(x: Float, y: Float): Boolean {
        val dx = (x - cx) / rx
        val dy = (y - cy) / ry
        return dx * dx + dy * dy <= 1f
    }
}

private data class Poly(val points: List<Pair<Float, Float>>) : Primitive {
    // Even-odd ray casting
    override fun contains(x: Float, y: Float): Boolean {
        var inside = false
        var j = points.lastIndex
        for (i in points.indices) {
            val (xi, yi) = points[i]
            val (xj, yj) = points[j]
            if ((yi > y) != (yj > y) && x < (xj - xi) * (y - yi) / (yj - yi) + xi) inside = !inside
            j = i
        }
        return inside
    }
}

/** A union of [include] shapes minus [exclude] shapes. */
private class Part(val include: List<Primitive>, val exclude: List<Primitive> = emptyList()) {
    fun contains(x: Float, y: Float) = include.any { it.contains(x, y) } && exclude.none { it.contains(x, y) }
}

private fun mirrored(points: List<Pair<Float, Float>>) = points.map { (x, y) -> (1f - x) to y }

private fun partsFor(icon: ProjectIcon): List<Part> = when (icon) {
    ProjectIcon.SWEATER -> {
        val leftSleeve = listOf(0.26f to 0.14f, 0.0f to 0.7f, 0.15f to 0.8f, 0.26f to 0.46f)
        listOf(
            Part(
                include = listOf(
                    Box(0.24f, 0.14f, 0.76f, 0.95f),
                    Poly(leftSleeve),
                    Poly(mirrored(leftSleeve)),
                ),
                exclude = listOf(Oval(0.5f, 0.12f, 0.12f, 0.12f)),
            ),
        )
    }
    ProjectIcon.SCARF -> listOf(
        Part(listOf(Box(0.32f, 0.04f, 0.68f, 0.96f), Box(0.2f, 0.18f, 0.8f, 0.34f))),
    )
    ProjectIcon.HAT -> listOf(
        Part(listOf(Oval(0.5f, 0.6f, 0.34f, 0.4f)), exclude = listOf(Box(0f, 0.6f, 1f, 1f))),
        Part(listOf(Box(0.14f, 0.6f, 0.86f, 0.82f))),
        Part(listOf(Oval(0.5f, 0.1f, 0.09f, 0.09f))),
    )
    ProjectIcon.SOCKS -> listOf(
        Part(listOf(Box(0.28f, 0.04f, 0.62f, 0.72f), Oval(0.52f, 0.76f, 0.34f, 0.17f))),
    )
    ProjectIcon.GLOVES -> listOf(
        Part(
            listOf(
                Box(0.24f, 0.46f, 0.76f, 0.96f),
                Box(0.24f, 0.12f, 0.35f, 0.5f),
                Box(0.38f, 0.04f, 0.49f, 0.5f),
                Box(0.52f, 0.08f, 0.63f, 0.5f),
                Box(0.66f, 0.2f, 0.77f, 0.5f),
                Poly(listOf(0.24f to 0.6f, 0.06f to 0.46f, 0.12f to 0.36f, 0.28f to 0.5f)),
            ),
        ),
    )
    ProjectIcon.BLANKET -> listOf(Part(listOf(Box(0.06f, 0.2f, 0.94f, 0.82f))))
    ProjectIcon.BAG -> listOf(
        Part(listOf(Box(0.16f, 0.36f, 0.84f, 0.94f))),
        Part(listOf(Oval(0.5f, 0.36f, 0.22f, 0.3f)), exclude = listOf(Oval(0.5f, 0.36f, 0.15f, 0.22f))),
    )
    ProjectIcon.YARN -> listOf(Part(listOf(Oval(0.5f, 0.5f, 0.42f, 0.42f))))
}

/** Stitches of [icon]'s silhouette, bottom row first, left to right within a row. */
internal fun knitCells(icon: ProjectIcon): List<KnitCell> {
    val parts = partsFor(icon)
    val cells = mutableListOf<KnitCell>()
    for (row in 0 until KNIT_GRID) {
        for (col in 0 until KNIT_GRID) {
            val x = (col + 0.5f) / KNIT_GRID
            val y = 1f - (row + 0.5f) / KNIT_GRID
            if (parts.any { it.contains(x, y) }) cells += KnitCell(col, row)
        }
    }
    return cells
}

/**
 * Decides which stitches are colored for a given [progress] (0..1): rows fill from the bottom
 * of the silhouette upward, and the row in progress fills left to right.
 */
internal fun filledStitchCount(cells: List<KnitCell>, progress: Float): Int {
    if (cells.isEmpty()) return 0
    val rows = cells.map { it.row }.distinct().sorted()
    val rowPosition = progress.coerceIn(0f, 1f) * rows.size
    var filled = 0
    rows.forEachIndexed { index, row ->
        val inRow = cells.filter { it.row == row }
        val fraction = (rowPosition - index).coerceIn(0f, 1f)
        filled += (fraction * inRow.size + 0.0001f).toInt().coerceAtMost(inRow.size)
    }
    return filled
}

/**
 * Knitted silhouette of the project's garment, made of V stitches that take on the project
 * [color] as [progress] grows, until the piece is complete.
 */
@Composable
fun KnitProgressArt(
    icon: ProjectIcon,
    progress: Float,
    color: Color,
    emptyColor: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "knitProgress",
    )
    val cells = remember(icon) { knitCells(icon) }
    val rows = remember(cells) { cells.map { it.row }.distinct().sorted() }
    Canvas(modifier.aspectRatio(1f).clearAndSetSemantics { }) {
        val cell = size.width / KNIT_GRID
        val stroke = Stroke(width = max(cell * 0.24f, 1.5.dp.toPx()), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val rowPosition = animated * rows.size
        val rowIndex = rows.withIndex().associate { (index, row) -> row to index }
        val perRow = cells.groupBy { it.row }
        cells.forEach { c ->
            val inRow = perRow.getValue(c.row)
            val fraction = (rowPosition - rowIndex.getValue(c.row)).coerceIn(0f, 1f)
            val filledInRow = (fraction * inRow.size + 0.0001f).toInt()
            val filled = inRow.indexOf(c) < filledInRow
            val x = c.col * cell
            // Row 0 is the bottom row
            val y = size.height - (c.row + 1) * cell
            val v = Path().apply {
                moveTo(x + cell * 0.18f, y + cell * 0.18f)
                lineTo(x + cell * 0.5f, y + cell * 0.82f)
                lineTo(x + cell * 0.82f, y + cell * 0.18f)
            }
            drawPath(v, if (filled) color else emptyColor, style = stroke)
        }
    }
}
