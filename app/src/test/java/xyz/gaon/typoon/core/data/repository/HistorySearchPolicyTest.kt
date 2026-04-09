package xyz.gaon.typoon.core.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HistorySearchPolicyTest {
    @Test
    fun normalizeUserQuery_collapsesWhitespaceAndControls() {
        val normalized = HistorySearchPolicy.normalizeUserQuery("  hello\t\t\nworld\u0000  ")
        assertEquals("hello world", normalized)
    }

    @Test
    fun normalizeUserQuery_capsLength() {
        val normalized = HistorySearchPolicy.normalizeUserQuery("a".repeat(100))

        assertEquals(64, normalized.length)
    }

    @Test
    fun buildSafeFtsPrefixQuery_buildsQuotedPrefixTerms() {
        val query = HistorySearchPolicy.buildSafeFtsPrefixQuery("apple 한글")
        assertEquals("\"apple\"* AND \"한글\"*", query)
    }

    @Test
    fun escapeLikeQuery_escapesWildcardChars() {
        val escaped = HistorySearchPolicy.escapeLikeQuery("ab%c_d\\e")
        assertEquals("ab\\%c\\_d\\\\e", escaped)
    }

    @Test
    fun choseongQuery_detectionAndMatching_workAsExpected() {
        assertTrue(HistorySearchPolicy.isChoseongOnlyQuery("ㅎㄱ"))
        assertFalse(HistorySearchPolicy.isChoseongOnlyQuery("한글"))
        assertTrue(
            HistorySearchPolicy.matchesChoseong(
                query = "ㅎㄱ",
                sourceText = "한글 테스트",
                resultText = "sample",
            ),
        )
        assertFalse(
            HistorySearchPolicy.matchesChoseong(
                query = "ㅂㅈ",
                sourceText = "한글 테스트",
                resultText = "sample",
            ),
        )
    }
}
