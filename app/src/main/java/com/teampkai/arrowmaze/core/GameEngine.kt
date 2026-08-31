package com.teampkai.arrowmaze.core

import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.generator.MazeGenerator
import com.teampkai.arrowmaze.generator.MazeResult

data class GameState(
    val currentLevel: Int = 1,
    val lives: Int = 1,
    val score: Int = 0,
    val currentPathIndex: Int = 0,
    val isLevelComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val maze: MazeResult? = null,
    val themeId: Int = 1
)

sealed class MoveResult {
    data class Correct(val stepIndex: Int) : MoveResult()
    object Wrong : MoveResult()
    object LevelComplete : MoveResult()
}

class GameEngine {

    var state: GameState = GameState()
        private set

    fun startLevel(level: Int, seed: Long = System.currentTimeMillis()): GameState {
        val maze = MazeGenerator.generate(level, seed)
        state = GameState(
            currentLevel = level,
            lives = 1,
            score = state.score,
            currentPathIndex = 0,
            isLevelComplete = false,
            isGameOver = false,
            maze = maze,
            themeId = state.themeId
        )
        return state
    }

    fun setTheme(themeId: Int) {
        state = state.copy(themeId = themeId)
    }

    fun startNewGame(): GameState {
        state = GameState()
        return startLevel(1)
    }

    fun makeMove(row: Int, col: Int): MoveResult {
        val maze = state.maze ?: return MoveResult.Wrong
        val path = maze.solutionPath

        if (state.isLevelComplete || state.isGameOver) return MoveResult.Wrong

        val expectedPos = path[state.currentPathIndex]
        if (row != expectedPos.first || col != expectedPos.second) {
            // Wrong move
            state = state.copy(
                lives = state.lives - 1,
                isGameOver = true
            )
            return MoveResult.Wrong
        }

        // Correct move
        val newIndex = state.currentPathIndex + 1

        return if (newIndex >= path.size) {
            // Level complete!
            val bonus = (100 * state.currentLevel)
            state = state.copy(
                currentPathIndex = newIndex,
                isLevelComplete = true,
                score = state.score + bonus
            )
            MoveResult.LevelComplete
        } else {
            state = state.copy(
                currentPathIndex = newIndex,
                score = state.score + 10
            )
            MoveResult.Correct(newIndex)
        }
    }

    fun advanceToNextLevel(): GameState {
        val nextLevel = state.currentLevel + 1
        return startLevel(nextLevel)
    }

    fun retryLevel(): GameState {
        return startLevel(state.currentLevel)
    }

    fun jumpToLevel(level: Int): GameState {
        return startLevel(level.coerceAtLeast(1))
    }

    fun getCurrentMaze(): MazeResult? = state.maze

    fun getDirectionAt(row: Int, col: Int): Direction? {
        return state.maze?.grid?.getOrNull(row)?.getOrNull(col)?.direction
    }
}
