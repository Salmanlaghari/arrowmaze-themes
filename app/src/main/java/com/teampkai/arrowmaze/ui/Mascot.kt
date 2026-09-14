package com.teampkai.arrowmaze.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Cute chibi bunny mascot, drawn procedurally on a Canvas (no image assets
 * required). Gently bobs with a subtle ear wiggle; when [celebrating] it
 * bounces faster with happy closed eyes — used on the level-complete screen.
 * The inner-ear/nose color follows the level's accent so the mascot matches
 * the active theme (e.g. neon pink on Cyberpunk Neon levels).
 */
@Composable
fun BunnyMascot(
    size: Dp = 64.dp,
    modifier: Modifier = Modifier,
    celebrating: Boolean = false,
    furColor: Color = Color.White,
    outlineColor: Color = Color(0xFF4A2C2A),
    accentColor: Color = Color(0xFFFF80AB),
    eyeColor: Color = Color(0xFF2B1B1B)
) {
    val transition = rememberInfiniteTransition(label = "bunny-mascot")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (celebrating) 650 else 1800,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "bunny-t"
    )

    Canvas(modifier = modifier.size(size)) {
        drawBunny(
            t = t,
            celebrating = celebrating,
            fur = furColor,
            outline = outlineColor,
            accent = accentColor,
            eye = eyeColor
        )
    }
}

private fun DrawScope.drawBunny(
    t: Float,
    celebrating: Boolean,
    fur: Color,
    outline: Color,
    accent: Color,
    eye: Color
) {
    val w = size.width
    val h = size.height
    val cx = w / 2f
    // Keep the whole chain Float: sin(Double) (via kotlin.math.PI) would
    // promote every coordinate to Double and break all Offset() calls.
    val wave = sin(t * 2f * PI.toFloat())
    val cycle = (wave + 1f) / 2f // smooth 0..1
    val bob = if (celebrating) cycle * h * 0.10f else cycle * h * 0.035f
    val earWiggle = wave * if (celebrating) 14f else 5f

    val headR = w * 0.30f
    val headCy = h * 0.60f + bob

    // ── Ears (drawn behind the head) ────────────────────────────────
    val earW = w * 0.13f
    val earH = h * 0.30f
    val earBaseY = headCy - headR * 0.75f
    listOf(-1f to (cx - headR * 0.45f), 1f to (cx + headR * 0.45f)).forEach { (side, ecx) ->
        rotate(degrees = side * (10f + earWiggle), pivot = Offset(ecx, earBaseY + earH * 0.35f)) {
            drawRoundRect(
                color = fur,
                topLeft = Offset(ecx - earW / 2f, earBaseY - earH),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f)
            )
            drawRoundRect(
                color = outline,
                topLeft = Offset(ecx - earW / 2f, earBaseY - earH),
                size = Size(earW, earH),
                cornerRadius = CornerRadius(earW / 2f, earW / 2f),
                style = Stroke(width = w * 0.02f)
            )
            drawRoundRect(
                color = accent.copy(alpha = 0.75f),
                topLeft = Offset(ecx - earW * 0.26f, earBaseY - earH * 0.86f),
                size = Size(earW * 0.52f, earH * 0.72f),
                cornerRadius = CornerRadius(earW * 0.26f, earW * 0.26f)
            )
        }
    }

    // ── Head ────────────────────────────────────────────────────────
    drawCircle(color = fur, radius = headR, center = Offset(cx, headCy))
    drawCircle(
        color = outline,
        radius = headR,
        center = Offset(cx, headCy),
        style = Stroke(width = w * 0.02f)
    )

    // Blush
    drawCircle(
        color = accent.copy(alpha = 0.45f),
        radius = headR * 0.16f,
        center = Offset(cx - headR * 0.62f, headCy + headR * 0.30f)
    )
    drawCircle(
        color = accent.copy(alpha = 0.45f),
        radius = headR * 0.16f,
        center = Offset(cx + headR * 0.62f, headCy + headR * 0.30f)
    )

    // Eyes: open dots normally, happy arcs when celebrating
    if (celebrating) {
        drawArc(
            color = eye,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx - headR * 0.62f, headCy - headR * 0.42f),
            size = Size(headR * 0.24f, headR * 0.24f),
            style = Stroke(width = w * 0.025f)
        )
        drawArc(
            color = eye,
            startAngle = 200f,
            sweepAngle = 140f,
            useCenter = false,
            topLeft = Offset(cx + headR * 0.38f, headCy - headR * 0.42f),
            size = Size(headR * 0.24f, headR * 0.24f),
            style = Stroke(width = w * 0.025f)
        )
    } else {
        drawCircle(
            color = eye,
            radius = headR * 0.11f,
            center = Offset(cx - headR * 0.42f, headCy - headR * 0.10f)
        )
        drawCircle(
            color = eye,
            radius = headR * 0.11f,
            center = Offset(cx + headR * 0.42f, headCy - headR * 0.10f)
        )
    }

    // Nose + mouth (small "w")
    drawCircle(color = accent, radius = headR * 0.09f, center = Offset(cx, headCy + headR * 0.16f))
    drawArc(
        color = eye,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx - headR * 0.20f, headCy + headR * 0.14f),
        size = Size(headR * 0.20f, headR * 0.16f),
        style = Stroke(width = w * 0.018f)
    )
    drawArc(
        color = eye,
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(cx, headCy + headR * 0.14f),
        size = Size(headR * 0.20f, headR * 0.16f),
        style = Stroke(width = w * 0.018f)
    )

    // Whiskers
    for (side in listOf(-1f, 1f)) {
        val sx = cx + side * headR * 0.55f
        drawLine(
            eye.copy(alpha = 0.55f),
            Offset(sx, headCy + headR * 0.05f),
            Offset(sx + side * headR * 0.38f, headCy - headR * 0.02f),
            strokeWidth = w * 0.012f
        )
        drawLine(
            eye.copy(alpha = 0.55f),
            Offset(sx, headCy + headR * 0.18f),
            Offset(sx + side * headR * 0.38f, headCy + headR * 0.22f),
            strokeWidth = w * 0.012f
        )
    }
}
