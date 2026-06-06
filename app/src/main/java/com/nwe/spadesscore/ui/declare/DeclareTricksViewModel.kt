package com.nwe.spadesscore.ui.declare

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.ui.deal.PlayerScore

class DeclareTricksViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): DeclareTricksUiState {
        val state = repository.state.value
        return DeclareTricksUiState(
            round = state.currentRound,
            players = state.players.map { PlayerScore(it.name, it.currentScore) },
            cardAmount = state.amountOfCards,
            amountOfRounds = state.amountOfRounds,
        )
    }

    fun setPredictions(predictions: List<Int>) = repository.setPredictions(predictions)
}
