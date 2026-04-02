package xyz.gaon.typoon.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exceptions")
data class ExceptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val word: String,
)
