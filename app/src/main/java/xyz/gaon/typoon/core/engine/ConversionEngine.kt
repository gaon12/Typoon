package xyz.gaon.typoon.core.engine

import java.text.Normalizer
import java.util.concurrent.atomic.AtomicReference

data class ConversionResult(
    val resultText: String,
    val direction: ConversionDirection,
    val confidence: Float,
)

enum class ConversionDirection {
    ENG_TO_KOR,
    KOR_TO_ENG,
    UNKNOWN,
    ;

    companion object {
        fun fromPersisted(value: String?): ConversionDirection =
            value?.trim()?.takeIf { it.isNotEmpty() }?.let { directionName ->
                runCatching { valueOf(directionName) }.getOrElse { UNKNOWN }
            } ?: UNKNOWN
    }
}

private data class ConversionRuntimeConfig(
    val exceptions: Set<String> = emptySet(),
    val feedbackPenalty: Float = 0f,
)

class ConversionEngine {
    private val runtimeConfig = AtomicReference(ConversionRuntimeConfig())

    private enum class SegmentScript {
        ENG,
        KOR,
        OTHER,
    }

    private data class TextSegment(
        val text: String,
        val script: SegmentScript,
    )

    fun setExceptions(words: Set<String>) {
        val normalizedWords = words.map { normalizeForProcessing(it).trim() }.toSet()
        runtimeConfig.updateAndGet { config -> config.copy(exceptions = normalizedWords) }
    }

    fun setFeedbackAdjustment(negativeFeedbackRate: Float) {
        val penalty = (negativeFeedbackRate * 0.3f).coerceIn(0f, 0.3f)
        runtimeConfig.updateAndGet { config -> config.copy(feedbackPenalty = penalty) }
    }

    fun convert(input: String): ConversionResult {
        val config = runtimeConfig.get()
        val normalizedInput = normalizeForProcessing(input)
        if (normalizedInput.isBlank()) {
            return ConversionResult(normalizedInput, ConversionDirection.UNKNOWN, 0.0f)
        }

        val baseResult =
            if (normalizedInput.hasMixedScripts()) {
                convertMixedInput(normalizedInput, config)
            } else {
                convertForced(normalizedInput, inferDirection(normalizedInput), config)
            }

        return boostConfidenceIfCommonWords(baseResult)
    }

    fun convertForced(
        input: String,
        direction: ConversionDirection,
    ): ConversionResult = convertForced(input, direction, runtimeConfig.get())

    fun resolveReverseDirection(
        sourceText: String,
        currentDirection: ConversionDirection,
    ): ConversionDirection =
        when (currentDirection) {
            ConversionDirection.ENG_TO_KOR -> ConversionDirection.KOR_TO_ENG
            ConversionDirection.KOR_TO_ENG -> ConversionDirection.ENG_TO_KOR
            ConversionDirection.UNKNOWN -> inferReverseDirectionForUnknown(sourceText)
        }

    private fun boostConfidenceIfCommonWords(result: ConversionResult): ConversionResult {
        val words = result.resultText.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isEmpty()) return result

