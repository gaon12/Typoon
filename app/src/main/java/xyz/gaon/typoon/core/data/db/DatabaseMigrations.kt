package xyz.gaon.typoon.core.data.db

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

object DatabaseMigrations {
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exceptions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `word` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

    val MIGRATION_2_3: Migration =
        object : Migration(2, 3) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `conversions` ADD COLUMN `isStarred` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

    val MIGRATION_3_4: Migration =
        object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    "ALTER TABLE `conversions` ADD COLUMN `wasEdited` INTEGER NOT NULL DEFAULT 0",
                )
                connection.execSQL(
                    "ALTER TABLE `conversions` ADD COLUMN `wasReversed` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

    val MIGRATION_4_5: Migration =
        object : Migration(4, 5) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE VIRTUAL TABLE IF NOT EXISTS `conversions_fts`
                    USING FTS4(
                        `sourceText` TEXT NOT NULL,
                        `resultText` TEXT NOT NULL,
                        content=`conversions`
                    )
                    """.trimIndent(),
                )
                connection.execSQL(
                    """
                    INSERT INTO `conversions_fts`(`docid`, `sourceText`, `resultText`)
                    SELECT `rowid`, `sourceText`, `resultText`
                    FROM `conversions`
                    """.trimIndent(),
                )
            }
        }

    val MIGRATION_5_6: Migration =
        object : Migration(5, 6) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exceptions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `word` TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
                connection.execSQL(
                    "INSERT INTO `exceptions_new`(`word`) SELECT `word` FROM `exceptions` GROUP BY `word`",
                )
                connection.execSQL("DROP TABLE `exceptions`")
                connection.execSQL("ALTER TABLE `exceptions_new` RENAME TO `exceptions`")
                connection.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_exceptions_word` ON `exceptions` (`word`)",
                )
            }
        }

    val ALL: Array<Migration> =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
        )
}
