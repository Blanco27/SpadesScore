package com.nwe.spadesscore.ui.names

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.NameValidation
import com.nwe.spadesscore.domain.NameValidationResult

sealed interface StartGameResult {
    data object Started : StartGameResult
    data object EmptyName : StartGameResult
    data object NameTooLong : StartGameResult
}

class PlayerNamesViewModel(private val repository: GameRepository) : ViewModel() {

    val playerCount: Int get() = repository.state.value.playerCount

    fun start(names: List<String>, randomDealer: Boolean): StartGameResult =
        when (NameValidation.validate(names)) {
            NameValidationResult.Empty -> StartGameResult.EmptyName
            NameValidationResult.TooLong -> StartGameResult.NameTooLong
            NameValidationResult.Ok -> {
                repository.startGame(names, randomDealer)
                StartGameResult.Started
            }
        }
}
