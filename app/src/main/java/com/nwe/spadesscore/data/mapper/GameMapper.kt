package com.nwe.spadesscore.data.mapper

import com.nwe.spadesscore.data.db.GameEntity
import com.nwe.spadesscore.data.db.GameSnapshot
import com.nwe.spadesscore.data.db.PlayerEntity
import com.nwe.spadesscore.data.db.PredictionEntity
import com.nwe.spadesscore.data.db.ScoreEntity
import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.Player

data class GameEntities(
    val game: GameEntity,
    val players: List<PlayerEntity>,
    val scores: List<ScoreEntity>,
    val predictions: List<PredictionEntity>,
)

object GameMapper {

    fun toEntities(state: GameState): GameEntities {
        val game = GameEntity(
            id = 1,
            playerCount = state.playerCount,
            currentRound = state.currentRound,
            dealerIndex = state.dealerIndex,
            amountOfRounds = state.amountOfRounds,
            isSecondHalf = state.isSecondHalf,
            language = state.language.name,
        )
        val players = state.players.mapIndexed { index, player ->
            PlayerEntity(playerIndex = index, name = player.name)
        }
        val scores = state.players.flatMapIndexed { playerIndex, player ->
            player.scores.mapIndexed { roundIndex, score ->
                ScoreEntity(playerIndex = playerIndex, roundIndex = roundIndex, cumulativeScore = score)
            }
        }
        val predictions = state.predictions.mapIndexed { index, value ->
            PredictionEntity(playerIndex = index, value = value)
        }
        return GameEntities(game, players, scores, predictions)
    }

    fun toDomain(snapshot: GameSnapshot): GameState {
        val game = snapshot.game
        val players = snapshot.players
            .sortedBy { it.playerIndex }
            .map { playerEntity ->
                val scores = snapshot.scores
                    .filter { it.playerIndex == playerEntity.playerIndex }
                    .sortedBy { it.roundIndex }
                    .map { it.cumulativeScore }
                Player(name = playerEntity.name, scores = scores)
            }
        val predictions = snapshot.predictions.sortedBy { it.playerIndex }.map { it.value }
        return GameState(
            playerCount = game.playerCount,
            players = players,
            currentRound = game.currentRound,
            dealerIndex = game.dealerIndex,
            amountOfRounds = game.amountOfRounds,
            isSecondHalf = game.isSecondHalf,
            predictions = predictions,
            language = runCatching { Language.valueOf(game.language) }.getOrDefault(Language.ENGLISH),
        )
    }
}
