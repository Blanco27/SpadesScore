package com.nwe.spadesscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Random

/** UI-State für den gesamten Spielfluss. `game` ist null vor [startGame]. */
data class GameUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH,
    val game: GameState? = null
)

/**
 * Activity-weites ViewModel des gesamten Spiels. Besitzt den unveränderlichen [GameState] und ruft
 * die reine [SpadesEngine] direkt – kein SpadesGame-Singleton. Überlebt Config-Changes als ViewModel
 * (keine Prozesstod-Persistenz; bewusst außerhalb des Umfangs). [random] ist für deterministische
 * Tests injizierbar.
 */
class GameViewModel(private val random: Random = Random()) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun selectPlayerCount(count: Int) = _uiState.update { it.copy(playerCount = count) }

    fun selectLanguage(language: Languages) = _uiState.update { it.copy(language = language) }

    fun startGame(names: List<String>, randomDealer: Boolean) {
        val start = if (randomDealer) SpadesEngine.randomStartingPlayer(_uiState.value.playerCount, random) else 0
        _uiState.update { it.copy(game = SpadesEngine.newGame(it.playerCount, names, start)) }
    }

    fun setPredictions(predictions: List<Int>) =
        _uiState.update { it.copy(game = SpadesEngine.setTickPredictions(requireGame(it.game), predictions)) }

    fun confirmRound(made: List<Boolean>) =
        _uiState.update { it.copy(game = SpadesEngine.confirmTricks(requireGame(it.game), made)) }

    fun continueSecondHalf() =
        _uiState.update { it.copy(game = SpadesEngine.startSecondHalf(requireGame(it.game))) }

    fun newGame() = _uiState.update { it.copy(game = null) }

    private fun requireGame(game: GameState?): GameState =
        requireNotNull(game) { "No game in progress" }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel() }
        }
    }
}
