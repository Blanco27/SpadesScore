package com.nwe.spadesscore.ui.result

data class ResultUiState(
    val playerCount: Int,
    val playerNames: List<String>,
    /** Je Spieler die Score-Werte ab Runde 1 (ohne den Startwert 0). */
    val scoresByPlayer: List<List<Int>>,
    /** Anzahl sichtbarer Runden-Zeilen (= gespielte Runden). */
    val visibleRoundCount: Int,
    /** Spalten-Index (0-basiert) der zuletzt gespielten Runde, der hervorgehoben wird. */
    val highlightColumnIndex: Int,
    /** Spieler-Index -> Platz (1 = höchster Score) für die Medaillenfarben. */
    val placementByPlayer: Map<Int, Int>,
    val isSecondHalf: Boolean,
)
