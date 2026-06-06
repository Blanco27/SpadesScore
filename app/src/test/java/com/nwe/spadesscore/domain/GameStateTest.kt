package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {

    private fun state(
        currentRound: Int,
        amountOfRounds: Int,
        isSecondHalf: Boolean,
    ) = GameState(
        playerCount = 4,
        players = listOf(
            Player("A", listOf(0)),
            Player("B", listOf(0)),
            Player("C", listOf(0)),
            Player("D", listOf(0)),
        ),
        currentRound = currentRound,
        dealerIndex = 0,
        amountOfRounds = amountOfRounds,
        isSecondHalf = isSecondHalf,
        predictions = listOf(0, 0, 0, 0),
        language = Language.ENGLISH,
    )

    @Test
    fun firstHalf_cardAmount_equalsCurrentRound() {
        assertEquals(1, state(1, 8, false).amountOfCards)
        assertEquals(8, state(8, 8, false).amountOfCards)
    }

    @Test
    fun secondHalf_cardAmount_rampsDown() {
        assertEquals(8, state(9, 16, true).amountOfCards)
        assertEquals(1, state(16, 16, true).amountOfCards)
    }

    @Test
    fun secondHalf_cardAmount_neverBelowOne() {
        assertEquals(1, state(20, 16, true).amountOfCards)
    }

    @Test
    fun isGameOver_whenCurrentRoundExceedsAmountOfRounds() {
        assertFalse(state(8, 8, false).isGameOver)
        assertTrue(state(9, 8, false).isGameOver)
    }

    @Test
    fun isGameOver_secondHalf_atBoundary() {
        assertFalse(state(16, 16, true).isGameOver) // last round, not over yet
        assertTrue(state(17, 16, true).isGameOver)  // one past the end
    }

    @Test
    fun currentDealerName_readsPlayerAtDealerIndex() {
        val s = state(1, 8, false).copy(dealerIndex = 2)
        assertEquals("C", s.currentDealerName)
    }
}
