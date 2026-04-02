package xyz.gaon.typoon.core.di

import xyz.gaon.typoon.core.engine.ConversionResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 화면 간 변환 결과를 전달하기 위한 단순 홀더.
 * NavArgs로 긴 텍스트를 전달하면 URL 길이 제한이 있으므로 Singleton으로 관리.
 */
@Singleton
class PendingConversionHolder
    @Inject
    constructor() {
        var sourceText: String = ""
        var result: ConversionResult? = null
        var isFromShare: Boolean = false
        var entryPoint: String = "HOME"
        var processTextReadOnly: Boolean = false
        var historyId: Long = 0L
        var isStarred: Boolean = false
        var shouldCheckReviewPrompt: Boolean = false
    }
