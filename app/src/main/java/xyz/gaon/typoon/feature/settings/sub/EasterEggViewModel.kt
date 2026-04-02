package xyz.gaon.typoon.feature.settings.sub

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.datastore.AppLanguage
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.engine.ConversionDirection
import xyz.gaon.typoon.core.engine.ConversionEngine
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.min

data class EasterEggUiState(
    val status: StackGameStatus = StackGameStatus.Playing,
    val score: Int = 0,
    val targetScore: Int = 100,
    val stackLayers: List<StackLayerUi> = emptyList(),
    val movingBlock: MovingBlockUi = MovingBlockUi(),
    val currentStory: StoryLineUi = StoryLineUi(),
    val fallingPiece: FallingPieceUi? = null,
    val canStack: Boolean = true,
)

enum class StackGameStatus {
    Playing,
    GameOver,
    Cleared,
}

data class StackLayerUi(
    val left: Float,
    val right: Float,
)

data class MovingBlockUi(
    val centerX: Float = 0.5f,
    val width: Float = 0.64f,
)

data class FallingPieceUi(
    val left: Float,
    val right: Float,
    val layerIndex: Int,
    val driftDirection: Float,
    val progress: Float,
)

data class StoryLineUi(
    val id: String = "",
    val wrongText: String = "",
    val correctText: String = "",
)

private data class StoryScript(
    val id: String,
    val koreanText: String,
    val englishText: String,
)

private data class StoryContent(
    val wrongText: String,
    val correctText: String,
)

