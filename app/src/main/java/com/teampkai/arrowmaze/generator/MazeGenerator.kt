package com.teampkai.arrowmaze.generator

import kotlin.random.Random

enum class Direction { UP, DOWN, LEFT, RIGHT }

data class ArrowCell(
    val row: Int,
    val col: Int,
    val direction: Direction,
    val isOnPath: Boolean
)

data class MazeResult(
    val gridSize: Int,
    val grid: List<List<ArrowCell>>,
    val solutionPath: List<Pair<Int, Int>>,
    val start: Pair<Int, Int>,
    val end: Pair<Int, Int>
)

object MazeGenerator {

    fun generate(level: Int, seed: Long = System.currentTimeMillis()): MazeResult {
        val rng = Random(seed)
        val gridSize = calculateGridSize(level)
        val arrowDensity = calculateArrowDensity(level)

        // Generate a guaranteed-solvable path using DFS
        val solutionPath = generateSolutionPath(gridSize, rng)

        // Fill the grid with arrows
        val grid = buildGrid(gridSize, solutionPath, arrowDensity, rng)

        val start = solutionPath.first()
        val end = solutionPath.last()

        return MazeResult(
            gridSize = gridSize,
            grid = grid,
            solutionPath = solutionPath,
            start = start,
            end = end
        )
    }

    fun calculateGridSize(level: Int): Int {
        return (4 + (level / 50).toInt()).coerceAtMost(12)
    }

    fun calculateArrowDensity(level: Int): Float {
        // Starts at 0.3 for level 1, approaches 0.95 by level 500
        return (0.3f + (level.toFloat() / 500f) * 0.65f).coerceAtMost(0.95f)
    }

    private fun generateSolutionPath(gridSize: Int, rng: Random): List<Pair<Int, Int>> {
        val start = Pair(0, 0)
        val end = Pair(gridSize - 1, gridSize - 1)
        val visited = mutableSetOf<Pair<Int, Int>>()
        val path = mutableListOf<Pair<Int, Int>>()

        // Use randomized DFS (backtracking) to find a path from start to end
        if (dfs(gridSize, start, end, visited, path, rng)) {
            return path
        }

        // Fallback: straight-line path (shouldn't happen)
        return buildList {
            for (i in 0 until gridSize) {
                add(Pair(i, 0))
            }
            for (j in 1 until gridSize) {
                add(Pair(gridSize - 1, j))
            }
        }
    }

    private fun dfs(
        gridSize: Int,
        current: Pair<Int, Int>,
        target: Pair<Int, Int>,
        visited: MutableSet<Pair<Int, Int>>,
        path: MutableList<Pair<Int, Int>>,
        rng: Random
    ): Boolean {
        visited.add(current)
        path.add(current)

        if (current == target) return true

        val neighbors = getShuffledNeighbors(current, gridSize, rng)
        for (next in neighbors) {
            if (next !in visited) {
                if (dfs(gridSize, next, target, visited, path, rng)) {
                    return true
                }
            }
        }

        path.removeAt(path.size - 1)
        return false
    }

    private fun getShuffledNeighbors(
        pos: Pair<Int, Int>,
        gridSize: Int,
        rng: Random
    ): List<Pair<Int, Int>> {
        val directions = listOf(
            Pair(-1, 0), // UP
            Pair(1, 0),  // DOWN
            Pair(0, -1), // LEFT
            Pair(0, 1)   // RIGHT
        )
        return directions
            .map { Pair(pos.first + it.first, pos.second + it.second) }
            .filter { it.first in 0 until gridSize && it.second in 0 until gridSize }
            .shuffled(rng)
    }

    private fun buildGrid(
        gridSize: Int,
        solutionPath: Set<Pair<Int, Int>>,
        arrowDensity: Float,
        rng: Random
    ): List<List<ArrowCell>> {
        val pathSet = solutionPath.toSet()
        val pathDirections = mutableMapOf<Pair<Int, Int>, Direction>()

        // Calculate direction for each cell on the path
        for (i in 0 until solutionPath.size - 1) {
            val curr = solutionPath[i]
            val next = solutionPath[i + 1]
            pathDirections[curr] = when {
                next.first < curr.first -> Direction.UP
                next.first > curr.first -> Direction.DOWN
                next.second < curr.second -> Direction.LEFT
                next.second > curr.second -> Direction.RIGHT
                else -> Direction.RIGHT
            }
        }
        // Last cell points in a reasonable direction toward end
        if (solutionPath.isNotEmpty()) {
            val last = solutionPath.last()
            val prev = solutionPath[solutionPath.size - 2]
            pathDirections[last] = when {
                prev.first < last.first -> Direction.DOWN
                prev.first > last.first -> Direction.UP
                prev.second < last.second -> Direction.RIGHT
                else -> Direction.LEFT
            }
        }

        val allDirections = Direction.entries.toTypedArray()

        return (0 until gridSize).map { row ->
            (0 until gridSize).map { col ->
                val pos = Pair(row, col)
                val isOnPath = pos in pathSet

                if (isOnPath) {
                    ArrowCell(
                        row = row,
                        col = col,
                        direction = pathDirections[pos] ?: Direction.RIGHT,
                        isOnPath = true
                    )
                } else {
                    // Off-path cells: fill with random arrows based on density
                    val shouldFill = rng.nextFloat() < arrowDensity
                    if (shouldFill) {
                        // Intentionally random directions (may or may not mislead)
                        val randomDir = allDirections[rng.nextInt(allDirections.size)]
                        ArrowCell(
                            row = row,
                            col = col,
                            direction = randomDir,
                            isOnPath = false
                        )
                    } else {
                        ArrowCell(
                            row = row,
                            col = col,
                            direction = allDirections[rng.nextInt(allDirections.size)],
                            isOnPath = false
                        )
                    }
                }
            }
        }
    }

    private fun buildList(builder: MutableList<Pair<Int, Int>>.() -> Unit): List<Pair<Int, Int>> {
        return mutableListOf<Pair<Int, Int>>().apply(builder)
    }
}
