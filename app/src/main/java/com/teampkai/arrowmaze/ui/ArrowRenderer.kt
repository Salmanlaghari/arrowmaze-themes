package com.teampkai.arrowmaze.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.themes.Theme
import com.teampkai.arrowmaze.themes.ThemeRegistry

/**
 * Minimalist standalone arrow icon for the "Arrows Escape" / clear-the-board
 * mechanic. Style goals (matching the reference look):
 *   - Dark outline, light/white fill — the body color stays neutral so the
 *     theme reads through the *background* colors, not the arrow itself.
 *   - Rounded curved tail so the arrow has the soft "winding" feel of the
 *     reference image, not a sharp geometric arrowhead.
 *   - Theme contributes via a small accent dot near the tip (very subtle tint).
 *   - `alpha` lets the same renderer fade out an arrow during the slide-out
 *     animation.
 */
@Composable
fun ArrowRenderer(
    direction: Direction,
    theme: Theme = ThemeRegistry.JUNGLE,
    isOnPath: Boolean = false,
    isVisited: Boolean = false,
    modifier: Modifier = Modifier.size(48.dp),
    onTap: (() -> Unit)? = null,
    alpha: Float = 1f
) {
    val clickMod = if (onTap != null) {
        Modifier.clickable { onTap() }
    } else {
        Modifier
    }

    Canvas(
        modifier = modifier.then(clickMod)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f
        val arrowLength = canvasWidth * 0.36f

        // Soft glow for "on the solution path" or currently-hinted cells.
        if (isOnPath && !isVisited) {
            drawCircle(
                color = theme.arrowPalette.glow,
                radius = canvasWidth * 0.42f,
                center = Offset(centerX, centerY)
            )
        }

        val rotationAngle = when (direction) {
            Direction.UP -> -90f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.RIGHT -> 0f
        }

        rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
            drawMinimalArrow(
                centerX = centerX,
                centerY = centerY,
                length = arrowLength,
                outlineColor = Color(0xFF1B1B1B).copy(alpha = alpha),
                fillColor = Color.White.copy(alpha = alpha),
                accentColor = theme.arrowPalette.accent.copy(alpha = alpha * 0.8f)
            )
        }
    }
}

/**
 * Draws a single minimalist arrow pointing right (caller handles rotation).
 * The shape: a rounded teardrop body with a soft curved tail, plus a dark
 * outline and a small accent dot near the tip.
 */
private fun DrawScope.drawMinimalArrow(
    centerX: Float,
    centerY: Float,
    length: Float,
    outlineColor: Color,
    fillColor: Color,
    accentColor: Color
) {
    val tipX = centerX + length
    val tailX = centerX - length * 0.55f
    val bodyWidth = length * 0.42f

    // Soft body: rounded teardrop with a slightly curved tail.
    val bodyPath = Path().apply {
        moveTo(tipX, centerY)
        // Top curve
        cubicTo(
            centerX + length * 0.3f, centerY - bodyWidth,
            centerX - length * 0.05f, centerY - bodyWidth * 0.9f,
            tailX + length * 0.15f, centerY - bodyWidth * 0.1f
        )
        // Tail tip (rounded)
        cubicTo(
            tailX, centerY,
            tailX, centerY,
            tailX + length * 0.15f, centerY + bodyWidth * 0.1f
        )
        // Bottom curve back to head
        cubicTo(
            centerX - length * 0.05f, centerY + bodyWidth * 0.9f,
            centerX + length * 0.3f, centerY + bodyWidth,
            tipX, centerY
        )
        close()
    }

    // Fill
    drawPath(
        path = bodyPath,
        color = fillColor,
        style = Fill
    )

    // Dark outline
    drawPath(
        path = bodyPath,
        color = outlineColor,
        style = Stroke(width = 2.dp.toPx())
    )

    // Small accent dot near the head (theme-tinted).
    val accentRadius = length * 0.06f
    drawCircle(
        color = accentColor,
        radius = accentRadius,
        center = Offset(centerX + length * 0.4f, centerY)
    )
}
