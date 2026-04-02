package xyz.gaon.typoon.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.clipboard.ClipboardHelper
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.model.ConversionStats
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.AppSessionState
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.engine.ConversionResult
import xyz.gaon.typoon.core.text.TextPayloadSanitizer
import javax.inject.Inject

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
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel
    @Inject
    constructor(
        private val conversionEngine: ConversionEngine,
        private val historyRepository: HistoryRepository,
        private val clipboardHelper: ClipboardHelper,
        private val appPreferences: AppPreferences,
        private val appSessionState: AppSessionState,
        private val pendingHolder: PendingConversionHolder,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _inputText = MutableStateFlow("")
        val inputText: StateFlow<String> = _inputText.asStateFlow()
        private val _events = MutableSharedFlow<HomeUiEvent>(extraBufferCapacity = 4)
        val events: SharedFlow<HomeUiEvent> = _events.asSharedFlow()
        private var hasHandledInitialLaunch = false

        val recentHistory: StateFlow<List<ConversionEntity>> =
            historyRepository
                .getRecentHistory(5)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        /** 검색어가 있으면 검색 결과, 없으면 최근 기록 10개 */
        val displayHistory: StateFlow<List<ConversionEntity>> =
            _searchQuery
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        historyRepository.getRecentHistory(5)
                    } else {
                        historyRepository.searchHistory(query)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun onClearInput() {
            _inputText.value = ""
        }

        fun onScreenEntered(forceReadClipboard: Boolean = false) {
            viewModelScope.launch {
                if (forceReadClipboard) {
                    hasHandledInitialLaunch = true
                    appSessionState.hasHandledHomeAutoRead = true
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
                if (!currentSettings.autoReadClipboardOnLaunch) return@launch

                readClipboardIntoInput(
                    autoConvert = currentSettings.autoConvertAfterClipboardRead,
                    emptyMessage = appContext.getString(R.string.home_message_clipboard_empty),
                    successMessage = appContext.getString(R.string.home_message_clipboard_success),
                )
            }
        }

        fun onReadClipboard() {
            viewModelScope.launch {
                readClipboardIntoInput(
                    autoConvert = settings.value.autoConvertAfterClipboardRead,
                    emptyMessage = appContext.getString(R.string.home_message_shortcut_empty),
                    successMessage = appContext.getString(R.string.home_message_clipboard_success),
                )
            }
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

        private suspend fun convertCurrentInput(): Boolean {
            val text = _inputText.value.trim()
            if (text.isBlank()) return false

            val result = conversionEngine.convert(text)
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
                    direction =
                        xyz.gaon.typoon.core.engine.ConversionDirection
                            .valueOf(entity.direction),
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
