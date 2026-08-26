package com.gatemaster.app.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [AttemptEntity::class, QuestionResultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GateMasterDatabase : RoomDatabase() {

    abstract fun attemptDao(): AttemptDao

    companion object {
        private const val NAME = "gatemaster.db"

        @Volatile
        private var instance: GateMasterDatabase? = null

        fun get(context: Context): GateMasterDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                GateMasterDatabase::class.java,
                NAME,
            ).build().also { instance = it }
        }
    }
}
