package xyz.gaon.typoon

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.ConversionDispatcher
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.text.TextPayloadSanitizer
import xyz.gaon.typoon.feature.share.ShareResultSheet
import xyz.gaon.typoon.ui.components.ConversionLoadingContent
import xyz.gaon.typoon.ui.system.configureTypoonEdgeToEdge
import xyz.gaon.typoon.ui.theme.TypoonTheme
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiveActivity : AppCompatActivity() {
    @Inject lateinit var conversionEngine: ConversionEngine

    @Inject lateinit var pendingHolder: PendingConversionHolder

    @Inject lateinit var historyRepository: HistoryRepository

    @Inject lateinit var appPreferences: AppPreferences

    @Inject @ConversionDispatcher
    lateinit var conversionDispatcher: CoroutineDispatcher

    private val isProcessing = mutableStateOf(true)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureTypoonEdgeToEdge()

        if (intent?.action != Intent.ACTION_SEND || intent?.type != "text/plain") {
            finish()
            return
        }

        val sharedText = TextPayloadSanitizer.sanitize(intent?.getStringExtra(Intent.EXTRA_TEXT))
        if (sharedText.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            TypoonTheme {
                if (isProcessing.value) {
                    ConversionLoadingContent()
                } else {
                    // 투명 윈도우 위에 BottomSheet를 띄우되 시스템 바 인셋은 Compose가 처리한다.
                    ShareResultSheet(
                        onDismiss = { finish() },
                    )
                }
            }
        }

        lifecycleScope.launch {
            runCatching {
                val result =
                    withContext(conversionDispatcher) {
                        conversionEngine.convert(sharedText)
                    }

                pendingHolder.sourceText = sharedText
                pendingHolder.result = result
                pendingHolder.isFromShare = true
                pendingHolder.entryPoint = "SHARE"
                pendingHolder.processTextReadOnly = false
                pendingHolder.historyId = 0L
                pendingHolder.isStarred = false
                pendingHolder.shouldCheckReviewPrompt = true

                val settings = appPreferences.settings.first()
                if (settings.saveHistory) {
                    pendingHolder.historyId =
                        historyRepository.insert(
                            ConversionEntity(
                                sourceText = sharedText,
                                resultText = result.resultText,
                                direction = result.direction.name,
                                confidence = result.confidence,
                                createdAt = System.currentTimeMillis(),
                                entryPoint = pendingHolder.entryPoint,
                            ),
                        )
                }
            }.onSuccess {
                isProcessing.value = false
            }.onFailure {
                finish()
            }
        }
    }
}
