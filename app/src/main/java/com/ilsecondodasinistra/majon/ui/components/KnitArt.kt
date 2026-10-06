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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.ilsecondodasinistra.majon.domain.model.ProjectIcon
import kotlin.math.max
import kotlin.math.sqrt

/** Number of knit stitches across (and rows down) the unit square the silhouettes are drawn in. */
internal const val KNIT_GRID = 30

/**
 * A stitch position in the grid; row 0 is the bottom row, as in real knitting.
 * [ribbed] stitches belong to cuffs, hems and borders and are drawn as 1x1 ribbing.
 */
internal data class KnitCell(val col: Int, val row: Int, val ribbed: Boolean = false)

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

/** A union of [include] shapes minus [exclude] shapes; [ribbed] parts are knitted in 1x1 rib. */
private class Part(
    val include: List<Primitive>,
    val exclude: List<Primitive> = emptyList(),
    val ribbed: Boolean = false,
) {
    fun contains(x: Float, y: Float) = include.any { it.contains(x, y) } && exclude.none { it.contains(x, y) }
}

private fun mirrored(points: List<Pair<Float, Float>>) = points.map { (x, y) -> (1f - x) to y }

/** A straight strip of [width] between two points. */
private fun band(x0: Float, y0: Float, x1: Float, y1: Float, width: Float): Poly {
    val dx = x1 - x0
    val dy = y1 - y0
    val len = sqrt(dx * dx + dy * dy)
    val nx = -dy / len * width / 2f
    val ny = dx / len * width / 2f
    return Poly(listOf((x0 + nx) to (y0 + ny), (x1 + nx) to (y1 + ny), (x1 - nx) to (y1 - ny), (x0 - nx) to (y0 - ny)))
}

/** A finger with a rounded tip, from [top] down to [bottom]. */
private fun finger(left: Float, right: Float, top: Float, bottom: Float): List<Primitive> {
    val r = (right - left) / 2f
    return listOf(Box(left, top + r, right, bottom), Oval((left + right) / 2f, top + r, r, r))
}

private fun partsFor(icon: ProjectIcon): List<Part> = when (icon) {
    ProjectIcon.SWEATER -> {
        // Sleeves slope down; the cuff is the last stretch of each sleeve
        val sleeve = listOf(0.27f to 0.14f, 0.076f to 0.578f, 0.206f to 0.648f, 0.27f to 0.46f)
        val cuff = listOf(0.076f to 0.578f, 0.206f to 0.648f, 0.17f to 0.73f, 0.04f to 0.66f)
        listOf(
            Part(
                include = listOf(Box(0.26f, 0.1f, 0.74f, 0.84f), Poly(sleeve), Poly(mirrored(sleeve))),
                exclude = listOf(Oval(0.5f, 0.08f, 0.11f, 0.1f)),
            ),
            Part(listOf(Box(0.26f, 0.84f, 0.74f, 0.95f)), ribbed = true),
            Part(listOf(Poly(cuff), Poly(mirrored(cuff))), ribbed = true),
            Part(
                include = listOf(Oval(0.5f, 0.08f, 0.16f, 0.15f)),
                exclude = listOf(Oval(0.5f, 0.08f, 0.11f, 0.1f)),
                ribbed = true,
            ),
        )
    }
    ProjectIcon.SCARF -> listOf(
        // Wrapped around the neck, with a long and a short tail hanging down
        Part(
            listOf(
                Box(0.2f, 0.08f, 0.8f, 0.3f),
                Oval(0.2f, 0.19f, 0.07f, 0.11f),
                Oval(0.8f, 0.19f, 0.07f, 0.11f),
                Box(0.3f, 0.3f, 0.5f, 0.82f),
                Box(0.52f, 0.3f, 0.7f, 0.68f),
            ),
        ),
        Part(listOf(Box(0.3f, 0.82f, 0.5f, 0.96f), Box(0.52f, 0.68f, 0.7f, 0.82f)), ribbed = true),
    )
    ProjectIcon.HAT -> listOf(
        Part(listOf(Oval(0.5f, 0.62f, 0.34f, 0.44f)), exclude = listOf(Box(0f, 0.62f, 1f, 1f))),
        Part(listOf(Box(0.15f, 0.62f, 0.85f, 0.84f)), ribbed = true),
        Part(listOf(Oval(0.5f, 0.14f, 0.1f, 0.1f))),
    )
    ProjectIcon.SOCKS -> listOf(
        Part(
            listOf(
                Box(0.28f, 0.2f, 0.58f, 0.62f),
                Box(0.28f, 0.55f, 0.76f, 0.86f),
                Oval(0.76f, 0.705f, 0.14f, 0.155f),
                Oval(0.28f, 0.705f, 0.08f, 0.155f),
            ),
        ),
        Part(listOf(Box(0.28f, 0.05f, 0.58f, 0.2f)), ribbed = true),
    )
    ProjectIcon.GLOVES -> listOf(
        Part(
            listOf(
                Box(0.24f, 0.46f, 0.76f, 0.8f),
                *finger(0.24f, 0.34f, 0.16f, 0.5f).toTypedArray(),
                *finger(0.38f, 0.48f, 0.06f, 0.5f).toTypedArray(),
                *finger(0.52f, 0.62f, 0.04f, 0.5f).toTypedArray(),
                *finger(0.66f, 0.76f, 0.12f, 0.5f).toTypedArray(),
                Poly(listOf(0.26f to 0.66f, 0.08f to 0.5f, 0.14f to 0.4f, 0.32f to 0.54f)),
                Oval(0.11f, 0.45f, 0.06f, 0.07f),
            ),
        ),
        Part(listOf(Box(0.26f, 0.8f, 0.74f, 0.96f)), ribbed = true),
    )
    ProjectIcon.BLANKET -> listOf(
        Part(listOf(Box(0.15f, 0.29f, 0.85f, 0.71f))),
        Part(
            include = listOf(Box(0.06f, 0.2f, 0.94f, 0.8f)),
            exclude = listOf(Box(0.15f, 0.29f, 0.85f, 0.71f)),
            ribbed = true,
        ),
    )
    ProjectIcon.BAG -> listOf(
        Part(listOf(Poly(listOf(0.18f to 0.46f, 0.82f to 0.46f, 0.86f to 0.94f, 0.14f to 0.94f)))),
        Part(listOf(Box(0.18f, 0.36f, 0.82f, 0.46f)), ribbed = true),
        Part(
            include = listOf(Oval(0.5f, 0.36f, 0.22f, 0.3f)),
            exclude = listOf(Oval(0.5f, 0.36f, 0.15f, 0.22f), Box(0f, 0.36f, 1f, 1f)),
        ),
    )
    ProjectIcon.YARN -> listOf(
        // A wound ball: diagonal gaps suggest the strands, a loose end trails off
        Part(
            include = listOf(
                Oval(0.46f, 0.46f, 0.36f, 0.36f),
                Poly(listOf(0.62f to 0.74f, 0.76f to 0.84f, 0.94f to 0.82f, 0.95f to 0.9f, 0.74f to 0.93f, 0.56f to 0.8f)),
            ),
            exclude = listOf(
                band(0.1f, 0.4f, 0.6f, 0.9f, 0.04f),
                band(0.2f, 0.2f, 0.78f, 0.78f, 0.04f),
                band(0.4f, 0.1f, 0.86f, 0.56f, 0.04f),
            ),
        ),
    )
}

