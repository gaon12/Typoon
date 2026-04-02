package xyz.gaon.typoon.core.text

object TextPayloadSanitizer {
    const val MAX_TEXT_LENGTH = 5000

    fun sanitize(input: CharSequence?): String {
        val normalized =
            input
                ?.toString()
                ?.replace("\u0000", "")
                ?.take(MAX_TEXT_LENGTH)
                .orEmpty()

        return normalized
    }
}
