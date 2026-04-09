package xyz.gaon.typoon

import android.app.Activity
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
import xyz.gaon.typoon.feature.processtext.ProcessTextSheet
import xyz.gaon.typoon.ui.components.ConversionLoadingContent
import xyz.gaon.typoon.ui.system.configureTypoonEdgeToEdge
import xyz.gaon.typoon.ui.theme.TypoonTheme
import javax.inject.Inject

@AndroidEntryPoint
class ProcessTextActivity : AppCompatActivity() {
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

        if (intent.action != Intent.ACTION_PROCESS_TEXT || intent.type != "text/plain") {
            finish()
            return
        }

        val selectedText = TextPayloadSanitizer.sanitize(intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT))
        val isReadOnly = intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, false)

        if (selectedText.isNullOrBlank()) {
            finish()
            return
        }

        setContent {
            TypoonTheme {
                if (isProcessing.value) {
                    ConversionLoadingContent()
                } else {
                    ProcessTextSheet(
                        onDismiss = { finish() },
                        onReplace =
                            if (isReadOnly) {
                                null
                            } else {
                                { text ->
                                    val resultIntent =
                                        Intent().apply {
                                            putExtra(Intent.EXTRA_PROCESS_TEXT, text)
                                        }
                                    setResult(Activity.RESULT_OK, resultIntent)
                                    finish()
                                }
                            },
                    )
                }
            }
        }

        lifecycleScope.launch {
            runCatching {
                val result =
                    withContext(conversionDispatcher) {
                        conversionEngine.convert(selectedText)
                    }
                pendingHolder.sourceText = selectedText
                pendingHolder.result = result
                pendingHolder.isFromShare = false
                pendingHolder.entryPoint = "PROCESS_TEXT"
                pendingHolder.processTextReadOnly = isReadOnly
                pendingHolder.historyId = 0L
                pendingHolder.isStarred = false
                pendingHolder.shouldCheckReviewPrompt = true

                val settings = appPreferences.settings.first()
                if (settings.saveHistory) {
                    pendingHolder.historyId =
                        historyRepository.insert(
                            ConversionEntity(
                                sourceText = selectedText,
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
