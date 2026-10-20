package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.CheckpointDao
import com.example.data.dao.FixtureDao
import com.example.data.dao.PaperBetDao
import com.example.data.dao.PredictionDao
import com.example.data.model.CollectionCheckpoint
import com.example.data.model.PaperBetEntity
import com.example.data.model.PredictionEntity
import com.example.data.model.UnifiedFixtureRecord

@Database(
    entities = [
        UnifiedFixtureRecord::class,
        CollectionCheckpoint::class,
        PredictionEntity::class,
        PaperBetEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fixtureDao(): FixtureDao
    abstract fun checkpointDao(): CheckpointDao
    abstract fun predictionDao(): PredictionDao
    abstract fun paperBetDao(): PaperBetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "virtual_bet_ai.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
