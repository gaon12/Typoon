package xyz.gaon.typoon.core.engine

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

class ConversionEngine {
    private var exceptions: Set<String> = emptySet()
    private var feedbackPenalty: Float = 0f

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
        exceptions = words.map { it.trim() }.toSet()
    }

    fun setFeedbackAdjustment(negativeFeedbackRate: Float) {
        feedbackPenalty = (negativeFeedbackRate * 0.3f).coerceIn(0f, 0.3f)
    }

    fun convert(input: String): ConversionResult {
        if (input.isBlank()) return ConversionResult(input, ConversionDirection.UNKNOWN, 0.0f)

        val hasEng = input.any { it.isEnglishLetter() }
        val hasKor = input.any { it.isHangul() }
        if (hasEng && hasKor) {
            return convertMixedInput(input)
        }

        val direction = inferDirection(input)
        val result = convertForced(input, direction)

        val words = result.resultText.split(Regex("\\s+")).filter { it.length >= 2 }
        if (words.isNotEmpty()) {
            val commonSet = if (direction == ConversionDirection.ENG_TO_KOR) {
                ConversionConstants.COMMON_KOR_WORDS
            } else {
                ConversionConstants.COMMON_ENG_WORDS
            }
            val matchCount = words.count { it in commonSet }
            if (matchCount.toFloat() / words.size >= 0.5f) {
                return result.copy(confidence = (result.confidence + 0.1f).coerceAtMost(1.0f))
            }
        }

        return result
    }

    private fun convertMixedInput(input: String): ConversionResult {
        val segments = splitIntoScriptSegments(input)
        val output = StringBuilder()
        var engToKorCount = 0
        var korToEngCount = 0

        segments.forEach { segment ->
            when (segment.script) {
                SegmentScript.ENG -> {
                    val converted = convertEnglishSegmentSmart(segment.text)
                    if (converted != segment.text) {
                        engToKorCount++
                    }
                    output.append(converted)
                }

                SegmentScript.KOR -> {
                    val converted = convertKoreanSegmentSmart(segment.text)
                    if (converted != segment.text) {
                        korToEngCount++
                    }
                    output.append(converted)
                }

                SegmentScript.OTHER -> output.append(segment.text)
            }
        }

        val direction = when {
            engToKorCount > 0 && korToEngCount == 0 -> ConversionDirection.ENG_TO_KOR
            korToEngCount > 0 && engToKorCount == 0 -> ConversionDirection.KOR_TO_ENG
            engToKorCount > korToEngCount -> ConversionDirection.ENG_TO_KOR
            korToEngCount > engToKorCount -> ConversionDirection.KOR_TO_ENG
            else -> ConversionDirection.UNKNOWN
        }
        val confidence = when {
            (engToKorCount + korToEngCount) == 0 -> 0.25f
            direction == ConversionDirection.UNKNOWN -> 0.58f
            else -> 0.74f
        }

        return ConversionResult(
            resultText = output.toString(),
            direction = direction,
            confidence = (confidence - feedbackPenalty * 0.5f).coerceIn(0.1f, 1.0f),
        )
    }

    fun convertForced(input: String, direction: ConversionDirection): ConversionResult {
        if (direction == ConversionDirection.UNKNOWN) return ConversionResult(input, direction, 0.0f)

        val confidence = LanguageScorer.calculateConfidence(input, direction, feedbackPenalty)
        val sb = StringBuilder()
        val currentToken = StringBuilder()

        for (char in input) {
            if (char.isLetter()) {
                currentToken.append(char)
            } else {
                if (currentToken.isNotEmpty()) {
                    sb.append(processToken(currentToken.toString(), direction))
                    currentToken.clear()
                }
                sb.append(char)
            }
        }

        if (currentToken.isNotEmpty()) {
            sb.append(processToken(currentToken.toString(), direction))
        }

        return ConversionResult(sb.toString(), direction, confidence)
    }

    private fun processToken(token: String, direction: ConversionDirection): String {
        if (token in exceptions) return token
        return if (direction == ConversionDirection.ENG_TO_KOR) {
            translateEngToKor(token)
        } else {
            translateKorToEng(token)
        }
    }

    private fun inferDirection(input: String): ConversionDirection {
        val engCount = input.count { it.isEnglishLetter() }
        val korCount = input.count { it.isHangul() }

        val totalLetters = engCount + korCount
        if (totalLetters == 0) return ConversionDirection.UNKNOWN

        val engRatio = engCount.toFloat() / totalLetters
        val korRatio = korCount.toFloat() / totalLetters

        if (LanguageScorer.looksLikeUrlOrEmail(input)) {
            return when {
                engRatio > 0.9f -> ConversionDirection.UNKNOWN
                engCount > korCount -> ConversionDirection.ENG_TO_KOR
                else -> ConversionDirection.KOR_TO_ENG
            }
        }

        return when {
            engRatio > 0.8f -> ConversionDirection.ENG_TO_KOR
            korRatio > 0.8f -> ConversionDirection.KOR_TO_ENG
            engCount > korCount -> ConversionDirection.ENG_TO_KOR
            korCount > engCount -> ConversionDirection.KOR_TO_ENG
            else -> ConversionDirection.UNKNOWN
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

    private fun charScript(char: Char): SegmentScript = when {
        char.isEnglishLetter() -> SegmentScript.ENG
        char.isHangul() -> SegmentScript.KOR
        else -> SegmentScript.OTHER
    }

    private fun convertEnglishSegmentSmart(segment: String): String {
        if (segment in exceptions || shouldPreserveEnglishSegment(segment)) return segment

        val converted = translateEngToKor(segment)
        val originalScore = LanguageScorer.englishTokenScore(segment)
        val convertedScore = LanguageScorer.koreanTokenScore(converted)

        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun convertKoreanSegmentSmart(segment: String): String {
        if (segment in exceptions || shouldPreserveKoreanSegment(segment)) return segment

        val converted = translateKorToEng(segment)
        val originalScore = LanguageScorer.koreanTokenScore(segment)
        val convertedScore = LanguageScorer.englishTokenScore(converted)

        return if (convertedScore >= originalScore + 0.08f) converted else segment
    }

    private fun shouldPreserveEnglishSegment(segment: String): Boolean {
        val lower = segment.lowercase()
        val englishLetterCount = segment.count { it.isEnglishLetter() }
        if (englishLetterCount == 0) return true
        if (LanguageScorer.looksLikeUrlOrEmail(segment)) return true
        if (lower in ConversionConstants.COMMON_ENG_WORDS) return true
        if (lower in setOf("a", "i")) return true
        if (segment.length <= 2 && segment.any { it.isUpperCase() }) return true
        if (segment.length <= 4 && segment.all { it.isUpperCase() }) return true
        return false
    }

    private fun shouldPreserveKoreanSegment(segment: String): Boolean {
        if (segment in ConversionConstants.COMMON_KOR_WORDS) return true
        val syllableCount = segment.count { it.isHangulSyllable() }
        val jamoCount = segment.count { it.isHangulJamo() }
        return syllableCount == 1 && jamoCount == 0
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
                    for (d in decomposed) {
                        sb.append(KeyboardMapper.mapKorToEng(d) ?: d)
                    }
                }
            } else if (char.isHangulJamo()) {
                val decomposed = HangulComposer.decomposeJamo(char.toString())
                for (d in decomposed) {
                    sb.append(KeyboardMapper.mapKorToEng(d) ?: d)
                }
            } else {
                sb.append(char)
            }
        }
        return sb.toString()
    }

    private fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    private fun Char.isHangulSyllable(): Boolean = this.code in 0xAC00..0xD7AF
    private fun Char.isHangulJamo(): Boolean = this.code in 0x3131..0x318E
    private fun Char.isHangul(): Boolean = isHangulSyllable() || isHangulJamo()
}
