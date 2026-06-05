package com.nwe.spadesscore.ui.game

import com.nwe.spadesscore.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class GameViewModelTest {

    private fun vm() = GameViewModel(random = Random(0))

    @Test
    fun initialState_isDefault() {
        assertEquals(GameUiState(playerCount = 4, language = Languages.ENGLISH, game = null), vm().uiState.value)
    }

    @Test
    fun selectPlayerCount_updatesState() {
        val vm = vm()
        vm.selectPlayerCount(3)
        assertEquals(3, vm.uiState.value.playerCount)
    }

    @Test
    fun selectLanguage_updatesState() {
        val vm = vm()
        vm.selectLanguage(Languages.GERMAN)
        assertEquals(Languages.GERMAN, vm.uiState.value.language)
    }

    @Test
    fun startGame_fixedDealer_seedsGameWithStartingPlayerZero() {
        val vm = vm()
        vm.selectPlayerCount(4)
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        val game = vm.uiState.value.game!!
        assertEquals(4, game.playerCount)
        assertEquals(listOf("A", "B", "C", "D"), game.playerNames)
        assertEquals(1, game.currentRound)
        assertEquals(0, game.currentPlayer)
        assertEquals(8, game.amountOfRounds)
    }

    @Test
    fun startGame_randomDealer_startingPlayerWithinRange() {
        val vm = vm()
        vm.selectPlayerCount(4)
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = true)
        assertTrue(vm.uiState.value.game!!.currentPlayer in 0 until 4)
    }

    @Test
    fun confirmRound_appendsScoresAndAdvancesRound() {
        val vm = vm()
        vm.selectPlayerCount(3)
        vm.startGame(listOf("A", "B", "C"), randomDealer = false)
        vm.setPredictions(listOf(2, 1, 0))
        vm.confirmRound(listOf(true, false, true))
        val game = vm.uiState.value.game!!
        assertEquals(listOf(0, 7), game.scores[0]) // 0 + 2 + 5
        assertEquals(listOf(0, 0), game.scores[1]) // missed
        assertEquals(listOf(0, 5), game.scores[2]) // 0 + 0 + 5
        assertEquals(2, game.currentRound)
    }

    @Test
    fun confirmRound_lastRoundOfHalf_flagsResultScreen() {
        val vm = vm()
        vm.selectPlayerCount(4) // 8 rounds
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        repeat(8) {
            vm.setPredictions(listOf(0, 0, 0, 0))
            vm.confirmRound(listOf(false, false, false, false))
        }
        assertTrue(vm.uiState.value.game!!.showResultScreen)
        assertFalse(vm.uiState.value.game!!.secondHalf)
    }

    @Test
    fun continueSecondHalf_doublesRoundsAndClearsResultFlag() {
        val vm = vm()
        vm.selectPlayerCount(4)
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        repeat(8) {
            vm.setPredictions(listOf(0, 0, 0, 0))
            vm.confirmRound(listOf(false, false, false, false))
        }
        vm.continueSecondHalf()
        val game = vm.uiState.value.game!!
        assertEquals(16, game.amountOfRounds)
        assertTrue(game.secondHalf)
        assertFalse(game.showResultScreen)
    }

    @Test
    fun isPredictionSumValid_falseWhenSumEqualsPossibleCards() {
        val vm = vm()
        vm.selectPlayerCount(4)
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        // Runde 1, erste Hälfte -> amountOfCards == currentRound == 1
        assertFalse(vm.isPredictionSumValid(1))
        assertTrue(vm.isPredictionSumValid(0))
        assertTrue(vm.isPredictionSumValid(2))
    }

    @Test
    fun newGame_clearsGame() {
        val vm = vm()
        vm.selectPlayerCount(4)
        vm.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        vm.newGame()
        assertNull(vm.uiState.value.game)
    }
}
