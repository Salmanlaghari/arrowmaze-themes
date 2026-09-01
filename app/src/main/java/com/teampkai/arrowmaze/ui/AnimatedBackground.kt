package com.teampkai.arrowmaze.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.teampkai.arrowmaze.themes.Theme
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Catalog of premium backgrounds. Each entry defines a unique visual
 * identity (palette + animated elements) that can be assigned to any
 * level. `BackgroundCatalog` maps a level number to one of these via
 * a deterministic cycle so every one of the 1500+ levels has its own
 * backdrop and live animation.
 *
 * Backgrounds include:
 *   - Sky, Sunset, Sunrise, Night, Aurora
 *   - Forest, Jungle, Bamboo, Autumn, Cherry Blossom
 *   - Ocean, Coral Reef, Deep Sea, Arctic, Beach
 *   - Desert, Sand Dunes, Oasis, Cactus, Savanna
 *   - Snow, Blizzard, Ice Cave, Tundra
 *   - Space, Nebula, Galaxy, Mars, Moon
 *   - Volcano, Lava, Magma, Geothermal
 *   - Neon City, Cyberpunk, Synthwave, Vaporwave
 *   - Candy Land, Cotton Candy, Rainbow, Pastel
 *   - Mushroom, Enchanted, Fairy, Crystal
 *   - Underwater Bubbles, Rain, Storm, Lightning
 */
