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

    /** Cards dealt this round: counts up in the first half, down in the second (min 1). */
    fun amountOfCards(state: GameState): Int =
        if (!state.secondHalf) state.currentRound
        else max(1, state.amountOfRounds - state.currentRound + 1)
}