        val commonSet =
            if (result.direction == ConversionDirection.ENG_TO_KOR) {
                ConversionConstants.COMMON_KOR_WORDS
            } else {
                ConversionConstants.COMMON_ENG_WORDS
            }
        val matchRatio = words.count { it in commonSet }.toFloat() / words.size
        return if (matchRatio >= 0.5f) {
            result.copy(confidence = (result.confidence + 0.1f).coerceAtMost(1.0f))
        } else {
            result
        }
    }

    private fun convertMixedInput(
        input: String,
        config: ConversionRuntimeConfig,
    ): ConversionResult {
        val segments = splitIntoScriptSegments(input)
        val output = StringBuilder()
        var engToKorCount = 0
        var korToEngCount = 0

        segments.forEach { segment ->
            when (segment.script) {
                SegmentScript.ENG -> {
                    val converted = convertEnglishSegmentSmart(segment.text, config.exceptions)
                    if (converted != segment.text) {
                        engToKorCount++
                    }
                    output.append(converted)
                }

                SegmentScript.KOR -> {
                    val converted = convertKoreanSegmentSmart(segment.text, config.exceptions)
                    if (converted != segment.text) {
                        korToEngCount++
                    }
                    output.append(converted)
                }

                SegmentScript.OTHER -> output.append(segment.text)
            }
        }

        val direction = determineMixedDirection(engToKorCount, korToEngCount)
        val confidence = determineMixedConfidence(direction, engToKorCount, korToEngCount)
        return ConversionResult(
            resultText = output.toString(),
            direction = direction,
            confidence = (confidence - config.feedbackPenalty * 0.5f).coerceIn(0.1f, 1.0f),
        )
    }

    private fun determineMixedDirection(
        engToKorCount: Int,
        korToEngCount: Int,
    ): ConversionDirection =
        when {
            engToKorCount > 0 && korToEngCount == 0 -> ConversionDirection.ENG_TO_KOR
            korToEngCount > 0 && engToKorCount == 0 -> ConversionDirection.KOR_TO_ENG
            engToKorCount > korToEngCount -> ConversionDirection.ENG_TO_KOR
            korToEngCount > engToKorCount -> ConversionDirection.KOR_TO_ENG
            else -> ConversionDirection.UNKNOWN
        }

    private fun determineMixedConfidence(
        direction: ConversionDirection,
        engToKorCount: Int,
        korToEngCount: Int,
    ): Float =
        when {
            (engToKorCount + korToEngCount) == 0 -> 0.25f
            direction == ConversionDirection.UNKNOWN -> 0.58f
            else -> 0.74f
        }

    private fun convertForced(
        input: String,
        direction: ConversionDirection,
        config: ConversionRuntimeConfig,
    ): ConversionResult {
        val normalizedInput = normalizeForProcessing(input)
        if (direction == ConversionDirection.UNKNOWN) {
            return ConversionResult(normalizedInput, direction, 0.0f)
        }

        val confidence = LanguageScorer.calculateConfidence(normalizedInput, direction, config.feedbackPenalty)
        val sb = StringBuilder()
        val currentToken = StringBuilder()

        for (char in normalizedInput) {
            if (char.isLetter()) {
                currentToken.append(char)
            } else {
                flushCurrentToken(currentToken, sb, direction, config.exceptions)
                sb.append(char)
            }
        }
        flushCurrentToken(currentToken, sb, direction, config.exceptions)

        return ConversionResult(sb.toString(), direction, confidence)
    }

    private fun flushCurrentToken(
        currentToken: StringBuilder,
        output: StringBuilder,
        direction: ConversionDirection,
        exceptions: Set<String>,
    ) {
        if (currentToken.isEmpty()) return
        output.append(processToken(currentToken.toString(), direction, exceptions))
        currentToken.clear()
    }

    private fun processToken(
        token: String,
        direction: ConversionDirection,
        exceptions: Set<String>,
    ): String =
        if (token in exceptions) {
            token
        } else if (direction == ConversionDirection.ENG_TO_KOR) {
            translateEngToKor(token)
        } else {
            translateKorToEng(token)
        }

    private fun inferDirection(input: String): ConversionDirection {
        val engCount = input.count { it.isEnglishLetter() }
        val korCount = input.count { it.isHangul() }
        val totalLetters = engCount + korCount
        if (totalLetters == 0) return ConversionDirection.UNKNOWN

        val engRatio = engCount.toFloat() / totalLetters
        val korRatio = korCount.toFloat() / totalLetters
        return if (LanguageScorer.looksLikeUrlOrEmail(input)) {
            when {
                engRatio > 0.9f -> ConversionDirection.UNKNOWN
                engCount > korCount -> ConversionDirection.ENG_TO_KOR
                else -> ConversionDirection.KOR_TO_ENG
            }
        } else {
            when {
                engRatio > 0.8f -> ConversionDirection.ENG_TO_KOR
                korRatio > 0.8f -> ConversionDirection.KOR_TO_ENG
                engCount > korCount -> ConversionDirection.ENG_TO_KOR
                korCount > engCount -> ConversionDirection.KOR_TO_ENG
                else -> ConversionDirection.UNKNOWN
            }
        }
    }

    private fun inferReverseDirectionForUnknown(input: String): ConversionDirection {
        val normalizedInput = normalizeForProcessing(input)
        val engCount = normalizedInput.count { it.isEnglishLetter() }
        val korCount = normalizedInput.count { it.isHangul() }

        return when {
            korCount > engCount -> ConversionDirection.KOR_TO_ENG
            engCount > korCount -> ConversionDirection.ENG_TO_KOR
            korCount > 0 -> ConversionDirection.KOR_TO_ENG
            else -> ConversionDirection.ENG_TO_KOR
        }
    }

    private fun splitIntoScriptSegments(input: String): List<TextSegment> {
        if (input.isEmpty()) return emptyList()

        val segments = mutableListOf<TextSegment>()
        val buffer = StringBuilder()
        var currentScript = charScript(input.first())

        input.forEach { char ->
            val nextScript = charScript(char)
            if (nextScript == currentScript) {
                buffer.append(char)
            } else {
                segments.add(TextSegment(buffer.toString(), currentScript))
                buffer.clear()
                buffer.append(char)
                currentScript = nextScript
            }
        }

        if (buffer.isNotEmpty()) {
            segments.add(TextSegment(buffer.toString(), currentScript))
        }

        return segments
    }

    private fun charScript(char: Char): SegmentScript =
        when {
            char.isEnglishLetter() -> SegmentScript.ENG
            char.isHangul() -> SegmentScript.KOR
            else -> SegmentScript.OTHER
        }

    private fun convertEnglishSegmentSmart(
        segment: String,
        exceptions: Set<String>,
    ): String {
        if (segment in exceptions || shouldPreserveEnglishSegment(segment)) return segment

        val converted = translateEngToKor(segment)
        val originalScore = LanguageScorer.englishTokenScore(segment)
        val convertedScore = LanguageScorer.koreanTokenScore(converted)
        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun convertKoreanSegmentSmart(
        segment: String,
        exceptions: Set<String>,
    ): String {
        if (segment in exceptions || shouldPreserveKoreanSegment(segment)) return segment

        val converted = translateKorToEng(segment)
        val originalScore = LanguageScorer.koreanTokenScore(segment)
        val convertedScore = LanguageScorer.englishTokenScore(converted)
        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun translateEngToKor(input: String): String {
        val sb = StringBuilder()
        val jamos = mutableListOf<String>()

        for (char in input) {
            val mapped = KeyboardMapper.mapEngToKor(char)
            if (mapped != null) {
                jamos.add(mapped)
            } else {
                if (jamos.isNotEmpty()) {
                    sb.append(HangulComposer.compose(jamos))
                    jamos.clear()
                }
                sb.append(char)
            }
        }

        if (jamos.isNotEmpty()) {
            sb.append(HangulComposer.compose(jamos))
        }

        return sb.toString()
    }

    private fun translateKorToEng(input: String): String {
        val sb = StringBuilder()
        for (char in input) {
            if (char.isHangulSyllable()) {
                val jamos = HangulComposer.getJamosFromSyllable(char)
                for (jamo in jamos) {
                    val decomposed = HangulComposer.decomposeJamo(jamo)
                    for (decomposedChar in decomposed) {
                        sb.append(KeyboardMapper.mapKorToEng(decomposedChar) ?: decomposedChar)
                    }
                }
            } else if (char.isHangulJamo()) {
                val decomposed = HangulComposer.decomposeJamo(char.toString())
                for (decomposedChar in decomposed) {
                    sb.append(KeyboardMapper.mapKorToEng(decomposedChar) ?: decomposedChar)
                }
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }
}

private fun shouldPreserveEnglishSegment(segment: String): Boolean {
    val lower = segment.lowercase()
    val hasNoEnglishLetters = segment.none { it.isEnglishLetter() }
    val isKnownEnglishWord = lower in ConversionConstants.COMMON_ENG_WORDS || lower in setOf("a", "i")
    val isUppercaseShortcut =
        (segment.length <= 2 && segment.any { it.isUpperCase() }) ||
            (segment.length <= 4 && segment.all { it.isUpperCase() })
    return (
        hasNoEnglishLetters ||
            LanguageScorer.looksLikeUrlOrEmail(segment) ||
            isKnownEnglishWord ||
            isUppercaseShortcut
    )
}

private fun shouldPreserveKoreanSegment(segment: String): Boolean {
    val syllableCount = segment.count { it.isHangulSyllable() }
    val jamoCount = segment.count { it.isHangulJamo() }
    return segment in ConversionConstants.COMMON_KOR_WORDS || (syllableCount == 1 && jamoCount == 0)
}

private fun normalizeForProcessing(input: String): String = Normalizer.normalize(input, Normalizer.Form.NFC)

private fun String.hasMixedScripts(): Boolean {
    val hasEng = any { it.isEnglishLetter() }
    val hasKor = any { it.isHangul() }
    return hasEng && hasKor
}

private fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'

private fun Char.isHangulSyllable(): Boolean = this.code in 0xAC00..0xD7AF

private fun Char.isHangulJamo(): Boolean =
    this.code in 0x3131..0x318E ||
        this.code in 0x1100..0x11FF ||
        this.code in 0xA960..0xA97F ||
        this.code in 0xD7B0..0xD7FF

private fun Char.isHangul(): Boolean = isHangulSyllable() || isHangulJamo()