enum class BackgroundType(
    val displayName: String,
    val skyTop: Color,
    val skyBottom: Color,
    val ground: Color,
    val accent: Color,
    val particle: Color,
    val animation: String  // "birds", "bubbles", "snow", "rain", "stars", "fireflies", "petals", "sand", "lava", "neon", "aurora", "fish", "lightning", "dust", "fire"
) {
    SKY("Sky",
        Color(0xFF4FC3F7), Color(0xFFB3E5FC), Color(0xFF81C784), Color(0xFFFFF59D), Color(0xFFFFFFFF), "birds"),
    SUNSET("Sunset",
        Color(0xFFFF6F00), Color(0xFFFFAB91), Color(0xFF6D4C41), Color(0xFFFFD54F), Color(0xFFFFFFFF), "birds"),
    SUNRISE("Sunrise",
        Color(0xFFFFCC80), Color(0xFFFFE0B2), Color(0xFF8D6E63), Color(0xFFFFAB91), Color(0xFFFFFFFF), "birds"),
    NIGHT("Night",
        Color(0xFF0D1B2A), Color(0xFF1B263B), Color(0xFF2E7D32), Color(0xFFFFD54F), Color(0xFFFFF59D), "stars"),
    AURORA("Aurora",
        Color(0xFF1A237E), Color(0xFF26A69A), Color(0xFF1B5E20), Color(0xFF69F0AE), Color(0xFFB9F6CA), "aurora"),

    FOREST("Forest",
        Color(0xFF388E3C), Color(0xFF66BB6A), Color(0xFF4E342E), Color(0xFFA5D6A7), Color(0xFFFFFFFF), "petals"),
    JUNGLE("Jungle",
        Color(0xFF1B5E20), Color(0xFF4CAF50), Color(0xFF3E2723), Color(0xFFCDDC39), Color(0xFFFFEB3B), "fireflies"),
    BAMBOO("Bamboo",
        Color(0xFF558B2F), Color(0xFF9CCC65), Color(0xFF33691E), Color(0xFFDCE775), Color(0xFFFFFFFF), "petals"),
    AUTUMN("Autumn",
        Color(0xFFE65100), Color(0xFFFFB74D), Color(0xFF6D4C41), Color(0xFFFF6F00), Color(0xFFFF8A65), "petals"),
    CHERRY_BLOSSOM("Cherry Blossom",
        Color(0xFFF8BBD0), Color(0xFFFCE4EC), Color(0xFF8D6E63), Color(0xFFF48FB1), Color(0xFFFCE4EC), "petals"),

    OCEAN("Ocean",
        Color(0xFF0277BD), Color(0xFF4FC3F7), Color(0xFF01579B), Color(0xFF80DEEA), Color(0xFFFFFFFF), "bubbles"),
    CORAL_REEF("Coral Reef",
        Color(0xFF00838F), Color(0xFF4DB6AC), Color(0xFF004D40), Color(0xFFFF8A65), Color(0xFFFFD54F), "fish"),
    DEEP_SEA("Deep Sea",
        Color(0xFF000A12), Color(0xFF0D47A1), Color(0xFF000000), Color(0xFF4FC3F7), Color(0xFF80D8FF), "bubbles"),
    ARCTIC("Arctic",
        Color(0xFFB3E5FC), Color(0xFFE1F5FE), Color(0xFFE0E0E0), Color(0xFF81D4FA), Color(0xFFFFFFFF), "snow"),
    BEACH("Beach",
        Color(0xFF4FC3F7), Color(0xFFB3E5FC), Color(0xFFFFE0B2), Color(0xFFFFB74D), Color(0xFFFFFFFF), "birds"),

    DESERT("Desert",
        Color(0xFFFFB74D), Color(0xFFFFCC80), Color(0xFFD7A86E), Color(0xFFFF8F00), Color(0xFFFFFFFF), "sand"),
    SAND_DUNES("Sand Dunes",
        Color(0xFFFFA726), Color(0xFFFFCC80), Color(0xFFBCAAA4), Color(0xFFFFB300), Color(0xFFFFFFFF), "sand"),
    OASIS("Oasis",
        Color(0xFF29B6F6), Color(0xFF81D4FA), Color(0xFF8D6E63), Color(0xFF66BB6A), Color(0xFFFFFFFF), "birds"),
    CACTUS("Cactus",
        Color(0xFFFFCC80), Color(0xFFFFE0B2), Color(0xFF6D4C41), Color(0xFF388E3C), Color(0xFFFFFFFF), "sand"),
    SAVANNA("Savanna",
        Color(0xFFFFA000), Color(0xFFFFCA28), Color(0xFF8D6E63), Color(0xFF6D4C41), Color(0xFFFFFFFF), "dust"),

    SNOW("Snow",
        Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFFAFAFA), Color(0xFFE1F5FE), Color(0xFFFFFFFF), "snow"),
    BLIZZARD("Blizzard",
        Color(0xFF78909C), Color(0xFFB0BEC5), Color(0xFFECEFF1), Color(0xFFFFFFFF), Color(0xFFFFFFFF), "snow"),
    ICE_CAVE("Ice Cave",
        Color(0xFF4FC3F7), Color(0xFFB3E5FC), Color(0xFF0277BD), Color(0xFF80DEEA), Color(0xFFE1F5FE), "snow"),
    TUNDRA("Tundra",
        Color(0xFF90A4AE), Color(0xFFCFD8DC), Color(0xFFB0BEC5), Color(0xFFB2EBF2), Color(0xFFFFFFFF), "snow"),

    SPACE("Space",
        Color(0xFF000000), Color(0xFF1A237E), Color(0xFF000000), Color(0xFFFFFFFF), Color(0xFFFFF59D), "stars"),
    NEBULA("Nebula",
        Color(0xFF4A148C), Color(0xFF7B1FA2), Color(0xFF000000), Color(0xFFE91E63), Color(0xFFCE93D8), "stars"),
    GALAXY("Galaxy",
        Color(0xFF000051), Color(0xFF1A237E), Color(0xFF000000), Color(0xFFFFD54F), Color(0xFFB388FF), "stars"),
    MARS("Mars",
        Color(0xFFD84315), Color(0xFFFF7043), Color(0xFF6D4C41), Color(0xFFFFAB91), Color(0xFFFFCCBC), "sand"),
    MOON("Moon",
        Color(0xFF263238), Color(0xFF455A64), Color(0xFF78909C), Color(0xFFE0E0E0), Color(0xFFFFFFFF), "stars"),

    VOLCANO("Volcano",
        Color(0xFF3E2723), Color(0xFF6D4C41), Color(0xFF1B0F0A), Color(0xFFFF6F00), Color(0xFFFFD54F), "lava"),
    LAVA("Lava",
        Color(0xFF1A0E0A), Color(0xFF6D1F0F), Color(0xFF0D0703), Color(0xFFFF6F00), Color(0xFFFFD54F), "lava"),
    MAGMA("Magma",
        Color(0xFF2C0B0B), Color(0xFFE64A19), Color(0xFF0D0303), Color(0xFFFFD54F), Color(0xFFFFAB91), "fire"),
    GEOTHERMAL("Geothermal",
        Color(0xFF263238), Color(0xFF455A64), Color(0xFF1B0F0A), Color(0xFF80DEEA), Color(0xFFFFCC80), "bubbles"),

    NEON_CITY("Neon City",
        Color(0xFF0A0E27), Color(0xFF1A1A2E), Color(0xFF0F0F1E), Color(0xFFFF00FF), Color(0xFF00FFFF), "neon"),
    CYBERPUNK("Cyberpunk",
        Color(0xFF120458), Color(0xFF2D0A5A), Color(0xFF060218), Color(0xFFFF1744), Color(0xFF00E5FF), "neon"),
    SYNTHWAVE("Synthwave",
        Color(0xFF2D0A4B), Color(0xFFE91E63), Color(0xFF1A0833), Color(0xFF00E5FF), Color(0xFFFFEB3B), "neon"),
    VAPORWAVE("Vaporwave",
        Color(0xFFFF6EC7), Color(0xFF7DF9FF), Color(0xFFE91E63), Color(0xFFFFFF00), Color(0xFFFFFFFF), "neon"),

    CANDY_LAND("Candy Land",
        Color(0xFFF8BBD0), Color(0xFFFFF9C4), Color(0xFFFFCDD2), Color(0xFFE91E63), Color(0xFFFFFFFF), "petals"),
    COTTON_CANDY("Cotton Candy",
        Color(0xFFFFB3DA), Color(0xFFB3E5FC), Color(0xFFFFD3E0), Color(0xFFFFFFFF), Color(0xFFFFCDD2), "petals"),
    RAINBOW("Rainbow",
        Color(0xFFEF5350), Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFF42A5F5), Color(0xFFAB47BC), "stars"),
    PASTEL("Pastel",
        Color(0xFFFFCCBC), Color(0xFFFFE0B2), Color(0xFFCFD8DC), Color(0xFFB39DDB), Color(0xFFF8BBD0), "petals"),

    MUSHROOM("Mushroom",
        Color(0xFF4A148C), Color(0xFF8E24AA), Color(0xFF1B5E20), Color(0xFFFF1744), Color(0xFFB388FF), "fireflies"),
    ENCHANTED("Enchanted",
        Color(0xFF1B5E20), Color(0xFF388E3C), Color(0xFF33691E), Color(0xFFB388FF), Color(0xFFFFEB3B), "fireflies"),
    FAIRY("Fairy",
        Color(0xFFFCE4EC), Color(0xFFF8BBD0), Color(0xFF81C784), Color(0xFFE91E63), Color(0xFFFFD54F), "petals"),
    CRYSTAL("Crystal",
        Color(0xFF4A148C), Color(0xFF00BCD4), Color(0xFF311B92), Color(0xFFE1F5FE), Color(0xFF80DEEA), "fireflies"),

    RAIN("Rain",
        Color(0xFF455A64), Color(0xFF607D8B), Color(0xFF37474F), Color(0xFFB0BEC5), Color(0xFFB3E5FC), "rain"),
    STORM("Storm",
        Color(0xFF263238), Color(0xFF37474F), Color(0xFF1B1B1B), Color(0xFFFFD54F), Color(0xFFFFFFFF), "lightning"),
    LIGHTNING("Lightning",
        Color(0xFF1A1A2E), Color(0xFF2C2C54), Color(0xFF0A0A1A), Color(0xFFFFFF00), Color(0xFFFFFFFF), "lightning"),
    DUST("Dust",
        Color(0xFFBCAAA4), Color(0xFFD7CCC8), Color(0xFF8D6E63), Color(0xFFFFB74D), Color(0xFFFFE0B2), "dust")
}

