package xyz.gaon.typoon.core.data.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.datastore.AppSettings
import xyz.gaon.typoon.core.data.db.ConversionDao
import xyz.gaon.typoon.core.data.db.ConversionEntity

class HistoryRepositoryImplTest {
    private val dao: ConversionDao = mockk()
    private val preferences: AppPreferences = mockk()
    private val repository = HistoryRepositoryImpl(dao, preferences)

    @Test
    fun insert_usesConfiguredMaxHistoryCount() =
        runTest {
            val entity =
                ConversionEntity(
                    sourceText = "source",
                    resultText = "result",
                    direction = "ENG_TO_KOR",
                    confidence = 0.9f,
                    createdAt = 1L,
                    entryPoint = "HOME",
                )
            every { preferences.settings } returns flowOf(AppSettings(maxHistoryCount = 37))
            coEvery { dao.insertAndTrim(entity, 37) } returns 7L

            val insertedId = repository.insert(entity)

            assertEquals(7L, insertedId)
            coVerify(exactly = 1) { dao.insertAndTrim(entity, 37) }
        }

    @Test
    fun searchHistory_withBlankQuery_returnsRecent() =
        runTest {
            val expected = listOf(sampleEntity(id = 1L, source = "hello"))
            every { dao.getRecent(50) } returns flowOf(expected)

            val result = repository.searchHistory("   ").first()

            assertEquals(expected, result)
            verify(exactly = 1) { dao.getRecent(50) }
        }

    @Test
    fun searchHistory_withChoseongQuery_filtersByInitialConsonants() =
        runTest {
            val matching = sampleEntity(id = 1L, source = "한글 테스트")
            val notMatching = sampleEntity(id = 2L, source = "영문 sample")
            every { dao.getRecent(250) } returns flowOf(listOf(notMatching, matching))

            val result = repository.searchHistory("ㅎㄱ").first()

            assertEquals(listOf(matching), result)
            verify(exactly = 1) { dao.getRecent(250) }
        }

    @Test
    fun searchHistory_mergesFtsAndLikeWithoutDuplicates() =
        runTest {
            val older = sampleEntity(id = 1L, source = "apple", createdAt = 10L)
            val newer = sampleEntity(id = 2L, source = "안녕 apple", createdAt = 20L)
            every { dao.searchFts(any()) } returns flowOf(listOf(older, newer))
            every { dao.searchLike(any()) } returns flowOf(listOf(newer))

            val result = repository.searchHistory("apple 안녕").first()

            assertEquals(listOf(newer, older), result)
            verify(exactly = 1) { dao.searchFts(any()) }
            verify(exactly = 1) { dao.searchLike(any()) }
        }

    private fun sampleEntity(
        id: Long,
        source: String,
        createdAt: Long = 100L,
    ): ConversionEntity =
        ConversionEntity(
            id = id,
            sourceText = source,
            resultText = "result-$id",
            direction = "ENG_TO_KOR",
            confidence = 0.8f,
            createdAt = createdAt,
            entryPoint = "HOME",
        )
}
