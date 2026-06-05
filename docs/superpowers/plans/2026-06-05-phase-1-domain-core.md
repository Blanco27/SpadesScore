# Phase 1: Getesteter Kotlin-Domain-Kern – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die Spielregeln aus dem Java-Singleton `SpadesGame` in einen reinen, immutablen, vollständig unit-getesteten Kotlin-Kern (`GameState` + `SpadesEngine`) herauslösen und `SpadesGame` zum dünnen Adapter machen – ohne sichtbare Verhaltensänderung.

**Architecture:** Reines Kotlin-Domain-Paket `com.nwe.spadesscore.domain` ohne Android-Abhängigkeiten. `GameState` ist eine immutable `data class`; `SpadesEngine` ist ein `object` mit reinen Übergangsfunktionen, die jeweils einen neuen `GameState` zurückgeben. Der bestehende `SpadesGame`-Singleton hält Setup-Eingaben + den aktuellen `GameState` und delegiert alle Regel-Transitionen an die Engine. Die Activities bleiben unangetastet.

**Tech Stack:** Kotlin, JUnit4 (`org.junit`, bereits als `testImplementation(libs.junit)` vorhanden), Android Gradle Wrapper (`.\gradlew.bat`). Keine neuen Dependencies.

---

## Hinweise zur Verhaltensparität

Phase 1 darf das für den Nutzer sichtbare Verhalten **nicht** ändern. Folgende Eigenheiten des aktuellen `SpadesGame` werden **bewusst exakt nachgebildet**:

- Runden/Halbzeit = `32 / playerCount` (Ganzzahl-Division = Floor): 8 bei 4 Spielern, 10 bei 3.
- Karten pro Runde: 1. Halbzeit zählt hoch (`currentRound`), 2. Halbzeit zählt runter (`amountOfRounds - currentRound + 1`), Untergrenze 1.
- Scoring: getroffen → `vorherigerStand + prediction + 5`; verfehlt → vorheriger Stand erneut angehängt.
- Es werden nur die ersten `playerCount` Spieler gewertet (der 4. wird im 3-Spieler-Spiel ignoriert, weil `scores` nur `playerCount` Listen hat).
- Dealer rotiert `(currentPlayer + 1) % playerCount` pro Runde.
- `showResultScreen`, sobald `currentRound > amountOfRounds`.
- Zufalls-Dealer: zufälliger Start in `[0, playerCount)` (jetzt über injizierte `java.util.Random` statt `Math.random()`, damit testbar).

**`rankingForLastRound` (Task 6)** wird in Phase 1 angelegt und getestet, aber **noch nicht** von `ResultScreenActivity` benutzt – die behält ihre eigene Färbe-Logik. Die Verdrahtung erfolgt erst in Phase 3. Das ist Absicht und stellt sicher, dass Phase 1 wirklich verhaltensneutral bleibt.

---

## File Structure

| Datei | Verantwortung |
|------|---------------|
| `app/src/main/java/com/nwe/spadesscore/domain/GameState.kt` | **Neu.** Immutable Snapshot eines laufenden Spiels (reine Daten). |
| `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt` | **Neu.** Reine Regel-Engine: Übergangs- und Ableitungsfunktionen. |
| `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt` | **Neu.** JUnit4-Tests, die die Regeln festnageln. |
| `app/src/main/java/com/nwe/spadesscore/SpadesGame.java` | **Umbau.** Vom Logik-Container zum dünnen Adapter, der an die Engine delegiert. |

Die Activities (`MainActivity`, `PlayerNamesActivity`, `DealCardsActivity`, `DeclareTricksActivity`, `ConfirmTicksActivity`, `ResultScreenActivity`) werden in Phase 1 **nicht** angefasst.

---

## Task 1: `GameState` + `SpadesEngine.newGame`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/domain/GameState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`:

```kotlin
package com.nwe.spadesscore.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SpadesEngineTest {

    private val names = listOf("A", "B", "C", "D")

    @Test
    fun newGame_initialisesFirstHalfState() {
        val state = SpadesEngine.newGame(playerCount = 4, playerNames = names, startingPlayer = 2)

        assertEquals(4, state.playerCount)
        assertEquals(names, state.playerNames)
        assertEquals(1, state.currentRound)
        assertEquals(2, state.currentPlayer)          // starting dealer preserved
        assertEquals(8, state.amountOfRounds)         // 32 / 4
        assertFalse(state.secondHalf)
        assertFalse(state.showResultScreen)
        assertEquals(listOf(listOf(0), listOf(0), listOf(0), listOf(0)), state.scores)
    }

    @Test
    fun newGame_threePlayers_hasTenRoundsAndThreeScoreLists() {
        val state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0)

        assertEquals(10, state.amountOfRounds)        // 32 / 3 = 10
        assertEquals(3, state.scores.size)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: SpadesEngine` / `GameState`.

