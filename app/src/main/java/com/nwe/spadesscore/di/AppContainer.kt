package com.nwe.spadesscore.di

import android.content.Context
import androidx.room.Room
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.data.ThemePreferences
import com.nwe.spadesscore.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers

/** Manuelles DI: lebt als Singleton in [com.nwe.spadesscore.SpadesApplication]. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "spades.db",
    ).build()

    val gameRepository: GameRepository = GameRepository(database.gameDao(), Dispatchers.IO)

    val themePreferences: ThemePreferences = ThemePreferences(context)
}
