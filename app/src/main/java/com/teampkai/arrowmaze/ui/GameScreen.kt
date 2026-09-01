package com.teampkai.arrowmaze.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teampkai.arrowmaze.audio.MusicManager
import com.teampkai.arrowmaze.audio.SoundManager
import com.teampkai.arrowmaze.core.GameEngine
import com.teampkai.arrowmaze.core.GameState
import com.teampkai.arrowmaze.core.MoveResult
import com.teampkai.arrowmaze.data.GameProgress
import com.teampkai.arrowmaze.data.GameProgressStore
import com.teampkai.arrowmaze.generator.ArrowCell
import com.teampkai.arrowmaze.generator.Direction
import com.teampkai.arrowmaze.generator.MazeResult
import com.teampkai.arrowmaze.themes.Theme
import com.teampkai.arrowmaze.themes.ThemeRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val context = LocalContext.current
    val progressStore = remember { GameProgressStore(context) }
    val soundManager = remember { SoundManager(context) }
    val musicManager = remember { MusicManager(context) }
    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
            musicManager.release()
        }
    }
    val savedProgress by progressStore.progress.collectAsState(
        initial = GameProgress(highestLevel = 1, score = 0, themeId = 1, soundEnabled = true)
    )

    // Keep SoundManager in sync with the persisted preference.
    LaunchedEffect(savedProgress.soundEnabled) {
        soundManager.soundEnabled = savedProgress.soundEnabled
        musicManager.musicEnabled = savedProgress.soundEnabled
    }

    // Start per-level music whenever the current level or screen changes.
    LaunchedEffect(currentScreen, gameState.currentLevel) {
        if (currentScreen == Screen.GAME) {
            musicManager.startForLevel(gameState.currentLevel)
        } else {
            musicManager.stop()
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val engine = remember {
        GameEngine(
            initialState = GameState(
                currentLevel = 1,
                highestLevelUnlocked = savedProgress.highestLevel,
                score = savedProgress.score,
                themeId = savedProgress.themeId
            )
        )
    }
    var gameState by remember { mutableStateOf(engine.state) }
    var currentScreen by remember { mutableStateOf(Screen.LEVEL_SELECT) }
    // Each level gets its own procedural theme (see ThemeRegistry.themeForLevel)
    // so the 1500+ levels all feel visually distinct.
    val theme = ThemeRegistry.themeForLevel(gameState.currentLevel)

    fun persistCurrentProgress() {
        coroutineScope.launch {
            progressStore.saveProgress(
                highestLevel = gameState.highestLevelUnlocked,
                score = gameState.score,
                themeId = gameState.themeId,
                soundEnabled = soundManager.soundEnabled
            )
        }
    }

    when (currentScreen) {
        Screen.LEVEL_SELECT -> {
            LevelSelectScreen(
                theme = theme,
                currentThemeId = gameState.themeId,
                highestLevel = gameState.highestLevelUnlocked,
                soundEnabled = soundManager.soundEnabled,
                onToggleSound = {
                    soundManager.soundEnabled = !soundManager.soundEnabled
                    soundManager.playButtonTap()
                    persistCurrentProgress()
                },
                onThemeSelected = { themeId ->
                    engine.setTheme(themeId)
                    gameState = engine.state
                    soundManager.playButtonTap()
                    persistCurrentProgress()
                },
                onLevelSelected = { level ->
                    gameState = engine.jumpToLevel(level)
                    soundManager.playButtonTap()
                    currentScreen = Screen.GAME
                }
            )
        }
        Screen.GAME -> {
            GamePlayScreen(
                engine = engine,
                gameState = gameState,
                theme = theme,
                soundManager = soundManager,
                onToggleSound = {
                    soundManager.soundEnabled = !soundManager.soundEnabled
                    soundManager.playButtonTap()
                    persistCurrentProgress()
                },
                onEngineStateChanged = { gameState = engine.state },
                onMoveResult = { result ->
                    gameState = engine.state
                    when (result) {
                        is MoveResult.ArrowCleared -> soundManager.playCorrectMove()
                        is MoveResult.Blocked -> soundManager.playWrongMove()
                        is MoveResult.GameOver -> { /* reset already applied in engine */ }
                        is MoveResult.LevelComplete -> {
                            soundManager.playLevelComplete()
                            persistCurrentProgress()
                        }
                        is MoveResult.Ignored -> { /* no-op */ }
                    }
                    if (result is MoveResult.LevelComplete) {
                        currentScreen = Screen.LEVEL_COMPLETE
                    }
                },
                onBack = {
                    soundManager.playButtonTap()
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
                    soundManager.playButtonTap()
                    gameState = engine.advanceToNextLevel()
                    persistCurrentProgress()
                    currentScreen = Screen.GAME
                },
                onBackToLevels = {
                    soundManager.playButtonTap()
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
    soundEnabled: Boolean,
    onToggleSound: () -> Unit,
    onThemeSelected: (Int) -> Unit,
    onLevelSelected: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        // Use the Jungle background as the level-select backdrop so the
        // menu feels alive (live particles + scenery).
        AnimatedBackground(
            type = BackgroundCatalog.forLevel(1),
            theme = theme
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "Arrow Maze",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                SoundToggleButton(soundEnabled = soundEnabled, onClick = onToggleSound)
            }

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
                // Show all 1500+ levels. LazyVerticalGrid is virtualized so
                // rendering 1500 items is efficient; locked levels are shown
                // greyed-out so the player can see the full game scope.
                val totalLevels = 1500
                items((1..totalLevels).toList()) { level ->
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
fun SoundToggleButton(
    soundEnabled: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (soundEnabled) "🔊" else "🔇",
            fontSize = 24.sp
        )
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
    soundManager: SoundManager,
    onToggleSound: () -> Unit,
    onEngineStateChanged: () -> Unit,
    onMoveResult: (MoveResult) -> Unit,
    onBack: () -> Unit
) {
    val maze = gameState.maze ?: return

    // Cells currently animating their slide-out. Once the animation finishes
    // they're removed from the set; the engine has already removed them from
    // the "remaining" set, so the cell will simply not be re-rendered.
    val slidingOut = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, *>>() }
    // Cells currently shaking (after a Blocked result).
    val shaking = remember { mutableStateMapOf<Pair<Int, Int>, Animatable<Float, *>>() }
    // Animation start time for each lost heart (for pop/fade on lose).
    val heartAnim = remember { mutableStateMapOf<Int, Animatable<Float, *>>() }
    // Currently-hinted cell (if any) and a 0..1 progress for its pulse.
    var hintCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    val hintPulse = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    // When the level changes, clear all per-cell animations and reset hearts/hint.
    LaunchedEffect(gameState.currentLevel, gameState.mazeSeed) {
        slidingOut.keys.toList().forEach { slidingOut.remove(it) }
        shaking.keys.toList().forEach { shaking.remove(it) }
        heartAnim.keys.toList().forEach { heartAnim.remove(it) }
        hintCell = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Top header bar — blue background, back button, "Level N" pill, settings button.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2196F3))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Text("←", fontSize = 24.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF42A5F5), RoundedCornerShape(50))
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Level ${gameState.currentLevel}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onToggleSound) {
                        Text(
                            text = if (soundManager.soundEnabled) "🔊" else "🔇",
                            fontSize = 20.sp
                        )
                    }
                }
                // Hearts row at the bottom of the header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    HeartRow(lives = gameState.lives, animMap = heartAnim)
                }
            }

            // Maze grid area — premium animated background visible around the white grid.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                // Per-level live-animated background (50+ unique scenes).
                AnimatedBackground(
                    type = BackgroundCatalog.forLevel(gameState.currentLevel),
                    theme = theme
                )
                MazeGrid(
                    maze = maze,
                    theme = theme,
                    clearedCells = gameState.clearedCells,
                    slidingOut = slidingOut,
                    shaking = shaking,
                    hintCell = hintCell,
                    hintPulseProgress = if (hintCell != null) hintPulse.value else 0f,
                    onCellTapped = { row, col ->
                        // If the player taps while a hint is showing, clear it.
                        if (hintCell != null) {
                            hintCell = null
                            scope.launch { hintPulse.snapTo(0f) }
                        }
                        val result = engine.tapArrow(row, col)
                        when (result) {
                            is MoveResult.ArrowCleared -> {
                                // Start slide-out animation; keep cell rendered
                                // until animation completes, then it disappears.
                                val anim = Animatable(0f)
                                slidingOut[Pair(row, col)] = anim
                                scope.launch {
                                    anim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 350, easing = LinearEasing)
                                    )
                                    slidingOut.remove(Pair(row, col))
                                }
                            }
                            is MoveResult.Blocked -> {
                                // Start shake animation.
                                val anim = Animatable(0f)
                                shaking[Pair(row, col)] = anim
                                scope.launch {
                                    anim.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 220, easing = LinearEasing)
                                    )
                                    shaking.remove(Pair(row, col))
                                }
                                // Pop/fade the just-lost heart (lives already decremented).
                                val lostIndex = gameState.lives
                                val heartAnimFor = Animatable(0f)
                                heartAnim[lostIndex] = heartAnimFor
                                scope.launch {
                                    heartAnimFor.animateTo(
                                        targetValue = 1f,
                                        animationSpec = tween(durationMillis = 300)
                                    )
                                    delay(400)
                                    heartAnim.remove(lostIndex)
                                }
                            }
                            is MoveResult.GameOver -> {
                                for (i in 0 until 3) {
                                    val a = Animatable(0f)
                                    heartAnim[i] = a
                                    scope.launch {
                                        a.animateTo(1f, tween(300))
                                        delay(200)
                                        heartAnim.remove(i)
                                    }
                                }
                            }
                            else -> { /* Ignored / LevelComplete — handled by caller */ }
                        }
                        onMoveResult(result)
                    }
                )
            }

            // Bottom footer — blue background with diagonal top edge, "Arrows Escape" title.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                // Diagonal blue shape at the top of the footer.
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(0f, 40f)
                        lineTo(w, 0f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path, Color(0xFF2196F3))
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("→", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Arrows Escape",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("←", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Score: ${gameState.score}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

/**
 * Renders 3 hearts. Hearts that have a `lost` animation are drawn smaller
 * and fading to grey.
 */
@Composable
private fun HeartRow(
    lives: Int,
    animMap: SnapshotStateMap<Int, Animatable<Float, *>>
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until 3) {
            val lost = i >= lives
            val progress = animMap[i]?.value ?: 0f
            val scale = if (lost) 1f - 0.4f * progress else 1f + 0.15f * progress
            val alpha = if (lost) 1f - 0.7f * progress else 1f
            Text(
                text = "❤️",
                fontSize = 24.sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .alpha(alpha)
            )
        }
    }
}

