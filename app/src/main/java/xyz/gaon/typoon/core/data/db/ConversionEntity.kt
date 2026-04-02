package xyz.gaon.typoon.core.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "conversions")
data class ConversionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sourceText: String,
    val resultText: String,
    val direction: String,
    val confidence: Float,
    val createdAt: Long,
    val entryPoint: String,
    val isStarred: Boolean = false,
    val wasEdited: Boolean = false,
    val wasReversed: Boolean = false,
)
