package com.nwe.spadesscore.ui.declare

import com.nwe.spadesscore.ui.deal.PlayerScore

data class DeclareTricksUiState(
    val round: Int,
    val players: List<PlayerScore>,
    val cardAmount: Int,
    val amountOfRounds: Int,
)