@HiltViewModel
class EasterEggViewModel
    @Inject
    constructor(
        private val conversionEngine: ConversionEngine,
        appPreferences: AppPreferences,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(EasterEggUiState())
        val uiState: StateFlow<EasterEggUiState> = _uiState.asStateFlow()

        private val stackLayers = mutableListOf(BASE_LAYER)
        private var movingCenterX = INITIAL_BLOCK_WIDTH / 2f
        private var movingWidth = INITIAL_BLOCK_WIDTH
        private var movingDirection = 1f
        private var score = 0
        private var status = StackGameStatus.Playing
        private var outcomeStoryIndex = 0
        private var fallingPiece: FallingPieceUi? = null

        private var movementJob: Job? = null
        private var outcomeStoryJob: Job? = null
        private var fallingPieceJob: Job? = null
        private val appLanguage =
            appPreferences.settings
                .map { it.appLanguage }
                .stateIn(viewModelScope, SharingStarted.Eagerly, AppLanguage.SYSTEM)
        private val storyCache by lazy { buildStoryCache() }

        init {
            resetGame()
            startMovementLoop()
        }

        fun onStack() {
            if (status == StackGameStatus.Playing) {
                val activeLeft = movingCenterX - movingWidth / 2f
                val activeRight = movingCenterX + movingWidth / 2f
                val topLayer = stackLayers.last()
                val overlapLeft = max(activeLeft, topLayer.left)
                val overlapRight = min(activeRight, topLayer.right)
                val overlapWidth = overlapRight - overlapLeft

                if (overlapWidth <= MIN_OVERLAP_WIDTH) {
                    startFallingPiece(
                        left = activeLeft,
                        right = activeRight,
                        layerIndex = stackLayers.lastIndex + 1,
                        driftDirection = movingDirection,
                    )
                    finishGame(StackGameStatus.GameOver)
                } else {
                    val trimmedPiece =
                        trimmedPiece(
                            activeLeft = activeLeft,
                            activeRight = activeRight,
                            overlapLeft = overlapLeft,
                            overlapRight = overlapRight,
                        )
                    stackLayers +=
                        StackLayerUi(
                            left = overlapLeft,
                            right = overlapRight,
                        )
                    trimmedPiece?.let { piece ->
                        startFallingPiece(
                            left = piece.left,
                            right = piece.right,
                            layerIndex = stackLayers.lastIndex,
                            driftDirection = piece.driftDirection,
                        )
                    }
                    score += 1

                    if (score >= TARGET_SCORE) {
                        finishGame(StackGameStatus.Cleared)
                    } else {
                        prepareNextBlock(overlapWidth)
                        publishState()
                    }
                }
            }
        }

        fun onRestart() {
            resetGame()
            publishState()
        }

        private fun resetGame() {
            outcomeStoryJob?.cancel()
            fallingPieceJob?.cancel()
            stackLayers.clear()
            stackLayers += BASE_LAYER
            movingWidth = INITIAL_BLOCK_WIDTH
            movingCenterX = movingWidth / 2f
            movingDirection = 1f
            score = 0
            status = StackGameStatus.Playing
            outcomeStoryIndex = 0
            fallingPiece = null
            publishState()
        }

        private fun startMovementLoop() {
            movementJob?.cancel()
            movementJob =
                viewModelScope.launch {
                    var lastFrameAt = SystemClock.elapsedRealtime()
                    while (isActive) {
                        val now = SystemClock.elapsedRealtime()
                        val deltaMs = (now - lastFrameAt).coerceAtMost(MAX_FRAME_DELTA_MS)
                        lastFrameAt = now

                        if (status == StackGameStatus.Playing) {
                            updateMovingBlock(deltaMs)
                            publishState()
                        }

                        delay(FRAME_DELAY_MS)
                    }
                }
        }

        private fun prepareNextBlock(overlapWidth: Float) {
            movingWidth = overlapWidth
            movingDirection *= -1f
            movingCenterX =
                if (movingDirection > 0f) {
                    movingWidth / 2f
                } else {
                    1f - movingWidth / 2f
                }
        }

        private fun finishGame(nextStatus: StackGameStatus) {
            status = nextStatus
            startOutcomeStory()
            publishState()
        }

        private fun updateMovingBlock(deltaMs: Long) {
            val speed = currentSpeed()
            val delta = speed * (deltaMs.toFloat() / FRAME_DELAY_MS.toFloat())
            val halfWidth = movingWidth / 2f
            var nextCenterX = movingCenterX + delta * movingDirection

            if (nextCenterX <= halfWidth) {
                nextCenterX = halfWidth
                movingDirection = 1f
            } else if (nextCenterX >= 1f - halfWidth) {
                nextCenterX = 1f - halfWidth
                movingDirection = -1f
            }

            movingCenterX = nextCenterX
        }

        private fun currentSpeed(): Float = (BASE_SPEED + score * SPEED_STEP).coerceAtMost(MAX_SPEED)

        private fun startOutcomeStory() {
            outcomeStoryJob?.cancel()
            outcomeStoryIndex = 0
            val scripts = currentStoryScripts()
            outcomeStoryJob =
                viewModelScope.launch {
                    while (scripts.isNotEmpty()) {
                        delay(OUTCOME_STORY_DELAY_MS)
                        outcomeStoryIndex = (outcomeStoryIndex + 1) % scripts.size
                        publishState()
                    }
                }
        }

        private fun publishState() {
            val story = currentStoryLine()
            _uiState.value =
                EasterEggUiState(
                    status = status,
                    score = score,
                    targetScore = TARGET_SCORE,
                    stackLayers = stackLayers.toList(),
                    movingBlock =
                        MovingBlockUi(
                            centerX = movingCenterX,
                            width = movingWidth,
                        ),
                    currentStory = story,
                    fallingPiece = fallingPiece,
                    canStack = status == StackGameStatus.Playing,
                )
        }

        private fun trimmedPiece(
            activeLeft: Float,
            activeRight: Float,
            overlapLeft: Float,
            overlapRight: Float,
        ): TrimmedPiece? =
            when {
                activeLeft < overlapLeft ->
                    TrimmedPiece(
                        left = activeLeft,
                        right = overlapLeft,
                        driftDirection = -1f,
                    )
                activeRight > overlapRight ->
                    TrimmedPiece(
                        left = overlapRight,
                        right = activeRight,
                        driftDirection = 1f,
                    )
                else -> null
            }

        private fun startFallingPiece(
            left: Float,
            right: Float,
            layerIndex: Int,
            driftDirection: Float,
        ) {
            if (right - left <= FALLING_PIECE_MIN_WIDTH) {
                fallingPiece = null
                return
            }
            fallingPieceJob?.cancel()
            fallingPiece =
                FallingPieceUi(
                    left = left,
                    right = right,
                    layerIndex = layerIndex,
                    driftDirection = driftDirection,
                    progress = 0f,
                )
            publishState()

            fallingPieceJob =
                viewModelScope.launch {
                    repeat(FALLING_PIECE_FRAMES) { step ->
                        delay(FALLING_PIECE_FRAME_DELAY_MS)
                        fallingPiece =
                            fallingPiece?.copy(
                                progress = (step + 1) / FALLING_PIECE_FRAMES.toFloat(),
                            )
                        publishState()
                    }
                    fallingPiece = null
                    publishState()
                }
        }

        private fun currentStoryLine(): StoryLineUi {
            val script =
                when (status) {
                    StackGameStatus.Playing -> {
                        val index = (score / PLAYING_SCRIPT_STEP).coerceAtMost(PLAYING_SCRIPTS.lastIndex)
                        PLAYING_SCRIPTS[index]
                    }

                    StackGameStatus.GameOver -> GAME_OVER_SCRIPTS[outcomeStoryIndex]
                    StackGameStatus.Cleared -> CLEAR_SCRIPTS[outcomeStoryIndex]
                }
            val isKoreanUi =
                when (appLanguage.value) {
                    AppLanguage.KOREAN -> true
                    AppLanguage.ENGLISH -> false
                    AppLanguage.SYSTEM ->
                        java.util.Locale
                            .getDefault()
                            .language
                            .startsWith("ko")
                }
            val storyContent =
                if (isKoreanUi) {
                    storyCache.getValue(script.id).korean
                } else {
                    storyCache.getValue(script.id).english
                }

            return StoryLineUi(
                id = "${status.name}_${script.id}_$outcomeStoryIndex",
                wrongText = storyContent.wrongText,
                correctText = storyContent.correctText,
            )
        }

        private fun buildStoryCache(): Map<String, CachedStoryLine> =
            (PLAYING_SCRIPTS + GAME_OVER_SCRIPTS + CLEAR_SCRIPTS).associate { script ->
                script.id to
                    CachedStoryLine(
                        korean =
                            StoryContent(
                                wrongText =
                                    conversionEngine
                                        .convertForced(
                                            script.koreanText,
                                            ConversionDirection.KOR_TO_ENG,
                                        ).resultText,
                                correctText = script.koreanText,
                            ),
                        english =
                            StoryContent(
                                wrongText =
                                    conversionEngine
                                        .convertForced(
                                            script.englishText,
                                            ConversionDirection.ENG_TO_KOR,
                                        ).resultText,
                                correctText = script.englishText,
                            ),
                    )
            }

        private fun currentStoryScripts(): List<StoryScript> =
            when (status) {
                StackGameStatus.Playing -> PLAYING_SCRIPTS
                StackGameStatus.GameOver -> GAME_OVER_SCRIPTS
                StackGameStatus.Cleared -> CLEAR_SCRIPTS
            }

        companion object {
            private const val TARGET_SCORE = 100
            private const val FRAME_DELAY_MS = 16L
            private const val MAX_FRAME_DELTA_MS = 48L
            private const val OUTCOME_STORY_DELAY_MS = 8_200L
            private const val PLAYING_SCRIPT_STEP = 8
            private const val BASE_SPEED = 0.0075f
            private const val SPEED_STEP = 0.00008f
            private const val MAX_SPEED = 0.018f
            private const val INITIAL_BLOCK_WIDTH = 0.64f
            private const val MIN_OVERLAP_WIDTH = 0.035f
            private const val FALLING_PIECE_MIN_WIDTH = 0.01f
            private const val FALLING_PIECE_FRAMES = 16
            private const val FALLING_PIECE_FRAME_DELAY_MS = 18L

            private val BASE_LAYER =
                StackLayerUi(
                    left = 0.18f,
                    right = 0.82f,
                )

            private val PLAYING_SCRIPTS =
                listOf(
                    StoryScript(
                        "play_01",
                        "게임이 어렵나요? 인생은 더 어려워요.",
                        "Is the game hard? Life is harder.",
                    ),
                    StoryScript(
                        "play_02",
                        "실패할 수 있어요. 넘어질 수 있어요.",
                        "You can fail. You can stumble.",
                    ),
                    StoryScript(
                        "play_03",
                        "하지만 손을 떼지 않으면 흐름은 다시 돌아와요.",
                        "But if you do not let go, the rhythm comes back.",
                    ),
                    StoryScript(
                        "play_04",
                        "한 번 흔들린 리듬은 다시 맞출 수 있어요.",
                        "A shaken rhythm can still be found again.",
                    ),
                    StoryScript(
                        "play_05",
                        "오타를 지우듯 오늘의 실수도 정리할 수 있어요.",
                        "You can clean up today’s mistakes like clearing a typo.",
                    ),
                    StoryScript(
                        "play_06",
                        "마음이 급해질수록 호흡부터 천천히 고르세요.",
                        "When your mind rushes, steady your breathing first.",
                    ),
                    StoryScript(
                        "play_07",
                        "작은 한 층이 쌓이면 다음 한 층도 버틸 힘이 생겨요.",
                        "One small layer gives you strength for the next.",
                    ),
                    StoryScript(
                        "play_08",
                        "누구나 중간에서 멈추고 싶을 때가 있어요.",
                        "Everyone has moments when they want to stop midway.",
                    ),
                    StoryScript(
                        "play_09",
                        "그래도 다시 한 번 누르면 길은 이어집니다.",
                        "Still, one more tap keeps the path going.",
                    ),
                    StoryScript(
                        "play_10",
                        "넘어지지 않는 사람보다 다시 세우는 사람이 더 멀리 가요.",
                        "The one who rebuilds goes farther than the one who never falls.",
                    ),
                    StoryScript(
                        "play_11",
                        "지금 이 한 칸이 내일의 문장을 바꿉니다.",
                        "This one block can change tomorrow’s sentence.",
                    ),
                    StoryScript(
                        "play_12",
                        "흔들려도 괜찮아요. 무너지지만 않으면 돼요.",
                        "Shaking is fine. You only need to keep standing.",
                    ),
                    StoryScript(
                        "play_13",
                        "끝까지 쌓으면 오늘의 문장도 결국 복구됩니다.",
                        "Stack to the end, and today’s sentence recovers too.",
                    ),
                )

            private val GAME_OVER_SCRIPTS =
                listOf(
                    StoryScript(
                        "over_01",
                        "멈춘 자리만 바라보면 오늘이 실패처럼 느껴질 수 있어요. " +
                            "하지만 조금 떨어진 곳에서 보면, 당신은 방금까지 분명히 더 높은 곳을 향해 손을 뻗고 있었어요. " +
                            "인생도 자주 이렇게 끝난 것처럼 보이는 장면에서 다시 시작되곤 합니다.",
                        "If you only look at where you stopped, today can feel like failure. " +
                            "But from a little farther away, you were clearly reaching higher just moments ago. " +
                            "Life often begins again in scenes that look like the end.",
                    ),
                    StoryScript(
                        "over_02",
                        "무너진 탑은 당신이 부족하다는 증거가 아니라, " +
                            "다시 쌓을 수 있는 손을 아직 놓지 않았다는 증거예요. " +
                            "방금의 흔들림은 감각을 잃었다는 뜻이 아니라 다음 번엔 어디에 힘을 주어야 하는지 배웠다는 뜻입니다. " +
                            "그러니 이번 판은 패배가 아니라 다음 시도를 위한 밑그림으로 남겨 두세요.",
                        "A fallen tower is not proof that you are lacking. " +
                            "It proves you still have hands that can build again. " +
                            "That shake did not mean you lost your touch. " +
                            "It meant you learned where to place your strength next time. " +
                            "Let this round stay as a sketch for the next attempt, not as defeat.",
                    ),
                    StoryScript(
                        "over_03",
                        "세상에는 완벽하게 첫 시도에 성공하는 사람보다, " +
                            "멈춘 뒤에도 다시 화면을 누르는 사람이 훨씬 오래 갑니다. " +
                            "당신이 지금 다시 시작을 고른다면 오늘의 실수는 기록이 아니라 연습이 되고, " +
                            "주저앉은 순간은 포기가 아니라 준비가 됩니다.",
                        "People who tap the screen again after stopping last longer " +
                            "than people who win perfectly on the first try. " +
                            "If you choose to restart now, today’s mistake becomes practice instead of a record, " +
                            "and the moment you sat down becomes preparation instead of surrender.",
                    ),
                    StoryScript(
                        "over_04",
                        "숨을 조금만 고르고 다시 한 번 올려 보세요. " +
                            "다음 블록은 이번보다 덜 흔들릴 수 있고, 다음 마음은 방금보다 더 단단할 수 있어요. " +
                            "천천히라도 좋으니, 당신이 다시 시작하는 장면을 이 게임은 끝없이 기다리고 있습니다.",
                        "Take one breath and lift it again. " +
                            "The next block can shake less than this one, " +
                            "and your next state of mind can be steadier than the last. " +
                            "Go slowly if you need to. This game keeps waiting for your restart.",
                    ),
                )

            private val CLEAR_SCRIPTS =
                listOf(
                    StoryScript(
                        "clear_01",
                        "백 층을 끝까지 올렸다는 건 단지 한 번의 클리어를 뜻하지 않아요. " +
                            "흔들릴 때마다 손끝의 리듬을 다시 맞추고, 어긋날 때마다 중심을 새로 세웠다는 뜻이에요. " +
                            "여기까지 올라온 사람에게 다음 도전은 두려움이 아니라 자연스러운 다음 장면이어야 합니다.",
                        "Reaching one hundred floors means more than a single clear. " +
                            "It means you found your rhythm again every time you shook, " +
                            "and rebuilt your center every time you drifted. " +
                            "For someone who came this far, the next challenge should feel like the next scene, not fear.",
                    ),
                    StoryScript(
                        "clear_02",
                        "이제 당신은 성공이 우연히 내려앉는 장면보다, 집중이 어떻게 쌓이는지를 아는 사람이 되었습니다. " +
                            "그러니 다음 목표는 단순히 더 높이 가는 것이 아니라, 더 정교하게, 더 침착하게, " +
                            "더 아름답게 쌓아 올리는 것이어도 괜찮아요.",
                        "Now you know how focus is stacked, not just how success lands by accident. " +
                            "So your next goal does not have to be only higher. " +
                            "It can be calmer, sharper, and more beautiful too.",
                    ),
                    StoryScript(
                        "clear_03",
                        "오늘의 정상은 끝이 아니라 기준이 됩니다. 한 번 해낸 사람은 다시 해낼 수 있고, " +
                            "다시 해낸 사람은 더 어려운 길도 고를 수 있어요. " +
                            "방금의 클리어를 기념하면서도, 마음 한편에는 다음 탑의 높이를 슬쩍 상상해 보세요.",
                        "Today’s summit becomes a standard, not an ending. " +
                            "Someone who has done it once can do it again, " +
                            "and someone who can do it again can choose a harder path. Celebrate this clear, " +
                            "but leave a little space in your mind to imagine the height of the next tower.",
                    ),
                    StoryScript(
                        "clear_04",
                        "당신이 방금 넘은 것은 숫자 백이 아니라, 도중에 흔들려도 끝까지 감각을 잃지 않는 자신일지도 모릅니다. " +
                            "이제 남은 건 질문 하나예요. 여기서 만족할 건가요, 아니면 조금 더 높은 집중과 " +
                            "조금 더 날카로운 리듬으로 다시 한 번 새로운 한계를 세워 볼 건가요?",
                        "What you just crossed may not be the number one hundred, " +
                            "but the version of yourself that keeps its rhythm to the end. " +
                            "Now one question remains. Will you stop here, " +
                            "or set a new limit with deeper focus and a sharper rhythm?",
                    ),
                )
        }
    }

private data class TrimmedPiece(
    val left: Float,
    val right: Float,
    val driftDirection: Float,
)

private data class CachedStoryLine(
    val korean: StoryContent,
    val english: StoryContent,
)
