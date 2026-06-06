package com.nwe.spadesscore.ui.result

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.GameEngine

class ResultViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): ResultUiState {
        val state = repository.state.value
        return ResultUiState(
            playerCount = state.playerCount,
            playerNames = state.players.map { it.name },
            scoresByPlayer = state.players.map { it.scores.drop(1) },
            visibleRoundCount = state.currentRound - 1,
            highlightColumnIndex = state.currentRound - 2,
            placementByPlayer = GameEngine.placementByPlayerIndex(state),
            isSecondHalf = state.isSecondHalf,
        )
    }

    fun startSecondHalf() = repository.startSecondHalf()
}
