package xyz.gaon.typoon.core.data.repository

import kotlinx.coroutines.flow.Flow
import xyz.gaon.typoon.core.data.db.ExceptionDao
import xyz.gaon.typoon.core.data.db.ExceptionEntity

interface ExceptionRepository {
    fun getAll(): Flow<List<String>>

    suspend fun add(word: String)

    suspend fun remove(word: String)

    suspend fun deleteAll()
}

class ExceptionRepositoryImpl(
    private val dao: ExceptionDao,
) : ExceptionRepository {
    override fun getAll(): Flow<List<String>> = dao.getAll()

    override suspend fun add(word: String) {
        dao.insert(ExceptionEntity(word = word))
    }

    override suspend fun remove(word: String) {
        dao.delete(word)
    }

    override suspend fun deleteAll() {
        dao.deleteAll()
    }
}
