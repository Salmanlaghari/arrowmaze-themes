package com.teampkai.arrowmaze.generator

import kotlin.math.roundToInt
import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

fun Direction.opposite(): Direction = when (this) {
    Direction.UP -> Direction.DOWN
    Direction.DOWN -> Direction.UP
    Direction.LEFT -> Direction.RIGHT
    Direction.RIGHT -> Direction.LEFT
}

/**
 * A single grid cell (kept for backward compatibility with legacy consumers
 * and as a fast occupancy lookup).
 */
data class ArrowCell(
    val row: Int,
    val col: Int,
    /** Direction of the path segment leaving this cell toward the head. */
    val direction: Direction,
    val hasArrow: Boolean
)

/**
 * One continuous, winding arrow line on the board.
 *
 * A path is an ordered chain of cells from its [tail] to its [head], bending
 * through the grid, with an [exitDirection] arrowhead at the head. Tapping any
 * cell of the path attempts to slide the WHOLE line out of the board along
 * [exitDirection]; it succeeds only if every cell of [corridorCells] (the
 * straight run from the head to the board edge) is free of other paths.
 */
data class ArrowPath(
    val id: Int,
    /** Cells of the line in order: tail (start) → head (arrowhead). */
    val cells: List<Pair<Int, Int>>,
    /** Direction the arrowhead points (also the slide-out direction). */
    val exitDirection: Direction,
    /** Straight cells from head+1 to the board edge along [exitDirection]. */
    val corridorCells: List<Pair<Int, Int>>
) {
    val head: Pair<Int, Int> get() = cells.last()
    val tail: Pair<Int, Int> get() = cells.first()
}

/**
 * Result of generating a single level: a dense field of interlocking winding
 * paths plus lookup indexes used by the engine and renderer.
 */
data class MazeResult(
    val gridSize: Int,
    val grid: List<List<ArrowCell>>,
    /**
     * Head cells of the paths in placement order. Reversed, this is one valid
     * clearing order (the reverse-solve guarantee below).
     */
    val generationOrder: List<Pair<Int, Int>>,
    val paths: List<ArrowPath>,
    /** cell → path id for every occupied cell. */
    val cellToPathId: Map<Pair<Int, Int>, Int>
) {
    val pathsById: Map<Int, ArrowPath> get() = paths.associateBy { it.id }
}

object MazeGenerator {

    /** How many seeds to try when hunting for a dense board. */
    private const val DENSITY_ATTEMPTS = 7

    /** Minimum acceptable fraction of grid cells covered by path bodies. */
    private const val MIN_COVERAGE = 0.75f

    fun generate(level: Int, seed: Long = System.currentTimeMillis()): MazeResult {
        val gridSize = calculateGridSize(level)
        val targetPaths = calculatePathCount(level, gridSize)
        val totalCells = gridSize * gridSize

        // Density-first: the greedy placement can stall early on an unlucky
        // seed, so generate several boards and keep the DENSEST one. Spec
        // requires the grid to be packed 80–90% full with interlocking paths.
        var best: MazeResult? = null
        var bestCoverage = -1
        var currentSeed = seed
        var attempt = 0
        // A real loop (not `repeat {}`) so `break` below is legal —
        // break/continue are forbidden inside lambdas in Kotlin.
        while (attempt < DENSITY_ATTEMPTS) {
            attempt++
            val result = generateWindingMaze(gridSize, targetPaths, Random(currentSeed))
            val coverage = result.cellToPathId.size
            if (coverage > bestCoverage) {
                bestCoverage = coverage
                best = result
            }
            if (bestCoverage >= totalCells * MIN_COVERAGE) break
            currentSeed = currentSeed * 6364136223846793005L + 1442695040888963407L
        }
        return best ?: generateWindingMaze(gridSize, targetPaths, Random(seed))
    }

    /**
     * Spec curve: Level 1 = 8×8, Level 10 = 15×15, Level 50 = 25×25, then +1
     * every 10 levels (capped so cells stay tappable).
     */
    fun calculateGridSize(level: Int): Int {
        val g = when {
            level <= 10 -> 8 + ((level - 1) * 7) / 9      // 8 → 15
            level <= 50 -> 15 + ((level - 10) * 10) / 40  // 15 → 25
            else -> 25 + (level - 50) / 10
        }
        return g.coerceIn(8, 34)
    }

