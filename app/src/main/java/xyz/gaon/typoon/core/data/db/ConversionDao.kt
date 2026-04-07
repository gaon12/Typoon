@file:Suppress("TooManyFunctions", "MaxLineLength")

package xyz.gaon.typoon.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionDao {
    @Query("SELECT * FROM conversions ORDER BY createdAt DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<ConversionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ConversionEntity): Long

    @Transaction
    suspend fun insertAndTrim(
        entity: ConversionEntity,
        maxHistoryCount: Int,
    ): Long {
        val id = insert(entity)
        val currentCount = count()
        val deleteCount = currentCount - maxHistoryCount
        if (deleteCount > 0) {
            deleteOldest(deleteCount)
        }
        return id
    }

    @Query("DELETE FROM conversions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM conversions")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM conversions")
    suspend fun count(): Int

    @Query(
        """
        DELETE FROM conversions
        WHERE id IN (
            SELECT id FROM conversions
            ORDER BY createdAt ASC
            LIMIT :count
        )
        """,
    )
    suspend fun deleteOldest(count: Int)

    @Query("SELECT COUNT(*) FROM conversions")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(LENGTH(sourceText)), 0) FROM conversions")
    fun getTotalCharsProcessed(): Flow<Int>

    @Query("UPDATE conversions SET isStarred = :starred WHERE id = :id")
    suspend fun updateStarred(
        id: Long,
        starred: Boolean,
    )

    @Query("SELECT * FROM conversions WHERE isStarred = 1 ORDER BY createdAt DESC")
    fun getStarred(): Flow<List<ConversionEntity>>

    @Query("UPDATE conversions SET wasEdited = 1 WHERE id = :id")
    suspend fun markEdited(id: Long)

    @Query("UPDATE conversions SET wasReversed = 1 WHERE id = :id")
    suspend fun markReversed(id: Long)

    @Query(
        "SELECT COALESCE(AVG(CASE WHEN wasEdited = 1 OR wasReversed = 1 THEN 1.0 ELSE 0.0 END), 0.0) FROM conversions WHERE createdAt > :since",
    )
    fun getRecentNegativeFeedbackRate(since: Long): Flow<Float>

    @Query(
        """
        SELECT c.* FROM conversions c
        JOIN conversions_fts fts ON c.rowid = fts.rowid
        WHERE conversions_fts MATCH :query
        ORDER BY c.createdAt DESC
        LIMIT 50
    """,
    )
    fun search(query: String): Flow<List<ConversionEntity>>

    @Query("SELECT * FROM conversions ORDER BY createdAt DESC")
    suspend fun getAllSortedByDate(): List<ConversionEntity>
}