@Composable
fun MazeGrid(
    maze: MazeResult,
    theme: Theme,
    clearedCells: Set<Pair<Int, Int>>,
    slidingOut: SnapshotStateMap<Pair<Int, Int>, Animatable<Float, *>>,
    shaking: SnapshotStateMap<Pair<Int, Int>, Animatable<Float, *>>,
    hintCell: Pair<Int, Int>?,
    hintPulseProgress: Float,
    onCellTapped: (Int, Int) -> Unit
) {
    val gridSize = maze.gridSize
    // Scale the board to fill the available screen width so the arrows form
    // a large, connected pipe-network matching the reference. Cap cell size
    // so the board doesn't get absurdly large on tablets.
    val maxBoardDp = 360.dp
    val cellSize = (maxBoardDp / gridSize).coerceAtMost(56.dp)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(Color.White)
    ) {
        for (row in 0 until gridSize) {
            Row(horizontalArrangement = Arrangement.Center) {
                for (col in 0 until gridSize) {
                    val cell = maze.grid[row][col]
                    val pos = Pair(row, col)
                    val isCleared = pos in clearedCells
                    val hasArrow = cell.hasArrow && !isCleared
                    val isHinted = hintCell == pos

                    Box(
                        modifier = Modifier
                            .size(cellSize)
                            .clickable(enabled = hasArrow) { onCellTapped(row, col) },
                        contentAlignment = Alignment.Center
                    ) {
                        // Pulsing hint ring (drawn behind the arrow).
                        if (isHinted && hasArrow) {
                            val pulse = (kotlin.math.sin(hintPulseProgress * Math.PI * 4.0) * 0.5 + 0.5).toFloat()
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f
                                val baseRadius = size.width * 0.32f
                                val radius = baseRadius + pulse * size.width * 0.18f
                                val ringColor = theme.arrowPalette.accent.copy(alpha = 0.45f + 0.4f * pulse)
                                drawCircle(
                                    color = ringColor,
                                    radius = radius,
                                    center = Offset(cx, cy),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 3f + 3f * pulse
                                    )
                                )
                                drawCircle(
                                    color = theme.arrowPalette.glow.copy(alpha = 0.5f + 0.3f * pulse),
                                    radius = radius * 0.7f,
                                    center = Offset(cx, cy)
                                )
                            }
                        }
                        if (hasArrow) {
                            ArrowCellView(
                                cell = cell,
                                theme = theme,
                                cellSize = cellSize,
                                slideProgress = slidingOut[pos]?.value,
                                shakeProgress = shaking[pos]?.value,
                                slideDirection = cell.direction
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArrowCellView(
    cell: ArrowCell,
    theme: Theme,
    cellSize: androidx.compose.ui.unit.Dp,
    slideProgress: Float?,
    shakeProgress: Float?,
    slideDirection: Direction
) {
    val slide = slideProgress ?: 0f
    val shake = shakeProgress ?: 0f

    // Convert slide offset (Dp) to pixels once, since graphicsLayer works in px.
    val density = androidx.compose.ui.platform.LocalDensity.current
    val slideOffsetPx = with(density) { (cellSize * slide).toPx() }
    val shakeOffsetPx = (kotlin.math.sin(shake * Math.PI * 6.0) * 6.0).toFloat()

    val translationX = when (slideDirection) {
        Direction.LEFT -> -slideOffsetPx
        Direction.RIGHT -> slideOffsetPx
        Direction.UP, Direction.DOWN -> shakeOffsetPx
    } - shakeOffsetPx

    val translationY = when (slideDirection) {
        Direction.UP -> -slideOffsetPx
        Direction.DOWN -> slideOffsetPx
        Direction.LEFT, Direction.RIGHT -> 0f
    }

    val alpha = (1f - slide).coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .size(cellSize - 4.dp)
            .graphicsLayer {
                this.translationX = translationX
                this.translationY = translationY
            }
            .alpha(alpha)
    ) {
        ArrowRenderer(
            direction = cell.direction,
            theme = theme,
            modifier = Modifier.fillMaxSize()
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
