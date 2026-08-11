package com.teampkai.arrowmaze.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teampkai.arrowmaze.core.GameEngine
import com.teampkai.arrowmaze.core.GameState
import com.teampkai.arrowmaze.core.MoveResult
import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.themes.Theme
import com.teampkai.arrowmaze.themes.ThemeRegistry

enum class Screen {
    LEVEL_SELECT,
    GAME,
    LEVEL_COMPLETE
}

@Composable
fun GameApp() {
    val engine = remember { GameEngine() }
    var gameState by remember { mutableStateOf(engine.startNewGame()) }
    var currentScreen by remember { mutableStateOf(Screen.LEVEL_SELECT) }
    val theme = ThemeRegistry.getTheme(gameState.themeId)

    when (currentScreen) {
        Screen.LEVEL_SELECT -> {
            LevelSelectScreen(
                theme = theme,
                highestLevel = gameState.currentLevel,
                onLevelSelected = { level ->
                    gameState = engine.jumpToLevel(level)
                    currentScreen = Screen.GAME
                }
            )
        }
        Screen.GAME -> {
            GamePlayScreen(
                engine = engine,
                gameState = gameState,
                theme = theme,
                onMoveResult = { result ->
                    gameState = engine.state
                    if (result is MoveResult.LevelComplete) {
                        currentScreen = Screen.LEVEL_COMPLETE
                    } else if (result is MoveResult.Wrong) {
                        // Wrong move, game over - retry
                        gameState = engine.retryLevel()
                    }
                },
                onBack = {
                    currentScreen = Screen.LEVEL_SELECT
                }
            )
        }
        Screen.LEVEL_COMPLETE -> {
            LevelCompleteScreen(
                level = gameState.currentLevel,
                score = gameState.score,
                theme = theme,
                onNextLevel = {
                    gameState = engine.advanceToNextLevel()
                    currentScreen = Screen.GAME
                },
                onBackToLevels = {
                    currentScreen = Screen.LEVEL_SELECT
                }
            )
        }
    }
}

@Composable
fun LevelSelectScreen(
    theme: Theme,
    highestLevel: Int,
    onLevelSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.backgroundColors))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Arrow Maze",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "${theme.name} Theme",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = TextAlign.Center
            )

            Text(
                text = "Select Level",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val maxDisplay = (highestLevel + 20).coerceAtMost(500)
                items((1..maxDisplay).toList()) { level ->
                    val isUnlocked = level <= highestLevel
                    LevelButton(
                        level = level,
                        isUnlocked = isUnlocked,
                        theme = theme,
                        onClick = { if (isUnlocked) onLevelSelected(level) }
                    )
                }
            }
        }
    }
}

