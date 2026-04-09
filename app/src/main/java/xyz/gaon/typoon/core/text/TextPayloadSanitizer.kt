package xyz.gaon.typoon.core.text

import java.text.Normalizer

object TextPayloadSanitizer {
    const val MAX_TEXT_LENGTH = 5000
    private val disallowedControlChars = Regex("[\\p{Cntrl}&&[^\\n\\t]]")
    private val zeroWidthChars = Regex("[\\u200B-\\u200D\\u2060\\uFEFF]")

    fun sanitize(input: CharSequence?): String {
        val normalized =
            input
                ?.toString()
                ?.replace("\r\n", "\n")
                ?.replace('\r', '\n')
                ?.replace("\u0000", "")
                ?.replace(disallowedControlChars, " ")
                ?.replace(zeroWidthChars, "")
                ?.let { Normalizer.normalize(it, Normalizer.Form.NFC) }
                ?.take(MAX_TEXT_LENGTH)
                .orEmpty()

        return normalized
    }
}
