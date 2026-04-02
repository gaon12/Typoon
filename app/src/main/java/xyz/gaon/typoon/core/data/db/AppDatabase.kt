package xyz.gaon.typoon.core.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ConversionEntity::class, ExceptionEntity::class, ConversionFts::class],
    version = 5,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversionDao(): ConversionDao

    abstract fun exceptionDao(): ExceptionDao
}
