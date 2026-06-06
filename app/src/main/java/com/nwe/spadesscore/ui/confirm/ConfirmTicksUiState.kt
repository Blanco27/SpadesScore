package com.nwe.spadesscore.ui.confirm

data class ConfirmPlayer(
    val name: String,
    val score: Int,
    val prediction: Int,
    val pointsIfHit: Int,
)

data class ConfirmTicksUiState(
    val round: Int,
    val players: List<ConfirmPlayer>,
    val amountOfRounds: Int,
)
