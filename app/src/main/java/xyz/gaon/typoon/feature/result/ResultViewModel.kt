package xyz.gaon.typoon.feature.result

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.R
import xyz.gaon.typoon.core.clipboard.ClipboardHelper
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionDirection
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.engine.ConversionResult
import javax.inject.Inject

sealed interface ResultUiEvent {
    data object TriggerReview : ResultUiEvent
}

@HiltViewModel
class ResultViewModel
    @Inject
    constructor(
        private val conversionEngine: ConversionEngine,
        private val historyRepository: HistoryRepository,
        private val clipboardHelper: ClipboardHelper,
        private val appPreferences: AppPreferences,
        private val pendingHolder: PendingConversionHolder,
        @param:ApplicationContext private val appContext: Context,
    ) : ViewModel() {
        private val _sourceText = MutableStateFlow(pendingHolder.sourceText)
        val sourceText: StateFlow<String> = _sourceText.asStateFlow()

        private val _result = MutableStateFlow(pendingHolder.result)
        val result: StateFlow<ConversionResult?> = _result.asStateFlow()

        val isFromShare: Boolean = pendingHolder.isFromShare
        val entryPoint: String = pendingHolder.entryPoint
        val isProcessTextReadOnly: Boolean = pendingHolder.processTextReadOnly

        private val _copyDone = MutableStateFlow(false)
        val copyDone: StateFlow<Boolean> = _copyDone.asStateFlow()

        /** 복사 후 팝업 닫기 등 외부 이벤트용 (1회성 emit) */
        private val _copyAndClose = MutableStateFlow(false)
        val copyAndClose: StateFlow<Boolean> = _copyAndClose.asStateFlow()

        /** 사용자가 직접 수정한 결과 텍스트. null = 수정 없음 (엔진 결과 사용) */
        private val _editedResultText = MutableStateFlow<String?>(null)
        val editedResultText: StateFlow<String?> = _editedResultText.asStateFlow()

        val settings: StateFlow<AppSettings> =
            appPreferences.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        private val _events = Channel<ResultUiEvent>(capacity = Channel.BUFFERED)
        val events: Flow<ResultUiEvent> = _events.receiveAsFlow()

        private fun effectiveResultText(): String? = _editedResultText.value ?: _result.value?.resultText

        private var currentHistoryId: Long = pendingHolder.historyId
        private var hasMarkedEdited = false
        private var hasCheckedInitialReviewPrompt = false

        private val _isStarred = MutableStateFlow(pendingHolder.isStarred)
        val isStarred: StateFlow<Boolean> = _isStarred.asStateFlow()

        init {
            triggerReviewPromptIfEligible(isNewConversion = pendingHolder.shouldCheckReviewPrompt)
        }

        private fun syncPendingHistoryMetadata() {
            if (currentHistoryId <= 0L && pendingHolder.historyId > 0L) {
                currentHistoryId = pendingHolder.historyId
                _isStarred.value = pendingHolder.isStarred
            }
        }

        private fun triggerReviewPromptIfEligible(isNewConversion: Boolean) {
            if (!isNewConversion) return
            if (hasCheckedInitialReviewPrompt) return
            hasCheckedInitialReviewPrompt = true
            pendingHolder.shouldCheckReviewPrompt = false

            viewModelScope.launch {
                val currentSettings = appPreferences.settings.first()
                if (currentSettings.reviewRequested) return@launch
                waitForPendingHistorySaveIfNeeded(saveHistory = currentSettings.saveHistory)

                val stats = historyRepository.getStats().first()
                if (stats.totalConversions >= REVIEW_REQUEST_THRESHOLD) {
                    _events.send(ResultUiEvent.TriggerReview)
                }
            }
        }

        fun onToggleStar() {
            syncPendingHistoryMetadata()
            if (currentHistoryId <= 0L) return
            val newValue = !_isStarred.value
            _isStarred.value = newValue
            pendingHolder.isStarred = newValue
            viewModelScope.launch { historyRepository.toggleStar(currentHistoryId, newValue) }
        }

        fun onResultEditChange(text: String) {
            syncPendingHistoryMetadata()
            if (_editedResultText.value == null && !hasMarkedEdited && currentHistoryId > 0L) {
                hasMarkedEdited = true
                viewModelScope.launch { historyRepository.markEdited(currentHistoryId) }
            }
            _editedResultText.value = text
        }

        fun onResultEditReset() {
            _editedResultText.value = null
        }

        fun onCopyResult(closeAfterCopy: Boolean = false) {
            effectiveResultText()?.let { text ->
                clipboardHelper.writeText(text)
                _copyDone.value = true
                viewModelScope.launch {
                    kotlinx.coroutines.delay(1200)
                    _copyDone.value = false
                    if (closeAfterCopy) _copyAndClose.value = true
                }
            }
        }

        fun onCopySource() {
            clipboardHelper.writeText(_sourceText.value)
        }

        fun onShare(shareAction: (String) -> Unit) {
            effectiveResultText()?.let { text ->
                val body =
                    if (settings.value.appendShareCredit) {
                        "$text\n\n${appContext.getString(R.string.result_share_credit)}"
                    } else {
                        text
                    }
                shareAction(body)
            }
        }

        fun onReviewShown() {
            viewModelScope.launch {
                appPreferences.update { it.copy(reviewRequested = true) }
            }
        }

        fun onReverse() {
            syncPendingHistoryMetadata()
            val current = _result.value ?: return
            val newDirection =
                when (current.direction) {
                    ConversionDirection.ENG_TO_KOR -> ConversionDirection.KOR_TO_ENG
                    ConversionDirection.KOR_TO_ENG -> ConversionDirection.ENG_TO_KOR
                    ConversionDirection.UNKNOWN -> ConversionDirection.ENG_TO_KOR
                }
            val newResult = conversionEngine.convertForced(_sourceText.value, newDirection)
            _result.value = newResult
            _editedResultText.value = null
            hasMarkedEdited = false
            _isStarred.value = false
            pendingHolder.result = newResult
            pendingHolder.isStarred = false
            pendingHolder.shouldCheckReviewPrompt = false

            viewModelScope.launch {
                if (currentHistoryId > 0L) {
                    historyRepository.markReversed(currentHistoryId)
                }
                if (settings.value.saveHistory) {
                    currentHistoryId =
                        historyRepository.insert(
                            ConversionEntity(
                                sourceText = _sourceText.value,
                                resultText = newResult.resultText,
                                direction = newResult.direction.name,
                                confidence = newResult.confidence,
                                createdAt = System.currentTimeMillis(),
                                entryPoint = entryPoint,
                            ),
                        )
                    pendingHolder.historyId = currentHistoryId
                } else {
                    currentHistoryId = 0L
                    pendingHolder.historyId = 0L
                }
                triggerReviewPromptAfterConversion()
            }
        }

        private suspend fun triggerReviewPromptAfterConversion() {
            val currentSettings = appPreferences.settings.first()
            if (currentSettings.reviewRequested) return

            val stats = historyRepository.getStats().first()
            if (stats.totalConversions >= REVIEW_REQUEST_THRESHOLD) {
                _events.send(ResultUiEvent.TriggerReview)
            }
        }

        private suspend fun waitForPendingHistorySaveIfNeeded(saveHistory: Boolean) {
            if (!saveHistory || pendingHolder.historyId > 0L) return

            repeat(PENDING_HISTORY_WAIT_RETRIES) {
                if (pendingHolder.historyId > 0L) return
                delay(PENDING_HISTORY_WAIT_MS)
            }
        }

        private companion object {
            const val REVIEW_REQUEST_THRESHOLD = 5
            const val PENDING_HISTORY_WAIT_RETRIES = 10
            const val PENDING_HISTORY_WAIT_MS = 50L
        }
    }
