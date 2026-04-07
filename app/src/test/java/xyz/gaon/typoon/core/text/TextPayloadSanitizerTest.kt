package xyz.gaon.typoon.core.text

import org.junit.Assert.assertEquals
import org.junit.Test

class TextPayloadSanitizerTest {
    @Test
    fun sanitize_returnsEmptyForNullInput() {
        assertEquals("", TextPayloadSanitizer.sanitize(null))
    }

    @Test
    fun sanitize_removesNullCharsAndTruncates() {
        val input = "\u0000hello\u0000" + "x".repeat(TextPayloadSanitizer.MAX_TEXT_LENGTH)
        val sanitized = TextPayloadSanitizer.sanitize(input)

        assertEquals(TextPayloadSanitizer.MAX_TEXT_LENGTH, sanitized.length)
        assertEquals('h', sanitized.first())
    }
}
