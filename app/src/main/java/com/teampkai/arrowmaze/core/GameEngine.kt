package com.teampkai.arrowmaze.core

import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.generator.MazeGenerator
import com.teampkai.arrowmaze.generator.MazeResult

/**
 * The puzzle's runtime state.
 *
 *   - `clearedCells` holds every (row, col) the player has successfully removed.
 *     The original (maze) grid is NEVER mutated; the displayed grid is
 *     `maze.grid` with `clearedCells` overlaid.
 *   - `lives` starts at 3 and decrements on every blocked tap. When it hits 0
 *     the level resets (the original `maze` is replayed) and the player can
 *     try again with the same starting arrangement.
 *   - `mazeSeed` is captured when a level is first started so `resetLevel`
 *     can replay the exact same arrow layout (per-step-3 requirement: do NOT
 *     regenerate; restore the original).
 */
data class GameState(
    val currentLevel: Int = 1,
    val highestLevelUnlocked: Int = 1,
    val lives: Int = 3,
    val score: Int = 0,
    val clearedCells: Set<Pair<Int, Int>> = emptySet(),
    val isLevelComplete: Boolean = false,
    val isGameOver: Boolean = false,
    val maze: MazeResult? = null,
    val mazeSeed: Long = 0L,
    val themeId: Int = 1
) {
    /** Cells that still hold an arrow and have not been cleared. */
    val remainingArrowCells: Set<Pair<Int, Int>>
        get() {
            val m = maze ?: return emptySet()
            val out = mutableSetOf<Pair<Int, Int>>()
            for (row in m.grid) for (cell in row) {
                if (cell.hasArrow && Pair(cell.row, cell.col) !in clearedCells) {
                    out.add(Pair(cell.row, cell.col))
                }
            }
            return out
        }
}

sealed class MoveResult {
    /** An arrow was successfully cleared. */
    data class ArrowCleared(
        val row: Int,
        val col: Int,
        val direction: Direction
    ) : MoveResult()

    /** Tapped arrow was blocked. Lives were decremented. */
    data class Blocked(val row: Int, val col: Int, val direction: Direction) : MoveResult()

    /** Lives reached 0. The level has been reset to its original layout. */
    data class GameOver(val resetLives: Int) : MoveResult()

    /** The last arrow was cleared; the level is complete. */
    object LevelComplete : MoveResult()

    /** Tap was on an already-cleared or out-of-bounds cell — silent no-op. */
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
            clearedCells = emptySet(),
            isLevelComplete = false,
            isGameOver = false,
            maze = maze,
            mazeSeed = seed,
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

    /**
     * Player tapped cell (row, col). Validates against the *current* remaining
     * arrows and the arrow's direction; does NOT use generationOrder to gate
     * moves (the player may clear in any valid order).
     */
    fun tapArrow(row: Int, col: Int): MoveResult {
        val maze = state.maze ?: return MoveResult.Ignored
        if (state.isLevelComplete || state.isGameOver) return MoveResult.Ignored

        val pos = Pair(row, col)
        if (pos in state.clearedCells) return MoveResult.Ignored
        val cell = maze.grid.getOrNull(row)?.getOrNull(col) ?: return MoveResult.Ignored
        if (!cell.hasArrow) return MoveResult.Ignored

        val remaining = state.remainingArrowCells
        val path = cellsUntilEdge(row, col, cell.direction, maze.gridSize)
        val pathClear = path.isNotEmpty() && path.all { it !in remaining }

        return if (pathClear) {
            val newCleared = state.clearedCells + pos
            val remainingAfter = remaining - pos
            if (remainingAfter.isEmpty()) {
                val bonus = 100 * state.currentLevel
                state = state.copy(
                    clearedCells = newCleared,
                    isLevelComplete = true,
                    score = state.score + 10 + bonus,
                    highestLevelUnlocked = maxOf(state.highestLevelUnlocked, state.currentLevel)
                )
                MoveResult.LevelComplete
            } else {
                state = state.copy(
                    clearedCells = newCleared,
                    score = state.score + 10
                )
                MoveResult.ArrowCleared(row, col, cell.direction)
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
                MoveResult.Blocked(row, col, cell.direction)
            }
        }
    }

    /**
     * For hint / preview: returns a cell from `remaining` whose path is currently
     * clear (i.e. a valid next move), or null if none exists. Always recomputes
     * from the *current* state — never relies on generationOrder for liveness.
     */
    fun findClearableHint(): Pair<Int, Int>? {
        val maze = state.maze ?: return null
        val remaining = state.remainingArrowCells
        for (pos in remaining) {
            val (r, c) = pos
            val cell = maze.grid[r][c]
            val path = cellsUntilEdge(r, c, cell.direction, maze.gridSize)
            if (path.isNotEmpty() && path.all { it !in remaining }) {
                return pos
            }
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

    fun getDirectionAt(row: Int, col: Int): Direction? {
        val m = state.maze ?: return null
        val cell = m.grid.getOrNull(row)?.getOrNull(col) ?: return null
        if (Pair(row, col) in state.clearedCells) return null
        return cell.direction
    }

    /**
     * Returns the cells an arrow at (row, col) pointing in `dir` would traverse
     * before reaching the grid edge (exclusive of the start cell). Empty list
     * if the arrow is already on that edge (degenerate case).
     */
    private fun cellsUntilEdge(row: Int, col: Int, dir: Direction, gridSize: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var r = row
        var c = col
        var stepped = false
        while (true) {
            val nr = r + dr(dir)
            val nc = c + dc(dir)
            if (nr !in 0 until gridSize || nc !in 0 until gridSize) break
            r = nr
            c = nc
            out.add(Pair(r, c))
            stepped = true
        }
        return if (stepped) out else emptyList()
    }

    private fun dr(dir: Direction): Int = when (dir) {
        Direction.UP -> -1
        Direction.DOWN -> 1
        Direction.LEFT, Direction.RIGHT -> 0
    }

    private fun dc(dir: Direction): Int = when (dir) {
        Direction.LEFT -> -1
        Direction.RIGHT -> 1
        Direction.UP, Direction.DOWN -> 0
    }
}
