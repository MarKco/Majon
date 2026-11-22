package com.ilsecondodasinistra.majon.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A ball of yarn drawn by hand: round, wrapped in crossing strands (a darker and a lighter shade of [color]),
 * a soft highlight, a shadow below and a loose end trailing off.
 */
@Composable
fun YarnBall(color: Color, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension
        val c = Offset(s * 0.47f, s * 0.45f)
        val r = s * 0.37f
        val dark = lerp(color, Color.Black, 0.28f)
        val light = lerp(color, Color.White, 0.35f)

        // Shadow on the table
        drawOval(Color.Black.copy(alpha = 0.12f), topLeft = Offset(c.x - r * 0.95f, c.y + r * 0.82f), size = Size(r * 1.9f, r * 0.42f))

        // The tail first, so the ball sits over its start
        val tail = Path().apply {
            moveTo(c.x + r * 0.55f, c.y + r * 0.78f)
            cubicTo(c.x + r * 0.9f, c.y + r * 1.25f, s * 0.78f, s * 0.98f, s * 0.97f, s * 0.86f)
        }
        drawPath(tail, dark, style = Stroke(width = s * 0.05f, cap = StrokeCap.Round))
        drawPath(tail, color, style = Stroke(width = s * 0.028f, cap = StrokeCap.Round))

        drawCircle(color, radius = r, center = c)

        val ball = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
        clipPath(ball) {
            // Strands wrapping around, bending one way...
            for (i in -4..4) {
                val shift = i * r * 0.30f
                val strand = Path().apply {
                    moveTo(c.x - r * 1.1f, c.y + shift + r * 0.55f)
                    quadraticTo(c.x, c.y + shift - r * 0.55f, c.x + r * 1.1f, c.y + shift + r * 0.55f)
                }
                drawPath(strand, dark.copy(alpha = 0.75f), style = Stroke(width = s * 0.034f, cap = StrokeCap.Round))
            }
            // ...and crossing them the other way
            for (i in -3..3) {
                val shift = i * r * 0.36f
                val strand = Path().apply {
                    moveTo(c.x + shift + r * 0.5f, c.y - r * 1.1f)
                    quadraticTo(c.x + shift - r * 0.55f, c.y, c.x + shift + r * 0.5f, c.y + r * 1.1f)
                }
                drawPath(strand, light.copy(alpha = 0.55f), style = Stroke(width = s * 0.026f, cap = StrokeCap.Round))
            }
            // Roundness: a soft light from the top left and a shade at the bottom right
            drawCircle(Color.White.copy(alpha = 0.22f), radius = r * 0.45f, center = Offset(c.x - r * 0.38f, c.y - r * 0.42f))
            drawCircle(Color.Black.copy(alpha = 0.14f), radius = r * 0.9f, center = Offset(c.x + r * 0.55f, c.y + r * 0.6f))
        }
    }
}
