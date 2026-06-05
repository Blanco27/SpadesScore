package com.nwe.spadesscore.ui.setup

import com.nwe.spadesscore.Languages
import org.junit.Assert.assertEquals
import org.junit.Test

/** In-memory fake of the setup port; records write-throughs so we can assert them. */
private class FakeGameSetup(
    private var playerCount: Int = 4,
    private var language: Languages? = null
) : GameSetup {
    val setPlayerCountCalls = mutableListOf<Int>()
    val setLanguageCalls = mutableListOf<Languages>()
    override fun playerCount(): Int = playerCount
    override fun language(): Languages? = language
    override fun setPlayerCount(count: Int) {
        playerCount = count
        setPlayerCountCalls += count
    }
    override fun setLanguage(language: Languages) {
        this.language = language
        setLanguageCalls += language
    }
}

class GameViewModelTest {

    @Test
    fun initialState_seedsFromSetup() {
        val vm = GameViewModel(FakeGameSetup(playerCount = 3, language = Languages.GERMAN))

        assertEquals(SetupUiState(playerCount = 3, language = Languages.GERMAN), vm.uiState.value)
    }

    @Test
    fun initialState_defaultsLanguageToEnglishWhenNull() {
        val vm = GameViewModel(FakeGameSetup(playerCount = 4, language = null))

        assertEquals(Languages.ENGLISH, vm.uiState.value.language)
    }

    @Test
    fun selectPlayerCount_updatesStateAndWritesThrough() {
        val setup = FakeGameSetup(playerCount = 4)
        val vm = GameViewModel(setup)

        vm.selectPlayerCount(3)

        assertEquals(3, vm.uiState.value.playerCount)
        assertEquals(listOf(3), setup.setPlayerCountCalls)
    }

    @Test
    fun selectLanguage_updatesStateAndWritesThrough() {
        val setup = FakeGameSetup(language = Languages.ENGLISH)
        val vm = GameViewModel(setup)

        vm.selectLanguage(Languages.GERMAN)

        assertEquals(Languages.GERMAN, vm.uiState.value.language)
        assertEquals(listOf(Languages.GERMAN), setup.setLanguageCalls)
    }
}
