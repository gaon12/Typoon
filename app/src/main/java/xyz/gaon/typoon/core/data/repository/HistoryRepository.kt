@file:Suppress("TooManyFunctions")

package xyz.gaon.typoon.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.db.ConversionDao
import xyz.gaon.typoon.core.data.db.ConversionEntity
import xyz.gaon.typoon.core.data.model.ConversionStats

interface HistoryRepository {
    fun getRecentHistory(limit: Int = 10): Flow<List<ConversionEntity>>

    fun searchHistory(query: String): Flow<List<ConversionEntity>>

    fun getStats(): Flow<ConversionStats>

    suspend fun toggleStar(
        id: Long,
        starred: Boolean,
    )

    fun getStarred(): Flow<List<ConversionEntity>>

    suspend fun markEdited(id: Long)

    suspend fun markReversed(id: Long)

    fun getRecentNegativeFeedbackRate(sinceMs: Long = System.currentTimeMillis() - 7 * 86_400_000L): Flow<Float>

    suspend fun insert(entity: ConversionEntity): Long

    suspend fun getAllForExport(): List<ConversionEntity>

    suspend fun delete(id: Long)

    suspend fun deleteAll()
}

class HistoryRepositoryImpl(
    private val dao: ConversionDao,
    private val preferences: AppPreferences,
) : HistoryRepository {
    override fun getRecentHistory(limit: Int): Flow<List<ConversionEntity>> = dao.getRecent(limit)

    override fun searchHistory(query: String): Flow<List<ConversionEntity>> {
        val normalized = HistorySearchPolicy.normalizeUserQuery(query)
        if (normalized.isBlank()) return dao.getRecent(50)

        if (HistorySearchPolicy.isChoseongOnlyQuery(normalized)) {
            return dao
                .getRecent(500)
                .map { entities ->
                    entities
                        .filter { entity ->
                            HistorySearchPolicy.matchesChoseong(
                                query = normalized,
                                sourceText = entity.sourceText,
                                resultText = entity.resultText,
                            )
                        }.take(50)
                }
        }

        val escapedLikeQuery = HistorySearchPolicy.escapeLikeQuery(normalized)
        val likeFlow = dao.searchLike(escapedLikeQuery)
        val ftsQuery = HistorySearchPolicy.buildSafeFtsPrefixQuery(normalized)
        if (ftsQuery == null) return likeFlow

        return combine(dao.searchFts(ftsQuery), likeFlow) { ftsMatches, likeMatches ->
            (ftsMatches + likeMatches)
                .distinctBy(ConversionEntity::id)
                .sortedByDescending { it.createdAt }
                .take(50)
        }
    }

    override suspend fun toggleStar(
        id: Long,
        starred: Boolean,
    ) = dao.updateStarred(id, starred)

    override fun getStarred(): Flow<List<ConversionEntity>> = dao.getStarred()

    override suspend fun markEdited(id: Long) = dao.markEdited(id)

    override suspend fun markReversed(id: Long) = dao.markReversed(id)

    override fun getRecentNegativeFeedbackRate(sinceMs: Long): Flow<Float> = dao.getRecentNegativeFeedbackRate(sinceMs)

    override fun getStats(): Flow<ConversionStats> =
        combine(dao.getTotalCount(), dao.getTotalCharsProcessed()) { count, chars ->
            ConversionStats(count, chars)
        }

    override suspend fun insert(entity: ConversionEntity): Long {
        val settings = preferences.settings.first()
        return dao.insertAndTrim(entity = entity, maxHistoryCount = settings.maxHistoryCount)
    }

    override suspend fun getAllForExport(): List<ConversionEntity> = dao.getAllSortedByDate()

    override suspend fun delete(id: Long) {
        dao.deleteById(id)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
