package xyz.gaon.typoon.core.engine

object HangulComposer {
    private val CHOSUNG = listOf(
        "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    )
    private val JUNGSUNG = listOf(
        "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    )
    private val JONGSUNG = listOf(
        "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    )

    private val CHOSUNG_MAP = CHOSUNG.withIndex().associate { it.value to it.index }
    private val JUNGSUNG_MAP = JUNGSUNG.withIndex().associate { it.value to it.index }
    private val JONGSUNG_MAP = JONGSUNG.withIndex().associate { it.value to it.index }

    private val COMPLEX_VOWELS = mapOf(
        "ㅗㅏ" to "ㅘ", "ㅗㅐ" to "ㅙ", "ㅗㅣ" to "ㅚ", "ㅜㅓ" to "ㅝ", "ㅜㅔ" to "ㅞ", "ㅜㅣ" to "ㅟ", "ㅡㅣ" to "ㅢ"
    )
    private val COMPLEX_CONSONANTS = mapOf(
        "ㄱㅅ" to "ㄳ", "ㄴㅈ" to "ㄵ", "ㄴㅎ" to "ㄶ", "ㄹㄱ" to "ㄺ", "ㄹㅁ" to "ㄻ", "ㄹㅂ" to "ㄼ", "ㄹㅅ" to "ㄽ", "ㄹㅌ" to "ㄾ", "ㄹㅍ" to "ㄿ", "ㄹㅎ" to "ㅀ", "ㅂㅅ" to "ㅄ"
    )

    private val REVERSE_COMPLEX_VOWELS = COMPLEX_VOWELS.entries.associate { it.value to it.key }
    private val REVERSE_COMPLEX_CONSONANTS = COMPLEX_CONSONANTS.entries.associate { it.value to it.key }

    fun decomposeJamo(jamo: String): String {
        REVERSE_COMPLEX_VOWELS[jamo]?.let { return it }
        REVERSE_COMPLEX_CONSONANTS[jamo]?.let { return it }
        return jamo
    }

    fun isChosung(jamo: String): Boolean = CHOSUNG_MAP.containsKey(jamo)
    fun isJungsung(jamo: String): Boolean = JUNGSUNG_MAP.containsKey(jamo)

    fun compose(jamos: List<String>): String {
        if (jamos.isEmpty()) return ""
        val result = StringBuilder()
        var i = 0

        while (i < jamos.size) {
            val cIdx = CHOSUNG_MAP[jamos[i]] ?: -1
            if (cIdx == -1) {
                result.append(jamos[i])
                i++
                continue
            }

            if (i + 1 >= jamos.size) {
                result.append(jamos[i])
                i++
                continue
            }

            var vIdx = JUNGSUNG_MAP[jamos[i + 1]] ?: -1
            var vSize = 1

            if (i + 2 < jamos.size) {
                val combinedV = COMPLEX_VOWELS[jamos[i + 1] + jamos[i + 2]]
                if (combinedV != null) {
                    val nextVIdx = JUNGSUNG_MAP[combinedV] ?: -1
                    if (nextVIdx != -1) {
                        vIdx = nextVIdx
                        vSize = 2
                    }
                }
            }

            if (vIdx == -1) {
                result.append(jamos[i])
                i++
                continue
            }

            var tIdx = 0
            var tSize = 0

            if (i + vSize + 1 < jamos.size) {
                val nextChar = jamos[i + vSize + 1]
                val isNextNextVowel = if (i + vSize + 2 < jamos.size) {
                    val nextNextChar = jamos[i + vSize + 2]
                    // Check if it's a direct vowel or start of a complex vowel
                    isJungsung(nextNextChar)
                } else {
                    false
                }

                if (CHOSUNG_MAP.containsKey(nextChar) && !isNextNextVowel) {
                    if (i + vSize + 2 < jamos.size) {
                        val nextNextChar = jamos[i + vSize + 2]
                        val isNextNextNextVowel = if (i + vSize + 3 < jamos.size) {
                            isJungsung(jamos[i + vSize + 3])
                        } else {
                            false
                        }

                        val combinedC = COMPLEX_CONSONANTS[nextChar + nextNextChar]
                        if (combinedC != null && !isNextNextNextVowel) {
                            tIdx = JONGSUNG_MAP[combinedC] ?: 0
                            tSize = 2
                        }
                    }

                    if (tIdx <= 0) {
                        tIdx = JONGSUNG_MAP[nextChar] ?: 0
                        if (tIdx != 0) {
                            tSize = 1
                        }
                    }
                }
            }

            val combinedCode = 0xAC00 + (cIdx * 21 * 28) + (vIdx * 28) + tIdx
            result.append(combinedCode.toChar())
            i += 1 + vSize + tSize
        }

        return result.toString()
    }

    fun getJamosFromSyllable(char: Char): List<String> {
        val code = char.code
        if (code !in 0xAC00..0xD7A3) return listOf(char.toString())

        val base = code - 0xAC00
        val chosungIdx = base / (21 * 28)
        val jungsungIdx = (base % (21 * 28)) / 28
        val jongsungIdx = base % 28

        val result = mutableListOf<String>()
        result.add(CHOSUNG[chosungIdx])
        result.add(JUNGSUNG[jungsungIdx])
        if (jongsungIdx > 0) {
            result.add(JONGSUNG[jongsungIdx])
        }
        return result
    }
}
