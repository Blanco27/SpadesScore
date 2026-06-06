package com.nwe.spadesscore.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.spadesscore.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {

    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()

    @Test
    fun startGameAndConfirm_persistsAndRestores() = runTest {
        val db = newDb()
        val repo = GameRepository(db.gameDao(), Dispatchers.IO)
        repo.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        repo.setPredictions(listOf(2, 0, 0, 0))
        repo.confirmTricks(listOf(true, false, false, false))

        // Persistenz deterministisch abwarten: der serielle Dispatcher garantiert,
        // dass mit dem letzten Job auch alle vorherigen Schreibvorgänge fertig sind.
        repo.lastPersistJob?.join()

        // Repository neu aufbauen -> lädt aus derselben DB
        val restored = GameRepository(db.gameDao(), Dispatchers.IO)
        assertEquals(2, restored.state.value.currentRound)
        assertEquals(listOf(0, 7), restored.state.value.players[0].scores) // 0 + 2 + 5
        db.close()
    }
}
