package com.nwe.spadesscore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpadesEngineTest {

    private val names = listOf("A", "B", "C", "D")

    @Test
    fun newGame_initialisesFirstHalfState() {
        val state = SpadesEngine.newGame(playerCount = 4, playerNames = names, startingPlayer = 2)

        assertEquals(4, state.playerCount)
        assertEquals(names, state.playerNames)
        assertEquals(1, state.currentRound)
        assertEquals(2, state.currentPlayer)
        assertEquals(8, state.amountOfRounds)
        assertFalse(state.secondHalf)
        assertFalse(state.showResultScreen)
        assertEquals(listOf(listOf(0), listOf(0), listOf(0), listOf(0)), state.scores)
    }

    @Test
    fun newGame_threePlayers_hasTenRoundsAndThreeScoreLists() {
        val state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0)

        assertEquals(10, state.amountOfRounds)
        assertEquals(3, state.scores.size)
    }

    @Test
    fun amountOfCards_firstHalf_countsUpWithRound() {
        val state = SpadesEngine.newGame(4, names, 0)

        assertEquals(1, SpadesEngine.amountOfCards(state))
        assertEquals(5, SpadesEngine.amountOfCards(state.copy(currentRound = 5)))
        assertEquals(8, SpadesEngine.amountOfCards(state.copy(currentRound = 8)))
    }

    @Test
    fun confirmTricks_madeAddsPredictionPlusFive_missedRepeatsTotal() {
        var state = SpadesEngine.newGame(4, names, 0)
        state = SpadesEngine.setTickPredictions(state, listOf(3, 2, 1, 0))
        state = SpadesEngine.confirmTricks(state, listOf(true, false, true, false))

        assertEquals(listOf(0, 8), state.scores[0])
        assertEquals(listOf(0, 0), state.scores[1])
        assertEquals(listOf(0, 6), state.scores[2])
        assertEquals(listOf(0, 0), state.scores[3])
        assertEquals(2, state.currentRound)
        assertEquals(1, state.currentPlayer)
        assertFalse(state.showResultScreen)
    }

    @Test
    fun confirmTricks_accumulatesAcrossRounds() {
        var state = SpadesEngine.newGame(4, names, 0)
        state = SpadesEngine.setTickPredictions(state, listOf(1, 1, 1, 1))
        state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))
        state = SpadesEngine.setTickPredictions(state, listOf(2, 2, 2, 2))
        state = SpadesEngine.confirmTricks(state, listOf(true, false, true, false))

        assertEquals(listOf(0, 6, 13), state.scores[0])
        assertEquals(listOf(0, 6, 6), state.scores[1])
        assertEquals(3, state.currentRound)
        assertEquals(2, state.currentPlayer)
    }

    @Test
    fun confirmTricks_threePlayers_ignoresFourthSlot() {
        var state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0)
        state = SpadesEngine.setTickPredictions(state, listOf(2, 2, 2, 2))
        state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))

        assertEquals(3, state.scores.size)
        assertEquals(listOf(0, 7), state.scores[0])
        assertEquals(1, state.currentPlayer)
    }

    @Test
    fun confirmTricks_lastRoundOfHalf_flagsResultScreen() {
        var state = SpadesEngine.newGame(4, names, 0).copy(currentRound = 8)
        state = SpadesEngine.setTickPredictions(state, listOf(0, 0, 0, 0))
        state = SpadesEngine.confirmTricks(state, listOf(false, false, false, false))

        assertEquals(9, state.currentRound)
        assertTrue(state.showResultScreen)
    }

    @Test
    fun randomStartingPlayer_isDeterministicForSeededRng_andInRange() {
        val first = SpadesEngine.randomStartingPlayer(4, java.util.Random(42))
        val second = SpadesEngine.randomStartingPlayer(4, java.util.Random(42))

        assertEquals(first, second)
        assertTrue(first in 0 until 4)
        assertTrue(SpadesEngine.randomStartingPlayer(3, java.util.Random(7)) in 0 until 3)
    }

    @Test
    fun rankingForLastRound_ordersByScoreDescThenIndex() {
        val state = SpadesEngine.newGame(4, names, 0).copy(
            scores = listOf(listOf(0, 20), listOf(0, 25), listOf(0, 20), listOf(0, 10))
        )
        assertEquals(listOf(1, 0, 2, 3), SpadesEngine.rankingForLastRound(state))
    }

    @Test
    fun rankingForLastRound_threePlayers_ignoresFourth() {
        val state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0).copy(
            scores = listOf(listOf(0, 5), listOf(0, 9), listOf(0, 7))
        )
        assertEquals(listOf(1, 2, 0), SpadesEngine.rankingForLastRound(state))
    }

    @Test
    fun startSecondHalf_doublesRoundsAndCountsDown() {
        val firstHalfEnd = SpadesEngine.newGame(4, names, 0).copy(currentRound = 9, showResultScreen = true)
        val second = SpadesEngine.startSecondHalf(firstHalfEnd)

        assertEquals(16, second.amountOfRounds)
        assertTrue(second.secondHalf)
        assertFalse(second.showResultScreen)

        assertEquals(8, SpadesEngine.amountOfCards(second))
        assertEquals(1, SpadesEngine.amountOfCards(second.copy(currentRound = 16)))
        assertEquals(1, SpadesEngine.amountOfCards(second.copy(currentRound = 20)))
    }
}
