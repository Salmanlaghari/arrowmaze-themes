package com.teampkai.arrowmaze.core

import com.teampkai.arrowmaze.generator.ArrowPath
import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.generator.MazeGenerator
import com.teampkai.arrowmaze.generator.MazeResult

/**
 * The puzzle's runtime state for the winding-path "Arrows Escape" mechanic.
 *
 * The original maze (and its paths) is NEVER mutated; clearing is tracked by
 * path id in [clearedPathIds]. A path is removed by sliding the whole line
 * out through its corridor, so per-cell tracking is unnecessary — a path is
 * either fully on the board or fully gone.
 */
data class GameState(
    val currentLevel: Int = 1,
    val highestLevelUnlocked: Int = 1,
    val lives: Int = 3,
    val score: Int = 0,
    val clearedPathIds: Set<Int> = emptySet(),
    val isLevelComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val maze: MazeResult? = null,
    val mazeSeed: Long = 0L,
    /** Number of hints the player can still use on the current level. */
    val hintsRemaining: Int = 3,
    val themeId: Int = 1
) {
    /** Paths still on the board. */
    val remainingPathIds: Set<Int>
        get() {
            val m = maze ?: return emptySet()
            return m.paths.map { it.id }.toSet() - clearedPathIds
        }

    /** Cells still covered by an uncleared path (for rendering/hit-tests). */
    val remainingCells: Set<Pair<Int, Int>>
        get() {
            val m = maze ?: return emptySet()
            return m.cellToPathId.filterValues { it !in clearedPathIds }.keys
        }
}

sealed class MoveResult {
    /** The tapped path slid out of the board cleanly. */
    data class ArrowCleared(
        val pathId: Int,
        val head: Pair<Int, Int>,
        val exitDirection: Direction
    ) : MoveResult()

    /** Another line blocks the exit corridor; the path stays and shakes red. */
    data class Blocked(
        val pathId: Int,
        val head: Pair<Int, Int>,
        val exitDirection: Direction
    ) : MoveResult()

    /** Lives reached 0. The level has been reset to its original layout. */
    data class GameOver(val resetLives: Int) : MoveResult()

    /** The last path was cleared; the level is complete. */
    object LevelComplete : MoveResult()

    /** Tap was on an empty/already-cleared cell — silent no-op. */
    object Ignored : MoveResult()
}

class GameEngine(initialState: GameState = GameState()) {

    var state: GameState = initialState
        private set

    fun startLevel(level: Int, seed: Long = System.currentTimeMillis()): GameState {
        val maze = MazeGenerator.generate(level, seed)
        state = GameState(
            currentLevel = level,
            highestLevelUnlocked = maxOf(state.highestLevelUnlocked, level, state.currentLevel),
            lives = 3,
            score = state.score,
            clearedPathIds = emptySet(),
            isLevelComplete = false,
            isGameOver = false,
            maze = maze,
            mazeSeed = seed,
            hintsRemaining = 3,
            themeId = state.themeId
        )
        return state
    }

    /**
     * Consume one hint and return the head cell of a currently-clearable
     * path (or null). Applies a small score penalty, as before.
     */
    fun useHint(): Pair<Int, Int>? {
        if (state.hintsRemaining <= 0) return null
        val target = findClearableHint() ?: return null
        state = state.copy(
            hintsRemaining = state.hintsRemaining - 1,
            score = (state.score - 50).coerceAtLeast(0)
        )
        return target
    }

    fun setTheme(themeId: Int) {
        state = state.copy(themeId = themeId)
    }

    fun startNewGame(): GameState {
        state = GameState()
        return startLevel(1)
    }

    /**
     * Player tapped a cell. Resolves the cell to its winding path and traces
     * the exit ray along the path's corridor: if any board cell in the
     * corridor is still occupied by ANOTHER uncleared path, the move is
     * blocked; otherwise the whole line slides out and is removed.
     */
    fun tapArrow(row: Int, col: Int): MoveResult {
        val maze = state.maze ?: return MoveResult.Ignored
        if (state.isLevelComplete || state.isGameOver) return MoveResult.Ignored

        val pathId = maze.cellToPathId[Pair(row, col)] ?: return MoveResult.Ignored
        if (pathId in state.clearedPathIds) return MoveResult.Ignored
        val path = maze.pathsById[pathId] ?: return MoveResult.Ignored

        val blocked = path.corridorCells.any { cell ->
            val occupying = maze.cellToPathId[cell]
            occupying != null && occupying != pathId && occupying !in state.clearedPathIds
        }

        return if (!blocked) {
            val newCleared = state.clearedPathIds + pathId
            if (newCleared.size == maze.paths.size) {
                val bonus = 100 * state.currentLevel
                state = state.copy(
                    clearedPathIds = newCleared,
                    isLevelComplete = true,
                    score = state.score + 10 + bonus,
                    highestLevelUnlocked = maxOf(state.highestLevelUnlocked, state.currentLevel)
                )
                MoveResult.LevelComplete
            } else {
                state = state.copy(
                    clearedPathIds = newCleared,
                    score = state.score + 10
                )
                MoveResult.ArrowCleared(pathId, path.head, path.exitDirection)
            }
        } else {
            val newLives = state.lives - 1
            if (newLives <= 0) {
                // Reset level to its starting arrangement (same seed).
                val reset = startLevel(state.currentLevel, state.mazeSeed)
                state = reset.copy(
                    isGameOver = true,
                    lives = 0
                )
                MoveResult.GameOver(resetLives = 0)
            } else {
                state = state.copy(lives = newLives)
                MoveResult.Blocked(pathId, path.head, path.exitDirection)
            }
        }
    }

    /**
     * For hint / preview: returns the head of a path whose corridor is
     * currently clear (a valid next move), or null. Always recomputed from
     * the live state — never trusts the placement order for liveness.
     */
    fun findClearableHint(): Pair<Int, Int>? {
        val maze = state.maze ?: return null
        for (path in maze.paths) {
            if (path.id in state.clearedPathIds) continue
            val blocked = path.corridorCells.any { cell ->
                val occupying = maze.cellToPathId[cell]
                occupying != null && occupying != path.id && occupying !in state.clearedPathIds
            }
            if (!blocked) return path.head
        }
        return null
    }

    fun advanceToNextLevel(): GameState {
        val nextLevel = state.currentLevel + 1
        return startLevel(nextLevel)
    }

    /** Reset the *current* level to its original layout (same seed). */
    fun resetCurrentLevel(): GameState {
        return startLevel(state.currentLevel, state.mazeSeed)
    }

    fun retryLevel(): GameState = resetCurrentLevel()

    fun jumpToLevel(level: Int): GameState {
        return startLevel(level.coerceAtLeast(1))
    }

    fun getCurrentMaze(): MazeResult? = state.maze

    /** The path occupying a cell, if any (and not yet cleared). */
    fun pathAt(row: Int, col: Int): ArrowPath? {
        val maze = state.maze ?: return null
        val id = maze.cellToPathId[Pair(row, col)] ?: return null
        if (id in state.clearedPathIds) return null
        return maze.pathsById[id]
    }
}
