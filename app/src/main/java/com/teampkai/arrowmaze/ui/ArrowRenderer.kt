package com.teampkai.arrowmaze.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.themes.Theme

/**
 * Minimalist line-style arrow for the "Arrows Escape" / flow-pipes puzzle.
 *
 * The shape is a thin black line spanning the cell with a small arrowhead
 * at the tip — matching the reference design (Flow Free / Arrows Escape
 * style: white background, thin black pipe-and-arrow arrows, one direction
 * per cell).
 *
 * The renderer is drawn in a "pointing right" frame; the caller rotates it
 * to match the arrow's data direction via [direction].
 */
@Composable
fun ArrowRenderer(
    direction: Direction,
    theme: Theme,
    modifier: Modifier = Modifier.size(48.dp),
    alpha: Float = 1f
) {
    val lineColor = Color(0xFF111111).copy(alpha = alpha)
    val accentColor = theme.arrowPalette.primary.copy(alpha = alpha * 0.9f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        // The shaft spans the full cell edge-to-edge so adjacent arrows
        // visually connect into continuous "pipes" matching the reference.
        val halfLen = (minOf(w, h) / 2f) * 0.95f
        val stroke = (minOf(w, h) * 0.10f).coerceAtLeast(2.5f)

        val rotationAngle = when (direction) {
            Direction.UP -> -90f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.RIGHT -> 0f
        }

        rotate(degrees = rotationAngle, pivot = Offset(cx, cy)) {
            // Main shaft: a thin black line from one cell edge to the other.
            drawLine(
                color = lineColor,
                start = Offset(cx - halfLen, cy),
                end = Offset(cx + halfLen, cy),
                strokeWidth = stroke,
                cap = Stroke.DefaultCap
            )
            // Arrowhead: two short strokes forming a V near the tip end.
            val headLen = halfLen * 0.20f
            val headSpread = headLen * 0.85f
            val tipX = cx + halfLen
            drawLine(
                color = lineColor,
                start = Offset(tipX, cy),
                end = Offset(tipX - headLen, cy - headSpread),
                strokeWidth = stroke,
                cap = Stroke.DefaultCap
            )
            drawLine(
                color = lineColor,
                start = Offset(tipX, cy),
                end = Offset(tipX - headLen, cy + headSpread),
                strokeWidth = stroke,
                cap = Stroke.DefaultCap
            )
        }
    }
}
