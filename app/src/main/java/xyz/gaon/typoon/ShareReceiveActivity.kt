package xyz.gaon.typoon

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.di.PendingConversionHolder
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.text.TextPayloadSanitizer
import xyz.gaon.typoon.feature.share.ShareResultSheet
import xyz.gaon.typoon.ui.theme.TypoonTheme
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiveActivity : AppCompatActivity() {
    @Inject lateinit var conversionEngine: ConversionEngine

    @Inject lateinit var pendingHolder: PendingConversionHolder

    @Inject lateinit var historyRepository: HistoryRepository

    @Inject lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action != Intent.ACTION_SEND || intent?.type != "text/plain") {
            finish()
            return
        }

        val sharedText = TextPayloadSanitizer.sanitize(intent?.getStringExtra(Intent.EXTRA_TEXT))
        if (sharedText.isNullOrBlank()) {
            finish()
            return
        }

        // 변환 즉시 수행 후 PendingHolder 세팅
        val result = conversionEngine.convert(sharedText)
        pendingHolder.sourceText = sharedText
        pendingHolder.result = result
        pendingHolder.isFromShare = true
        pendingHolder.entryPoint = "SHARE"
        pendingHolder.processTextReadOnly = false
        pendingHolder.historyId = 0L
        pendingHolder.isStarred = false
        pendingHolder.shouldCheckReviewPrompt = true

        // 기록 저장 (백그라운드)
        lifecycleScope.launch {
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
        }

        // BottomSheet 팝업으로 표시 — enableEdgeToEdge 미적용 (투명 배경 유지)
        setContent {
            TypoonTheme {
                ShareResultSheet(
                    onDismiss = { finish() },
                )
            }
        }
    }
}
