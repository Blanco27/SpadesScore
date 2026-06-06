package com.nwe.spadesscore.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, PlayerEntity::class, ScoreEntity::class, PredictionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
