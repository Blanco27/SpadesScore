package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.Player
import kotlin.math.floor
import kotlin.random.Random

/** Reine, zustandslose Transitionen. Jede Funktion liefert einen neuen [GameState]. */
object GameEngine {

    fun startGame(
        playerNames: List<String>,
        randomDealer: Boolean,
        language: Language,
        random: Random = Random.Default,
    ): GameState {
        val playerCount = playerNames.size
        return GameState(
            playerCount = playerCount,
            players = playerNames.map { Player(name = it, scores = listOf(0)) },
            currentRound = 1,
            dealerIndex = if (randomDealer) random.nextInt(playerCount) else 0,
            amountOfRounds = floor(GameRules.TOTAL_CARDS.toFloat() / playerCount).toInt(),
            isSecondHalf = false,
            predictions = List(playerCount) { 0 },
            language = language,
        )
    }

    fun confirmTricks(state: GameState, hits: List<Boolean>): GameState {
        val updatedPlayers = state.players.mapIndexed { index, player ->
            val last = player.scores.last()
            val newScore = if (hits[index]) last + state.predictions[index] + GameRules.HIT_BONUS else last
            player.copy(scores = player.scores + newScore)
        }
        return state.copy(
            players = updatedPlayers,
            currentRound = state.currentRound + 1,
            dealerIndex = (state.dealerIndex + 1) % state.playerCount,
        )
    }

    fun startSecondHalf(state: GameState): GameState =
        state.copy(amountOfRounds = state.amountOfRounds * 2, isSecondHalf = true)

    /** Spieler-Index -> Platz (1 = höchster Score). Bildet die Reihenfolge des alten Codes nach. */
    fun placementByPlayerIndex(state: GameState): Map<Int, Int> =
        state.players.indices
            .map { it to state.players[it].currentScore }
            .sortedBy { it.second }
            .reversed()
            .mapIndexed { place, (playerIndex, _) -> playerIndex to place + 1 }
            .toMap()
}