    /**
     * Target number of winding paths, calibrated for 80–90% cell coverage:
     * bodies average ~4 cells and every path also needs headroom for its
     * exit corridor, so ~17–20% of cells worth of paths packs the tightest.
     */
    fun calculatePathCount(level: Int, gridSize: Int): Int {
        val cells = gridSize * gridSize
        val base = (cells * 0.18f).toInt() + level / 32
        val maxDensity = (cells * 0.28f).toInt()
        return base.coerceIn(8, maxDensity.coerceAtLeast(8))
    }

    /**
     * Reverse-solving generator for winding multi-segment paths.
     *
     * Paths are placed one at a time; each placement claims:
     *   - its BODY (the winding line cells), and
     *   - requires its CORRIDOR (the straight run from head to the nearest
     *     edge along the arrowhead direction) to be free of all previously
     *     placed bodies.
     *
     * Later paths may not cross earlier corridors, but they MAY park their
     * bodies inside them — that's exactly the interlock: the later path blocks
     * the earlier one until it is removed. Clearing in reverse placement order
     * is therefore always valid (when path i is cleared, only bodies 1..i-1
     * remain, and corridor_i was verified clear against exactly those), giving
     * 100% solvability with no deadlocks — and the player can also find other
     * valid orders, with illegal taps shaking + flashing red.
     */
    private fun generateWindingMaze(
        gridSize: Int,
        targetPaths: Int,
        rng: Random
    ): MazeResult {
        val occupied = HashSet<Pair<Int, Int>>()      // bodies of placed paths
        val cellToPath = HashMap<Pair<Int, Int>, Int>()
        val paths = mutableListOf<ArrowPath>()
        val generationOrder = mutableListOf<Pair<Int, Int>>()
        val allDirs = Direction.entries

        val maxBody = 4 + gridSize / 2
        var attempts = 0
        val maxAttempts = targetPaths * 130

        /** Smallest body a normal placement accepts. */
        val minBody = 2

        while (paths.size < targetPaths && attempts < maxAttempts) {
            attempts++

            // ── 1. Random head + arrowhead direction with a clear corridor ──
            val head = Pair(rng.nextInt(gridSize), rng.nextInt(gridSize))
            if (head in occupied) continue
            val dir = allDirs[rng.nextInt(allDirs.size)]
            val corridor = corridorOf(head, dir, gridSize)
            if (corridor.any { it in occupied }) continue

            // ── 2. Wind the body backward from the head ──────────────────
            val body = buildWindingBody(head, dir, corridor, occupied, gridSize, maxBody, rng)
                ?: continue
            if (body.size < minBody) continue

            // ── 3. Commit the path ───────────────────────────────────────
            val path = ArrowPath(
                id = paths.size,
                cells = body.asReversed(),   // store tail → head
                exitDirection = dir,
                corridorCells = corridor
            )
            for (cell in body) {
                occupied.add(cell)
                cellToPath[cell] = path.id
            }
            paths.add(path)
            generationOrder.add(head)
        }

        // Density fallback: greedy placement stalls when no multi-cell body
        // fits, but the spec requires a tightly packed board — so sweep every
        // free cell and squeeze in a short path wherever a corridor is clear.
        if (paths.size < targetPaths) {
            for (row in 0 until gridSize) {
                if (paths.size >= targetPaths) break
                for (col in 0 until gridSize) {
                    if (paths.size >= targetPaths) break
                    val head = Pair(row, col)
                    if (head in occupied) continue
                    for (dir in allDirs) {
                        val corridor = corridorOf(head, dir, gridSize)
                        if (corridor.any { it in occupied }) continue
                        val body = buildWindingBody(head, dir, corridor, occupied, gridSize, 2, rng)
                            ?: continue
                        val path = ArrowPath(
                            id = paths.size,
                            cells = body.asReversed(),
                            exitDirection = dir,
                            corridorCells = corridor
                        )
                        for (cell in body) {
                            occupied.add(cell)
                            cellToPath[cell] = path.id
                        }
                        paths.add(path)
                        generationOrder.add(head)
                        break
                    }
                }
            }
        }

        // Legacy grid view: every path cell "has an arrow"; direction points
        // along the path toward its head (head cells use the exit direction).
        val dirByCell = HashMap<Pair<Int, Int>, Direction>()
        for (path in paths) {
            for (i in path.cells.indices) {
                val cell = path.cells[i]
                dirByCell[cell] = if (i == path.cells.size - 1) {
                    path.exitDirection
                } else {
                    val next = path.cells[i + 1]
                    directionBetween(cell, next)
                }
            }
        }
        val grid = (0 until gridSize).map { row ->
            (0 until gridSize).map { col ->
                val pos = Pair(row, col)
                ArrowCell(
                    row = row,
                    col = col,
                    direction = dirByCell[pos] ?: allDirs[rng.nextInt(allDirs.size)],
                    hasArrow = dirByCell.containsKey(pos)
                )
            }
        }

        return MazeResult(
            gridSize = gridSize,
            grid = grid,
            generationOrder = generationOrder,
            paths = paths,
            cellToPathId = cellToPath
        )
    }

