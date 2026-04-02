package xyz.gaon.typoon.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.clipboard.ClipboardHelper
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionDirection
import xyz.gaon.typoon.core.engine.ConversionResult
import javax.inject.Inject

sealed interface HistoryUiEvent {
    data class ItemDeleted(
        val entity: ConversionEntity,
    ) : HistoryUiEvent
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
        private val clipboardHelper: ClipboardHelper,
        private val appPreferences: AppPreferences,
        private val pendingHolder: PendingConversionHolder,
    ) : ViewModel() {
        val allHistory: StateFlow<List<ConversionEntity>> =
            historyRepository
                .getRecentHistory(200)
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val settings: StateFlow<AppSettings> =
            appPreferences.settings
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        val filteredHistory: StateFlow<List<ConversionEntity>> =
            _searchQuery
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        historyRepository.getRecentHistory(200)
                    } else {
                        historyRepository.searchHistory(query)
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        val starredHistory: StateFlow<List<ConversionEntity>> =
            historyRepository
                .getStarred()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        private val _events = MutableSharedFlow<HistoryUiEvent>(extraBufferCapacity = 4)
        val events: SharedFlow<HistoryUiEvent> = _events.asSharedFlow()
        private var lastDeleted: ConversionEntity? = null

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

        fun onSearchQueryChange(query: String) {
            _searchQuery.value = query
        }

        fun deleteHistoryItem(entity: ConversionEntity) {
            lastDeleted = entity
            viewModelScope.launch {
                historyRepository.delete(entity.id)
                _events.emit(HistoryUiEvent.ItemDeleted(entity))
            }
        }

        fun undoDelete() {
            val entity = lastDeleted ?: return
            lastDeleted = null
            viewModelScope.launch {
                historyRepository.insert(entity.copy(id = 0))
            }
        }

        fun deleteAll() {
            viewModelScope.launch {
                historyRepository.deleteAll()
            }
        }

        fun loadHistoryToResult(entity: ConversionEntity) {
            pendingHolder.sourceText = entity.sourceText
            pendingHolder.result =
                ConversionResult(
                    resultText = entity.resultText,
                    direction = ConversionDirection.valueOf(entity.direction),
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
