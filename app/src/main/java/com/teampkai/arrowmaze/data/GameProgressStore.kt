package com.teampkai.arrowmaze.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class GameProgress(
    val highestLevel: Int,
    val score: Int,
    val themeId: Int
)

private val Context.gameProgressDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "game_progress"
)

class GameProgressStore(private val context: Context) {

    private val dataStore: DataStore<Preferences> get() = context.gameProgressDataStore

    val progress: Flow<GameProgress> = dataStore.data.map { prefs ->
        GameProgress(
            highestLevel = prefs[HIGHEST_LEVEL] ?: 1,
            score = prefs[SCORE] ?: 0,
            themeId = prefs[THEME_ID] ?: 1
        )
    }

    suspend fun saveProgress(highestLevel: Int, score: Int, themeId: Int) {
        dataStore.edit { prefs ->
            prefs[HIGHEST_LEVEL] = highestLevel
            prefs[SCORE] = score
            prefs[THEME_ID] = themeId
        }
    }

    companion object {
        private val HIGHEST_LEVEL = intPreferencesKey("highest_level")
        private val SCORE = intPreferencesKey("score")
        private val THEME_ID = intPreferencesKey("theme_id")
    }
}