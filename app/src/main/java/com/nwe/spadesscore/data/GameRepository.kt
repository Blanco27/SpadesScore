package com.nwe.spadesscore.data

import com.nwe.spadesscore.data.db.GameDao
import com.nwe.spadesscore.data.mapper.GameMapper
import com.nwe.spadesscore.domain.GameEngine
import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Single Source of Truth für den Spielzustand. Hält [state] im Speicher und persistiert
 * jede Mutation nach Room. Der initiale Zustand wird einmalig blockierend geladen
 * (winzige Datenmenge), damit [state].value sofort gültig ist – wichtig für die
 * Wiederherstellung nach Prozess-Tod in einer tiefer liegenden Activity.
 */
class GameRepository(
    private val dao: GameDao,
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)

    private val _state = MutableStateFlow(loadInitialState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private fun loadInitialState(): GameState = runBlocking(ioDispatcher) {
        runCatching { dao.loadGame()?.let { GameMapper.toDomain(it) } }.getOrNull() ?: GameState()
    }

    private fun update(newState: GameState) {
        _state.value = newState
        persist(newState)
    }

    private fun persist(state: GameState) {
        scope.launch {
            val entities = GameMapper.toEntities(state)
            runCatching {
                dao.saveGame(entities.game, entities.players, entities.scores, entities.predictions)
            }
        }
    }

    fun setPlayerCount(playerCount: Int) = update(_state.value.copy(playerCount = playerCount))

    fun setLanguage(language: Language) = update(_state.value.copy(language = language))

    fun startGame(playerNames: List<String>, randomDealer: Boolean) =
        update(GameEngine.startGame(playerNames, randomDealer, _state.value.language))

    fun setPredictions(predictions: List<Int>) = update(_state.value.copy(predictions = predictions))

    fun confirmTricks(hits: List<Boolean>) = update(GameEngine.confirmTricks(_state.value, hits))

    fun startSecondHalf() = update(GameEngine.startSecondHalf(_state.value))
}
