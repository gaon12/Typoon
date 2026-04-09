package xyz.gaon.typoon.core.engine

object LanguageScorer {
    fun englishTokenScore(token: String): Float {
        if (token.isEmpty()) return 0f

        val lower = token.lowercase()
        if (lower in ConversionConstants.COMMON_ENG_WORDS) return 1.0f
        if (looksLikeUrlOrEmail(token)) return 1.0f

        val englishLetterCount = token.count { it.isEnglishLetter() }
        if (englishLetterCount == 0) return 0f

        val vowelRatio = token.count { it.lowercaseChar() in "aeiou" }.toFloat() / englishLetterCount
        var score = englishLetterCount.toFloat() / token.length * 0.25f
        if (vowelRatio in 0.25f..0.65f) {
            score += 0.18f
        } else if (vowelRatio == 0f && token.length > 2) {
            score -= 0.25f
        }
        if (token.length in 3..12) score += 0.08f
        score += englishPatternScore(lower)
        score -= englishClusterPenalty(lower)
        if (token.any { it.isUpperCase() }) score += 0.05f
        return score.coerceIn(0f, 0.95f)
    }

    fun koreanTokenScore(token: String): Float {
        if (token.isEmpty()) return 0f
        if (token in ConversionConstants.COMMON_KOR_WORDS) return 1.0f

        val syllableCount = token.count { it.isHangulSyllable() }
        val jamoCount = token.count { it.isHangulJamo() }
        val koreanCount = syllableCount + jamoCount
        if (koreanCount == 0) return 0f

        var score = koreanCount.toFloat() / token.length * 0.35f
        if (syllableCount > 0) score += 0.3f
        if (token.length in 2..12) score += 0.15f
        if (jamoCount > 0 && syllableCount == 0) score += 0.05f
        if (token.lastOrNull() in ConversionConstants.COMMON_KOR_ENDINGS) score += 0.08f
        return score.coerceAtMost(0.95f)
    }

    private fun englishPatternScore(lower: String): Float {
        if (lower.length < 2) return 0f
        val hits = ConversionConstants.COMMON_ENG_PATTERNS.count { pattern -> lower.contains(pattern) }
        return (hits * 0.06f).coerceAtMost(0.24f)
    }

    private fun englishClusterPenalty(lower: String): Float {
        var currentCluster = 0
        var maxCluster = 0

        lower.forEach { char ->
            if (char in 'a'..'z' && char !in "aeiou") {
                currentCluster += 1
                if (currentCluster > maxCluster) {
                    maxCluster = currentCluster
                }
            } else {
                currentCluster = 0
            }
        }

        return when {
            maxCluster >= 5 -> 0.28f
            maxCluster == 4 -> 0.18f
            maxCluster == 3 -> 0.08f
            else -> 0f
        }
    }

    fun looksLikeUrlOrEmail(input: String): Boolean {
        val lower = input.lowercase()
        return lower.contains("://") || lower.contains("www.") || lower.contains(".com") || lower.contains("@")
    }

    fun calculateConfidence(
        input: String,
        direction: ConversionDirection,
        feedbackPenalty: Float
    ): Float {
        val engCount = input.count { it.isEnglishLetter() }
        val korCount = input.count { it.isHangul() }
        val specialCount = input.count { !it.isLetterOrDigit() && !it.isWhitespace() }

        val totalLetters = engCount + korCount
        if (totalLetters == 0) return 0f

        val initialConfidence = when (direction) {
            ConversionDirection.ENG_TO_KOR -> engCount.toFloat() / input.length
            ConversionDirection.KOR_TO_ENG -> korCount.toFloat() / input.length
            else -> 0f
        }

        var penalty = 0f
        if (looksLikeUrlOrEmail(input)) penalty += 0.4f
        if (specialCount > 0) penalty += (specialCount.toFloat() / input.length) * 0.5f

        return (initialConfidence - penalty - feedbackPenalty).coerceIn(0.1f, 1.0f)
    }

    private fun Char.isEnglishLetter(): Boolean = this in 'a'..'z' || this in 'A'..'Z'
    private fun Char.isHangulSyllable(): Boolean = this.code in 0xAC00..0xD7AF
    private fun Char.isHangulJamo(): Boolean =
        this.code in 0x3131..0x318E ||
            this.code in 0x1100..0x11FF ||
            this.code in 0xA960..0xA97F ||
            this.code in 0xD7B0..0xD7FF
    private fun Char.isHangul(): Boolean = isHangulSyllable() || isHangulJamo()
}
