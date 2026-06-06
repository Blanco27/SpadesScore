package com.nwe.spadesscore.domain.model

/**
 * Unveränderlicher Gesamtzustand eines Spiels. Reine Daten + abgeleitete Werte,
 * keine Android-Abhängigkeiten.
 */
data class GameState(
    val playerCount: Int = 4,
    val players: List<Player> = emptyList(),
    val currentRound: Int = 1,
    val dealerIndex: Int = 0,
    val amountOfRounds: Int = 0,
    val isSecondHalf: Boolean = false,
    val predictions: List<Int> = emptyList(),
    val language: Language = Language.ENGLISH,
) {
    /** Karten in der aktuellen Runde: 1. Halbzeit Hochrampe, 2. Halbzeit Runterrampe (min. 1). */
    val amountOfCards: Int
        get() = if (!isSecondHalf) currentRound else maxOf(1, amountOfRounds - currentRound + 1)

    /** True, sobald die aktuelle Runde die Rundenzahl überschreitet (löst Ergebnisbildschirm aus). */
    val isGameOver: Boolean
        get() = currentRound > amountOfRounds

    val currentDealerName: String
        get() = players[dealerIndex].name
}
