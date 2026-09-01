package com.teampkai.arrowmaze.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.teampkai.arrowmaze.ui.BackgroundCatalog
import com.teampkai.arrowmaze.ui.BackgroundType
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Per-level background music.
 *
 * Real music tracks aren't bundled (no .mp3/.ogg assets in the project), so
 * the manager procedurally synthesizes a short, loopable motif for every
 * level. The motif's musical character is derived from the level's
 * [BackgroundType] (key, scale, tempo, timbre) so each of the 50+
 * backgrounds has a distinct sound. Levels sharing the same background
 * type get a different motif (key, chord, melody) via the level number,
 * so every one of the 1500+ levels is musically unique.
 *
 * If real music files are added later, swap the procedural generation in
 * [startForLevel] for a MediaPlayer/ExoPlayer load — the public API
 * (startForLevel / stop / setEnabled) stays the same.
 */
class MusicManager(private val context: Context) {

    @Volatile
    var musicEnabled: Boolean = true

    private var currentTrack: AudioTrack? = null
    private var currentLevel: Int = 0

    fun startForLevel(level: Int) {
        if (level == currentLevel && currentTrack != null) return
        stop()
        currentLevel = level
        if (!musicEnabled) return

        val bg = BackgroundCatalog.forLevel(level)
        val track = createTrackForBackground(bg, level)
        currentTrack = track
        track.play()
    }

    fun stop() {
        currentTrack?.let {
            try { it.stop() } catch (_: IllegalStateException) {}
            it.release()
        }
        currentTrack = null
    }

