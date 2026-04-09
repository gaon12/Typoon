package xyz.gaon.typoon.core.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import xyz.gaon.typoon.core.clipboard.ClipboardHelper
import xyz.gaon.typoon.core.data.datastore.AppPreferences
import xyz.gaon.typoon.core.data.db.AppDatabase
import xyz.gaon.typoon.core.data.db.ConversionDao
import xyz.gaon.typoon.core.data.db.DatabaseMigrations
import xyz.gaon.typoon.core.data.db.ExceptionDao
import xyz.gaon.typoon.core.data.repository.ExceptionRepository
import xyz.gaon.typoon.core.data.repository.ExceptionRepositoryImpl
import xyz.gaon.typoon.core.data.repository.HistoryRepository
import xyz.gaon.typoon.core.data.repository.HistoryRepositoryImpl
import xyz.gaon.typoon.core.engine.ConversionEngine
import xyz.gaon.typoon.core.export.CsvExporter
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ConversionDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(
                context,
                AppDatabase::class.java,
                "typoon_db",
            ).addMigrations(*DatabaseMigrations.ALL)
            .build()

    @Provides
    fun provideConversionDao(database: AppDatabase): ConversionDao = database.conversionDao()

    @Provides
    fun provideExceptionDao(database: AppDatabase): ExceptionDao = database.exceptionDao()

    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context,
    ): AppPreferences = AppPreferences(context)

    @Provides
    @Singleton
    fun provideHistoryRepository(
        dao: ConversionDao,
        preferences: AppPreferences,
    ): HistoryRepository = HistoryRepositoryImpl(dao, preferences)

    @Provides
    @Singleton
    fun provideExceptionRepository(dao: ExceptionDao): ExceptionRepository = ExceptionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideConversionEngine(): ConversionEngine = ConversionEngine()

    @Provides
    @Singleton
    fun provideClipboardHelper(
        @ApplicationContext context: Context,
    ): ClipboardHelper = ClipboardHelper(context)

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    @Singleton
    @ConversionDispatcher
    fun provideConversionDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @Singleton
    fun provideCsvExporter(historyRepository: HistoryRepository): CsvExporter = CsvExporter(historyRepository)
}
