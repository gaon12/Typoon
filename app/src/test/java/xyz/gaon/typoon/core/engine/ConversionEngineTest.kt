package xyz.gaon.typoon.core.engine

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ConversionEngineTest {
    private lateinit var engine: ConversionEngine

    @Before
    fun setup() {
        engine = ConversionEngine()
    }

    @Test
    fun `ENG_TO_KOR - Simple translation`() {
        val input = "dkssudgktpdy"
        val expected = "안녕하세요"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Mixed text with spaces`() {
        val input = "dkssud gktpdy"
        val expected = "안녕 하세요"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Mixed text with numbers`() {
        val input = "dkssud123"
        val expected = "안녕123"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Capital letters`() {
        val input = "RkRk"
        val expected = "까까"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `KOR_TO_ENG - Simple translation`() {
        val input = "안녕하세요"
        val expected = "dkssudgktpdy"
        val result = engine.convertForced(input, ConversionDirection.KOR_TO_ENG)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `KOR_TO_ENG - Mixed text with spaces`() {
        val input = "안녕 하세요"
        val expected = "dkssud gktpdy"
        val result = engine.convertForced(input, ConversionDirection.KOR_TO_ENG)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `Direction inference - English`() {
        val input = "dkssudgktpdy"
        val result = engine.convert(input)
        assertEquals(ConversionDirection.ENG_TO_KOR, result.direction)
    }

    @Test
    fun `ENG_TO_KOR - Non-explicit uppercase letters`() {
        val input = "DKSSUD"
        val expected = "안녕"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Special characters`() {
        val input = "dkssud! gktpdy?"
        val expected = "안녕! 하세요?"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `KOR_TO_ENG - Mixed special characters`() {
        val input = "안녕! 하세요?"
        val expected = "dkssud! gktpdy?"
        val result = engine.convertForced(input, ConversionDirection.KOR_TO_ENG)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `Direction inference - Higher ratio of numbers`() {
        // "abc123456" - 3 letters, 6 numbers.
        // My logic: totalLetters = 3. engRatio = 3/3 = 1.0. -> ENG_TO_KOR
        val input = "abc123456"
        val result = engine.convert(input)
        assertEquals(ConversionDirection.ENG_TO_KOR, result.direction)
    }

    @Test
    fun `Mixed text keeps normal english and converts mistyped korean`() {
        val input = "오늘 dhsmf 일정은 hello로 시작해서 ㅕㅔㅇㅁㅅㄷ 공지와 함께 이어지고, 마지막에는 dkssudgktpdy 같은 인사로 끝난다."
        val expected = "오늘 오늘 일정은 hello로 시작해서 update 공지와 함께 이어지고, 마지막에는 안녕하세요 같은 인사로 끝난다."

        val result = engine.convert(input)

        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Keyboard typo token with no vowels converts`() {
        val input = "ghkrdls"
        val expected = "확인"

        val result = engine.convert(input)

        assertEquals(expected, result.resultText)
    }

    @Test
    fun `KOR_TO_ENG - Keyboard typo token converts to english word`() {
        val input = "ㅔㄱㅐㅓㄷㅊㅅ"
        val expected = "project"

        val result = engine.convert(input)

        assertEquals(expected, result.resultText)
    }

    @Test
    fun `ENG_TO_KOR - Complex vowels`() {
        val inputs = mapOf(
            "ghk" to "화",
            "rhk" to "과",
            "dnj" to "워",
            "dml" to "의"
        )
        inputs.forEach { (input, expected) ->
            val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
            assertEquals("Failed for $input", expected, result.resultText)
        }
    }

    @Test
    fun `ENG_TO_KOR - Complex consonants in syllables`() {
        val inputs = mapOf(
            "rkqt" to "값",
            "dksw" to "앉",
            "aksg" to "많",
            "ekfr" to "닭"
        )
        inputs.forEach { (input, expected) ->
            val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
            assertEquals("Failed for $input", expected, result.resultText)
        }
    }

    @Test
    fun `Mixed script tokens attached without spaces`() {
        // "dkssud안녕하세요" -> "안녕안녕하세요" (if dkssud is converted)
        // In mixed mode, it splits into ["dkssud", "안녕하세요"]
        // "dkssud" score will be low for English, high for Korean when converted.
        val input = "dkssud안녕하세요"
        val expected = "안녕안녕하세요"
        val result = engine.convert(input)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `English words attached to Korean typo`() {
        val input = "helloㅔㄱㅐㅓㄷㅊㅅ"
        val expected = "helloproject"
        val result = engine.convert(input)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `Complex jamo in syllables`() {
        val input = "rhksflwk" // 관리자
        val expected = "관리자"
        val result = engine.convertForced(input, ConversionDirection.ENG_TO_KOR)
        assertEquals(expected, result.resultText)
    }

    @Test
    fun `Complex jamo with trailing vowels`() {
        val input = "rksms"
        assertEquals("가는", engine.convertForced(input, ConversionDirection.ENG_TO_KOR).resultText)
    }
}