/**
 * Maps a level number (1..1500+) to a deterministic background + theme.
 * The mapping cycles through BackgroundType values, with the cycle length
 * growing as the player progresses (early levels change every 25 levels
 * for variety; later levels change every 50 for visual stability).
 */
object BackgroundCatalog {
    fun forLevel(level: Int): BackgroundType {
        val cycleLength = when {
            level <= 100  -> 20   // fast variety in the tutorial zone
            level <= 500  -> 30
            level <= 1000 -> 40
            else          -> 50
        }
        val index = ((level - 1) % cycleLength)
        // Offset by a prime so the cycle doesn't always start on the same
        // background at level 1.
        val all = BackgroundType.entries
        val safeIndex = ((index + 7) % all.size + all.size) % all.size
        return all[safeIndex]
    }
}

/**
 * Full-screen animated background. Drawn behind the white maze grid in
 * GamePlayScreen. Uses a single `rememberInfiniteTransition` to drive
 * all animation, so it composes efficiently.
 */
@Composable
fun AnimatedBackground(
    type: BackgroundType,
    theme: Theme,
    modifier: Modifier = Modifier.fillMaxSize()
) {
    val transition = rememberInfiniteTransition(label = "bg-${type.name}")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = type.tempoMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t-${type.name}"
    )
    val t2 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = type.tempoMs * 2, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "t2-${type.name}"
    )

    // Deterministic per-type random (so the same type always renders the same
    // element layout).
    val rng = remember(type) { Random(type.ordinal * 9973L + 17L) }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1) Sky gradient (top to ground)
        drawSkyGradient(w, h, type)

        // 2) Ground / horizon band
        drawGround(w, h, type)

        // 3) Distant scenery (mountains, trees, buildings) — depends on type
        drawScenery(w, h, type, t, rng)

        // 4) Foreground elements (cacti, buildings, mushrooms, etc.) — type specific
        drawForeground(w, h, type, t, rng)

        // 5) Animated particles — the "live" layer
        drawParticles(w, h, type, t, t2, theme, rng)
    }
}

