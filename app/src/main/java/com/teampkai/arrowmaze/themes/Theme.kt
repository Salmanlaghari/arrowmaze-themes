package com.teampkai.arrowmaze.themes

import androidx.compose.ui.graphics.Color

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
            primary = Color(0xFF81C784),     // light green
            secondary = Color(0xFF6D4C41),   // brown
            accent = Color(0xFFFFD54F),      // golden yellow
            glow = Color(0x804CAF50)         // semi-transparent green
        ),
        ambientAnimation = "birds",
        parallaxLayers = listOf(
            ParallaxLayer(speed = 0.1f, drawType = "mountains"),
            ParallaxLayer(speed = 0.3f, drawType = "trees_far"),
            ParallaxLayer(speed = 0.6f, drawType = "trees_near")
        )
    )

    // Placeholder for future themes
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

    fun getTheme(id: Int): Theme = allThemes.firstOrNull { it.id == id } ?: JUNGLE
}