/** Stitches of [icon]'s silhouette, bottom row first, left to right within a row. */
internal fun knitCells(icon: ProjectIcon): List<KnitCell> {
    val parts = partsFor(icon)
    val cells = mutableListOf<KnitCell>()
    for (row in 0 until KNIT_GRID) {
        for (col in 0 until KNIT_GRID) {
            val x = (col + 0.5f) / KNIT_GRID
            val y = 1f - (row + 0.5f) / KNIT_GRID
            val covering = parts.filter { it.contains(x, y) }
            if (covering.isNotEmpty()) cells += KnitCell(col, row, ribbed = covering.any { it.ribbed })
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

/** A stitch with its place in the fill order, so drawing needn't regroup the cells every frame. */
private class PlacedStitch(val cell: KnitCell, val rowIndex: Int, val indexInRow: Int, val rowSize: Int)

private fun placeStitches(cells: List<KnitCell>): List<PlacedStitch> {
    val perRow = cells.groupBy { it.row }
    val rowIndex = perRow.keys.sorted().withIndex().associate { (index, row) -> row to index }
    return perRow.flatMap { (row, inRow) ->
        inRow.mapIndexed { i, cell -> PlacedStitch(cell, rowIndex.getValue(row), i, inRow.size) }
    }
}

/**
 * Knitted silhouette of the project's garment, made of V stitches that take on the project
 * [color] as [progress] grows, until the piece is complete. Cuffs and borders are ribbed, the
 * two legs of each stitch have slightly different tones like twisted yarn, and a dot marks the
 * stitch on the needle.
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
    val stitches = remember(icon) { placeStitches(knitCells(icon)) }
    val rowCount = remember(stitches) { stitches.maxOfOrNull { it.rowIndex + 1 } ?: 0 }
    val highlight = remember(color) { lerp(color, Color.White, 0.32f) }
    Canvas(modifier.aspectRatio(1f).clearAndSetSemantics { }) {
        val cell = size.width / KNIT_GRID
        val strokeWidth = max(cell * 0.26f, 1.5.dp.toPx())
        val rowPosition = animated * rowCount
        val inProgress = animated > 0f && animated < 1f
        stitches.forEach { s ->
            val fraction = (rowPosition - s.rowIndex).coerceIn(0f, 1f)
            val filledInRow = (fraction * s.rowSize + 0.0001f).toInt()
            val filled = s.indexInRow < filledInRow
            val x = s.cell.col * cell
            // Row 0 is the bottom row
            val y = size.height - (s.cell.row + 1) * cell
            val left = if (filled) color else emptyColor
            val right = if (filled) highlight else emptyColor
            if (s.cell.ribbed && s.cell.col % 2 == 1) {
                // Purl column of the rib: a recessed vertical strand
                drawLine(
                    color = left.copy(alpha = left.alpha * 0.55f),
                    start = Offset(x + cell * 0.5f, y + cell * 0.2f),
                    end = Offset(x + cell * 0.5f, y + cell * 0.8f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            } else {
                val top = y + cell * 0.18f
                val bottom = Offset(x + cell * 0.5f, y + cell * 0.82f)
                drawLine(left, Offset(x + cell * 0.18f, top), bottom, strokeWidth, StrokeCap.Round)
                drawLine(right, bottom, Offset(x + cell * 0.82f, top), strokeWidth, StrokeCap.Round)
            }
            if (inProgress && fraction > 0f && fraction < 1f && s.indexInRow == filledInRow) {
                drawCircle(color, radius = cell * 0.42f, center = Offset(x + cell * 0.5f, y + cell * 0.5f), alpha = 0.5f)
            }
        }
    }
}
