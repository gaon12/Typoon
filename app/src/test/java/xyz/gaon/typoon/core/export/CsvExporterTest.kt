package xyz.gaon.typoon.core.export

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.model.ConversionStats
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import java.io.ByteArrayOutputStream

class CsvExporterTest {
    @Test
    fun exportToCsv_writesHeaderAndEscapedRows() =
        runBlocking {
            val repository =
                object : HistoryRepository {
                    override fun getRecentHistory(limit: Int): Flow<List<ConversionEntity>> = emptyFlow()

                    override fun searchHistory(query: String): Flow<List<ConversionEntity>> = emptyFlow()

                    override fun getStats(): Flow<ConversionStats> = emptyFlow()

                    override suspend fun toggleStar(
                        id: Long,
                        starred: Boolean,
                    ) = Unit

                    override fun getStarred(): Flow<List<ConversionEntity>> = emptyFlow()

                    override suspend fun markEdited(id: Long) = Unit

                    override suspend fun markReversed(id: Long) = Unit

                    override fun getRecentNegativeFeedbackRate(sinceMs: Long): Flow<Float> = emptyFlow()

                    override suspend fun insert(entity: ConversionEntity): Long = 0L

                    override suspend fun getAllForExport(): List<ConversionEntity> =
                        listOf(
                            ConversionEntity(
                                id = 1L,
                                sourceText = "hello,\"world\"",
                                resultText = "안녕,\"세상\"",
                                direction = "ENG_TO_KOR",
                                confidence = 0.92f,
                                createdAt = 1710000000000L,
                                entryPoint = "HOME",
                                isStarred = true,
                            ),
                        )

                    override suspend fun delete(id: Long) = Unit

                    override suspend fun deleteAll() = Unit
                }

            val exporter = CsvExporter(repository)
            val output = ByteArrayOutputStream()

            exporter.exportToCsv(output)
            val csv = output.toString(Charsets.UTF_8.name())

            assertTrue(csv.startsWith("id,sourceText,resultText,direction,confidence,createdAt,isStarred"))
            assertTrue(csv.contains("\"hello,\"\"world\"\"\""))
            assertTrue(csv.contains("\"안녕,\"\"세상\"\"\""))
        }
}