private val BackgroundType.tempoMs: Int
    get() = when (animation) {
        "snow", "rain"      -> 4000
        "sand", "dust"      -> 6000
        "bubbles", "fish"   -> 7000
        "stars"             -> 12000
        "aurora"            -> 9000
        "lava", "fire"      -> 3000
        "neon"              -> 2000
        "lightning"         -> 3500
        "petals"            -> 9000
        "fireflies"         -> 5000
        else                -> 8000
    }

private fun DrawScope.drawSkyGradient(w: Float, h: Float, type: BackgroundType) {
    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(type.skyTop, type.skyBottom),
            startY = 0f,
            endY = h
        ),
        size = Size(w, h)
    )
}

private fun DrawScope.drawGround(w: Float, h: Float, type: BackgroundType) {
    val groundY = h * 0.78f
    drawRect(
        color = type.ground,
        topLeft = Offset(0f, groundY),
        size = Size(w, h - groundY)
    )
}

private fun DrawScope.drawScenery(
    w: Float, h: Float, type: BackgroundType, t: Float, rng: Random
) {
    val horizon = h * 0.78f
    when (type) {
        BackgroundType.SKY, BackgroundType.SUNSET, BackgroundType.SUNRISE,
        BackgroundType.BEACH, BackgroundType.OASIS -> {
            // Soft mountain silhouettes
            val path = Path().apply {
                moveTo(0f, horizon)
                var x = 0f
                val step = w / 7f
                while (x <= w) {
                    val peakY = horizon - (40f + (sin(x * 0.013 + type.ordinal) * 35f))
                    lineTo(x, peakY)
                    x += step / 2
                }
                lineTo(w, horizon)
                close()
            }
            drawPath(path, type.ground.copy(alpha = 0.55f))
        }
        BackgroundType.FOREST, BackgroundType.JUNGLE, BackgroundType.BAMBOO,
        BackgroundType.AUTUMN, BackgroundType.CHERRY_BLOSSOM, BackgroundType.ENCHANTED,
        BackgroundType.MUSHROOM, BackgroundType.SAVANNA -> {
            // Tree line silhouettes
            val treeCount = 14
            val baseY = horizon
            for (i in 0..treeCount) {
                val x = i * (w / treeCount) + rng.nextFloat() * 12f
                val treeH = 28f + rng.nextFloat() * 40f
                val treeW = 18f + rng.nextFloat() * 14f
                val trunk = Path().apply {
                    moveTo(x - treeW * 0.15f, baseY)
                    lineTo(x + treeW * 0.15f, baseY)
                    lineTo(x + treeW * 0.08f, baseY - treeH * 0.4f)
                    lineTo(x - treeW * 0.08f, baseY - treeH * 0.4f)
                    close()
                }
                val canopy = Path().apply {
                    moveTo(x, baseY - treeH)
                    lineTo(x - treeW * 0.5f, baseY - treeH * 0.45f)
                    lineTo(x + treeW * 0.5f, baseY - treeH * 0.45f)
                    close()
                }
                drawPath(trunk, type.ground.copy(alpha = 0.6f))
                drawPath(canopy, type.ground.copy(alpha = 0.45f))
            }
        }
        BackgroundType.DESERT, BackgroundType.SAND_DUNES, BackgroundType.MARS, BackgroundType.CACTUS -> {
            // Wavy dunes
            val path = Path().apply {
                moveTo(0f, horizon)
                var x = 0f
                val step = w / 16f
                while (x <= w) {
                    val y = horizon - (12f + sin(x * 0.02f + type.ordinal) * 10f)
                    lineTo(x, y)
                    x += step
                }
                lineTo(w, horizon)
                close()
            }
            drawPath(path, type.ground.copy(alpha = 0.5f))
        }
        BackgroundType.NEON_CITY, BackgroundType.CYBERPUNK, BackgroundType.SYNTHWAVE,
        BackgroundType.VAPORWAVE -> {
            // City skyline — rectangular buildings
            val buildingCount = 10
            for (i in 0 until buildingCount) {
                val x = i * (w / buildingCount) + 4f
                val bw = (w / buildingCount) - 8f
                val bh = 40f + rng.nextFloat() * 80f
                drawRect(
                    color = type.ground.copy(alpha = 0.7f),
                    topLeft = Offset(x, horizon - bh),
                    size = Size(bw, bh)
                )
                // Windows
                for (wy in 0 until (bh / 12f).toInt()) {
                    for (wx in 0 until (bw / 10f).toInt()) {
                        if (rng.nextFloat() > 0.6f) {
                            drawRect(
                                color = type.accent.copy(alpha = 0.7f + rng.nextFloat() * 0.3f),
                                topLeft = Offset(x + 3f + wx * 10f, horizon - bh + 4f + wy * 12f),
                                size = Size(5f, 6f)
                            )
                        }
                    }
                }
            }
        }
        BackgroundType.ICE_CAVE, BackgroundType.DEEP_SEA -> {
            // Crystal pillars or coral
            val pillarCount = 8
            for (i in 0 until pillarCount) {
                val x = i * (w / pillarCount) + rng.nextFloat() * 10f
                val pw = 10f + rng.nextFloat() * 20f
                val ph = 30f + rng.nextFloat() * 60f
                val path = Path().apply {
                    moveTo(x, horizon)
                    lineTo(x + pw / 2f, horizon - ph)
                    lineTo(x + pw, horizon)
                    close()
                }
                drawPath(path, type.ground.copy(alpha = 0.45f))
            }
        }
        BackgroundType.SPACE, BackgroundType.NEBULA, BackgroundType.GALAXY, BackgroundType.MOON -> {
            // Distant nebula clouds
            for (i in 0 until 5) {
                val cx = rng.nextFloat() * w
                val cy = rng.nextFloat() * (horizon * 0.8f)
                val r = 40f + rng.nextFloat() * 80f
                drawCircle(
                    color = type.accent.copy(alpha = 0.08f + rng.nextFloat() * 0.12f),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
        BackgroundType.VOLCANO, BackgroundType.LAVA, BackgroundType.MAGMA -> {
            // Mountain silhouette
            val path = Path().apply {
                moveTo(0f, horizon)
                lineTo(w * 0.3f, horizon - 80f)
                lineTo(w * 0.5f, horizon - 130f)
                lineTo(w * 0.7f, horizon - 70f)
                lineTo(w, horizon)
                close()
            }
            drawPath(path, type.ground.copy(alpha = 0.7f))
        }
        else -> { /* scenery-less backgrounds (snow, tundra, etc.) */ }
    }
}

private fun DrawScope.drawForeground(
    w: Float, h: Float, type: BackgroundType, t: Float, rng: Random
) {
    val horizon = h * 0.78f
    when (type) {
        BackgroundType.CACTUS -> {
            // A couple of cacti silhouettes
            for (i in 0 until 3) {
                val x = w * (0.15f + i * 0.3f) + rng.nextFloat() * 20f
                val baseY = horizon + (h - horizon) * 0.5f
                val ch = 35f + rng.nextFloat() * 20f
                val cw = 10f
                drawRect(
                    color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                    topLeft = Offset(x - cw / 2f, baseY - ch),
                    size = Size(cw, ch)
                )
                // Arm
                drawRect(
                    color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                    topLeft = Offset(x + cw / 2f, baseY - ch * 0.6f),
                    size = Size(8f, 6f)
                )
                drawRect(
                    color = Color(0xFF2E7D32).copy(alpha = 0.85f),
                    topLeft = Offset(x + cw / 2f + 4f, baseY - ch * 0.8f),
                    size = Size(4f, ch * 0.25f)
                )
            }
        }
        BackgroundType.MUSHROOM -> {
            for (i in 0 until 6) {
                val x = w * (0.08f + i * 0.16f) + rng.nextFloat() * 12f
                val baseY = horizon + (h - horizon) * 0.65f
                val stemH = 18f + rng.nextFloat() * 14f
                val capW = 22f + rng.nextFloat() * 12f
                val capH = 12f + rng.nextFloat() * 8f
                drawRect(
                    color = Color(0xFFFFF8E1),
                    topLeft = Offset(x - 4f, baseY - stemH),
                    size = Size(8f, stemH)
                )
                val cap = Path().apply {
                    moveTo(x - capW / 2f, baseY - stemH)
                    lineTo(x + capW / 2f, baseY - stemH)
                    lineTo(x + capW * 0.35f, baseY - stemH - capH)
                    lineTo(x - capW * 0.35f, baseY - stemH - capH)
                    close()
                }
                drawPath(cap, type.accent)
                // Spots
                drawCircle(Color.White, 2.5f, Offset(x - capW * 0.2f, baseY - stemH - capH * 0.5f))
                drawCircle(Color.White, 2.0f, Offset(x + capW * 0.15f, baseY - stemH - capH * 0.3f))
            }
        }
        BackgroundType.CRYSTAL -> {
            for (i in 0 until 5) {
                val x = w * (0.1f + i * 0.18f) + rng.nextFloat() * 16f
                val baseY = horizon + (h - horizon) * 0.7f
                val ch = 25f + rng.nextFloat() * 20f
                val cw = 8f + rng.nextFloat() * 6f
                val p = Path().apply {
                    moveTo(x, baseY)
                    lineTo(x + cw, baseY - ch * 0.4f)
                    lineTo(x + cw * 0.4f, baseY - ch)
                    lineTo(x - cw * 0.4f, baseY - ch * 0.6f)
                    lineTo(x - cw, baseY - ch * 0.2f)
                    close()
                }
                drawPath(p, type.accent.copy(alpha = 0.6f))
                drawPath(p, Color.White.copy(alpha = 0.5f),
                    style = Stroke(width = 1.5f))
            }
        }
        BackgroundType.NEON_CITY, BackgroundType.CYBERPUNK, BackgroundType.SYNTHWAVE,
        BackgroundType.VAPORWAVE -> {
            // Sun/grid horizon (synthwave trademark)
            val sunY = horizon - 50f
            val sunR = 60f
            drawCircle(type.accent, sunR, Offset(w / 2f, sunY))
            // Horizontal lines (grid)
            for (i in 1..6) {
                val y = horizon + (h - horizon) * (i / 7f)
                drawLine(
                    color = type.accent.copy(alpha = 0.6f),
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1.5f
                )
            }
        }
        BackgroundType.VOLCANO, BackgroundType.LAVA, BackgroundType.MAGMA -> {
            // Glow at the base
            for (i in 0 until 5) {
                val cx = rng.nextFloat() * w
                val cy = horizon + (h - horizon) * (0.2f + rng.nextFloat() * 0.5f)
                val r = 18f + rng.nextFloat() * 22f
                drawCircle(
                    color = type.accent.copy(alpha = 0.18f + rng.nextFloat() * 0.12f),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
        }
        BackgroundType.CORAL_REEF, BackgroundType.OCEAN -> {
            // Coral silhouettes
            for (i in 0 until 4) {
                val x = w * (0.12f + i * 0.25f)
                val baseY = horizon + (h - horizon) * 0.85f
                val ch = 30f + rng.nextFloat() * 20f
                val p = Path().apply {
                    moveTo(x - 5f, baseY)
                    lineTo(x + 5f, baseY)
                    lineTo(x + 2f, baseY - ch)
                    lineTo(x - 2f, baseY - ch)
                    close()
                }
                drawPath(p, type.ground.copy(alpha = 0.7f))
            }
        }
        BackgroundType.RAINBOW -> {
            // Rainbow arc
            for (i in 0 until 7) {
                val color = when (i) {
                    0 -> Color(0xFFE53935)
                    1 -> Color(0xFFFB8C00)
                    2 -> Color(0xFFFDD835)
                    3 -> Color(0xFF43A047)
                    4 -> Color(0xFF1E88E5)
                    5 -> Color(0xFF3949AB)
                    else -> Color(0xFF8E24AA)
                }
                drawArc(
                    color = color.copy(alpha = 0.55f),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(w * 0.1f, h * 0.15f),
                    size = Size(w * 0.8f, w * 0.8f),
                    style = Stroke(width = 8f)
                )
            }
        }
        else -> { /* no foreground */ }
    }
}

private fun DrawScope.drawParticles(
    w: Float, h: Float, type: BackgroundType, t: Float, t2: Float,
    theme: Theme, rng: Random
) {
    val count = when (type.animation) {
        "snow", "rain", "sand", "dust"     -> 60
        "bubbles", "fireflies", "petals"  -> 24
        "stars"                            -> 90
        "fish"                             -> 8
        "birds"                            -> 5
        "neon"                             -> 30
        "lightning"                        -> 0
        "lava", "fire"                     -> 20
        "aurora"                           -> 0  // drawn as bands below
        else                                -> 18
    }

    for (i in 0 until count) {
        val seed = (i * 9301L + type.ordinal * 49297L) and 0x7FFFFFFF
        val r = Random(seed)
        val baseX = r.nextFloat() * w
        val speed = 0.3f + r.nextFloat() * 0.7f
        val sizePx = 1.5f + r.nextFloat() * 4f
        val phase = r.nextFloat()

        when (type.animation) {
            "snow" -> {
                val x = (baseX + t * w * 0.4f * speed + phase * w) % w
                val y = ((t + phase) * h * speed) % h
                drawCircle(
                    color = type.particle.copy(alpha = 0.8f),
                    radius = sizePx,
                    center = Offset(x, y)
                )
            }
            "rain" -> {
                val x = (baseX + t * w * 1.5f * speed) % w
                val y = ((t + phase) * h * 2.0f * speed) % h
                drawLine(
                    color = type.particle.copy(alpha = 0.6f),
                    start = Offset(x, y),
                    end = Offset(x - 1.5f, y + 8f),
                    strokeWidth = 1.2f
                )
            }
            "sand", "dust" -> {
                val x = (baseX + t * w * 0.3f * speed + sin(phase * 6.28f) * 20f)
                val y = (phase * h + t * 30f * speed) % h
                drawCircle(
                    color = type.particle.copy(alpha = 0.4f),
                    radius = sizePx,
                    center = Offset(x.coerceIn(0f, w), y)
                )
            }
            "bubbles" -> {
                val x = (baseX + sin((t + phase) * 6.28f) * 18f)
                val y = h - ((t + phase) * h * 0.5f * speed) % h
                drawCircle(
                    color = type.particle.copy(alpha = 0.5f),
                    radius = sizePx * 2.5f,
                    center = Offset(x.coerceIn(0f, w), y.coerceIn(0f, h)),
                    style = Stroke(width = 1.2f)
                )
            }
            "fish" -> {
                val x = (baseX + t * w * 0.6f * speed) % w
                val y = phase * h * 0.7f
                val tailX = x - 8f * speed
                drawLine(
                    color = type.accent.copy(alpha = 0.7f),
                    start = Offset(x, y),
                    end = Offset(tailX, y - 3f),
                    strokeWidth = 2f
                )
                drawLine(
                    color = type.accent.copy(alpha = 0.7f),
                    start = Offset(x, y),
                    end = Offset(tailX, y + 3f),
                    strokeWidth = 2f
                )
            }
            "birds" -> {
                val x = (baseX + t * w * 0.25f * speed + phase * w) % (w + 60f) - 30f
                val y = h * (0.08f + phase * 0.6f) + sin((t + phase) * 6.28f) * 6f
                val wingSpan = 8f
                drawLine(type.particle, Offset(x - wingSpan, y), Offset(x, y - 3f), strokeWidth = 1.2f)
                drawLine(type.particle, Offset(x, y - 3f), Offset(x + wingSpan, y), strokeWidth = 1.2f)
            }
            "stars" -> {
                val x = baseX
                val y = phase * h * 0.85f
                val twinkle = 0.5f + 0.5f * sin((t * 6.28f + phase * 12f))
                drawCircle(
                    color = type.particle.copy(alpha = 0.4f + 0.6f * twinkle),
                    radius = 0.8f + sizePx * 0.4f * twinkle,
                    center = Offset(x, y)
                )
            }
            "petals" -> {
                val x = (baseX + sin((t + phase) * 3.14f) * 25f)
                val y = ((t + phase) * h * 0.3f * speed) % h
                drawCircle(
                    color = type.particle.copy(alpha = 0.85f),
                    radius = sizePx * 1.4f,
                    center = Offset(x.coerceIn(0f, w), y)
                )
            }
            "fireflies" -> {
                val x = (baseX + sin((t + phase) * 4f) * 30f).coerceIn(0f, w)
                val y = (phase * h + cos((t + phase) * 3f) * 20f).coerceIn(0f, h)
                val pulse = 0.5f + 0.5f * sin((t2 + phase) * 6.28f)
                drawCircle(
                    color = type.particle.copy(alpha = 0.3f + 0.6f * pulse),
                    radius = 3f + pulse * 4f,
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f * pulse),
                    radius = 1.2f,
                    center = Offset(x, y)
                )
            }
            "lava" -> {
                val x = baseX
                val y = h - ((t + phase) * h * 0.4f * speed) % h
                drawCircle(
                    color = type.accent.copy(alpha = 0.7f - phase * 0.5f),
                    radius = 2f + sizePx,
                    center = Offset(x, y.coerceIn(h * 0.4f, h))
                )
            }
            "fire" -> {
                val x = (baseX + sin((t + phase) * 8f) * 6f)
                val y = h * 0.95f - ((t + phase) * h * 0.3f * speed) % (h * 0.5f)
                drawCircle(
                    color = type.particle.copy(alpha = 0.9f - phase),
                    radius = 2f + sizePx,
                    center = Offset(x, y)
                )
            }
            "neon" -> {
                // Pulsing dots
                val x = baseX
                val y = phase * h * 0.9f
                val pulse = 0.4f + 0.6f * sin((t * 6.28f + phase * 10f))
                drawCircle(
                    color = type.accent.copy(alpha = 0.4f + 0.5f * pulse),
                    radius = 2f + sizePx * 0.6f,
                    center = Offset(x, y)
                )
            }
            "aurora" -> { /* drawn separately */ }
            "lightning" -> { /* drawn separately */ }
        }
    }

    // Aurora bands (drawn over particles, behind everything else? actually over)
    if (type.animation == "aurora") {
        val path = Path().apply {
            moveTo(0f, h * 0.2f)
            for (i in 0..20) {
                val x = w * i / 20f
                val y = h * 0.25f + sin((i * 0.6f) + t * 6.28f) * 22f + sin((i * 0.3f) + t2 * 4f) * 12f
                lineTo(x, y)
            }
            for (i in 20 downTo 0) {
                val x = w * i / 20f
                val y = h * 0.25f + sin((i * 0.6f) + t * 6.28f) * 22f + sin((i * 0.3f) + t2 * 4f) * 12f + 40f
                lineTo(x, y)
            }
            close()
        }
        drawPath(path, type.accent.copy(alpha = 0.25f))
    }

    // Lightning flashes
    if (type.animation == "lightning" && t > 0.92f) {
        drawRect(
            color = Color.White.copy(alpha = (t - 0.92f) * 4f),
            topLeft = Offset.Zero,
            size = Size(w, h)
        )
    }
}
