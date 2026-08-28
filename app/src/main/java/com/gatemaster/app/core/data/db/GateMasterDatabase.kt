package com.gatemaster.app.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [AttemptEntity::class, QuestionResultEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class GateMasterDatabase : RoomDatabase() {

    abstract fun attemptDao(): AttemptDao

    companion object {
        private const val NAME = "gatemaster.db"

        /**
         * Adds the three columns sync needs.
         *
         * A destructive migration would have been one line, and would have
         * thrown away every attempt the user has ever sat -- which is the only
         * thing in this database and the whole reason the Progress tab has
         * anything to show. So: real migration.
         *
         * Existing rows get a client id derived from their row id. They have
         * never left this device, so uniqueness within it is all they need,
         * and it has to be deterministic because SQLite cannot generate a UUID
         * and the unique index has to hold the moment it is created.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE attempts ADD COLUMN clientAttemptId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE attempts ADD COLUMN syncedAt INTEGER")
                db.execSQL("ALTER TABLE attempts ADD COLUMN serverSeq INTEGER")
                db.execSQL("UPDATE attempts SET clientAttemptId = 'local-' || id")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_attempts_clientAttemptId " +
                        "ON attempts (clientAttemptId)",
                )
            }
        }

        @Volatile
        private var instance: GateMasterDatabase? = null

        fun get(context: Context): GateMasterDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                GateMasterDatabase::class.java,
                NAME,
            ).addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
