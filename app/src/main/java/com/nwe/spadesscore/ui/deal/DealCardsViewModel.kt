package com.nwe.spadesscore.ui.deal

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository

class DealCardsViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): DealCardsUiState {
        val state = repository.state.value
        return DealCardsUiState(
            round = state.currentRound,
            dealerName = state.currentDealerName,
            cardAmount = state.amountOfCards,
            players = state.players.map { PlayerScore(it.name, it.currentScore) },
            amountOfRounds = state.amountOfRounds,
        )
    }
}
