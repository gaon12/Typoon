package xyz.gaon.typoon.core.export

import xyz.gaon.typoon.core.data.repository.HistoryRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CsvExporter
    @Inject
    constructor(
        private val historyRepository: HistoryRepository,
    ) {
        suspend fun exportToCsv(outputStream: java.io.OutputStream) {
            val records = historyRepository.getAllForExport()
            outputStream.bufferedWriter().use { writer ->
                writer.write("id,sourceText,resultText,direction,confidence,createdAt,isStarred\n")
                records.forEach { entity ->
                    val escapedSource = entity.sourceText.replace("\"", "\"\"")
                    val escapedResult = entity.resultText.replace("\"", "\"\"")
                    writer.write(
                        "${entity.id},\"$escapedSource\",\"$escapedResult\",${entity.direction}," +
                            "${entity.confidence},${entity.createdAt},${entity.isStarred}\n",
                    )
                }
            }
        }
    }
