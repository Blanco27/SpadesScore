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
}
