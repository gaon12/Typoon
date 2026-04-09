package xyz.gaon.typoon.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.clipboard.ClipboardHelper
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.model.ConversionStats
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.AppSessionState
import xyz.gaon.typoon.core.di.ConversionDispatcher
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionDirection
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.engine.ConversionResult
import xyz.gaon.typoon.core.text.TextPayloadSanitizer
import javax.inject.Inject

data class ClipboardSuggestionState(
    val originalText: String,
    val suggestedText: String,
    val confidence: Float,
)

sealed interface HomeUiEvent {
    data object NavigateToResult : HomeUiEvent

    data class ShowMessage(
        val message: String,
    ) : HomeUiEvent

    data class ItemDeleted(
        val entity: ConversionEntity,
    ) : HomeUiEvent
}

@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val conversionEngine: ConversionEngine,
        private val historyRepository: HistoryRepository,
        private val clipboardHelper: ClipboardHelper,
        private val appPreferences: AppPreferences,
        private val appSessionState: AppSessionState,
        private val pendingHolder: PendingConversionHolder,
        @param:ConversionDispatcher private val conversionDispatcher: CoroutineDispatcher,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _inputText = MutableStateFlow("")
        val inputText: StateFlow<String> = _inputText.asStateFlow()
        private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 4)
        val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()
        private var hasHandledInitialLaunch = false
        private var hasCheckedSuggestionThisSession = false
        private var lastSuggestedClipboardText: String? = null

        private val _clipboardSuggestion = MutableStateFlow<ClipboardSuggestionState?>(null)
        val clipboardSuggestion: StateFlow<ClipboardSuggestionState?> = _clipboardSuggestion.asStateFlow()

        val recentHistory: StateFlow<List<ConversionEntity>> =
            historyRepository
                .getRecentHistory(5)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val settings: StateFlow<AppSettings> =
            appPreferences.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        val stats: StateFlow<ConversionStats> =
            historyRepository
                .getStats()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversionStats())

        val starredHistory: StateFlow<List<ConversionEntity>> =
            historyRepository
                .getStarred()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        fun onInputChange(text: String) {
            _inputText.value = TextPayloadSanitizer.sanitize(text).take(MAX_INPUT_LENGTH)
        }

        companion object {
            const val MAX_INPUT_LENGTH = 1000
        }

        fun onClearInput() {
            _inputText.value = ""
        }

        fun onScreenEntered(forceReadClipboard: Boolean = false) {
            viewModelScope.launch {
                if (forceReadClipboard) {
                    hasHandledInitialLaunch = true
                    appSessionState.hasHandledHomeAutoRead = true
                    _clipboardSuggestion.value = null
                    readClipboardIntoInput(
                        autoConvert = true,
                        emptyMessage = appContext.getString(R.string.home_message_shortcut_empty),
                        successMessage = appContext.getString(R.string.home_message_shortcut_success),
                    )
                    return@launch
                }

                if (hasHandledInitialLaunch) return@launch
                hasHandledInitialLaunch = true
                if (appSessionState.hasHandledHomeAutoRead) return@launch
                appSessionState.hasHandledHomeAutoRead = true

                val currentSettings = appPreferences.settings.first()
                if (currentSettings.autoReadClipboardOnLaunch) {
                    readClipboardIntoInput(
                        autoConvert = currentSettings.autoConvertAfterClipboardRead,
                        emptyMessage = appContext.getString(R.string.home_message_clipboard_empty),
                        successMessage = appContext.getString(R.string.home_message_clipboard_success),
                    )
                    return@launch
                }

                if (hasCheckedSuggestionThisSession) return@launch
                if (!currentSettings.clipboardSuggestionEnabled) return@launch
                maybeShowClipboardSuggestion()
            }
        }

        fun onReadClipboard() {
            viewModelScope.launch {
                _clipboardSuggestion.value = null
                readClipboardIntoInput(
                    autoConvert = settings.value.autoConvertAfterClipboardRead,
                    emptyMessage = appContext.getString(R.string.home_message_shortcut_empty),
                    successMessage = appContext.getString(R.string.home_message_clipboard_success),
                )
            }
        }

        fun applyClipboardSuggestion() {
            val suggestion = _clipboardSuggestion.value ?: return
            _inputText.value = suggestion.suggestedText.take(MAX_INPUT_LENGTH)
            _clipboardSuggestion.value = null
            viewModelScope.launch {
                _events.emit(HomeUiEvent.ShowMessage(appContext.getString(R.string.home_message_suggestion_applied)))
            }
        }

        fun dismissClipboardSuggestion() {
            _clipboardSuggestion.value = null
        }

        fun onConvert() {
            viewModelScope.launch {
                if (convertCurrentInput()) {
                    _events.emit(HomeUiEvent.NavigateToResult)
                } else {
                    _events.emit(HomeUiEvent.ShowMessage(appContext.getString(R.string.home_message_input_required)))
                }
            }
        }

        fun dismissHelpBanner() {
            viewModelScope.launch {
                appPreferences.update { it.copy(showHelpBanner = false) }
            }
        }

        private suspend fun readClipboardIntoInput(
            autoConvert: Boolean,
            emptyMessage: String,
            successMessage: String,
        ) {
            val text = TextPayloadSanitizer.sanitize(clipboardHelper.readText()?.trim()).take(MAX_INPUT_LENGTH)
            if (text.isBlank()) {
                _events.emit(HomeUiEvent.ShowMessage(emptyMessage))
                return
            }

            _inputText.value = text
            _events.emit(HomeUiEvent.ShowMessage(successMessage))

            if (autoConvert && convertCurrentInput()) {
                _events.emit(HomeUiEvent.NavigateToResult)
            }
        }

        private suspend fun maybeShowClipboardSuggestion() {
            hasCheckedSuggestionThisSession = true
            val clipboardText =
                TextPayloadSanitizer
                    .sanitize(clipboardHelper.readText()?.trim())
                    .take(MAX_INPUT_LENGTH)

            val isEligibleClipboardText =
                clipboardText.isNotBlank() &&
                    clipboardText != lastSuggestedClipboardText &&
                    clipboardText.isSuspiciousTypoCandidate()
            if (isEligibleClipboardText) {
                val conversion = runConversion(clipboardText)
                val suggestedText = conversion.resultText.trim()
                if (shouldOfferSuggestion(clipboardText, suggestedText, conversion.confidence)) {
                    _clipboardSuggestion.value =
                        ClipboardSuggestionState(
                            originalText = clipboardText,
                            suggestedText = suggestedText,
                            confidence = conversion.confidence,
                        )
                    lastSuggestedClipboardText = clipboardText
                }
            }
        }

        private fun shouldOfferSuggestion(
            original: String,
            suggested: String,
            confidence: Float,
        ): Boolean =
            confidence >= 0.55f &&
                suggested.isNotBlank() &&
                original != suggested &&
                normalizedSuggestionText(original) != normalizedSuggestionText(suggested)

        private fun normalizedSuggestionText(text: String): String = text.filter(Char::isLetterOrDigit).lowercase()

        private fun String.isSuspiciousTypoCandidate(): Boolean {
            val hasHangul =
                any {
                    it in '\uAC00'..'\uD7A3' ||
                        it in '\u3131'..'\u318E' ||
                        it.code in 0x1100..0x11FF ||
                        it.code in 0xA960..0xA97F ||
                        it.code in 0xD7B0..0xD7FF
                }
            val hasLatin = any { it.isLetter() && it.code < 128 }
            val hasDigit = any(Char::isDigit)
            val isLengthAllowed = length in 2..180
            return isLengthAllowed && !contains('\n') && (hasHangul || hasLatin || hasDigit)
        }

        private suspend fun convertCurrentInput(): Boolean {
            val text = _inputText.value.trim()
            if (text.isBlank()) return false

            val result = runConversion(text)
            pendingHolder.sourceText = text
            pendingHolder.result = result
            pendingHolder.isFromShare = false
            pendingHolder.entryPoint = "HOME"
            pendingHolder.processTextReadOnly = false
            pendingHolder.historyId = 0L
            pendingHolder.isStarred = false
            pendingHolder.shouldCheckReviewPrompt = true

            if (settings.value.saveHistory) {
                val id =
                    historyRepository.insert(
                        ConversionEntity(
                            sourceText = text,
                            resultText = result.resultText,
                            direction = result.direction.name,
                            confidence = result.confidence,
                            createdAt = System.currentTimeMillis(),
                            entryPoint = "HOME",
                        ),
                    )
                pendingHolder.historyId = id
            }
            return true
        }

        private suspend fun runConversion(text: String): ConversionResult =
            withContext(conversionDispatcher) {
                conversionEngine.convert(text)
            }

        fun copyHistoryResult(entity: ConversionEntity) {
            clipboardHelper.writeText(entity.resultText)
        }

        fun onToggleStar(
            id: Long,
            isStarred: Boolean,
        ) {
            viewModelScope.launch {
                historyRepository.toggleStar(id, isStarred)
            }
        }

        private var lastDeleted: ConversionEntity? = null

        fun deleteHistoryItem(entity: ConversionEntity) {
            lastDeleted = entity
            viewModelScope.launch {
                historyRepository.delete(entity.id)
                _events.emit(HomeUiEvent.ItemDeleted(entity))
            }
        }

        fun undoDelete() {
            val entity = lastDeleted ?: return
            lastDeleted = null
            viewModelScope.launch {
                historyRepository.insert(entity.copy(id = 0))
            }
        }

        /** 기록 항목을 PendingHolder에 직접 세팅 후 Result 화면으로 이동 가능하게 준비 */
        fun loadHistoryToResult(entity: ConversionEntity) {
            pendingHolder.sourceText = entity.sourceText
            pendingHolder.result =
                ConversionResult(
                    resultText = entity.resultText,
                    direction = ConversionDirection.fromPersisted(entity.direction),
                    confidence = entity.confidence,
                )
            pendingHolder.isFromShare = false
            pendingHolder.entryPoint = entity.entryPoint
            pendingHolder.processTextReadOnly = false
            pendingHolder.historyId = entity.id
            pendingHolder.isStarred = entity.isStarred
            pendingHolder.shouldCheckReviewPrompt = false
        }
    }
