package com.nwe.spadesscore.ui.main

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.model.Language

class MainViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): MainUiState {
        val state = repository.state.value
        return MainUiState(playerCount = state.playerCount, language = state.language)
    }

    fun setPlayerCount(playerCount: Int) = repository.setPlayerCount(playerCount)

    fun setLanguage(language: Language) = repository.setLanguage(language)
}
