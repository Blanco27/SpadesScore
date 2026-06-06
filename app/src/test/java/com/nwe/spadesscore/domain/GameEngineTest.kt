package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class GameEngineTest {

    @Test
    fun startGame_fourPlayers_hasEightRounds() {
        val s = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
        assertEquals(4, s.playerCount)
        assertEquals(8, s.amountOfRounds)
        assertEquals(1, s.currentRound)
        assertEquals(0, s.dealerIndex)
        assertEquals(listOf(0, 0, 0, 0), s.predictions)
        assertTrue(s.players.all { it.scores == listOf(0) })
    }

    @Test
    fun startGame_threePlayers_hasTenRounds() {
        val s = GameEngine.startGame(listOf("A", "B", "C"), randomDealer = false, language = Language.ENGLISH)
        assertEquals(3, s.playerCount)
        assertEquals(10, s.amountOfRounds)
        assertEquals(listOf(0, 0, 0), s.predictions)
    }

    @Test
    fun startGame_randomDealer_isWithinPlayerRange() {
        repeat(50) { seed ->
            val s = GameEngine.startGame(
                listOf("A", "B", "C", "D"),
                randomDealer = true,
                language = Language.ENGLISH,
                random = Random(seed),
            )
            assertTrue(s.dealerIndex in 0..3)
        }
    }

    @Test
    fun confirmTricks_hit_addsPredictionPlusFive() {
        val start = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
            .copy(predictions = listOf(3, 2, 0, 1))
        val next = GameEngine.confirmTricks(start, listOf(true, false, true, false))
        assertEquals(listOf(0, 8), next.players[0].scores) // 0 + 3 + 5
        assertEquals(listOf(0, 0), next.players[1].scores) // miss -> unchanged
        assertEquals(listOf(0, 5), next.players[2].scores) // 0 + 0 + 5
        assertEquals(listOf(0, 0), next.players[3].scores) // miss -> unchanged
    }

    @Test
    fun confirmTricks_incrementsRoundAndRotatesDealer() {
        val start = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
        val next = GameEngine.confirmTricks(start, listOf(false, false, false, false))
        assertEquals(2, next.currentRound)
        assertEquals(1, next.dealerIndex)
    }

    @Test
    fun confirmTricks_dealerRotationWrapsAround() {
        val start = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
            .copy(dealerIndex = 3)
        val next = GameEngine.confirmTricks(start, listOf(false, false, false, false))
        assertEquals(0, next.dealerIndex)
    }

    @Test
    fun confirmTricks_scoresAreCumulative() {
        var s: GameState = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
        s = GameEngine.confirmTricks(s.copy(predictions = listOf(1, 0, 0, 0)), listOf(true, false, false, false))
        s = GameEngine.confirmTricks(s.copy(predictions = listOf(2, 0, 0, 0)), listOf(true, false, false, false))
        assertEquals(listOf(0, 6, 13), s.players[0].scores) // 0 -> +6 -> +7
    }

    @Test
    fun startSecondHalf_doublesRoundsAndSetsFlag() {
        val start = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
        val half = GameEngine.startSecondHalf(start)
        assertEquals(16, half.amountOfRounds)
        assertTrue(half.isSecondHalf)
    }

    @Test
    fun placementByPlayerIndex_ranksByScoreDescending() {
        val s = GameEngine.startGame(listOf("A", "B", "C", "D"), randomDealer = false, language = Language.ENGLISH)
            .copy(
                players = listOf(
                    com.nwe.spadesscore.domain.model.Player("A", listOf(0, 10)),
                    com.nwe.spadesscore.domain.model.Player("B", listOf(0, 30)),
                    com.nwe.spadesscore.domain.model.Player("C", listOf(0, 20)),
                    com.nwe.spadesscore.domain.model.Player("D", listOf(0, 5)),
                ),
            )
        val places = GameEngine.placementByPlayerIndex(s)
        assertEquals(1, places[1]) // B höchster
        assertEquals(2, places[2]) // C
        assertEquals(3, places[0]) // A
        assertEquals(4, places[3]) // D
    }
}
