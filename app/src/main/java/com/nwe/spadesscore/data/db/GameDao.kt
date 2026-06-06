package com.nwe.spadesscore.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GameDao {

    @Query("SELECT * FROM game WHERE id = 1")
    suspend fun getGame(): GameEntity?

    @Query("SELECT * FROM player ORDER BY playerIndex")
    suspend fun getPlayers(): List<PlayerEntity>

    @Query("SELECT * FROM score ORDER BY playerIndex, roundIndex")
    suspend fun getScores(): List<ScoreEntity>

    @Query("SELECT * FROM prediction ORDER BY playerIndex")
    suspend fun getPredictions(): List<PredictionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert
    suspend fun insertScores(scores: List<ScoreEntity>)

    @Insert
    suspend fun insertPredictions(predictions: List<PredictionEntity>)

    @Query("DELETE FROM game")
    suspend fun clearGame()

    @Query("DELETE FROM player")
    suspend fun clearPlayers()

    @Query("DELETE FROM score")
    suspend fun clearScores()

    @Query("DELETE FROM prediction")
    suspend fun clearPredictions()

    @Transaction
    suspend fun saveGame(
        game: GameEntity,
        players: List<PlayerEntity>,
        scores: List<ScoreEntity>,
        predictions: List<PredictionEntity>,
    ) {
        clearScores()
        clearPredictions()
        clearPlayers()
        clearGame()
        insertGame(game)
        insertPlayers(players)
        insertScores(scores)
        insertPredictions(predictions)
    }

    @Transaction
    suspend fun loadGame(): GameSnapshot? {
        val game = getGame() ?: return null
        return GameSnapshot(game, getPlayers(), getScores(), getPredictions())
    }
}
