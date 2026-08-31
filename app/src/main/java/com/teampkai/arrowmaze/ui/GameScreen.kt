package com.teampkai.arrowmaze.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
fun AmbientAnimation(animation: String, modifier: Modifier = Modifier.fillMaxSize()) {
    val transition = rememberInfiniteTransition(label = "ambient")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ambient-t"
    )

    androidx.compose.foundation.Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (animation) {
            "birds" -> {
                // 4 small birds drifting across horizontally at different heights.
                val birdCount = 4
                for (i in 0 until birdCount) {
                    val rawProgress = (t + i * 0.25f) % 1f
                    val x = (rawProgress * (w + 80f)) - 40f
                    val y = h * (0.1f + i * 0.12f)
                    val wingSpan = 18f

                    // Bird shape: two small arcs forming a "v"
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(x - wingSpan, y),
                        end = Offset(x, y - 6f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.85f),
                        start = Offset(x, y - 6f),
                        end = Offset(x + wingSpan, y),
                        strokeWidth = 2f
                    )
                }
            }
            "bubbles" -> {
                // Small circles rising vertically with a slight horizontal drift.
                val bubbleCount = 8
                for (i in 0 until bubbleCount) {
                    val rawProgress = (t + i * 0.125f) % 1f
                    val x = (i * (w / bubbleCount)) +
                        (Math.sin((t + i) * Math.PI * 2).toFloat()) * 12f
                    val y = h - rawProgress * h
                    val radius = 4f + (i % 3) * 2f

                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = radius,
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.9f),
                        radius = radius * 0.3f,
                        center = Offset(x - radius * 0.3f, y - radius * 0.3f)
                    )
                }
            }
        }
    }
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
                currentThemeId = gameState.themeId,
                highestLevel = gameState.currentLevel,
                onThemeSelected = { themeId ->
                    engine.setTheme(themeId)
                    gameState = engine.state
                },
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
    currentThemeId: Int,
    highestLevel: Int,
    onThemeSelected: (Int) -> Unit,
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
                    .padding(bottom = 16.dp),
                textAlign = TextAlign.Center
            )

            // Theme picker
            Text(
                text = "Choose Theme",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ThemeRegistry.allThemes.forEach { t ->
                    ThemeCard(
                        theme = t,
                        isSelected = t.id == currentThemeId,
                        modifier = Modifier.weight(1f),
                        onClick = { onThemeSelected(t.id) }
                    )
                }
            }

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
fun ThemeCard(
    theme: Theme,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) theme.arrowPalette.primary.copy(alpha = 0.4f)
                else Color.White.copy(alpha = 0.1f)
            )
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) theme.arrowPalette.accent else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = theme.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isSelected) "Selected" else "Tap to select",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
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

        // Ambient animation overlay (birds / bubbles / etc.)
        AmbientAnimation(animation = theme.ambientAnimation)

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
    androidx.compose.foundation.Canvas(
        modifier = Modifier.fillMaxSize()
    ) {
        val w = size.width
        val h = size.height

        theme.parallaxLayers.forEach { layer ->
            val baseColor = theme.backgroundColors.firstOrNull() ?: Color.White
            when (layer.drawType) {
                "mountains" -> drawMountains(w, h, layer.speed, baseColor.copy(alpha = 0.3f))
                "trees_far" -> drawTreeLine(w, h, layer.speed, baseColor.copy(alpha = 0.2f))
                "trees_near" -> drawTreeLine(w, h, layer.speed, baseColor.copy(alpha = 0.15f))
                "deep_sea" -> drawDeepSea(w, h, layer.speed, baseColor.copy(alpha = 0.35f))
                "coral" -> drawCoral(w, h, layer.speed, baseColor.copy(alpha = 0.25f))
                "fish" -> drawFishLayer(w, h, layer.speed, theme.arrowPalette.accent.copy(alpha = 0.6f))
            }
        }
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

private fun DrawScope.drawDeepSea(width: Float, height: Float, heightFactor: Float, color: Color) {
    // Wavy deep-sea gradient silhouettes: a couple of overlapping rolling wave shapes.
    val baseY1 = height * (1f - heightFactor)
    val baseY2 = height * (1f - heightFactor * 0.7f)

    val wave1 = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, height)
        var x = 0f
        val step = width / 14f
        while (x <= width) {
            val y = baseY1 +
                (Math.sin(x.toDouble() / width * Math.PI * 4) * height * 0.04f).toFloat() +
                (Math.cos(x.toDouble() / width * Math.PI * 2) * height * 0.02f).toFloat()
            lineTo(x, y.toFloat())
            x += step
        }
        lineTo(width, height)
        close()
    }
    drawPath(wave1, color)

    val wave2 = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, height)
        var x = 0f
        val step = width / 18f
        while (x <= width) {
            val y = baseY2 +
                (Math.cos(x.toDouble() / width * Math.PI * 3) * height * 0.03f).toFloat() +
                (Math.sin(x.toDouble() / width * Math.PI * 5) * height * 0.015f).toFloat()
            lineTo(x, y.toFloat())
            x += step
        }
        lineTo(width, height)
        close()
    }
    drawPath(wave2, color.copy(alpha = (color.alpha * 0.7f).coerceIn(0f, 1f)))
}

