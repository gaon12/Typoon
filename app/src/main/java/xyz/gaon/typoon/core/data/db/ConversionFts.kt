package xyz.gaon.typoon.core.data.db

import androidx.room.Entity
import androidx.room.Fts4

@Entity(tableName = "conversions_fts")
@Fts4(contentEntity = ConversionEntity::class)
data class ConversionFts(
    val sourceText: String,
    val resultText: String,
)