    /**
     * Grows a winding polyline backward from the head. At every step it
     * forbids: leaving the grid, revisiting itself, entering the exit
     * corridor (a line must never cross its own escape path) and entering
     * cells occupied by other paths. Turns are weighted over straights so
     * lines bend and weave like maze paths.
     */
    private fun buildWindingBody(
        head: Pair<Int, Int>,
        exitDir: Direction,
        corridor: List<Pair<Int, Int>>,
        occupied: Set<Pair<Int, Int>>,
        gridSize: Int,
        maxBody: Int,
        rng: Random
    ): List<Pair<Int, Int>>? {
        val corridorSet = corridor.toHashSet()
        val body = mutableListOf(head)
        val inBody = hashSetOf(head)
        var cur = head
        var arrival: Direction? = null
        val targetLen = 2 + rng.nextInt((maxBody - 1).coerceAtLeast(1))  // 2..maxBody

        while (body.size < targetLen) {
            val options = allSteps(cur)
                .filter { (d, _) -> arrival == null || d != arrival.opposite() }
                .filter { (_, n) ->
                    n.first in 0 until gridSize &&
                        n.second in 0 until gridSize &&
                        n !in inBody &&
                        n !in corridorSet &&
                        n !in occupied
                }
            if (options.isEmpty()) break

            // Snapshot into an immutable local — Kotlin can't smart-cast the
            // mutable `arrival` var inside the capturing lambda below.
            val lastArrival = arrival
            val chosen = if (lastArrival == null) {
                options[rng.nextInt(options.size)]
            } else {
                // Weight: turning 3×, continuing straight 1× → winding feel.
                val weighted = options.flatMap { (d, n) ->
                    if (d == lastArrival.opposite() || d == lastArrival) listOf(d to n) else listOf(d to n, d to n, d to n)
                }
                weighted[rng.nextInt(weighted.size)]
            }
            body.add(chosen.second)
            inBody.add(chosen.second)
            arrival = chosen.first
            cur = chosen.second
        }
        return if (body.size >= 2) body else null
    }

    private fun allSteps(cell: Pair<Int, Int>): List<Pair<Direction, Pair<Int, Int>>> = listOf(
        Direction.UP to Pair(cell.first - 1, cell.second),
        Direction.DOWN to Pair(cell.first + 1, cell.second),
        Direction.LEFT to Pair(cell.first, cell.second - 1),
        Direction.RIGHT to Pair(cell.first, cell.second + 1)
    )

    /** Cells strictly after [from] stepping in [dir] until the grid edge. */
    private fun corridorOf(from: Pair<Int, Int>, dir: Direction, gridSize: Int): List<Pair<Int, Int>> {
        val out = mutableListOf<Pair<Int, Int>>()
        var r = from.first + dr(dir)
        var c = from.second + dc(dir)
        while (r in 0 until gridSize && c in 0 until gridSize) {
            out.add(Pair(r, c))
            r += dr(dir)
            c += dc(dir)
        }
        return out
    }

    private fun directionBetween(a: Pair<Int, Int>, b: Pair<Int, Int>): Direction = when {
        b.first < a.first -> Direction.UP
        b.first > a.first -> Direction.DOWN
        b.second < a.second -> Direction.LEFT
        else -> Direction.RIGHT
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
