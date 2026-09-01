package com.teampkai.arrowmaze.themes

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

data class Theme(
    val id: Int,
    val name: String,
    val backgroundColors: List<Color>,
    val arrowPalette: ArrowPalette,
    val ambientAnimation: String,
    val parallaxLayers: List<ParallaxLayer>
)

data class ArrowPalette(
    val primary: Color,
    val secondary: Color,
    val accent: Color,
    val glow: Color
)

data class ParallaxLayer(
    val speed: Float,
    val drawType: String
)

object ThemeRegistry {

    val JUNGLE = Theme(
        id = 1,
        name = "Jungle",
        backgroundColors = listOf(
            Color(0xFF1B5E20),
            Color(0xFF2E7D32),
            Color(0xFF4CAF50)
        ),
        arrowPalette = ArrowPalette(
            primary = Color(0xFF81C784),
            secondary = Color(0xFF6D4C41),
            accent = Color(0xFFFFD54F),
            glow = Color(0x804CAF50)
        ),
        ambientAnimation = "birds",
        parallaxLayers = listOf(
            ParallaxLayer(speed = 0.1f, drawType = "mountains"),
            ParallaxLayer(speed = 0.3f, drawType = "trees_far"),
            ParallaxLayer(speed = 0.6f, drawType = "trees_near")
        )
    )

    val OCEAN = Theme(
        id = 2,
        name = "Ocean",
        backgroundColors = listOf(
            Color(0xFF0D47A1),
            Color(0xFF1565C0),
            Color(0xFF42A5F5)
        ),
        arrowPalette = ArrowPalette(
            primary = Color(0xFF90CAF9),
            secondary = Color(0xFF004D40),
            accent = Color(0xFFFFCC02),
            glow = Color(0x8042A5F5)
        ),
        ambientAnimation = "bubbles",
        parallaxLayers = listOf(
            ParallaxLayer(speed = 0.15f, drawType = "deep_sea"),
            ParallaxLayer(speed = 0.4f, drawType = "coral"),
            ParallaxLayer(speed = 0.7f, drawType = "fish")
        )
    )

    val allThemes = listOf(JUNGLE, OCEAN)

    /**
     * Named theme palette seeds. Each entry gives a base hue and a theme name.
     * `themeForLevel` cycles through these so every level gets a distinct
     * color identity while reusing the same overall look.
     */
    private data class PaletteSeed(
        val name: String,
        val baseHue: Float,       // 0..360
        val saturation: Float,    // 0..1
        val lightness: Float      // 0..1
    )

    private val paletteSeeds = listOf(
        PaletteSeed("Sky",       210f, 0.75f, 0.50f),
        PaletteSeed("Ocean",     195f, 0.70f, 0.45f),
        PaletteSeed("Forest",    130f, 0.55f, 0.40f),
        PaletteSeed("Sunset",     20f, 0.80f, 0.55f),
        PaletteSeed("Lavender",  270f, 0.55f, 0.55f),
        PaletteSeed("Coral",     350f, 0.70f, 0.55f),
        PaletteSeed("Mint",      160f, 0.60f, 0.45f),
        PaletteSeed("Amber",      40f, 0.85f, 0.50f),
        PaletteSeed("Violet",    285f, 0.65f, 0.50f),
        PaletteSeed("Teal",      180f, 0.65f, 0.40f),
        PaletteSeed("Rose",      330f, 0.60f, 0.55f),
        PaletteSeed("Lime",       90f, 0.65f, 0.45f)
    )

    /**
     * Returns a unique theme for the given level number. The first two levels
     * use the canonical Jungle/Ocean themes; subsequent levels cycle through
     * procedural palette seeds so every level feels visually distinct.
     */
    fun themeForLevel(level: Int): Theme {
        if (level == 1) return JUNGLE
        if (level == 2) return OCEAN
        val seedIndex = ((level - 3) % paletteSeeds.size + paletteSeeds.size) % paletteSeeds.size
        val seed = paletteSeeds[seedIndex]
        val tier = (level - 1) / paletteSeeds.size  // 0,1,2,... shifts hue slightly
        val hue = (seed.baseHue + tier * 7f) % 360f
        val primary = hslToColor(hue, seed.saturation, seed.lightness)
        val secondary = hslToColor((hue + 200f) % 360f, seed.saturation * 0.7f, seed.lightness * 0.5f)
        val accent = hslToColor((hue + 30f) % 360f, seed.saturation * 0.9f, (seed.lightness + 0.15f).coerceAtMost(0.7f))
        val glow = primary.copy(alpha = 0.5f)
        return Theme(
            id = level,
            name = "${seed.name} ${tier + 1}",
            backgroundColors = listOf(
                hslToColor(hue, seed.saturation * 0.6f, (seed.lightness * 0.4f).coerceAtLeast(0.15f)),
                hslToColor(hue, seed.saturation * 0.8f, seed.lightness * 0.6f),
                hslToColor(hue, seed.saturation, (seed.lightness + 0.15f).coerceAtMost(0.7f))
            ),
            arrowPalette = ArrowPalette(
                primary = primary,
                secondary = secondary,
                accent = accent,
                glow = glow
            ),
            ambientAnimation = if (level % 2 == 0) "birds" else "bubbles",
            parallaxLayers = listOf(
                ParallaxLayer(speed = 0.1f, drawType = "mountains"),
                ParallaxLayer(speed = 0.3f, drawType = "trees_far"),
                ParallaxLayer(speed = 0.6f, drawType = "trees_near")
            )
        )
    }

    fun getTheme(id: Int): Theme = allThemes.firstOrNull { it.id == id } ?: JUNGLE

    /**
     * Convert HSL to an ARGB Color. Used by the procedural theme generator.
     */
    private fun hslToColor(h: Float, s: Float, l: Float): Color {
        val hh = ((h % 360f) + 360f) % 360f
        val c = (1f - abs(2f * l - 1f)) * s
        val x = c * (1f - abs((hh / 60f) % 2f - 1f))
        val m = l - c / 2f
        val (rp, gp, bp) = when {
            hh < 60f  -> Triple(c, x, 0f)
            hh < 120f -> Triple(x, c, 0f)
            hh < 180f -> Triple(0f, c, x)
            hh < 240f -> Triple(0f, x, c)
            hh < 300f -> Triple(x, 0f, c)
            else      -> Triple(c, 0f, x)
        }
        return Color(
            red = (rp + m).coerceIn(0f, 1f),
            green = (gp + m).coerceIn(0f, 1f),
            blue = (bp + m).coerceIn(0f, 1f),
            alpha = 1f
        )
    }
}
