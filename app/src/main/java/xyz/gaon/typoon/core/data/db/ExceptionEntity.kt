package xyz.gaon.typoon.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exceptions",
    indices = [Index(value = ["word"], unique = true)],
)
data class ExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
)
