package xyz.gaon.typoon.core.text

import java.text.Normalizer

object TextPayloadSanitizer {
    const val MAX_TEXT_LENGTH = 5000

    fun sanitize(input: CharSequence?): String {
        val normalized =
            input
                ?.toString()
                ?.replace("\u0000", "")
                ?.let { Normalizer.normalize(it, Normalizer.Form.NFC) }
                ?.take(MAX_TEXT_LENGTH)
                .orEmpty()

        return normalized
    }
}
