package com.nwe.spadesscore.ui.setup

import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.SpadesGame

/**
 * Port through which the setup screen reads the initial player count / language and writes
 * the user's choices. Keeps [GameViewModel] decoupled from the [SpadesGame] singleton, so the
 * ViewModel is unit-testable on the JVM with a fake. Bridge for the legacy Activity flow until
 * the singleton is removed in a later phase.
 */
interface GameSetup {
    fun playerCount(): Int
    fun language(): Languages?
    fun setPlayerCount(count: Int)
    fun setLanguage(language: Languages)
}

/** Real implementation delegating to the in-memory [SpadesGame] singleton. */
class SpadesGameSetup(
    private val game: SpadesGame = SpadesGame.getInstance()
) : GameSetup {
    override fun playerCount(): Int = game.playerCount
    override fun language(): Languages? = game.languages
    override fun setPlayerCount(count: Int) {
        game.setPlayerCount(count)
    }
    override fun setLanguage(language: Languages) {
        game.setLanguages(language)
    }
}