private fun DrawScope.drawCoral(width: Float, height: Float, heightFactor: Float, color: Color) {
    // Coral silhouettes rising from the bottom at varied heights.
    val baseY = height * (1f - heightFactor * 0.4f)
    val coralSpacing = width / 8f

    for (i in 0..8) {
        val x = i * coralSpacing + coralSpacing * 0.3f
        val coralHeight = height * 0.1f +
            (Math.sin(i.toDouble() * 1.7) * height * 0.04f).toFloat()

        val stemPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(x - coralSpacing * 0.04f, baseY)
            lineTo(x + coralSpacing * 0.04f, baseY)
            lineTo(x + coralSpacing * 0.02f, baseY - coralHeight)
            lineTo(x - coralSpacing * 0.02f, baseY - coralHeight)
            close()
        }
        drawPath(stemPath, color)

        // Branching top
        val branchPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(x, baseY - coralHeight)
            lineTo(x - coralSpacing * 0.12f, baseY - coralHeight * 0.7f)
            moveTo(x, baseY - coralHeight)
            lineTo(x + coralSpacing * 0.12f, baseY - coralHeight * 0.6f)
            moveTo(x, baseY - coralHeight * 0.7f)
            lineTo(x + coralSpacing * 0.08f, baseY - coralHeight * 0.95f)
        }
        drawPath(
            path = branchPath,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

private fun DrawScope.drawFishLayer(width: Float, height: Float, heightFactor: Float, color: Color) {
    // Small fish silhouettes drifting across the upper portion of the canvas.
    val fishY = height * (1f - heightFactor * 0.6f)
    val fishCount = 6
    val step = width / fishCount

    for (i in 0 until fishCount) {
        val baseX = i * step + step * 0.3f
        val offset = (Math.sin(i.toDouble() * 2.3) * step * 0.3f).toFloat()
        val cx = baseX + offset
        val cy = fishY + (Math.cos(i.toDouble() * 1.1) * height * 0.04f).toFloat()
        val fishWidth = step * 0.4f
        val fishHeight = fishWidth * 0.45f

        val fishPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(cx + fishWidth / 2f, cy)
            cubicTo(
                cx + fishWidth * 0.2f, cy - fishHeight,
                cx - fishWidth * 0.3f, cy - fishHeight * 0.8f,
                cx - fishWidth * 0.35f, cy
            )
            cubicTo(
                cx - fishWidth * 0.3f, cy + fishHeight * 0.8f,
                cx + fishWidth * 0.2f, cy + fishHeight,
                cx + fishWidth / 2f, cy
            )
            close()
        }
        drawPath(fishPath, color)
    }
}
