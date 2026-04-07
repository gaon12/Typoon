package xyz.gaon.typoon.core.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ConversionDirectionTest {
    @Test
    fun `fromPersisted returns enum for valid value`() {
        assertEquals(
            ConversionDirection.ENG_TO_KOR,
            ConversionDirection.fromPersisted("ENG_TO_KOR"),
        )
    }

    @Test
    fun `fromPersisted falls back to unknown for invalid value`() {
        assertEquals(
            ConversionDirection.UNKNOWN,
            ConversionDirection.fromPersisted("BROKEN_DIRECTION"),
        )
    }

    @Test
    fun `fromPersisted falls back to unknown for blank value`() {
        assertEquals(
            ConversionDirection.UNKNOWN,
            ConversionDirection.fromPersisted("  "),
        )
    }
}
