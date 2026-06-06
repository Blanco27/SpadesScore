package com.nwe.spadesscore.ui.confirm

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.GameRules

class ConfirmTicksViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): ConfirmTicksUiState {
        val state = repository.state.value
        return ConfirmTicksUiState(
            round = state.currentRound,
            players = state.players.mapIndexed { index, player ->
                val prediction = state.predictions[index]
                ConfirmPlayer(
                    name = player.name,
                    score = player.currentScore,
                    prediction = prediction,
                    pointsIfHit = prediction + GameRules.HIT_BONUS,
                )
            },
            amountOfRounds = state.amountOfRounds,
        )
    }

    /** Bestätigt die Treffer und gibt zurück, ob der Ergebnisbildschirm folgt. */
    fun confirm(hits: List<Boolean>): Boolean {
        repository.confirmTricks(hits)
        return repository.state.value.isGameOver
    }
}
