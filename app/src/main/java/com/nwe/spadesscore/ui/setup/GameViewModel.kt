package com.nwe.spadesscore.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.Languages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI state for the Main/Setup screen. */
data class SetupUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH
)

/**
 * Holds the setup screen state and writes every choice through to [GameSetup] so the legacy
 * Activity flow sees the values. Survives configuration changes by virtue of being a ViewModel
 * (no SavedStateHandle needed; process-death persistence is out of scope for this phase).
 */
class GameViewModel(private val setup: GameSetup) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SetupUiState(
            playerCount = setup.playerCount(),
            language = setup.language() ?: Languages.ENGLISH
        )
    )
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun selectPlayerCount(count: Int) {
        setup.setPlayerCount(count)
        _uiState.update { it.copy(playerCount = count) }
    }

    fun selectLanguage(language: Languages) {
        setup.setLanguage(language)
        _uiState.update { it.copy(language = language) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(SpadesGameSetup()) }
        }
    }
}
