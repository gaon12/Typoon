package xyz.gaon.typoon.core.engine

object KeyboardMapper {
    private val ENG_TO_KOR_MAP =
        mapOf(
            'q' to "ㅂ",
            'w' to "ㅈ",
            'e' to "ㄷ",
            'r' to "ㄱ",
            't' to "ㅅ",
            'y' to "ㅛ",
            'u' to "ㅕ",
            'i' to "ㅑ",
            'o' to "ㅐ",
            'p' to "ㅔ",
            'a' to "ㅁ",
            's' to "ㄴ",
            'd' to "ㅇ",
            'f' to "ㄹ",
            'g' to "ㅎ",
            'h' to "ㅗ",
            'j' to "ㅓ",
            'k' to "ㅏ",
            'l' to "ㅣ",
            'z' to "ㅋ",
            'x' to "ㅌ",
            'c' to "ㅊ",
            'v' to "ㅍ",
            'b' to "ㅠ",
            'n' to "ㅜ",
            'm' to "ㅡ",
            'Q' to "ㅃ",
            'W' to "ㅉ",
            'E' to "ㄸ",
            'R' to "ㄲ",
            'T' to "ㅆ",
            'O' to "ㅒ",
            'P' to "ㅖ",
        )

    private val KOR_TO_ENG_MAP = ENG_TO_KOR_MAP.entries.associate { it.value[0] to it.key.toString() }

    fun mapEngToKor(char: Char): String? = ENG_TO_KOR_MAP[char] ?: ENG_TO_KOR_MAP[char.lowercaseChar()]

    fun mapKorToEng(jamo: Char): String? = KOR_TO_ENG_MAP[jamo]
}