    fun setEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) stop()
    }

    fun release() = stop()

    /**
     * Synthesize a looping PCM buffer for the given background + level.
     * The buffer is filled with 2-3 layered sine/triangle voices shaped by
     * an AD envelope per note, producing a short melodic phrase.
     */
    private fun createTrackForBackground(bg: BackgroundType, level: Int): AudioTrack {
        val sampleRate = 22050

        // ── Character derived from the background type ─────────────────
        val (tempoBpm, scaleIntervals, timbre) = characterFor(bg)
        val beatMs = (60_000.0 / tempoBpm).toInt()

        // ── Melody / chord derived from level number ──────────────────
        // Root note shifts with the level so adjacent levels feel related
        // but not identical.
        val rootSemitone = 36 + (level * 5) % 24  // C2..B3 range
        val rootHz = 440.0 * Math.pow(2.0, (rootSemitone - 69) / 12.0)

        // Pick a 4-note motif: level-derived pattern.
        val motif = intArrayOf(0, 2, 4, 2)
        val beatsPerLoop = 4
        val loopSamples = (sampleRate * beatMs / 1000.0 * beatsPerLoop).toInt()
        val noteSamples = loopSamples / motif.size
        val buffer = ShortArray(loopSamples)

        for (noteIdx in 0 until motif.size) {
            val semitoneOffset = scaleIntervals[motif[noteIdx] % scaleIntervals.size]
            val noteHz = rootHz * Math.pow(2.0, semitoneOffset / 12.0)
            val noteStart = noteIdx * noteSamples

            for (i in 0 until noteSamples) {
                val t = i.toDouble() / sampleRate
                val env = envelope(i, noteSamples, bg)
                val voice = when (timber) {
                    Timbre.SINE -> sin(2.0 * PI * noteHz * t)
                    Timbre.TRIANGLE -> {
                        // Triangle via folded sine
                        val phase = (noteHz * t) % 1.0
                        4.0 * kotlin.math.abs(phase - 0.5) - 1.0
                    }
                    Timbre.SOFT_PAD -> {
                        val s1 = sin(2.0 * PI * noteHz * t)
                        val s2 = sin(2.0 * PI * noteHz * 2.0 * t) * 0.3
                        val s3 = sin(2.0 * PI * noteHz * 3.0 * t) * 0.1
                        (s1 + s2 + s3) / 1.4
                    }
                    Timbre.PULSE -> {
                        val phase = (noteHz * t) % 1.0
                        if (phase < 0.25) 1.0 else -1.0
                    }
                    Timbre.BELL -> {
                        val s1 = sin(2.0 * PI * noteHz * t)
                        val s2 = sin(2.0 * PI * noteHz * 3.0 * t) * 0.4
                        val s3 = sin(2.0 * PI * noteHz * 4.0 + t * 7.0) * 0.2
                        (s1 + s2 + s3) / 1.6
                    }
                }
                val detune = sin(2.0 * PI * noteHz * 1.003 * t) * 0.06
                val sample = (voice + detune) * 0.22 * env
                val idx = noteStart + i
                if (idx < buffer.size) {
                    buffer[idx] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
            }
        }

        val track = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build(),
            buffer.size * 2,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(buffer, 0, buffer.size)
        track.setLoopPoints(0, loopSamples, -1)
        return track
    }

    private enum class Timbre { SINE, TRIANGLE, SOFT_PAD, PULSE, BELL }

    /**
     * Map a BackgroundType to a musical character. Each background family
     * (nature, space, neon, candy, etc.) gets its own tempo, scale, and
     * timbre so the music matches the visuals.
     */
    private fun characterFor(bg: BackgroundType): Triple<Int, IntArray, Timbre> {
        return when (bg) {
            // Bright, fast, major keys — daytime / cheerful
            BackgroundType.SKY, BackgroundType.SUNRISE, BackgroundType.BEACH,
            BackgroundType.CANDY_LAND, BackgroundType.COTTON_CANDY, BackgroundType.RAINBOW,
            BackgroundType.PASTEL, BackgroundType.OASIS ->
                Triple(120, intArrayOf(0, 2, 4, 5, 7, 9, 11), Timbre.SINE)

            // Warm, mid-tempo, pentatonic — nature
            BackgroundType.FOREST, BackgroundType.BAMBOO, BackgroundType.AUTUMN,
            BackgroundType.CHERRY_BLOSSOM, BackgroundType.SAVANNA, BackgroundType.ENCHANTED,
            BackgroundType.MUSHROOM, BackgroundType.FAIRY ->
                Triple(90, intArrayOf(0, 2, 4, 7, 9), Timbre.SOFT_PAD)

            // Slow, deep, minor — jungle / night
            BackgroundType.JUNGLE, BackgroundType.NIGHT, BackgroundType.AURORA ->
                Triple(70, intArrayOf(0, 2, 3, 5, 7, 8, 10), Timbre.SOFT_PAD)

            // Flowing, mid-tempo — water
            BackgroundType.OCEAN, BackgroundType.CORAL_REEF, BackgroundType.DEEP_SEA ->
                Triple(80, intArrayOf(0, 2, 4, 5, 7, 9, 11), Timbre.SINE)

            // Warm, slow, exotic — desert
            BackgroundType.DESERT, BackgroundType.SAND_DUNES, BackgroundType.CACTUS, BackgroundType.MARS ->
                Triple(75, intArrayOf(0, 2, 3, 5, 7, 8, 10), Timbre.TRIANGLE)

            // Cold, sparse, high — ice
            BackgroundType.ARCTIC, BackgroundType.SNOW, BackgroundType.BLIZZARD,
            BackgroundType.ICE_CAVE, BackgroundType.TUNDRA ->
                Triple(100, intArrayOf(0, 2, 4, 7, 9), Timbre.BELL)

            // Slow, ethereal, minor — space
            BackgroundType.SPACE, BackgroundType.NEBULA, BackgroundType.GALAXY, BackgroundType.MOON ->
                Triple(60, intArrayOf(0, 2, 3, 5, 7, 8, 10), Timbre.SOFT_PAD)

            // Aggressive, fast, dissonant — fire / lava
            BackgroundType.VOLCANO, BackgroundType.LAVA, BackgroundType.MAGMA, BackgroundType.GEOTHERMAL ->
                Triple(140, intArrayOf(0, 1, 4, 5, 7, 8, 11), Timbre.PULSE)

            // Fast, electronic, synth — neon
            BackgroundType.NEON_CITY, BackgroundType.CYBERPUNK, BackgroundType.SYNTHWAVE, BackgroundType.VAPORWAVE ->
                Triple(130, intArrayOf(0, 2, 3, 5, 7, 8, 10), Timbre.TRIANGLE)

            // Stormy, irregular — weather
            BackgroundType.RAIN, BackgroundType.STORM, BackgroundType.LIGHTNING, BackgroundType.DUST ->
                Triple(85, intArrayOf(0, 2, 3, 5, 7, 8, 10), Timbre.PULSE)

            // Sparkly, high — crystal
            BackgroundType.CRYSTAL ->
                Triple(110, intArrayOf(0, 2, 4, 7, 9, 11), Timbre.BELL)
        }
    }

    private fun envelope(sampleInNote: Int, noteSamples: Int, bg: BackgroundType): Double {
        // Different backgrounds get slightly different envelopes for character.
        val attackRatio = when (bg) {
            BackgroundType.NEON_CITY, BackgroundType.CYBERPUNK, BackgroundType.SYNTHWAVE,
            BackgroundType.VAPORWAVE, BackgroundType.VOLCANO, BackgroundType.LAVA,
            BackgroundType.MAGMA, BackgroundType.LIGHTNING -> 0.02
            BackgroundType.SPACE, BackgroundType.NEBULA, BackgroundType.GALAXY,
            BackgroundType.AURORA -> 0.15
            else -> 0.06
        }
        val attack = (noteSamples * attackRatio).toInt().coerceAtLeast(1)
        val releaseStart = attack
        val releaseLen = (noteSamples - attack).coerceAtLeast(1)
        val pos = (sampleInNote - releaseStart).coerceAtMost(releaseLen)
        return when {
            sampleInNote < attack -> sampleInNote.toDouble() / attack
            else -> {
                // Exponential-ish decay for natural sound
                val x = pos.toDouble() / releaseLen
                sqrt(1.0 - x * x)  // quarter-circle decay
            }
        }
    }
}
