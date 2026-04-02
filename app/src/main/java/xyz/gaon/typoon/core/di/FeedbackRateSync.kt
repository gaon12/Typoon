package xyz.gaon.typoon.core.di

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.engine.ConversionEngine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRateSync
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
        private val conversionEngine: ConversionEngine,
        @param:ApplicationScope private val scope: CoroutineScope,
    ) {
        init {
            scope.launch {
                historyRepository
                    .getRecentNegativeFeedbackRate()
                    .collect { rate ->
                        conversionEngine.setFeedbackAdjustment(rate)
                    }
            }
        }
    }
