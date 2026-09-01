package com.teampkai.arrowmaze.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import androidx.compose.ui.graphics.Color
import com.teampkai.arrowmaze.themes.ThemeRegistry
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Per-level background music.
 *
 * Real music tracks aren't bundled (no .mp3/.ogg assets in the project), so
 * the manager generates a short, loopable procedural tone cluster for each
 * level. The cluster's key, tempo, and timbre are derived from the level
 * number via [ThemeRegistry.themeForLevel], so every level sounds
 * distinct while still being a pleasant, low-volume background pad.
 *
 * If real music files are added later, swap the procedural generation in
 * [start] for a MediaPlayer/ExoPlayer load — the public API
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

        val theme = ThemeRegistry.themeForLevel(level)
        // Derive a musical key from the theme's primary hue (0..360 → 0..11).
        val hue = hueOf(theme.arrowPalette.primary)
        val rootHz = 220.0 * Math.pow(2.0, (hue / 360.0) * 2.0) // A3 base, octave-ish sweep
        val tempoBpm = 80 + (level % 40)          // 80..119 bpm
        val beatMs = (60_000.0 / tempoBpm).toInt()

        val track = createToneTrack(rootHz, beatMs, level)
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

    fun release() {
        stop()
    }

    private fun createToneTrack(rootHz: Double, beatMs: Int, level: Int): AudioTrack {
        val sampleRate = 22050
        val beatsPerLoop = 4
        val loopSamples = (sampleRate * beatMs / 1000.0 * beatsPerLoop).toInt()
        val buffer = ShortArray(loopSamples)

        // A short motif: root, fifth, octave, fifth — sine waves blended.
        val intervals = doubleArrayOf(1.0, 1.5, 2.0, 1.5)
        val segSamples = loopSamples / intervals.size
        for (i in 0 until loopSamples) {
            val seg = (i / segSamples).coerceAtMost(intervals.size - 1)
            val freq = rootHz * intervals[seg]
            val t = i.toDouble() / sampleRate
            val env = envelope(i % segSamples, segSamples)
            val s = sin(2.0 * PI * freq * t) * 0.18 * env
            // Add a soft detuned second voice for richness.
            val s2 = sin(2.0 * PI * freq * 1.005 * t) * 0.10 * env
            buffer[i] = ((s + s2) * Short.MAX_VALUE).toInt().toShort()
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

    private fun envelope(sampleInSeg: Int, segSamples: Int): Double {
        // Simple AD envelope: quick attack, long release.
        val attack = (segSamples * 0.05).toInt().coerceAtLeast(1)
        return when {
            sampleInSeg < attack -> sampleInSeg.toDouble() / attack
            else -> {
                val releaseStart = attack
                val releaseLen = (segSamples - attack).coerceAtLeast(1)
                val pos = (sampleInSeg - releaseStart).coerceAtMost(releaseLen)
                1.0 - (pos.toDouble() / releaseLen)
            }
        }
    }

    private fun hueOf(color: Color): Float {
        val r = color.red
        val g = color.green
        val b = color.blue
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        if (d == 0f) return 0f
        val h = when (max) {
            r -> ((g - b) / d) % 6f
            g -> ((b - r) / d) + 2f
            else -> ((r - g) / d) + 4f
        }
        return ((h * 60f) + 360f) % 360f
    }
}
