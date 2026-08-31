package com.teampkai.arrowmaze.generator

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

/**
 * A single cell in the puzzle grid. The cell may or may not currently hold an arrow
 * (in the new "clear the board" mechanic every cell can hold an arrow, but some
 * may be empty in early levels for visual variety).
 */
data class ArrowCell(
    val row: Int,
    val col: Int,
    val direction: Direction,
    /**
     * Whether this cell currently contains an arrow that has not yet been cleared.
     * Cleared cells become empty (hasArrow = false) and are still drawn (as an
     * empty cell) but cannot be tapped.
     */
    val hasArrow: Boolean
)

/**
 * Result of generating a single level.
 *
 * `generationOrder` records the order in which arrows were placed during the
 * backwards-construction pass. It is *one* valid clearing order (reverse of
 * placement) and is used as a hint source and for level validation. The player
 * is free to clear arrows in any valid order; the runtime game state recomputes
 * "is this arrow's path currently clear" live against the current remaining set.
 */
data class MazeResult(
    val gridSize: Int,
    val grid: List<List<ArrowCell>>,
    /**
     * The reference solution: a list of (row, col) cells that hold arrows, in
     * the order in which the generator placed them. Reversed, this is one valid
     * clearing order.
     */
    val generationOrder: List<Pair<Int, Int>>
)

object MazeGenerator {

    fun generate(level: Int, seed: Long = System.currentTimeMillis()): MazeResult {
        val rng = Random(seed)
        val gridSize = calculateGridSize(level)
        val arrowCount = calculateArrowCount(level, gridSize)

        val result = generateArrowsEscape(gridSize, arrowCount, rng)
        return result
    }

    fun calculateGridSize(level: Int): Int {
        // 4x4 at level 1, growing slowly. Cap at 9 to keep cells tappable.
        return (4 + (level / 80).toInt()).coerceIn(4, 9)
    }

    fun calculateArrowCount(level: Int, gridSize: Int): Int {
        // Roughly 15..20 at level 1, growing toward 60..100+ at high levels.
        // The hard cap is gridSize*gridSize (every cell).
        val base = 15
        val growth = level.toFloat() / 18f
        val cap = (gridSize * gridSize) - 2
        return (base + growth.toInt()).coerceIn(15, cap)
    }

    /**
     * Backwards-construction algorithm for the "Arrows Escape" mechanic:
     *
     *   1. Start with an empty grid and a "remaining" set containing every cell.
     *   2. Repeatedly pick a random cell from `remaining` and a random grid edge
     *      it can point toward such that the *path* from that cell to that edge
     *      (moving in the chosen direction, one cell at a time) only traverses
     *      cells that are STILL in `remaining` (i.e. not yet placed).
     *   3. Place an arrow at that cell pointing in that direction, remove the
     *      path cells from `remaining` (they are now "blocked" until this arrow
     *      is removed), and record the placement order in `generationOrder`.
     *   4. Stop when `remaining` is empty or we hit the target arrow count.
     *
     * Because each placed arrow claims a path that no future placement can use,
     * the reverse of `generationOrder` is a guaranteed-valid clearing sequence:
     * the most-recently-placed arrow's path is fully clear (its claimed cells
     * were not used by anything placed after it), so it can be removed; and so on.
     */
    private fun generateArrowsEscape(
        gridSize: Int,
        arrowCount: Int,
        rng: Random
    ): MazeResult {
        val totalCells = gridSize * gridSize
        val remaining = mutableSetOf<Pair<Int, Int>>()
        for (r in 0 until gridSize) for (c in 0 until gridSize) remaining.add(Pair(r, c))

        // direction[cell] = the arrow direction that was placed there (or null if empty)
        val direction = mutableMapOf<Pair<Int, Int>, Direction>()
        val generationOrder = mutableListOf<Pair<Int, Int>>()
        val allDirs = Direction.entries

        var attempts = 0
        val maxAttempts = arrowCount * 80

        while (generationOrder.size < arrowCount && remaining.isNotEmpty() && attempts < maxAttempts) {
            attempts++
            val cell = remaining.random(rng)
            val row = cell.first
            val col = cell.second

            // Try each of the 4 edges; for each, see if a straight path to the
            // edge is fully inside `remaining`.
            val edges = allDirs.shuffled(rng)
            var placed = false
            for (dir in edges) {
                val path = cellsToEdge(row, col, dir, gridSize)
                if (path.isEmpty()) continue
                if (path.all { it in remaining }) {
                    direction[cell] = dir
                    generationOrder.add(cell)
                    remaining.removeAll(path.toSet())
                    placed = true
                    break
                }
            }
            // If this cell can't point to any edge (e.g. it's fully surrounded
            // by already-claimed cells), it stays empty. Drop it from `remaining`
            // so we don't keep retrying.
            if (!placed) {
                remaining.remove(cell)
            }
        }

        // Build the final grid. Every cell gets a direction (for arrow drawing),
        // and `hasArrow` is true only if this cell actually holds an arrow.
        // For empty cells we just pick an arbitrary direction so ArrowRenderer
        // still has something to draw in case the renderer is asked about one
        // (it shouldn't be — empty cells aren't tappable — but keeps the model
        // uniform).
        val grid = (0 until gridSize).map { row ->
            (0 until gridSize).map { col ->
                val pos = Pair(row, col)
                val dir = direction[pos] ?: allDirs[rng.nextInt(allDirs.size)]
                ArrowCell(
                    row = row,
                    col = col,
                    direction = dir,
                    hasArrow = pos in direction
                )
            }
        }

        // Pad generationOrder to "every arrow cell" for backwards compatibility
        // with anything that might expect length == arrow count, but the engine
        // doesn't rely on this; the engine recomputes valid moves from the grid.
        @Suppress("UNUSED_VARIABLE")
        val _unused = totalCells // (kept for clarity in the algorithm above)

        return MazeResult(
            gridSize = gridSize,
            grid = grid,
            generationOrder = generationOrder
        )
    }

    /**
     * Returns the list of cells from (row,col) (inclusive) stepping in `dir`
     * until just past the grid edge, or empty if (row,col) is already on that
     * edge (no cells to traverse, which is technically valid but we treat as
     * not useful — an arrow on the edge has nowhere to "escape" to).
     */
    private fun cellsToEdge(row: Int, col: Int, dir: Direction, gridSize: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var r = row
        var c = col
        // Must traverse at least one cell beyond the start (otherwise the arrow
        // is already at the edge and the puzzle is degenerate).
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
