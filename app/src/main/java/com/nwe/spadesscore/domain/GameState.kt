package com.nwe.spadesscore.domain

/**
 * Immutable snapshot of an in-progress Spades game.
 *
 * Pure data – no Android dependencies. Produced and transformed only by [SpadesEngine].
 * `scores[p]` is player p's cumulative running total, one entry per completed round,
 * always starting with a single `0` entry representing the pre-game state.
 */
data class GameState(
    val playerCount: Int = 4,
    val playerNames: List<String> = emptyList(),
    val currentRound: Int = 0,
    val currentPlayer: Int = 0,
    val amountOfRounds: Int = 0,
    val secondHalf: Boolean = false,
    val showResultScreen: Boolean = false,
    val scores: List<List<Int>> = emptyList(),
    val tickPredictions: List<Int> = emptyList()
)
