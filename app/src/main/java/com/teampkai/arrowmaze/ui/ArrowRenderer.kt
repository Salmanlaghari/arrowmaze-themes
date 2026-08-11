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
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArrowRenderer(
    direction: Direction,
    theme: Theme = ThemeRegistry.JUNGLE,
    isOnPath: Boolean = false,
    isVisited: Boolean = false,
    modifier: Modifier = Modifier.size(48.dp),
    onTap: (() -> Unit)? = null
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
        val arrowLength = canvasWidth * 0.35f

        // Draw glow effect for path cells
        if (isOnPath && !isVisited) {
            drawCircle(
                color = theme.arrowPalette.glow,
                radius = canvasWidth * 0.4f,
                center = Offset(centerX, centerY)
            )
        }

        // Draw visited indicator
        if (isVisited) {
            drawCircle(
                color = Color(0x40FFFFFF),
                radius = canvasWidth * 0.4f,
                center = Offset(centerX, centerY)
            )
        }

        // Rotate canvas based on direction
        val rotationAngle = when (direction) {
            Direction.UP -> -90f
            Direction.DOWN -> 90f
            Direction.LEFT -> 180f
            Direction.RIGHT -> 0f
        }

        rotate(degrees = rotationAngle, pivot = Offset(centerX, centerY)) {
            drawLeafArrow(
                centerX = centerX,
                centerY = centerY,
                length = arrowLength,
                primaryColor = if (isVisited) theme.arrowPalette.accent else theme.arrowPalette.primary,
                secondaryColor = theme.arrowPalette.secondary,
                accentColor = theme.arrowPalette.accent
            )
        }
    }
}

private fun DrawScope.drawLeafArrow(
    centerX: Float,
    centerY: Float,
    length: Float,
    primaryColor: Color,
    secondaryColor: Color,
    accentColor: Color
) {
    val tipX = centerX + length
    val tailX = centerX - length * 0.6f
    val leafWidth = length * 0.5f

    // Leaf body (pointing right by default, rotation handles direction)
    val leafPath = Path().apply {
        moveTo(tipX, centerY) // tip
        // Upper curve
        cubicTo(
            centerX + length * 0.3f, centerY - leafWidth,
            centerX - length * 0.2f, centerY - leafWidth * 0.7f,
            tailX, centerY
        )
        // Lower curve
        cubicTo(
            centerX - length * 0.2f, centerY + leafWidth * 0.7f,
            centerX + length * 0.3f, centerY + leafWidth,
            tipX, centerY
        )
        close()
    }

    // Draw leaf shadow
    drawPath(
        path = leafPath,
        color = Color(0x40000000),
        style = Fill
    )

    // Draw leaf body
    drawPath(
        path = leafPath,
        color = primaryColor,
        style = Fill
    )

    // Draw leaf vein (center line)
    drawLine(
        color = secondaryColor,
        start = Offset(tipX, centerY),
        end = Offset(tailX, centerY),
        strokeWidth = 2.dp.toPx()
    )

    // Draw leaf veins (side veins)
    val veinCount = 4
    for (i in 1..veinCount) {
        val t = i.toFloat() / (veinCount + 1)
        val veinX = tailX + (tipX - tailX) * t
        val veinLength = leafWidth * 0.4f * (1f - t * 0.5f)

        // Upper vein
        drawLine(
            color = secondaryColor.copy(alpha = 0.5f),
            start = Offset(veinX, centerY),
            end = Offset(veinX - veinLength * 0.3f, centerY - veinLength),
            strokeWidth = 1.dp.toPx()
        )
        // Lower vein
        drawLine(
            color = secondaryColor.copy(alpha = 0.5f),
            start = Offset(veinX, centerY),
            end = Offset(veinX - veinLength * 0.3f, centerY + veinLength),
            strokeWidth = 1.dp.toPx()
        )
    }

    // Arrow tip accent
    val tipPath = Path().apply {
        moveTo(tipX + length * 0.1f, centerY)
        lineTo(tipX - length * 0.1f, centerY - leafWidth * 0.3f)
        lineTo(tipX - length * 0.1f, centerY + leafWidth * 0.3f)
        close()
    }
    drawPath(
        path = tipPath,
        color = accentColor,
        style = Fill
    )
}

// Standard geometric arrow for non-jungle themes
private fun DrawScope.drawGeometricArrow(
    centerX: Float,
    centerY: Float,
    length: Float,
    primaryColor: Color,
    accentColor: Color
) {
    val tipX = centerX + length
    val tailX = centerX - length * 0.5f
    val halfWidth = length * 0.35f

    // Arrow shaft
    drawLine(
        color = primaryColor,
        start = Offset(tailX, centerY),
        end = Offset(tipX - halfWidth * 0.5f, centerY),
        strokeWidth = 3.dp.toPx()
    )

    // Arrow head
    val headPath = Path().apply {
        moveTo(tipX, centerY)
        lineTo(tipX - halfWidth, centerY - halfWidth)
        lineTo(tipX - halfWidth, centerY + halfWidth)
        close()
    }
    drawPath(path = headPath, color = accentColor, style = Fill)
}