- [ ] **Step 3: Create `GameState`**

Create `app/src/main/java/com/nwe/spadesscore/domain/GameState.kt`:

```kotlin
package com.nwe.spadesscore.domain

/**
 * Immutable snapshot of an in-progress Spades game.
 *
 * Pure data – no Android dependencies. Produced and transformed only by [SpadesEngine].
 * `scores[p]` is player p's cumulative running total, one entry per completed round,
 * always starting with a single `0` entry representing the pre-game state.
 */
data class GameState(
    val playerCount: Int = 4,
    val playerNames: List<String> = emptyList(),
    val currentRound: Int = 0,
    val currentPlayer: Int = 0,
    val amountOfRounds: Int = 0,
    val secondHalf: Boolean = false,
    val showResultScreen: Boolean = false,
    val scores: List<List<Int>> = emptyList(),
    val tickPredictions: List<Int> = emptyList()
)
```

- [ ] **Step 4: Create `SpadesEngine` with `newGame`**

Create `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`:

```kotlin
package com.nwe.spadesscore.domain

/**
 * Pure game-rule engine for Spades scoring. Every transition takes a [GameState]
 * and returns a new one; no mutation, no Android, fully unit-testable.
 */
object SpadesEngine {

    /** Starts a fresh first half. [startingPlayer] is the round-1 dealer. */
    fun newGame(playerCount: Int, playerNames: List<String>, startingPlayer: Int): GameState =
        GameState(
            playerCount = playerCount,
            playerNames = playerNames,
            currentRound = 1,
            currentPlayer = startingPlayer,
            amountOfRounds = 32 / playerCount,
            secondHalf = false,
            showResultScreen = false,
            scores = List(playerCount) { listOf(0) },
            tickPredictions = emptyList()
        )
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/GameState.kt app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add GameState and SpadesEngine.newGame\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: `amountOfCards` – erste Halbzeit zählt hoch

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `SpadesEngineTest`:

```kotlin
    @Test
    fun amountOfCards_firstHalf_countsUpWithRound() {
        val state = SpadesEngine.newGame(4, names, 0)

        assertEquals(1, SpadesEngine.amountOfCards(state))                       // round 1
        assertEquals(5, SpadesEngine.amountOfCards(state.copy(currentRound = 5)))
        assertEquals(8, SpadesEngine.amountOfCards(state.copy(currentRound = 8)))
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: amountOfCards`.

- [ ] **Step 3: Implement `amountOfCards`**

Add to the `SpadesEngine` object (above the closing brace), importing `kotlin.math.max` at the top of the file:

```kotlin
import kotlin.math.max
```

```kotlin
    /** Cards dealt this round: counts up in the first half, down in the second (min 1). */
    fun amountOfCards(state: GameState): Int =
        if (!state.secondHalf) state.currentRound
        else max(1, state.amountOfRounds - state.currentRound + 1)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add amountOfCards (first-half count-up)\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: `setTickPredictions` + `confirmTricks` (Scoring, Rotation, Result-Flag)

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

- [ ] **Step 1: Write the failing tests**

Add to `SpadesEngineTest` (and add the import `import org.junit.Assert.assertTrue` at the top):

```kotlin
    @Test
    fun confirmTricks_madeAddsPredictionPlusFive_missedRepeatsTotal() {
        var state = SpadesEngine.newGame(4, names, 0)
        state = SpadesEngine.setTickPredictions(state, listOf(3, 2, 1, 0))
        state = SpadesEngine.confirmTricks(state, listOf(true, false, true, false))

        assertEquals(listOf(0, 8), state.scores[0])   // made 3 -> 0 + 3 + 5
        assertEquals(listOf(0, 0), state.scores[1])   // missed -> 0 repeated
        assertEquals(listOf(0, 6), state.scores[2])   // made 1 -> 0 + 1 + 5
        assertEquals(listOf(0, 0), state.scores[3])   // missed -> 0 repeated
        assertEquals(2, state.currentRound)
        assertEquals(1, state.currentPlayer)          // (0 + 1) % 4
        assertFalse(state.showResultScreen)
    }

    @Test
    fun confirmTricks_accumulatesAcrossRounds() {
        var state = SpadesEngine.newGame(4, names, 0)
        state = SpadesEngine.setTickPredictions(state, listOf(1, 1, 1, 1))
        state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))   // each 0 -> 6
        state = SpadesEngine.setTickPredictions(state, listOf(2, 2, 2, 2))
        state = SpadesEngine.confirmTricks(state, listOf(true, false, true, false)) // p0 6 -> 13

        assertEquals(listOf(0, 6, 13), state.scores[0])
        assertEquals(listOf(0, 6, 6), state.scores[1])  // missed second round
        assertEquals(3, state.currentRound)
        assertEquals(2, state.currentPlayer)            // rotated twice from 0
    }

    @Test
    fun confirmTricks_threePlayers_ignoresFourthSlot() {
        var state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0)
        state = SpadesEngine.setTickPredictions(state, listOf(2, 2, 2, 2))
        state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))

        assertEquals(3, state.scores.size)              // no 4th player list exists
        assertEquals(listOf(0, 7), state.scores[0])     // 0 + 2 + 5
        assertEquals(2, state.currentPlayer)            // (0 + 1) % 3
    }

    @Test
    fun confirmTricks_lastRoundOfHalf_flagsResultScreen() {
        var state = SpadesEngine.newGame(4, names, 0).copy(currentRound = 8) // amountOfRounds = 8
        state = SpadesEngine.setTickPredictions(state, listOf(0, 0, 0, 0))
        state = SpadesEngine.confirmTricks(state, listOf(false, false, false, false))

        assertEquals(9, state.currentRound)
        assertTrue(state.showResultScreen)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: setTickPredictions` / `confirmTricks`.

- [ ] **Step 3: Implement `setTickPredictions` and `confirmTricks`**

Add to the `SpadesEngine` object:

```kotlin
    /** Records this round's trick predictions (one per player slot). */
    fun setTickPredictions(state: GameState, predictions: List<Int>): GameState =
        state.copy(tickPredictions = predictions.toList())

    /**
     * Applies the made/missed outcome of the current round: appends each player's new
     * cumulative total, rotates the dealer, advances the round, and flags the result
     * screen once the half is over. Only the first `playerCount` players are scored.
     */
    fun confirmTricks(state: GameState, made: List<Boolean>): GameState {
        val updatedScores = state.scores.mapIndexed { player, history ->
            val previousTotal = history.last()
            val newTotal =
                if (made[player]) previousTotal + state.tickPredictions[player] + 5
                else previousTotal
            history + newTotal
        }
        val nextRound = state.currentRound + 1
        return state.copy(
            scores = updatedScores,
            currentRound = nextRound,
            currentPlayer = (state.currentPlayer + 1) % state.playerCount,
            showResultScreen = nextRound > state.amountOfRounds
        )
    }
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (7 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add setTickPredictions and confirmTricks scoring\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: `startSecondHalf` + zweite Halbzeit zählt runter

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `SpadesEngineTest`:

```kotlin
    @Test
    fun startSecondHalf_doublesRoundsAndCountsDown() {
        val firstHalfEnd = SpadesEngine.newGame(4, names, 0).copy(currentRound = 9, showResultScreen = true)
        val second = SpadesEngine.startSecondHalf(firstHalfEnd)

        assertEquals(16, second.amountOfRounds)   // 8 doubled
        assertTrue(second.secondHalf)
        assertFalse(second.showResultScreen)

        assertEquals(8, SpadesEngine.amountOfCards(second))                        // round 9: 16-9+1
        assertEquals(1, SpadesEngine.amountOfCards(second.copy(currentRound = 16))) // last round
        assertEquals(1, SpadesEngine.amountOfCards(second.copy(currentRound = 20))) // floored at 1
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: startSecondHalf`.

- [ ] **Step 3: Implement `startSecondHalf`**

Add to the `SpadesEngine` object:

```kotlin
    /** Doubles the round budget and switches to the descending second half. */
    fun startSecondHalf(state: GameState): GameState =
        state.copy(
            amountOfRounds = state.amountOfRounds * 2,
            secondHalf = true,
            showResultScreen = false
        )
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (8 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add startSecondHalf (round doubling, count-down)\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: `randomStartingPlayer` (injizierbare RNG)

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

- [ ] **Step 1: Write the failing test**

Add to `SpadesEngineTest`:

```kotlin
    @Test
    fun randomStartingPlayer_isDeterministicForSeededRng_andInRange() {
        val first = SpadesEngine.randomStartingPlayer(4, java.util.Random(42))
        val second = SpadesEngine.randomStartingPlayer(4, java.util.Random(42))

        assertEquals(first, second)                 // same seed -> same result
        assertTrue(first in 0 until 4)              // within range
        assertTrue(SpadesEngine.randomStartingPlayer(3, java.util.Random(7)) in 0 until 3)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: randomStartingPlayer`.

- [ ] **Step 3: Implement `randomStartingPlayer`**

Add the import at the top of `SpadesEngine.kt`:

```kotlin
import java.util.Random
```

Add to the `SpadesEngine` object:

```kotlin
    /** Random round-1 dealer in [0, playerCount). RNG is injected for deterministic tests. */
    fun randomStartingPlayer(playerCount: Int, rng: Random): Int = rng.nextInt(playerCount)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (9 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add randomStartingPlayer with injectable RNG\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: `rankingForLastRound` (für spätere Result-Färbung, noch nicht verdrahtet)

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

> Diese Funktion wird in Phase 1 **nur** angelegt und getestet. `ResultScreenActivity` benutzt sie noch nicht – das passiert erst in Phase 3. Reihenfolge: nach letztem Score absteigend, Gleichstand nach Spieler-Index aufsteigend.

- [ ] **Step 1: Write the failing test**

Add to `SpadesEngineTest`:

```kotlin
    @Test
    fun rankingForLastRound_ordersByScoreDescThenIndex() {
        val state = SpadesEngine.newGame(4, names, 0).copy(
            scores = listOf(listOf(0, 20), listOf(0, 25), listOf(0, 20), listOf(0, 10))
        )
        // p1=25, then p0=20 and p2=20 (tie broken by index -> 0 before 2), then p3=10
        assertEquals(listOf(1, 0, 2, 3), SpadesEngine.rankingForLastRound(state))
    }

    @Test
    fun rankingForLastRound_threePlayers_ignoresFourth() {
        val state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0).copy(
            scores = listOf(listOf(0, 5), listOf(0, 9), listOf(0, 7))
        )
        assertEquals(listOf(1, 2, 0), SpadesEngine.rankingForLastRound(state))
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: FAILS to compile – `unresolved reference: rankingForLastRound`.

- [ ] **Step 3: Implement `rankingForLastRound`**

Add to the `SpadesEngine` object:

```kotlin
    /**
     * Player indices ranked by their latest cumulative score, highest first.
     * Ties are broken by player index (ascending). Used to colour the final standings.
     */
    fun rankingForLastRound(state: GameState): List<Int> =
        (0 until state.playerCount).sortedWith(
            compareByDescending<Int> { state.scores[it].last() }.thenBy { it }
        )
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (11 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/domain/SpadesEngine.kt app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'feat(domain): add rankingForLastRound (not yet wired to UI)\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: Vollspiel-Simulation (Integrationstests 3p & 4p)

**Files:**
- Test: `app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt`

> Reine Tests, kein Produktivcode. Sie pinnen das End-to-End-Verhalten beider Halbzeiten fest.

- [ ] **Step 1: Write the failing tests**

Add to `SpadesEngineTest`:

```kotlin
    @Test
    fun fullFourPlayerGame_runsBothHalvesAndReachesResultScreen() {
        var state = SpadesEngine.newGame(4, names, 0)

        for (round in 1..8) {
            assertEquals(round, SpadesEngine.amountOfCards(state))          // counts up
            state = SpadesEngine.setTickPredictions(state, listOf(0, 0, 0, 0))
            state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))
        }
        assertTrue(state.showResultScreen)
        assertEquals(9, state.currentRound)
        assertEquals(9, state.scores[0].size)                              // 1 initial + 8 rounds

        state = SpadesEngine.startSecondHalf(state)
        assertEquals(16, state.amountOfRounds)

        for (round in 9..16) {
            assertEquals(16 - round + 1, SpadesEngine.amountOfCards(state)) // counts down
            state = SpadesEngine.setTickPredictions(state, listOf(0, 0, 0, 0))
            state = SpadesEngine.confirmTricks(state, listOf(false, false, false, false))
        }
        assertTrue(state.showResultScreen)
        assertEquals(17, state.currentRound)
        assertEquals(17, state.scores[0].size)                             // 1 + 16 rounds
    }

    @Test
    fun fullThreePlayerGame_hasTenRoundsPerHalf() {
        var state = SpadesEngine.newGame(3, listOf("A", "B", "C"), 0)
        assertEquals(10, state.amountOfRounds)

        for (round in 1..10) {
            state = SpadesEngine.setTickPredictions(state, listOf(1, 1, 1, 1))
            state = SpadesEngine.confirmTricks(state, listOf(true, true, true, true))
        }
        assertTrue(state.showResultScreen)

        state = SpadesEngine.startSecondHalf(state)
        assertEquals(20, state.amountOfRounds)
        assertEquals(10, SpadesEngine.amountOfCards(state.copy(currentRound = 11))) // 20-11+1
    }
```

- [ ] **Step 2: Run tests to verify they pass**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.domain.SpadesEngineTest"`
Expected: PASS (13 tests). These compile and pass immediately because every function they use already exists – they document end-to-end behaviour.

- [ ] **Step 3: Commit**

```bash
git add app/src/test/java/com/nwe/spadesscore/domain/SpadesEngineTest.kt
git commit -m "$(printf 'test(domain): add full-game simulations for 3 and 4 players\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 8: `SpadesGame` zum Adapter umbauen (delegiert an die Engine)

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/SpadesGame.java` (komplett ersetzen)

> Kein neuer Test – diese Aufgabe ist ein verhaltenserhaltender Umbau. Verifikation: alle Engine-Tests bleiben grün **und** die App kompiliert weiter. Die öffentliche API von `SpadesGame` bleibt identisch, deshalb sind keine Activity-Änderungen nötig.

- [ ] **Step 1: Replace the contents of `SpadesGame.java`**

Replace the **entire** file `app/src/main/java/com/nwe/spadesscore/SpadesGame.java` with:

```java
package com.nwe.spadesscore;

import android.content.Context;

import com.nwe.spadesscore.domain.GameState;
import com.nwe.spadesscore.domain.SpadesEngine;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Thin singleton adapter around the pure {@link SpadesEngine} / {@link GameState} core.
 * <p>
 * Holds the setup inputs (player count, names, starting dealer, language) plus the current
 * immutable {@link GameState}, and exposes the same API the Activities already use. All
 * game-rule transitions delegate to {@link SpadesEngine}; this class only adapts types and
 * formats Context-dependent strings.
 * <p>
 * There is still no persistence – state survives only as long as the process.
 */
public class SpadesGame {
    private static SpadesGame instance;

    // Setup inputs (set before the game starts)
    private int playerCount = 4;
    private String[] playerNames;
    private int startingPlayer = 0;
    private Languages languages;

    // Immutable in-progress game state (null until startGame())
    private GameState state;

    private final Random random = new Random();

    private SpadesGame() {
    }

    public static SpadesGame getInstance() {
        if (instance == null) {
            instance = new SpadesGame();
        }
        return instance;
    }

    public void startGame() {
        final List<String> names = new ArrayList<>();
        for (int i = 0; i < playerCount; i++) {
            names.add(playerNames[i]);
        }
        state = SpadesEngine.INSTANCE.newGame(playerCount, names, startingPlayer);
    }

    public void startSecondHalf() {
        state = SpadesEngine.INSTANCE.startSecondHalf(state);
    }

    public void setTickPredictions(final int... values) {
        final List<Integer> predictions = new ArrayList<>();
        for (final int value : values) {
            predictions.add(value);
        }
        state = SpadesEngine.INSTANCE.setTickPredictions(state, predictions);
    }

    public void confirmTickPredictions(final boolean... values) {
        final List<Boolean> made = new ArrayList<>();
        for (final boolean value : values) {
            made.add(value);
        }
        state = SpadesEngine.INSTANCE.confirmTricks(state, made);
    }

    public void setPlayerCount(final int playerCount) {
        this.playerCount = playerCount;
    }

    public void setPlayerNames(String... playerNames) {
        this.playerNames = playerNames;
    }

    public void setRandomDealer(boolean checked) {
        startingPlayer = checked ? SpadesEngine.INSTANCE.randomStartingPlayer(playerCount, random) : 0;
    }

    public LinkedList<Integer> getScoreListForPlayer(final int playerNumber) {
        return new LinkedList<>(state.getScores().get(playerNumber));
    }

    public int getCurrentRound() {
        return state.getCurrentRound();
    }

    public String getPlayerName(final int playerNumber) {
        return playerNames[playerNumber];
    }

    public String getLastScoreAndPlayerNameAsString(final int playerNumber) {
        final List<Integer> playerScores = state.getScores().get(playerNumber);
        final int last = playerScores.get(playerScores.size() - 1);
        return getPlayerName(playerNumber) + ": " + last;
    }

    public String getPlayerTricksString(Context context, final int playerNumber) {
        final int prediction = state.getTickPredictions().get(playerNumber);
        final String tricks = prediction == 1 ? context.getString(R.string.trick) : context.getString(R.string.tricks);
        return String.format(Locale.getDefault(), "%d %s", prediction, tricks);
    }

    public String getPlayerPointsString(Context context, final int playerNumber) {
        final int prediction = state.getTickPredictions().get(playerNumber);
        final int points = prediction + 5;
        return String.format(Locale.getDefault(), context.getString(R.string.points_added), points);
    }

    public int getAmountOfCards() {
        return SpadesEngine.INSTANCE.amountOfCards(state);
    }

    public boolean isSecondHalfOfTheGame() {
        return state.getSecondHalf();
    }

    public boolean isShowResultScreen() {
        return state.getShowResultScreen();
    }

    public String getCurrentRoundString(Context context) {
        return String.format(Locale.getDefault(), context.getString(R.string.round), state.getCurrentRound());
    }

    public int getPlayerCount() {
        return playerCount;
    }

    public String getCurrentPlayerName() {
        return getPlayerName(state.getCurrentPlayer());
    }

    public void setLanguages(Languages languages) {
        this.languages = languages;
    }

    public Languages getLanguages() {
        return languages;
    }

    public String getCombinedTrickPredictionString(DeclareTricksActivity declareTricksActivity, int tricks1, int tricks2, int tricks3, int tricks4) {
        final int combinedTricks = tricks1 + tricks2 + tricks3 + (playerCount == 4 ? tricks4 : 0);
        final int possibleTricks = getAmountOfCards();
        return String.format(Locale.getDefault(), declareTricksActivity.getString(R.string.combined_trick_prediction), combinedTricks, possibleTricks, declareTricksActivity.getString(R.string.tricks));
    }

    public int getAmountOfRounds() {
        return state.getAmountOfRounds();
    }
}
```

- [ ] **Step 2: Run the full unit-test suite**

Run: `.\gradlew.bat test`
Expected: PASS – all 13 `SpadesEngineTest` tests plus the existing `ExampleUnitTest` are green.

- [ ] **Step 3: Verify the app still compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. No Activity changes were needed because `SpadesGame`'s public method signatures are unchanged.

- [ ] **Step 4: Manual smoke check (optional but recommended)**

Run: `.\gradlew.bat installDebug` on a connected device/emulator and play one short game (3 players and 4 players): start a game, declare tricks, confirm them across a full half, and confirm the result screen shows the same scores, ordering, and gold/silver/bronze colouring as before. Behaviour must be identical.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/SpadesGame.java
git commit -m "$(printf 'refactor: make SpadesGame a thin adapter over SpadesEngine\n\nThe singleton now holds setup inputs plus an immutable GameState and\ndelegates every rule transition to the pure Kotlin engine. Public API\nand user-visible behaviour are unchanged.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Definition of Done (Phase 1)

- [ ] `GameState` und `SpadesEngine` liegen unter `com.nwe.spadesscore.domain`, **ohne** Android-Imports.
- [ ] `.\gradlew.bat test` ist grün (13 Engine-Tests + Beispieltest).
- [ ] `.\gradlew.bat assembleDebug` baut erfolgreich; keine Activity wurde geändert.
- [ ] Verhalten ist für den Nutzer unverändert (manuell stichprobenartig geprüft, 3p und 4p).
- [ ] Jede Engine-Funktion aus dem Spec (Abschnitt 6.1/6.2) ist implementiert und getestet; `rankingForLastRound` existiert, ist aber noch nicht mit der UI verdrahtet (Phase 3).
