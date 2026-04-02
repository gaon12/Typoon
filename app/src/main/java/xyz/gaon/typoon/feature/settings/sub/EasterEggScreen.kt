package xyz.gaon.typoon.feature.settings.sub

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import kotlinx.coroutines.delay
import xyz.gaon.typoon.R
import xyz.gaon.typoon.ui.theme.MonoFontFamily

@Composable
fun EasterEggScreen(
    onNavigateBack: () -> Unit,
    viewModel: EasterEggViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    val progress = (uiState.score / uiState.targetScore.toFloat()).coerceIn(0f, 1f)
    val surfaceColor = MaterialTheme.colorScheme.surface
    val darkMode = surfaceColor.luminance() < 0.5f
    val palette =
        remember(progress, surfaceColor.toArgb(), darkMode) {
            paletteFor(progress = progress, darkMode = darkMode)
        }
    val skyTop by animateColorAsState(
        targetValue = palette.skyTop,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        label = "skyTop",
    )
    val skyBottom by animateColorAsState(
        targetValue = palette.skyBottom,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        label = "skyBottom",
    )
    val overlayGlow by animateColorAsState(
        targetValue = palette.overlayGlow,
        animationSpec = tween(durationMillis = 700, easing = LinearEasing),
        label = "overlayGlow",
    )
    EasterEggGameScene(
        uiState = uiState,
        skyTop = skyTop,
        skyBottom = skyBottom,
        overlayGlow = overlayGlow,
        palette = palette,
        onNavigateBack = onNavigateBack,
        onStack = viewModel::onStack,
    )

    val dialogState =
        rememberDialogState(
            uiState = uiState,
            onRestart = viewModel::onRestart,
            onNavigateBack = onNavigateBack,
        )
    if (dialogState != null) {
        OutcomeDialog(state = dialogState)
    }
}

@Composable
private fun EasterEggGameScene(
    uiState: EasterEggUiState,
    skyTop: Color,
    skyBottom: Color,
    overlayGlow: Color,
    palette: GamePalette,
    onNavigateBack: () -> Unit,
    onStack: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(colors = listOf(skyTop, skyBottom)))
                .clickable(
                    enabled = uiState.canStack,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onStack,
                ),
    ) {
        AtmosphericGlow(
            modifier = Modifier.fillMaxSize(),
            sparkleColor = palette.sparkle,
            glowColor = overlayGlow,
        )

        MinimalBackButton(
            onNavigateBack = onNavigateBack,
            modifier =
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(start = 18.dp, top = 10.dp),
        )

        ScoreHud(
            score = uiState.score,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 28.dp),
        )

        StackGameStage(
            uiState = uiState,
            palette = palette,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp)
                    .padding(bottom = 44.dp)
                    .navigationBarsPadding(),
        )
    }
}

@Composable
private fun MinimalBackButton(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.18f),
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(
            modifier =
                Modifier
                    .size(44.dp)
                    .clickable(onClick = onNavigateBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
            )
        }
    }
}

@Composable
private fun ScoreHud(
    score: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = ".${score.toString().padStart(2, '0')}",
            style = MaterialTheme.typography.displayLarge,
            fontFamily = MonoFontFamily,
            fontWeight = FontWeight.Light,
            color = Color.White.copy(alpha = 0.96f),
        )
    }
}

