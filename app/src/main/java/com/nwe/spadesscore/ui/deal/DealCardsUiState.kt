package com.nwe.spadesscore.ui.deal

data class PlayerScore(val name: String, val score: Int)

data class DealCardsUiState(
    val round: Int,
    val dealerName: String,
    val cardAmount: Int,
    val players: List<PlayerScore>,
    val amountOfRounds: Int,
)