@Composable
fun LevelButton(
    level: Int,
    isUnlocked: Boolean,
    theme: Theme,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isUnlocked) theme.arrowPalette.primary.copy(alpha = 0.3f)
                else Color.Gray.copy(alpha = 0.2f)
            )
            .border(
                width = 1.dp,
                color = if (isUnlocked) theme.arrowPalette.primary else Color.Gray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(enabled = isUnlocked) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$level",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (isUnlocked) Color.White else Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun GamePlayScreen(
    engine: GameEngine,
    gameState: GameState,
    theme: Theme,
    onMoveResult: (MoveResult) -> Unit,
    onBack: () -> Unit
) {
    val maze = gameState.maze ?: return
    val visitedCells = remember { mutableStateListOf<Pair<Int, Int>>() }

    // Reset visited cells when level changes
    LaunchedEffect(gameState.currentLevel) {
        visitedCells.clear()
        // Mark start cell as visited
        if (maze.solutionPath.isNotEmpty()) {
            visitedCells.add(maze.solutionPath.first())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(theme.backgroundColors))
    ) {
        // Parallax background layers
        ParallaxBackground(theme = theme)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBack) {
                    Text("← Back", color = Color.White)
                }

                Text(
                    text = "Level ${gameState.currentLevel}",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                // Heart
                Text(
                    text = "❤️",
                    fontSize = 24.sp
                )
            }

            // Score
            Text(
                text = "Score: ${gameState.score}",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // Maze grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                MazeGrid(
                    maze = maze,
                    theme = theme,
                    visitedCells = visitedCells,
                    onCellTapped = { row, col ->
                        val result = engine.makeMove(row, col)
                        when (result) {
                            is MoveResult.Correct -> {
                                visitedCells.add(Pair(row, col))
                            }
                            is MoveResult.LevelComplete -> {
                                visitedCells.add(Pair(row, col))
                            }
                            else -> { /* handled by callback */ }
                        }
                        onMoveResult(result)
                    }
                )
            }

            // Instruction
            Text(
                text = "Follow the path from START to END",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MazeGrid(
    maze: com.teampkai.arrowmaze.generator.MazeResult,
    theme: Theme,
    visitedCells: List<Pair<Int, Int>>,
    onCellTapped: (Int, Int) -> Unit
) {
    val gridSize = maze.gridSize
    val cellSize = (320 / gridSize).dp.coerceAtMost(64.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Start indicator
        Text(
            text = "START ↓",
            fontSize = 12.sp,
            color = theme.arrowPalette.accent,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        for (row in 0 until gridSize) {
            Row(
                horizontalArrangement = Arrangement.Center
            ) {
                for (col in 0 until gridSize) {
                    val cell = maze.grid[row][col]
                    val isVisited = visitedCells.contains(Pair(row, col))
                    val isStart = Pair(row, col) == maze.start
                    val isEnd = Pair(row, col) == maze.end

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                when {
                                    isStart -> Color(0x40FFD54F)
                                    isEnd -> Color(0x40FF5722)
                                    isVisited -> Color(0x30FFFFFF)
                                    else -> Color(0x15FFFFFF)
                                }
                            )
                            .border(
                                width = if (isStart || isEnd) 2.dp else 0.5.dp,
                                color = when {
                                    isStart -> theme.arrowPalette.accent
                                    isEnd -> Color(0xFFFF5722)
                                    else -> Color(0x20FFFFFF)
                                },
                                shape = RoundedCornerShape(4.dp)
                            )
                            .clickable { onCellTapped(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        ArrowRenderer(
                            direction = cell.direction,
                            theme = theme,
                            isOnPath = cell.isOnPath,
                            isVisited = isVisited,
                            modifier = Modifier.size(cellSize - 4.dp)
                        )
                    }
                }
            }
        }

        // End indicator
        Text(
            text = "END ↑",
            fontSize = 12.sp,
            color = Color(0xFFFF5722),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun LevelCompleteScreen(
    level: Int,
    score: Int,
    theme: Theme,
    onNextLevel: () -> Unit,
    onBackToLevels: () -> Unit
) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(theme.backgroundColors)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🎉",
                    fontSize = 64.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Level $level Complete!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Score: $score",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Button(
                    onClick = onNextLevel,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = theme.arrowPalette.accent
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = "Next Level →",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                }

                TextButton(onClick = onBackToLevels) {
                    Text(
                        text = "Back to Levels",
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun ParallaxBackground(theme: Theme) {
    // Simplified parallax using static layers with Canvas
    // In a full implementation, these would animate based on scroll/gesture offset
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        // Far mountains (slowest parallax)
        drawMountains(w, h, 0.15f, theme.backgroundColors[0].copy(alpha = 0.3f))

        // Far trees
        drawTreeLine(w, h, 0.4f, theme.backgroundColors[1].copy(alpha = 0.2f))

        // Near trees
        drawTreeLine(w, h, 0.7f, theme.backgroundColors[0].copy(alpha = 0.15f))
    }
}

private fun DrawScope.drawMountains(width: Float, height: Float, heightFactor: Float, color: Color) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, height)
        var x = 0f
        val step = width / 6f
        val baseY = height * (1f - heightFactor)
        while (x <= width) {
            val peakY = baseY - (Math.sin(x.toDouble() / width * Math.PI * 3) * height * 0.08f).toFloat()
            lineTo(x, peakY.toFloat())
            x += step / 2
        }
        lineTo(width, height)
        close()
    }
    drawPath(path, color)
}

private fun DrawScope.drawTreeLine(width: Float, height: Float, heightFactor: Float, color: Color) {
    val baseY = height * (1f - heightFactor)
    val treeSpacing = width / 10f

    for (i in 0..10) {
        val x = i * treeSpacing
        val treeHeight = height * 0.08f + (Math.sin(i.toDouble()) * height * 0.03f).toFloat()

        val trunkPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(x, baseY)
            lineTo(x - treeSpacing * 0.05f, baseY - treeHeight * 0.3f)
            lineTo(x + treeSpacing * 0.05f, baseY - treeHeight * 0.3f)
            close()
        }

        val canopyPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(x, baseY - treeHeight)
            lineTo(x - treeSpacing * 0.2f, baseY - treeHeight * 0.3f)
            lineTo(x + treeSpacing * 0.2f, baseY - treeHeight * 0.3f)
            close()
        }

        drawPath(trunkPath, color)
        drawPath(canopyPath, color)
    }
}
