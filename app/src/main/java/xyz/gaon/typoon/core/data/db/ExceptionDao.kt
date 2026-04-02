package xyz.gaon.typoon.core.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExceptionDao {
    @Query("SELECT word FROM exceptions")
    fun getAll(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: ExceptionEntity)

    @Query("DELETE FROM exceptions WHERE word = :word")
    suspend fun delete(word: String)

    @Query("DELETE FROM exceptions")
    suspend fun deleteAll()
}