@Composable
private fun StackGameStage(
    uiState: EasterEggUiState,
    palette: GamePalette,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
    ) {
        val density = LocalDensity.current
        val stageHeight = maxHeight * 0.86f
        val blockDepth = with(density) { 26.dp.toPx() }
        val blockHeight = with(density) { 34.dp.toPx() }
        val cameraTarget = ((uiState.stackLayers.size - AUTO_SCROLL_START_LAYER).coerceAtLeast(0) * blockHeight * 0.78f)
        val cameraOffset by animateFloatAsState(
            targetValue = cameraTarget,
            animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            label = "cameraOffset",
        )

        Canvas(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(stageHeight),
        ) {
            val metrics =
                StageMetrics(
                    centerX = size.width / 2f - blockDepth * 0.55f,
                    laneWidth = size.width * 0.38f,
                    baseY = size.height - blockHeight * 0.9f + cameraOffset,
                    blockHeight = blockHeight,
                    blockDepth = blockDepth,
                )
            val visibleLayers = uiState.stackLayers.takeLast(MAX_VISIBLE_LAYERS)
            val layerStartIndex = uiState.stackLayers.size - visibleLayers.size

            drawCircle(
                color = Color.Black.copy(alpha = 0.16f),
                radius = size.width * 0.16f,
                center = Offset(metrics.centerX + blockDepth * 1.4f, size.height - blockHeight * 0.1f),
            )
            drawPlacedLayers(
                visibleLayers = visibleLayers,
                layerStartIndex = layerStartIndex,
                targetScore = uiState.targetScore,
                palette = palette,
                metrics = metrics,
            )
            drawFallingPiece(
                piece = uiState.fallingPiece,
                layerStartIndex = layerStartIndex,
                visibleLayerCount = visibleLayers.size,
                palette = palette,
                metrics = metrics,
            )
            drawMovingBlock(
                movingBlock = uiState.movingBlock,
                canStack = uiState.canStack,
                visibleLayerCount = visibleLayers.size,
                palette = palette,
                metrics = metrics,
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPlacedLayers(
    visibleLayers: List<StackLayerUi>,
    layerStartIndex: Int,
    targetScore: Int,
    palette: GamePalette,
    metrics: StageMetrics,
) {
    visibleLayers.forEachIndexed { index, layer ->
        val globalIndex = layerStartIndex + index
        val layerProgress = ((globalIndex - 1).coerceAtLeast(0) / targetScore.toFloat()).coerceIn(0f, 1f)
        val center = metrics.centerX + metrics.laneWidth * ((layer.left + layer.right) / 2f - 0.5f)
        val width = metrics.laneWidth * (layer.right - layer.left)
        val topY = metrics.baseY - index * metrics.blockHeight * 0.78f

        drawStackBlock(
            centerX = center,
            topY = topY,
            width = width,
            height = metrics.blockHeight,
            depth = metrics.blockDepth,
            colors = layerColors(progress = layerProgress, palette = palette),
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFallingPiece(
    piece: FallingPieceUi?,
    layerStartIndex: Int,
    visibleLayerCount: Int,
    palette: GamePalette,
    metrics: StageMetrics,
) {
    piece ?: return
    val pieceVisibleIndex = piece.layerIndex - layerStartIndex
    if (pieceVisibleIndex !in 0..visibleLayerCount) return

    val center = metrics.centerX + metrics.laneWidth * ((piece.left + piece.right) / 2f - 0.5f)
    val width = metrics.laneWidth * (piece.right - piece.left)
    val baseTopY = metrics.baseY - pieceVisibleIndex * metrics.blockHeight * 0.78f
    val fallOffsetY = metrics.blockHeight * 2.4f * piece.progress
    val driftOffsetX = metrics.blockDepth * 2.1f * piece.progress * piece.driftDirection
    val colors =
        BlockColors(
            top = palette.activeTop.copy(alpha = 1f - piece.progress * 0.5f),
            front = palette.activeFront.copy(alpha = 1f - piece.progress * 0.6f),
            side = palette.activeSide.copy(alpha = 1f - piece.progress * 0.6f),
        )

    drawStackBlock(
        centerX = center + driftOffsetX,
        topY = baseTopY + fallOffsetY,
        width = width,
        height = metrics.blockHeight,
        depth = metrics.blockDepth,
        colors = colors,
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawMovingBlock(
    movingBlock: MovingBlockUi,
    canStack: Boolean,
    visibleLayerCount: Int,
    palette: GamePalette,
    metrics: StageMetrics,
) {
    if (!canStack) return

    val movingCenter = metrics.centerX + metrics.laneWidth * (movingBlock.centerX - 0.5f)
    val movingWidth = metrics.laneWidth * movingBlock.width
    val topY = metrics.baseY - visibleLayerCount * metrics.blockHeight * 0.78f

    drawStackBlock(
        centerX = movingCenter,
        topY = topY,
        width = movingWidth,
        height = metrics.blockHeight,
        depth = metrics.blockDepth,
        colors =
            BlockColors(
                top = palette.activeTop,
                front = palette.activeFront,
                side = palette.activeSide,
            ),
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStackBlock(
    centerX: Float,
    topY: Float,
    width: Float,
    height: Float,
    depth: Float,
    colors: BlockColors,
) {
    val halfWidth = width / 2f
    val depthY = depth * 0.58f
    val left = centerX - halfWidth
    val right = centerX + halfWidth
    val backLeft = left + depth
    val backRight = right + depth

    val topFace =
        Path().apply {
            moveTo(left, topY)
            lineTo(right, topY)
            lineTo(backRight, topY - depthY)
            lineTo(backLeft, topY - depthY)
            close()
        }
    val frontFace =
        Path().apply {
            moveTo(left, topY)
            lineTo(right, topY)
            lineTo(right, topY + height)
            lineTo(left, topY + height)
            close()
        }
    val sideFace =
        Path().apply {
            moveTo(right, topY)
            lineTo(backRight, topY - depthY)
            lineTo(backRight, topY + height - depthY)
            lineTo(right, topY + height)
            close()
        }

    drawPath(path = frontFace, color = colors.front)
    drawPath(path = sideFace, color = colors.side)
    drawPath(path = topFace, color = colors.top)
}

@Composable
private fun AnimatedTypoRecoveryText(
    story: StoryLineUi,
    color: Color,
    modifier: Modifier = Modifier,
) {
    var visibleText by remember(story.id) { mutableStateOf(story.wrongText) }

    LaunchedEffect(story.id) {
        visibleText = story.wrongText
        delay(520)
        for (length in story.wrongText.length downTo 0) {
            visibleText = story.wrongText.take(length)
            delay(12)
        }
        delay(120)
        for (length in 1..story.correctText.length) {
            visibleText = story.correctText.take(length)
            delay(18)
        }
        delay(1_250)
        for (length in story.correctText.length downTo 0) {
            visibleText = story.correctText.take(length)
            delay(12)
        }
    }

    Text(
        text = visibleText.ifBlank { " " },
        modifier = modifier,
        style = MaterialTheme.typography.bodyLarge,
        fontFamily = MonoFontFamily,
        fontWeight = FontWeight.Medium,
        color = color,
    )
}

@Composable
private fun OutcomeDialog(state: OutcomeDialogState) {
    Dialog(onDismissRequest = {}) {
        Surface(
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
            tonalElevation = 10.dp,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
            ) {
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = pluralStringResource(R.plurals.easter_egg_record, state.score, state.score),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = MonoFontFamily,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(18.dp))
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        AnimatedTypoRecoveryText(
                            story = state.story,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = state.onSecondary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(state.secondaryLabel)
                    }
                    Spacer(modifier = Modifier.size(10.dp))
                    FilledTonalButton(
                        onClick = state.onPrimary,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(state.primaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AtmosphericGlow(
    sparkleColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(glowColor.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.74f, size.height * 0.28f),
                    radius = size.width * 0.52f,
                ),
        )

        repeat(12) { index ->
            val x = size.width * ((index * 13 % 100) / 100f)
            val y = size.height * (0.14f + (index * 7 % 44) / 100f)
            val radius = if (index % 4 == 0) 3.4f else 2.2f
            drawCircle(
                color = sparkleColor.copy(alpha = if (index % 3 == 0) 0.92f else 0.68f),
                radius = radius,
                center = Offset(x, y),
            )
        }
    }
}

private data class GamePalette(
    val skyTop: Color,
    val skyBottom: Color,
    val overlayGlow: Color,
    val sparkle: Color,
    val layerTopStart: Color,
    val layerTopEnd: Color,
    val layerFrontStart: Color,
    val layerFrontEnd: Color,
    val layerSideStart: Color,
    val layerSideEnd: Color,
    val activeTop: Color,
    val activeFront: Color,
    val activeSide: Color,
)

private data class BlockColors(
    val top: Color,
    val front: Color,
    val side: Color,
)

private data class StageMetrics(
    val centerX: Float,
    val laneWidth: Float,
    val baseY: Float,
    val blockHeight: Float,
    val blockDepth: Float,
)

private data class OutcomeDialogState(
    val title: String,
    val score: Int,
    val message: String,
    val story: StoryLineUi,
    val primaryLabel: String,
    val secondaryLabel: String,
    val onPrimary: () -> Unit,
    val onSecondary: () -> Unit,
)

@Composable
private fun rememberDialogState(
    uiState: EasterEggUiState,
    onRestart: () -> Unit,
    onNavigateBack: () -> Unit,
): OutcomeDialogState? =
    when (uiState.status) {
        StackGameStatus.Playing -> null
        StackGameStatus.GameOver ->
            OutcomeDialogState(
                title = stringResource(R.string.easter_egg_game_over_title),
                score = uiState.score,
                message =
                    pluralStringResource(
                        R.plurals.easter_egg_game_over_message,
                        uiState.score,
                        uiState.score,
                    ),
                story = uiState.currentStory,
                primaryLabel = stringResource(R.string.easter_egg_restart),
                secondaryLabel = stringResource(R.string.easter_egg_exit),
                onPrimary = onRestart,
                onSecondary = onNavigateBack,
            )
        StackGameStatus.Cleared ->
            OutcomeDialogState(
                title = stringResource(R.string.easter_egg_clear_title),
                score = uiState.score,
                message = stringResource(R.string.easter_egg_clear_message),
                story = uiState.currentStory,
                primaryLabel = stringResource(R.string.easter_egg_retry),
                secondaryLabel = stringResource(R.string.common_close),
                onPrimary = onRestart,
                onSecondary = onNavigateBack,
            )
    }

private fun layerColors(
    progress: Float,
    palette: GamePalette,
): BlockColors =
    BlockColors(
        top = lerp(palette.layerTopStart, palette.layerTopEnd, progress),
        front = lerp(palette.layerFrontStart, palette.layerFrontEnd, progress),
        side = lerp(palette.layerSideStart, palette.layerSideEnd, progress),
    )

private fun paletteFor(
    progress: Float,
    darkMode: Boolean,
): GamePalette {
    val palettes = if (darkMode) DARK_PALETTES else LIGHT_PALETTES
    if (progress >= 1f) {
        return palettes.last()
    }
    val scaled = progress * (palettes.size - 1)
    val startIndex = scaled.toInt().coerceAtMost(palettes.lastIndex - 1)
    val localProgress = scaled - startIndex
    return blendPalette(
        start = palettes[startIndex],
        end = palettes[startIndex + 1],
        progress = localProgress,
    )
}

private fun blendPalette(
    start: GamePalette,
    end: GamePalette,
    progress: Float,
): GamePalette =
    GamePalette(
        skyTop = lerp(start.skyTop, end.skyTop, progress),
        skyBottom = lerp(start.skyBottom, end.skyBottom, progress),
        overlayGlow = lerp(start.overlayGlow, end.overlayGlow, progress),
        sparkle = lerp(start.sparkle, end.sparkle, progress),
        layerTopStart = lerp(start.layerTopStart, end.layerTopStart, progress),
        layerTopEnd = lerp(start.layerTopEnd, end.layerTopEnd, progress),
        layerFrontStart = lerp(start.layerFrontStart, end.layerFrontStart, progress),
        layerFrontEnd = lerp(start.layerFrontEnd, end.layerFrontEnd, progress),
        layerSideStart = lerp(start.layerSideStart, end.layerSideStart, progress),
        layerSideEnd = lerp(start.layerSideEnd, end.layerSideEnd, progress),
        activeTop = lerp(start.activeTop, end.activeTop, progress),
        activeFront = lerp(start.activeFront, end.activeFront, progress),
        activeSide = lerp(start.activeSide, end.activeSide, progress),
    )

private val LIGHT_PALETTES =
    listOf(
        GamePalette(
            skyTop = Color(0xFFCE4C81),
            skyBottom = Color(0xFFFCE6AE),
            overlayGlow = Color(0xFFFFE0A6),
            sparkle = Color.White,
            layerTopStart = Color(0xFF574053),
            layerTopEnd = Color(0xFFCB4E82),
            layerFrontStart = Color(0xFF2E2330),
            layerFrontEnd = Color(0xFF8B325A),
            layerSideStart = Color(0xFF3B2A3B),
            layerSideEnd = Color(0xFFAA416A),
            activeTop = Color(0xFFFDF19A),
            activeFront = Color(0xFFD9B05E),
            activeSide = Color(0xFFE9CA78),
        ),
        GamePalette(
            skyTop = Color(0xFFD8577D),
            skyBottom = Color(0xFFFFD596),
            overlayGlow = Color(0xFFFFE09C),
            sparkle = Color(0xFFFDF6FF),
            layerTopStart = Color(0xFF80406A),
            layerTopEnd = Color(0xFFE0756C),
            layerFrontStart = Color(0xFF5C2750),
            layerFrontEnd = Color(0xFFBE4E55),
            layerSideStart = Color(0xFF6B3159),
            layerSideEnd = Color(0xFFD0615E),
            activeTop = Color(0xFFFFF09B),
            activeFront = Color(0xFFE2AF64),
            activeSide = Color(0xFFF0CA7C),
        ),
        GamePalette(
            skyTop = Color(0xFF5076D7),
            skyBottom = Color(0xFFFFC976),
            overlayGlow = Color(0xFFF9DBB2),
            sparkle = Color(0xFFEFF7FF),
            layerTopStart = Color(0xFF5C4091),
            layerTopEnd = Color(0xFFEF9667),
            layerFrontStart = Color(0xFF392964),
            layerFrontEnd = Color(0xFFC56C4C),
            layerSideStart = Color(0xFF4A357B),
            layerSideEnd = Color(0xFFDC815B),
            activeTop = Color(0xFFFFF2A3),
            activeFront = Color(0xFFE0B16E),
            activeSide = Color(0xFFF0CF84),
        ),
        GamePalette(
            skyTop = Color(0xFF2040A0),
            skyBottom = Color(0xFF88E1D1),
            overlayGlow = Color(0xFFB8F0E3),
            sparkle = Color(0xFFEAF8FF),
            layerTopStart = Color(0xFF345D9C),
            layerTopEnd = Color(0xFF8BE0D0),
            layerFrontStart = Color(0xFF233E69),
            layerFrontEnd = Color(0xFF4BA79F),
            layerSideStart = Color(0xFF2E4E83),
            layerSideEnd = Color(0xFF67C7B9),
            activeTop = Color(0xFFFFF6B1),
            activeFront = Color(0xFFE6C37C),
            activeSide = Color(0xFFF3D792),
        ),
    )

private val DARK_PALETTES =
    listOf(
        GamePalette(
            skyTop = Color(0xFF4C1D39),
            skyBottom = Color(0xFF1D1720),
            overlayGlow = Color(0xFFD66A8D),
            sparkle = Color(0xFFF7E8EE),
            layerTopStart = Color(0xFF1E1822),
            layerTopEnd = Color(0xFF7F2E59),
            layerFrontStart = Color(0xFF0D0A12),
            layerFrontEnd = Color(0xFF521B39),
            layerSideStart = Color(0xFF17111D),
            layerSideEnd = Color(0xFF672247),
            activeTop = Color(0xFFF4E590),
            activeFront = Color(0xFFB38643),
            activeSide = Color(0xFFD2A85A),
        ),
        GamePalette(
            skyTop = Color(0xFF5A2B6F),
            skyBottom = Color(0xFF201A2D),
            overlayGlow = Color(0xFFFF9A6E),
            sparkle = Color(0xFFF6F0FF),
            layerTopStart = Color(0xFF221A36),
            layerTopEnd = Color(0xFFB2525D),
            layerFrontStart = Color(0xFF120E1D),
            layerFrontEnd = Color(0xFF793141),
            layerSideStart = Color(0xFF1C1530),
            layerSideEnd = Color(0xFF96424F),
            activeTop = Color(0xFFF8E89A),
            activeFront = Color(0xFFBA8A4F),
            activeSide = Color(0xFFD9AF67),
        ),
        GamePalette(
            skyTop = Color(0xFF1D4C8E),
            skyBottom = Color(0xFF1B2133),
            overlayGlow = Color(0xFFEEB872),
            sparkle = Color(0xFFE9F3FF),
            layerTopStart = Color(0xFF18243D),
            layerTopEnd = Color(0xFFB86A48),
            layerFrontStart = Color(0xFF0D1426),
            layerFrontEnd = Color(0xFF844930),
            layerSideStart = Color(0xFF131D32),
            layerSideEnd = Color(0xFFA25B3D),
            activeTop = Color(0xFFFAECA4),
            activeFront = Color(0xFFB7894F),
            activeSide = Color(0xFFD7B46B),
        ),
        GamePalette(
            skyTop = Color(0xFF155B74),
            skyBottom = Color(0xFF0F1B26),
            overlayGlow = Color(0xFF8DEAD4),
            sparkle = Color(0xFFE6FFF8),
            layerTopStart = Color(0xFF143041),
            layerTopEnd = Color(0xFF4FB9A8),
            layerFrontStart = Color(0xFF0B1B24),
            layerFrontEnd = Color(0xFF2A7F78),
            layerSideStart = Color(0xFF102534),
            layerSideEnd = Color(0xFF3F9C90),
            activeTop = Color(0xFFFCF0AC),
            activeFront = Color(0xFFBE9358),
            activeSide = Color(0xFFD7B770),
        ),
    )

private const val MAX_VISIBLE_LAYERS = 16
private const val AUTO_SCROLL_START_LAYER = 5
