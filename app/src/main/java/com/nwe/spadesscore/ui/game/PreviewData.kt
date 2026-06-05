package com.nwe.spadesscore.ui.game

import com.nwe.spadesscore.domain.GameState

/** Beispielzustand nur für @Preview – nicht in Produktionslogik verwenden. */
internal fun previewGameState(playerCount: Int = 4): GameState {
    val names = listOf("Anna", "Ben", "Cleo", "Dan").take(playerCount)
    return GameState(
        playerCount = playerCount,
        playerNames = names,
        currentRound = 3,
        currentPlayer = 1,
        amountOfRounds = 8,
        secondHalf = false,
        showResultScreen = false,
        scores = List(playerCount) { p -> listOf(0, 5 + p, 10 + p, 12 + p) },
        tickPredictions = List(playerCount) { 1 }
    )
}
