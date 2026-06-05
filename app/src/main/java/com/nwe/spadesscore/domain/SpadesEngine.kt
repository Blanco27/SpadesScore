package com.nwe.spadesscore.domain

import java.util.Random
import kotlin.math.max

/**
 * Pure game-rule engine for Spades scoring. Every transition takes a [GameState]
 * and returns a new one; no mutation, no Android, fully unit-testable.
 */
object SpadesEngine {

    /** Starts a fresh first half. [startingPlayer] is the round-1 dealer. */
    fun newGame(playerCount: Int, playerNames: List<String>, startingPlayer: Int): GameState =
        GameState(
            playerCount = playerCount,
            playerNames = playerNames,
            currentRound = 1,
            currentPlayer = startingPlayer,
            amountOfRounds = 32 / playerCount,
            secondHalf = false,
            showResultScreen = false,
            scores = List(playerCount) { listOf(0) },
            tickPredictions = emptyList()
        )

    /** Records this round's trick predictions (one per player slot). */
    fun setTickPredictions(state: GameState, predictions: List<Int>): GameState =
        state.copy(tickPredictions = predictions.toList())

    /**
     * Applies the made/missed outcome of the current round: appends each player's new
     * cumulative total, rotates the dealer, advances the round, and flags the result
     * screen once the half is over. Only the first `playerCount` players are scored.
     */
    fun confirmTricks(state: GameState, made: List<Boolean>): GameState {
        val updatedScores = state.scores.mapIndexed { player, history ->
            val previousTotal = history.last()
            val newTotal =
                if (made[player]) previousTotal + state.tickPredictions[player] + 5
                else previousTotal
            history + newTotal
        }
        val nextRound = state.currentRound + 1
        return state.copy(
            scores = updatedScores,
            currentRound = nextRound,
            currentPlayer = (state.currentPlayer + 1) % state.playerCount,
            showResultScreen = nextRound > state.amountOfRounds
        )
    }

    /** Cards dealt this round: counts up in the first half, down in the second (min 1). */
    fun amountOfCards(state: GameState): Int =
        if (!state.secondHalf) state.currentRound
        else max(1, state.amountOfRounds - state.currentRound + 1)
}
