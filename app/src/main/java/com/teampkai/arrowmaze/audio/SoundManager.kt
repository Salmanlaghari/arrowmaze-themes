package com.teampkai.arrowmaze.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.teampkai.arrowmaze.R

class SoundManager(private val context: Context) {

    @Volatile
    var soundEnabled: Boolean = true

    private val soundPool: SoundPool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private var correctId: Int = 0
    private var wrongId: Int = 0
    private var levelCompleteId: Int = 0
    private var buttonTapId: Int = 0

    private val loaded: MutableMap<Int, Boolean> = mutableMapOf()

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, _ ->
            loaded[sampleId] = true
        }
        correctId = soundPool.load(context, R.raw.correct_move, 1)
        wrongId = soundPool.load(context, R.raw.wrong_move, 1)
        levelCompleteId = soundPool.load(context, R.raw.level_complete, 1)
        buttonTapId = soundPool.load(context, R.raw.button_tap, 1)
    }

    fun playCorrectMove() = play(correctId)
    fun playWrongMove() = play(wrongId)
    fun playLevelComplete() = play(levelCompleteId)
    fun playButtonTap() = play(buttonTapId)

    private fun play(sampleId: Int) {
        if (!soundEnabled) return
        if (sampleId == 0) return
        soundPool.play(sampleId, 1f, 1f, 1, 0, 1f)
    }

    fun release() {
        soundPool.release()
    }
}
