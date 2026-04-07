package xyz.gaon.typoon.core.data.repository

internal object HistorySearchPolicy {
    private val ftsTokenRegex = Regex("[0-9A-Za-z가-힣ㄱ-ㅎㅏ-ㅣ]+")
    private val choseongOnlyRegex = Regex("^[ㄱ-ㅎ\\s]+$")
    private val choseongChars =
        charArrayOf(
            'ㄱ',
            'ㄲ',
            'ㄴ',
            'ㄷ',
            'ㄸ',
            'ㄹ',
            'ㅁ',
            'ㅂ',
            'ㅃ',
            'ㅅ',
            'ㅆ',
            'ㅇ',
            'ㅈ',
            'ㅉ',
            'ㅊ',
            'ㅋ',
            'ㅌ',
            'ㅍ',
            'ㅎ',
        )

    fun normalizeUserQuery(raw: String): String =
        raw
            .replace(Regex("[\\p{Cntrl}]"), " ")
            .trim()
            .replace(Regex("\\s+"), " ")

    fun buildSafeFtsPrefixQuery(normalized: String): String? {
        val tokens = ftsTokenRegex.findAll(normalized).map { it.value }.filter { it.isNotBlank() }.toList()
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" AND ") { token -> "\"${token.replace("\"", "")}\"*" }
    }

    fun escapeLikeQuery(normalized: String): String =
        normalized
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    fun isChoseongOnlyQuery(normalized: String): Boolean =
        choseongOnlyRegex.matches(normalized) && normalized.any { it in 'ㄱ'..'ㅎ' }

    fun matchesChoseong(
        query: String,
        sourceText: String,
        resultText: String,
    ): Boolean {
        val compactQuery = query.replace(" ", "")
        if (compactQuery.isBlank()) return false
        val sourceInitials = toInitialConsonants(sourceText)
        val resultInitials = toInitialConsonants(resultText)
        return sourceInitials.contains(compactQuery) || resultInitials.contains(compactQuery)
    }

    private fun toInitialConsonants(text: String): String =
        buildString(text.length) {
            text.forEach { char ->
                when {
                    char in '\uAC00'..'\uD7A3' -> {
                        val index = (char.code - 0xAC00) / 588
                        append(choseongChars[index])
                    }
                    char in 'ㄱ'..'ㅎ' -> append(char)
                }
            }
        }
}
