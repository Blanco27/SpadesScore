# Kotlin + MVVM + Room Backend-Refactor — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Das gesamte Backend von SpadesScore auf idiomatisches Kotlin mit MVVM-Architektur und Room-Persistenz umstellen, ohne UI oder beobachtbares Verhalten zu verändern (Ausnahmen: drei freigegebene Verbesserungen).

**Architecture:** Drei Schichten — `domain` (reines Kotlin, kein Android: `GameState`, `GameEngine`, Regeln, Validierung), `data` (Room + `GameRepository` als Single Source of Truth mit `StateFlow<GameState>`), und UI (Activities + ViewModels + UiStates) im Root-Package. Manuelles DI über `AppContainer` in `SpadesApplication`. Der bisherige statische `SpadesGame`-Singleton entfällt.

**Tech Stack:** Kotlin 2.2.21, Android Views + viewBinding, AndroidX Lifecycle ViewModel + StateFlow, Room 2.8.4 (via KSP 2.2.21-2.0.4), JUnit4.

**Spec:** `docs/superpowers/specs/2026-06-06-backend-refactor-kotlin-mvvm-design.md`

**Branch:** `refactor/kotlin-mvvm` (bereits aktiv)

---

## Abweichungen von der Spec (bewusst, dokumentiert)

1. **UI-Klassen bleiben im Root-Package** `com.nwe.spadesscore` (Activities, Basisklasse, `TrickSpinner`). Nur `domain/` und `data/` erhalten eigene Packages; ViewModels/UiStates unter `ui/<screen>/`. Grund: gegenseitige `Intent`-Verweise der Activities würden bei paketweiser Einzelmigration brechen. Architektur-Intention bleibt erhalten.
2. **Activity-Migration: „alle konvertieren, dann E2E"** — Singleton und Repository können nicht gleichzeitig Quelle der Wahrheit sein. Jede Zwischenstufe kompiliert + Unit-Tests laufen; durchgängiger Spieltest am Ende von Phase 6.

## Drei freigegebene Verhaltensänderungen (alles andere bleibt 1:1)
1. Laufendes Spiel überlebt Prozess-Tod (Room).
2. Kein Debug-Prefill der Namensfelder mehr.
3. Warn-Dialoge werden lokalisiert (DE im Deutsch-Modus).

## Test- & Build-Befehle (Windows / PowerShell)
- Unit-Tests: `.\gradlew.bat testDebugUnitTest`
- Einzelner Test: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameEngineTest"`
- Debug-Build: `.\gradlew.bat assembleDebug`
- Instrumentierte Tests (Gerät nötig): `.\gradlew.bat connectedDebugAndroidTest`

---

## Phase 0: Baseline

### Task 0: Ausgangs-Build verifizieren

**Files:** keine

- [ ] **Step 1: Branch prüfen**

Run: `git branch --show-current`
Expected: `refactor/kotlin-mvvm`

- [ ] **Step 2: Sauberen Baseline-Build sicherstellen**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Falls nicht, zuerst die bestehende Fehlerquelle klären, bevor refactored wird.

---

## Phase 1: Build-Konfiguration (Compose raus, Room/KSP/Lifecycle rein)

### Task 1.1: Version-Catalog aktualisieren

**Files:**
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Datei vollständig durch folgenden Inhalt ersetzen**

```toml
[versions]
agp = "8.13.1"
kotlin = "2.2.21"
ksp = "2.2.21-2.0.4"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
lifecycleRuntimeKtx = "2.10.0"
activityKtx = "1.12.0"
appcompat = "1.7.1"
material = "1.13.0"
recyclerview = "1.4.0"
fragment = "1.8.9"
constraintlayout = "2.2.1"
room = "2.8.4"
coroutines = "1.10.2"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-lifecycle-runtime-ktx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-viewmodel-ktx = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-ktx", version.ref = "lifecycleRuntimeKtx" }
androidx-activity-ktx = { group = "androidx.activity", name = "activity-ktx", version.ref = "activityKtx" }
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
androidx-fragment = { group = "androidx.fragment", name = "fragment", version.ref = "fragment" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutines" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

Hinweis: Der gesamte Compose-Block (`kotlin-compose`-Plugin, `activityCompose`, `composeBom`, alle `androidx-ui*`-, `androidx-material3`-Einträge) wurde entfernt.

### Task 1.2: Root-`build.gradle.kts` bereinigen

**Files:**
- Modify: `build.gradle.kts`

- [ ] **Step 1: Datei vollständig ersetzen**

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}
```

### Task 1.3: App-`build.gradle.kts` umstellen

**Files:**
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Datei vollständig ersetzen**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nwe.spadesscore"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.nwe.spadesscore"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
```

### Task 1.4: Vestigiale Compose-Theme-Dateien löschen

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt`
- Delete: `app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt`
- Delete: `app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt`

- [ ] **Step 1: Die drei Dateien löschen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt`
Expected: drei `rm`-Bestätigungen. (Diese Dateien sind reine Compose-Reste, werden zur Laufzeit nie verwendet.)

### Task 1.5: Build verifizieren

- [ ] **Step 1: Vollen Build laufen lassen**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Der bestehende Java-Code (Singleton + Activities) ist unverändert und baut weiterhin.

- [ ] **Step 2: Commit**

```bash
git add -A
git commit -m "build: drop vestigial Compose, add Room/KSP/Lifecycle deps"
```

---

## Phase 2: Domain-Layer (reines Kotlin, TDD)

Alle Dateien unter `app/src/main/java/com/nwe/spadesscore/domain/`. **Kein** `android.*`- oder `R.*`-Import erlaubt.

### Task 2.1: Language-Enum

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/domain/model/Language.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.domain.model

enum class Language {
    ENGLISH,
    GERMAN
}
```

- [ ] **Step 2: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

### Task 2.2: GameRules-Konstanten

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/domain/GameRules.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.domain

/** Reine Spielregel-Konstanten (keine UI-/Präsentationswerte). */
object GameRules {
    /** Gesamtzahl Karten, geteilt durch Spielerzahl ergibt die Rundenzahl je Halbzeit. */
    const val TOTAL_CARDS = 32

    /** Bonus auf den Score, wenn ein Spieler seine Vorhersage trifft. */
    const val HIT_BONUS = 5

    /** Maximale Länge eines Spielernamens. */
    const val MAX_NAME_LENGTH = 10
}
```

### Task 2.3: Player- und GameState-Modelle (Test zuerst)

**Files:**
- Create: `app/src/test/java/com/nwe/spadesscore/domain/GameStateTest.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/domain/model/Player.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/domain/model/GameState.kt`

- [ ] **Step 1: Failing test schreiben**

```kotlin
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
    fun currentDealerName_readsPlayerAtDealerIndex() {
        val s = state(1, 8, false).copy(dealerIndex = 2)
        assertEquals("C", s.currentDealerName)
    }
}
```

- [ ] **Step 2: Test ausführen, Fehlschlag bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameStateTest"`
Expected: FAIL — Kompilierfehler, weil `Player`/`GameState` noch nicht existieren.

- [ ] **Step 3: Player anlegen**

```kotlin
package com.nwe.spadesscore.domain.model

/** Ein Spieler mit Namen und kumulativer Score-Historie (Index 0 = Startwert 0). */
data class Player(
    val name: String,
    val scores: List<Int>,
) {
    val currentScore: Int get() = scores.last()
}
```

- [ ] **Step 4: GameState anlegen**

```kotlin
package com.nwe.spadesscore.domain.model

/**
 * Unveränderlicher Gesamtzustand eines Spiels. Reine Daten + abgeleitete Werte,
 * keine Android-Abhängigkeiten.
 */
data class GameState(
    val playerCount: Int = 4,
    val players: List<Player> = emptyList(),
    val currentRound: Int = 1,
    val dealerIndex: Int = 0,
    val amountOfRounds: Int = 0,
    val isSecondHalf: Boolean = false,
    val predictions: List<Int> = emptyList(),
    val language: Language = Language.ENGLISH,
) {
    /** Karten in der aktuellen Runde: 1. Halbzeit Hochrampe, 2. Halbzeit Runterrampe (min. 1). */
    val amountOfCards: Int
        get() = if (!isSecondHalf) currentRound else maxOf(1, amountOfRounds - currentRound + 1)

    /** True, sobald die aktuelle Runde die Rundenzahl überschreitet (löst Ergebnisbildschirm aus). */
    val isGameOver: Boolean
        get() = currentRound > amountOfRounds

    val currentDealerName: String
        get() = players[dealerIndex].name
}
```

- [ ] **Step 5: Test ausführen, Erfolg bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameStateTest"`
Expected: PASS (5 Tests)

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(domain): add Language, GameRules, Player, GameState with tests"
```

### Task 2.4: NameValidation (Test zuerst)

**Files:**
- Create: `app/src/test/java/com/nwe/spadesscore/domain/NameValidationTest.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/domain/NameValidation.kt`

- [ ] **Step 1: Failing test schreiben**

```kotlin
package com.nwe.spadesscore.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class NameValidationTest {

    @Test
    fun allValid_returnsOk() {
        assertEquals(NameValidationResult.Ok, NameValidation.validate(listOf("Anna", "Ben", "Cara")))
    }

    @Test
    fun blankName_returnsEmpty() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("Anna", "   ", "Cara")))
    }

    @Test
    fun emptyName_returnsEmpty() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("Anna", "", "Cara")))
    }

    @Test
    fun tooLongName_returnsTooLong() {
        assertEquals(NameValidationResult.TooLong, NameValidation.validate(listOf("Anna", "ElevenChars", "Cara")))
    }

    @Test
    fun emptyTakesPrecedenceOverTooLong() {
        assertEquals(NameValidationResult.Empty, NameValidation.validate(listOf("", "ElevenChars")))
    }
}
```

Hinweis: "ElevenChars" hat 11 Zeichen (> `MAX_NAME_LENGTH` 10).

- [ ] **Step 2: Test ausführen, Fehlschlag bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.NameValidationTest"`
Expected: FAIL — `NameValidation`/`NameValidationResult` existieren nicht.

- [ ] **Step 3: NameValidation implementieren**

```kotlin
package com.nwe.spadesscore.domain

sealed interface NameValidationResult {
    data object Ok : NameValidationResult
    data object Empty : NameValidationResult
    data object TooLong : NameValidationResult
}

/** Pure Validierung der Spielernamen. Reihenfolge: leer hat Vorrang vor zu lang. */
object NameValidation {
    fun validate(names: List<String>): NameValidationResult = when {
        names.any { it.trim().isEmpty() } -> NameValidationResult.Empty
        names.any { it.length > GameRules.MAX_NAME_LENGTH } -> NameValidationResult.TooLong
        else -> NameValidationResult.Ok
    }
}
```

- [ ] **Step 4: Test ausführen, Erfolg bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.NameValidationTest"`
Expected: PASS (5 Tests)

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "feat(domain): add NameValidation with tests"
```

### Task 2.5: GameEngine (Test zuerst)

**Files:**
- Create: `app/src/test/java/com/nwe/spadesscore/domain/GameEngineTest.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/domain/GameEngine.kt`

- [ ] **Step 1: Failing test schreiben**

```kotlin
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
```

- [ ] **Step 2: Test ausführen, Fehlschlag bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameEngineTest"`
Expected: FAIL — `GameEngine` existiert nicht.

- [ ] **Step 3: GameEngine implementieren**

```kotlin
package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.Player
import kotlin.math.floor
import kotlin.random.Random

/** Reine, zustandslose Transitionen. Jede Funktion liefert einen neuen [GameState]. */
object GameEngine {

    fun startGame(
        playerNames: List<String>,
        randomDealer: Boolean,
        language: Language,
        random: Random = Random.Default,
    ): GameState {
        val playerCount = playerNames.size
        return GameState(
            playerCount = playerCount,
            players = playerNames.map { Player(name = it, scores = listOf(0)) },
            currentRound = 1,
            dealerIndex = if (randomDealer) random.nextInt(playerCount) else 0,
            amountOfRounds = floor(GameRules.TOTAL_CARDS.toFloat() / playerCount).toInt(),
            isSecondHalf = false,
            predictions = List(playerCount) { 0 },
            language = language,
        )
    }

    fun confirmTricks(state: GameState, hits: List<Boolean>): GameState {
        val updatedPlayers = state.players.mapIndexed { index, player ->
            val last = player.scores.last()
            val newScore = if (hits[index]) last + state.predictions[index] + GameRules.HIT_BONUS else last
            player.copy(scores = player.scores + newScore)
        }
        return state.copy(
            players = updatedPlayers,
            currentRound = state.currentRound + 1,
            dealerIndex = (state.dealerIndex + 1) % state.playerCount,
        )
    }

    fun startSecondHalf(state: GameState): GameState =
        state.copy(amountOfRounds = state.amountOfRounds * 2, isSecondHalf = true)

    /** Spieler-Index -> Platz (1 = höchster Score). Bildet die Reihenfolge des alten Codes nach. */
    fun placementByPlayerIndex(state: GameState): Map<Int, Int> =
        state.players.indices
            .map { it to state.players[it].currentScore }
            .sortedBy { it.second }
            .reversed()
            .mapIndexed { place, (playerIndex, _) -> playerIndex to place + 1 }
            .toMap()
}
```

- [ ] **Step 4: Test ausführen, Erfolg bestätigen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameEngineTest"`
Expected: PASS (9 Tests)

- [ ] **Step 5: Gesamte Unit-Test-Suite laufen lassen**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS (alle Domain-Tests; der alte `ExampleUnitTest` läuft weiterhin mit).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "feat(domain): add GameEngine (transitions + ranking) with tests"
```

---

## Phase 3: Data-Layer (Room + Repository)

Alle Dateien unter `app/src/main/java/com/nwe/spadesscore/data/`. Genau **ein** laufendes Spiel wird persistiert (feste `id = 1`). Speicherstrategie: bei jeder Mutation alle Zeilen löschen und neu schreiben (winzige Datenmenge, einfach & korrekt).

### Task 3.1: Room-Entities

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/GameEntity.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/PlayerEntity.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/ScoreEntity.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/PredictionEntity.kt`

- [ ] **Step 1: GameEntity anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey val id: Int = 1,
    val playerCount: Int,
    val currentRound: Int,
    val dealerIndex: Int,
    val amountOfRounds: Int,
    val isSecondHalf: Boolean,
    val language: String,
)
```

- [ ] **Step 2: PlayerEntity anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "player")
data class PlayerEntity(
    @PrimaryKey val playerIndex: Int,
    val name: String,
)
```

- [ ] **Step 3: ScoreEntity anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "score")
data class ScoreEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Int = 0,
    val playerIndex: Int,
    val roundIndex: Int,
    val cumulativeScore: Int,
)
```

- [ ] **Step 4: PredictionEntity anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction")
data class PredictionEntity(
    @PrimaryKey val playerIndex: Int,
    val value: Int,
)
```

### Task 3.2: GameDao + Snapshot

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/GameSnapshot.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/GameDao.kt`

- [ ] **Step 1: GameSnapshot anlegen** (reiner Transport, keine Entity)

```kotlin
package com.nwe.spadesscore.data.db

data class GameSnapshot(
    val game: GameEntity,
    val players: List<PlayerEntity>,
    val scores: List<ScoreEntity>,
    val predictions: List<PredictionEntity>,
)
```

- [ ] **Step 2: GameDao anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface GameDao {

    @Query("SELECT * FROM game WHERE id = 1")
    suspend fun getGame(): GameEntity?

    @Query("SELECT * FROM player ORDER BY playerIndex")
    suspend fun getPlayers(): List<PlayerEntity>

    @Query("SELECT * FROM score ORDER BY playerIndex, roundIndex")
    suspend fun getScores(): List<ScoreEntity>

    @Query("SELECT * FROM prediction ORDER BY playerIndex")
    suspend fun getPredictions(): List<PredictionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    @Insert
    suspend fun insertPlayers(players: List<PlayerEntity>)

    @Insert
    suspend fun insertScores(scores: List<ScoreEntity>)

    @Insert
    suspend fun insertPredictions(predictions: List<PredictionEntity>)

    @Query("DELETE FROM game")
    suspend fun clearGame()

    @Query("DELETE FROM player")
    suspend fun clearPlayers()

    @Query("DELETE FROM score")
    suspend fun clearScores()

    @Query("DELETE FROM prediction")
    suspend fun clearPredictions()

    @Transaction
    suspend fun saveGame(
        game: GameEntity,
        players: List<PlayerEntity>,
        scores: List<ScoreEntity>,
        predictions: List<PredictionEntity>,
    ) {
        clearScores()
        clearPredictions()
        clearPlayers()
        clearGame()
        insertGame(game)
        insertPlayers(players)
        insertScores(scores)
        insertPredictions(predictions)
    }

    @Transaction
    suspend fun loadGame(): GameSnapshot? {
        val game = getGame() ?: return null
        return GameSnapshot(game, getPlayers(), getScores(), getPredictions())
    }
}
```

### Task 3.3: AppDatabase

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/db/AppDatabase.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [GameEntity::class, PlayerEntity::class, ScoreEntity::class, PredictionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameDao(): GameDao
}
```

### Task 3.4: Mapper

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/mapper/GameMapper.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.data.mapper

import com.nwe.spadesscore.data.db.GameEntity
import com.nwe.spadesscore.data.db.GameSnapshot
import com.nwe.spadesscore.data.db.PlayerEntity
import com.nwe.spadesscore.data.db.PredictionEntity
import com.nwe.spadesscore.data.db.ScoreEntity
import com.nwe.spadesscore.domain.model.GameState
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.Player

data class GameEntities(
    val game: GameEntity,
    val players: List<PlayerEntity>,
    val scores: List<ScoreEntity>,
    val predictions: List<PredictionEntity>,
)

object GameMapper {

    fun toEntities(state: GameState): GameEntities {
        val game = GameEntity(
            id = 1,
            playerCount = state.playerCount,
            currentRound = state.currentRound,
            dealerIndex = state.dealerIndex,
            amountOfRounds = state.amountOfRounds,
            isSecondHalf = state.isSecondHalf,
            language = state.language.name,
        )
        val players = state.players.mapIndexed { index, player ->
            PlayerEntity(playerIndex = index, name = player.name)
        }
        val scores = state.players.flatMapIndexed { playerIndex, player ->
            player.scores.mapIndexed { roundIndex, score ->
                ScoreEntity(playerIndex = playerIndex, roundIndex = roundIndex, cumulativeScore = score)
            }
        }
        val predictions = state.predictions.mapIndexed { index, value ->
            PredictionEntity(playerIndex = index, value = value)
        }
        return GameEntities(game, players, scores, predictions)
    }

    fun toDomain(snapshot: GameSnapshot): GameState {
        val game = snapshot.game
        val players = snapshot.players
            .sortedBy { it.playerIndex }
            .map { playerEntity ->
                val scores = snapshot.scores
                    .filter { it.playerIndex == playerEntity.playerIndex }
                    .sortedBy { it.roundIndex }
                    .map { it.cumulativeScore }
                Player(name = playerEntity.name, scores = scores)
            }
        val predictions = snapshot.predictions.sortedBy { it.playerIndex }.map { it.value }
        return GameState(
            playerCount = game.playerCount,
            players = players,
            currentRound = game.currentRound,
            dealerIndex = game.dealerIndex,
            amountOfRounds = game.amountOfRounds,
            isSecondHalf = game.isSecondHalf,
            predictions = predictions,
            language = runCatching { Language.valueOf(game.language) }.getOrDefault(Language.ENGLISH),
        )
    }
}
```

### Task 3.5: GameRepository

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/GameRepository.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
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
```

- [ ] **Step 2: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` (Room generiert DAO-Implementierung via KSP).

### Task 3.6: Instrumentierter Repository-Roundtrip-Test (optional, Gerät nötig)

**Files:**
- Create: `app/src/androidTest/java/com/nwe/spadesscore/data/GameRepositoryTest.kt`

- [ ] **Step 1: Test schreiben**

```kotlin
package com.nwe.spadesscore.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.spadesscore.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {

    private fun newDb(): AppDatabase =
        Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()

    @Test
    fun startGameAndConfirm_persistsAndRestores() = runTest {
        val db = newDb()
        val repo = GameRepository(db.gameDao(), Dispatchers.IO)
        repo.startGame(listOf("A", "B", "C", "D"), randomDealer = false)
        repo.setPredictions(listOf(2, 0, 0, 0))
        repo.confirmTricks(listOf(true, false, false, false))

        // Repository neu aufbauen -> lädt aus derselben DB
        val restored = GameRepository(db.gameDao(), Dispatchers.IO)
        assertEquals(2, restored.state.value.currentRound)
        assertEquals(listOf(0, 7), restored.state.value.players[0].scores) // 0 + 2 + 5
        db.close()
    }
}
```

- [ ] **Step 2: Test ausführen (falls Gerät/Emulator verfügbar)**

Run: `.\gradlew.bat connectedDebugAndroidTest --tests "com.nwe.spadesscore.data.GameRepositoryTest"`
Expected: PASS. Ohne Gerät überspringen und im Commit vermerken.

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat(data): add Room entities, DAO, database, mapper, repository"
```

---

## Phase 4: Dependency Injection (Application + AppContainer)

### Task 4.1: AppContainer

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/di/AppContainer.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.di

import android.content.Context
import androidx.room.Room
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers

/** Manuelles DI: lebt als Singleton in [com.nwe.spadesscore.SpadesApplication]. */
class AppContainer(context: Context) {

    private val database: AppDatabase = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        "spades.db",
    ).build()

    val gameRepository: GameRepository = GameRepository(database.gameDao(), Dispatchers.IO)
}
```

### Task 4.2: SpadesApplication + Manifest-Registrierung

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/SpadesApplication.kt`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Application-Klasse anlegen**

```kotlin
package com.nwe.spadesscore

import android.app.Application
import com.nwe.spadesscore.di.AppContainer

class SpadesApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 2: Im Manifest registrieren** — `android:name=".SpadesApplication"` zum `<application>`-Tag hinzufügen.

Ersetze die Zeile:

```xml
    <application
        android:allowBackup="true"
```

durch:

```xml
    <application
        android:name=".SpadesApplication"
        android:allowBackup="true"
```

- [ ] **Step 3: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. (Die alten Java-Activities + Singleton laufen weiterhin; Application ist registriert, aber noch ungenutzt.)

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(di): add AppContainer and SpadesApplication"
```

---

## Phase 5: ViewModels + UiStates

Pro Screen ein Package `ui/<screen>/`. Eine kleine Helfer-Erweiterung liefert das Repository an die ViewModels.

### Task 5.1: ViewModel-Factory-Helfer

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/repository.kt`

- [ ] **Step 1: Datei anlegen** (Extension, um in jeder Activity knapp das Repository zu holen)

```kotlin
package com.nwe.spadesscore.ui

import android.content.Context
import com.nwe.spadesscore.SpadesApplication
import com.nwe.spadesscore.data.GameRepository

val Context.gameRepository: GameRepository
    get() = (applicationContext as SpadesApplication).container.gameRepository
```

### Task 5.2: MainViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/main/MainUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/main/MainViewModel.kt`

- [ ] **Step 1: MainUiState anlegen**

```kotlin
package com.nwe.spadesscore.ui.main

import com.nwe.spadesscore.domain.model.Language

data class MainUiState(
    val playerCount: Int,
    val language: Language,
)
```

- [ ] **Step 2: MainViewModel anlegen**

```kotlin
package com.nwe.spadesscore.ui.main

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.model.Language

class MainViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): MainUiState {
        val state = repository.state.value
        return MainUiState(playerCount = state.playerCount, language = state.language)
    }

    fun setPlayerCount(playerCount: Int) = repository.setPlayerCount(playerCount)

    fun setLanguage(language: Language) = repository.setLanguage(language)
}
```

### Task 5.3: PlayerNamesViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/names/PlayerNamesViewModel.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.spadesscore.ui.names

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.NameValidation
import com.nwe.spadesscore.domain.NameValidationResult

sealed interface StartGameResult {
    data object Started : StartGameResult
    data object EmptyName : StartGameResult
    data object NameTooLong : StartGameResult
}

class PlayerNamesViewModel(private val repository: GameRepository) : ViewModel() {

    val playerCount: Int get() = repository.state.value.playerCount

    fun start(names: List<String>, randomDealer: Boolean): StartGameResult =
        when (NameValidation.validate(names)) {
            NameValidationResult.Empty -> StartGameResult.EmptyName
            NameValidationResult.TooLong -> StartGameResult.NameTooLong
            NameValidationResult.Ok -> {
                repository.startGame(names, randomDealer)
                StartGameResult.Started
            }
        }
}
```

### Task 5.4: DealCardsViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/deal/DealCardsUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/deal/DealCardsViewModel.kt`

- [ ] **Step 1: UiState anlegen**

```kotlin
package com.nwe.spadesscore.ui.deal

data class PlayerScore(val name: String, val score: Int)

data class DealCardsUiState(
    val round: Int,
    val dealerName: String,
    val cardAmount: Int,
    val players: List<PlayerScore>,
    val amountOfRounds: Int,
)
```

- [ ] **Step 2: ViewModel anlegen**

```kotlin
package com.nwe.spadesscore.ui.deal

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository

class DealCardsViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): DealCardsUiState {
        val state = repository.state.value
        return DealCardsUiState(
            round = state.currentRound,
            dealerName = state.currentDealerName,
            cardAmount = state.amountOfCards,
            players = state.players.map { PlayerScore(it.name, it.currentScore) },
            amountOfRounds = state.amountOfRounds,
        )
    }
}
```

### Task 5.5: DeclareTricksViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/declare/DeclareTricksUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/declare/DeclareTricksViewModel.kt`

- [ ] **Step 1: UiState anlegen**

```kotlin
package com.nwe.spadesscore.ui.declare

import com.nwe.spadesscore.ui.deal.PlayerScore

data class DeclareTricksUiState(
    val round: Int,
    val players: List<PlayerScore>,
    val cardAmount: Int,
    val amountOfRounds: Int,
)
```

- [ ] **Step 2: ViewModel anlegen**

```kotlin
package com.nwe.spadesscore.ui.declare

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.ui.deal.PlayerScore

class DeclareTricksViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): DeclareTricksUiState {
        val state = repository.state.value
        return DeclareTricksUiState(
            round = state.currentRound,
            players = state.players.map { PlayerScore(it.name, it.currentScore) },
            cardAmount = state.amountOfCards,
            amountOfRounds = state.amountOfRounds,
        )
    }

    fun setPredictions(predictions: List<Int>) = repository.setPredictions(predictions)
}
```

### Task 5.6: ConfirmTicksViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/confirm/ConfirmTicksUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/confirm/ConfirmTicksViewModel.kt`

- [ ] **Step 1: UiState anlegen**

```kotlin
package com.nwe.spadesscore.ui.confirm

data class ConfirmPlayer(
    val name: String,
    val score: Int,
    val prediction: Int,
    val pointsIfHit: Int,
)

data class ConfirmTicksUiState(
    val round: Int,
    val players: List<ConfirmPlayer>,
    val amountOfRounds: Int,
)
```

- [ ] **Step 2: ViewModel anlegen**

```kotlin
package com.nwe.spadesscore.ui.confirm

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.GameRules

class ConfirmTicksViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): ConfirmTicksUiState {
        val state = repository.state.value
        return ConfirmTicksUiState(
            round = state.currentRound,
            players = state.players.mapIndexed { index, player ->
                val prediction = state.predictions[index]
                ConfirmPlayer(
                    name = player.name,
                    score = player.currentScore,
                    prediction = prediction,
                    pointsIfHit = prediction + GameRules.HIT_BONUS,
                )
            },
            amountOfRounds = state.amountOfRounds,
        )
    }

    /** Bestätigt die Treffer und gibt zurück, ob der Ergebnisbildschirm folgt. */
    fun confirm(hits: List<Boolean>): Boolean {
        repository.confirmTricks(hits)
        return repository.state.value.isGameOver
    }
}
```

### Task 5.7: ResultViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/result/ResultUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/result/ResultViewModel.kt`

- [ ] **Step 1: UiState anlegen**

```kotlin
package com.nwe.spadesscore.ui.result

data class ResultUiState(
    val playerCount: Int,
    val playerNames: List<String>,
    /** Je Spieler die Score-Werte ab Runde 1 (ohne den Startwert 0). */
    val scoresByPlayer: List<List<Int>>,
    /** Anzahl sichtbarer Runden-Zeilen (= gespielte Runden). */
    val visibleRoundCount: Int,
    /** Spalten-Index (0-basiert) der zuletzt gespielten Runde, der hervorgehoben wird. */
    val highlightColumnIndex: Int,
    /** Spieler-Index -> Platz (1 = höchster Score) für die Medaillenfarben. */
    val placementByPlayer: Map<Int, Int>,
    val isSecondHalf: Boolean,
)
```

- [ ] **Step 2: ViewModel anlegen**

```kotlin
package com.nwe.spadesscore.ui.result

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.domain.GameEngine

class ResultViewModel(private val repository: GameRepository) : ViewModel() {

    fun uiState(): ResultUiState {
        val state = repository.state.value
        return ResultUiState(
            playerCount = state.playerCount,
            playerNames = state.players.map { it.name },
            scoresByPlayer = state.players.map { it.scores.drop(1) },
            visibleRoundCount = state.currentRound - 1,
            highlightColumnIndex = state.currentRound - 2,
            placementByPlayer = GameEngine.placementByPlayerIndex(state),
            isSecondHalf = state.isSecondHalf,
        )
    }

    fun startSecondHalf() = repository.startSecondHalf()
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat(ui): add ViewModels and UiStates for all six screens"
```

---

## Phase 6: Activities (Java → Kotlin) + Lokalisierung

**Wichtig:** Zwischen Task 6.2 und 6.7 ist die durchgängige Spiellogik gemischt (alte Activities nutzen den Singleton, neue das Repository). Jede Zwischenstufe muss **kompilieren** und die **Unit-Tests** müssen grün sein, aber den **vollständigen Spieldurchlauf erst am Ende (Task 6.9 / Phase 7) testen.**

### Task 6.0: Lokalisierte Dialog-Strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: EN-Strings ergänzen** — vor `</resources>` in `values/strings.xml` einfügen:

```xml
    <string name="dialog_empty_name_title">Empty Name</string>
    <string name="dialog_empty_name_message">Please give each player a name</string>
    <string name="dialog_name_too_long_title">Name too long</string>
    <string name="dialog_name_too_long_message">Names can only be a maximum of 10 characters long</string>
```

- [ ] **Step 2: DE-Strings ergänzen** — vor `</resources>` in `values-de/strings.xml` einfügen:

```xml
    <string name="dialog_empty_name_title">Leerer Name</string>
    <string name="dialog_empty_name_message">Bitte gib jedem Spieler einen Namen</string>
    <string name="dialog_name_too_long_title">Name zu lang</string>
    <string name="dialog_name_too_long_message">Namen dürfen maximal 10 Zeichen lang sein</string>
```

### Task 6.1: Template-Methoden auf `protected` vereinheitlichen (noch Java)

Damit die später konvertierten Kotlin-Activities die Basisklassen-Methoden sauber überschreiben können, müssen alle Template-Methoden `protected` sein (Sichtbarkeit nur erweitern — verhaltensneutral). Die Basisklasse bleibt vorerst Java.

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.java`
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.java`
- Modify: `app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.java`
- Modify: `app/src/main/java/com/nwe/spadesscore/DealCardsActivity.java`
- Modify: `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.java`

- [ ] **Step 1: Basisklasse** — die drei abstrakten Deklarationen ersetzen:

```java
    abstract void initializeUIComponents();

    abstract void setupUI();

    abstract void initContentView();
```

durch:

```java
    protected abstract void initializeUIComponents();

    protected abstract void setupUI();

    protected abstract void initContentView();
```

- [ ] **Step 2: MainActivity** — die drei Methodensignaturen `void initializeUIComponents()`, `void setupUI()`, `void initContentView()` jeweils um `protected` ergänzen (zu `protected void ...`).

- [ ] **Step 3: PlayerNamesActivity** — analog die drei Signaturen `void initializeUIComponents()`, `void setupUI()`, `void initContentView()` um `protected` ergänzen.

- [ ] **Step 4: DealCardsActivity** — nur `void initContentView()` zu `protected void initContentView()`.

- [ ] **Step 5: DeclareTricksActivity** — nur `void initContentView()` zu `protected void initContentView()`.

- [ ] **Step 6: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` (reine Sichtbarkeitserweiterung, keine Verhaltensänderung).

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: widen template methods to protected; add localized dialog strings"
```

### Task 6.2: MainActivity → Kotlin

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/MainActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/MainActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.main.MainViewModel
import java.util.Locale

class MainActivity : SpadesAppCompatActivity() {

    private val viewModel: MainViewModel by viewModels {
        viewModelFactory { initializer { MainViewModel(gameRepository) } }
    }

    private lateinit var btn3Players: Button
    private lateinit var btn4Players: Button
    private lateinit var btnLanguageEnglish: Button
    private lateinit var btnLanguageGerman: Button
    private lateinit var startGameButton: Button

    private var colorActive = 0
    private var colorDeactive = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val desired = if (viewModel.uiState().language == Language.GERMAN) "de" else "en"
        if (resources.configuration.locales[0].language != desired) {
            setLocale(desired)
        }
    }

    override fun initContentView() {
        setContentView(R.layout.activity_main)
        colorActive = ContextCompat.getColor(this, R.color.background_button_enabled)
        colorDeactive = ContextCompat.getColor(this, R.color.background_button_disabled)
    }

    override fun initializeUIComponents() {
        btn3Players = findViewById(R.id.btn3Players)
        btn4Players = findViewById(R.id.btn4Players)
        btnLanguageEnglish = findViewById(R.id.btnLanguageEnglish)
        btnLanguageGerman = findViewById(R.id.btnLanguageGerman)
        startGameButton = findViewById(R.id.start_game_button)
    }

    override fun setupUI() {
        val state = viewModel.uiState()

        if (state.playerCount == 4) {
            btn4Players.isSelected = true
            btn3Players.isSelected = false
            (btn4Players.background as GradientDrawable).setColor(colorActive)
            (btn3Players.background as GradientDrawable).setColor(colorDeactive)
        } else {
            btn4Players.isSelected = false
            btn3Players.isSelected = true
            (btn4Players.background as GradientDrawable).setColor(colorDeactive)
            (btn3Players.background as GradientDrawable).setColor(colorActive)
        }

        if (state.language == Language.ENGLISH) {
            btnLanguageEnglish.isSelected = true
            btnLanguageGerman.isSelected = false
            (btnLanguageEnglish.background as GradientDrawable).setColor(colorActive)
            (btnLanguageGerman.background as GradientDrawable).setColor(colorDeactive)
        } else {
            btnLanguageEnglish.isSelected = false
            btnLanguageGerman.isSelected = true
            (btnLanguageEnglish.background as GradientDrawable).setColor(colorDeactive)
            (btnLanguageGerman.background as GradientDrawable).setColor(colorActive)
        }

        btn3Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 3) return@setOnClickListener
            btn3Players.isSelected = true
            btn4Players.isSelected = false
            viewModel.setPlayerCount(3)
            select(btn3Players)
            deselect(btn4Players)
        }
        btn4Players.setOnClickListener {
            if (viewModel.uiState().playerCount == 4) return@setOnClickListener
            btn4Players.isSelected = true
            btn3Players.isSelected = false
            viewModel.setPlayerCount(4)
            select(btn4Players)
            deselect(btn3Players)
        }
        btnLanguageEnglish.setOnClickListener {
            if (viewModel.uiState().language == Language.ENGLISH) return@setOnClickListener
            btnLanguageEnglish.isSelected = true
            btnLanguageGerman.isSelected = false
            viewModel.setLanguage(Language.ENGLISH)
            select(btnLanguageEnglish)
            deselect(btnLanguageGerman)
            setLocale("en")
        }
        btnLanguageGerman.setOnClickListener {
            if (viewModel.uiState().language == Language.GERMAN) return@setOnClickListener
            btnLanguageGerman.isSelected = true
            btnLanguageEnglish.isSelected = false
            viewModel.setLanguage(Language.GERMAN)
            select(btnLanguageGerman)
            deselect(btnLanguageEnglish)
            setLocale("de")
        }
        startGameButton.setOnClickListener {
            startActivity(Intent(this, PlayerNamesActivity::class.java))
        }
    }

    private fun select(button: Button) = animateFill(button, colorDeactive, colorActive)

    private fun deselect(button: Button) = animateFill(button, colorActive, colorDeactive)

    private fun animateFill(view: View, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = 250
            addUpdateListener { animation ->
                (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
            }
            start()
        }
    }

    private fun setLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert MainActivity to Kotlin + MVVM"
```

### Task 6.3: PlayerNamesActivity → Kotlin (Debug-Prefill raus, Dialoge lokalisiert)

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.app.AlertDialog
import android.content.Intent
import android.view.View
import android.widget.CheckBox
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.android.material.textfield.TextInputLayout
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.names.PlayerNamesViewModel
import com.nwe.spadesscore.ui.names.StartGameResult

class PlayerNamesActivity : SpadesAppCompatActivity() {

    private val viewModel: PlayerNamesViewModel by viewModels {
        viewModelFactory { initializer { PlayerNamesViewModel(gameRepository) } }
    }

    private lateinit var player1NameInput: TextInputLayout
    private lateinit var player2NameInput: TextInputLayout
    private lateinit var player3NameInput: TextInputLayout
    private lateinit var player4NameInput: TextInputLayout
    private lateinit var randomDealerCheckBox: CheckBox

    override fun initContentView() {
        setContentView(R.layout.activity_player_names)
    }

    override fun initializeUIComponents() {
        player1NameInput = findViewById(R.id.player1_name_input)
        player2NameInput = findViewById(R.id.player2_name_input)
        player3NameInput = findViewById(R.id.player3_name_input)
        player4NameInput = findViewById(R.id.player4_name_input)
        randomDealerCheckBox = findViewById(R.id.random_dealer_checkBox)
    }

    override fun setupUI() {
        if (viewModel.playerCount == 3) {
            player4NameInput.visibility = View.GONE
            findViewById<View>(R.id.player4_space).visibility = View.GONE
        }
        findViewById<View>(R.id.start_game_button).setOnClickListener { startGame() }
    }

    private fun nameOf(input: TextInputLayout): String = input.editText?.text?.toString().orEmpty()

    private fun startGame() {
        val names = buildList {
            add(nameOf(player1NameInput))
            add(nameOf(player2NameInput))
            add(nameOf(player3NameInput))
            if (viewModel.playerCount == 4) add(nameOf(player4NameInput))
        }
        when (viewModel.start(names, randomDealerCheckBox.isChecked)) {
            StartGameResult.EmptyName ->
                showWarning(R.string.dialog_empty_name_title, R.string.dialog_empty_name_message)
            StartGameResult.NameTooLong ->
                showWarning(R.string.dialog_name_too_long_title, R.string.dialog_name_too_long_message)
            StartGameResult.Started ->
                startActivity(Intent(this, DealCardsActivity::class.java))
        }
    }

    private fun showWarning(@StringRes titleRes: Int, @StringRes messageRes: Int) {
        AlertDialog.Builder(this)
            .setTitle(titleRes)
            .setMessage(messageRes)
            .create()
            .show()
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert PlayerNamesActivity to Kotlin; drop debug prefill; localize dialogs"
```

### Task 6.4: DealCardsActivity → Kotlin

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/DealCardsActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/DealCardsActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/DealCardsActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.content.Intent
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.deal.DealCardsUiState
import com.nwe.spadesscore.ui.deal.DealCardsViewModel
import com.nwe.spadesscore.ui.gameRepository
import java.util.Locale

class DealCardsActivity : SpadesAppCompatActivity() {

    private val viewModel: DealCardsViewModel by viewModels {
        viewModelFactory { initializer { DealCardsViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var playerNameTextView: TextView
    private lateinit var amountTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var progressBar: ProgressBar

    override fun initContentView() {
        setContentView(R.layout.activity_deal_cards)
    }

    override fun initializeUIComponents() {
        amountTextView = findViewById(R.id.amount_TextView)
        roundTextView = findViewById(R.id.round_TextView)
        playerNameTextView = findViewById(R.id.player_name_TextView)
        progressBar = findViewById(R.id.progressBar)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        if (state.players.size == 3) {
            scoreViews[3].visibility = View.GONE
        }
        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round
        findViewById<View>(R.id.start_Button).setOnClickListener {
            startActivity(Intent(this, DeclareTricksActivity::class.java))
        }
        render(state)
    }

    private fun render(state: DealCardsUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        playerNameTextView.text = getString(R.string.player_must_deal_cards, state.dealerName)
        amountTextView.text = String.format(Locale.getDefault(), "%dx", state.cardAmount)
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
        }
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert DealCardsActivity to Kotlin + MVVM"
```

### Task 6.5: DeclareTricksActivity → Kotlin

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.declare.DeclareTricksUiState
import com.nwe.spadesscore.ui.declare.DeclareTricksViewModel
import com.nwe.spadesscore.ui.gameRepository

class DeclareTricksActivity : SpadesAppCompatActivity() {

    private val viewModel: DeclareTricksViewModel by viewModels {
        viewModelFactory { initializer { DeclareTricksViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var combinedTricksTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var nameViews: List<TextView>
    private lateinit var spinners: List<TrickSpinner>
    private lateinit var progressBar: ProgressBar
    private lateinit var startButton: Button

    private var colorActive = 0
    private var colorDeactive = 0
    private var warningColor = 0
    private var defaultColor = 0
    private var lastTricksAreValid = true
    private var playerCount = 4
    private var cardAmount = 0

    override fun initContentView() {
        setContentView(R.layout.activity_declare_tricks)
        colorActive = ContextCompat.getColor(this, R.color.background_button_enabled)
        colorDeactive = ContextCompat.getColor(this, R.color.background_button_disabled)
        warningColor = ContextCompat.getColor(this, R.color.warningText)
    }

    override fun initializeUIComponents() {
        roundTextView = findViewById(R.id.round_TextView)
        progressBar = findViewById(R.id.progressBar)
        combinedTricksTextView = findViewById(R.id.combined_tricks_textView)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
        nameViews = listOf(
            findViewById(R.id.player1_name_text_view),
            findViewById(R.id.player2_name_text_view),
            findViewById(R.id.player3_name_text_view),
            findViewById(R.id.player4_name_text_view),
        )
        spinners = listOf(
            findViewById(R.id.spinner1),
            findViewById(R.id.spinner2),
            findViewById(R.id.spinner3),
            findViewById(R.id.spinner4),
        )
        startButton = findViewById(R.id.start_Button)

        val state = viewModel.uiState()
        cardAmount = state.cardAmount
        playerCount = state.players.size
        spinners.forEach { it.setMaxAmount(state.cardAmount) }
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        if (state.players.size == 3) {
            scoreViews[3].visibility = View.GONE
            findViewById<View>(R.id.spinner4).visibility = View.GONE
            findViewById<View>(R.id.player4_layout).visibility = View.GONE
        }

        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round

        (startButton.background as GradientDrawable).setColor(colorActive)
        defaultColor = combinedTricksTextView.textColors.defaultColor
        startButton.setOnClickListener { confirmTricks() }

        spinners.forEach { spinner ->
            spinner.setOnValueChangedListener { updateCombinedTricksTextView() }
        }

        render(state)
    }

    private fun render(state: DeclareTricksUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        updateCombinedTricksTextView()
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
            nameViews[index].text = player.name
        }
    }

    private fun updateCombinedTricksTextView() {
        val values = spinners.map { it.value }
        val combined = values[0] + values[1] + values[2] + if (playerCount == 4) values[3] else 0
        val possible = cardAmount

        combinedTricksTextView.text = getString(
            R.string.combined_trick_prediction, combined, possible, getString(R.string.tricks),
        )

        val isValid = combined != possible
        if (!isValid) {
            combinedTricksTextView.setTextColor(warningColor)
            startButton.isEnabled = false
            if (lastTricksAreValid) {
                animateFill(startButton, colorActive, colorDeactive)
                shakeView(startButton)
            }
        } else {
            combinedTricksTextView.setTextColor(defaultColor)
            startButton.isEnabled = true
            if (!lastTricksAreValid) {
                animateFill(startButton, colorDeactive, colorActive)
            }
        }
        lastTricksAreValid = isValid
    }

    private fun animateFill(view: View, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = 250
            addUpdateListener { animation ->
                (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
            }
            start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 16f, -16f, 12f, -12f, 6f, -6f, 0f).apply {
            duration = 350
            start()
        }
    }

    private fun confirmTricks() {
        viewModel.setPredictions(spinners.map { it.value })
        startActivity(Intent(this, ConfirmTicksActivity::class.java))
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert DeclareTricksActivity to Kotlin + MVVM"
```

### Task 6.6: ConfirmTicksActivity → Kotlin

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.content.Intent
import android.view.View
import android.widget.CheckBox
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.confirm.ConfirmTicksUiState
import com.nwe.spadesscore.ui.confirm.ConfirmTicksViewModel
import com.nwe.spadesscore.ui.gameRepository
import java.util.Locale

class ConfirmTicksActivity : SpadesAppCompatActivity() {

    private val viewModel: ConfirmTicksViewModel by viewModels {
        viewModelFactory { initializer { ConfirmTicksViewModel(gameRepository) } }
    }

    private lateinit var roundTextView: TextView
    private lateinit var scoreViews: List<TextView>
    private lateinit var nameViews: List<TextView>
    private lateinit var tricksViews: List<TextView>
    private lateinit var pointsViews: List<TextView>
    private lateinit var checkboxes: List<CheckBox>
    private lateinit var progressBar: ProgressBar

    private var defaultColor = 0
    private var greenColor = 0

    override fun initContentView() {
        setContentView(R.layout.activity_confirm_tricks)
        greenColor = ContextCompat.getColor(this, R.color.plusPointsText)
    }

    override fun initializeUIComponents() {
        roundTextView = findViewById(R.id.round_TextView)
        progressBar = findViewById(R.id.progressBar)
        scoreViews = listOf(
            findViewById(R.id.current_score_player1_text_view),
            findViewById(R.id.current_score_player2_text_view),
            findViewById(R.id.current_score_player3_text_view),
            findViewById(R.id.current_score_player4_text_view),
        )
        nameViews = listOf(
            findViewById(R.id.player1_name_text),
            findViewById(R.id.player2_name_text),
            findViewById(R.id.player3_name_text),
            findViewById(R.id.player4_name_text),
        )
        tricksViews = listOf(
            findViewById(R.id.player1_amount_of_tricks_text),
            findViewById(R.id.player2_amount_of_tricks_text),
            findViewById(R.id.player3_amount_of_tricks_text),
            findViewById(R.id.player4_amount_of_tricks_text),
        )
        pointsViews = listOf(
            findViewById(R.id.player1_points_text),
            findViewById(R.id.player2_points_text),
            findViewById(R.id.player3_points_text),
            findViewById(R.id.player4_points_text),
        )
        checkboxes = listOf(
            findViewById(R.id.player1_checkbox),
            findViewById(R.id.player2_checkbox),
            findViewById(R.id.player3_checkbox),
            findViewById(R.id.player4_checkbox),
        )
    }

    override fun setupUI() {
        val state = viewModel.uiState()
        val playerCount = state.players.size
        if (playerCount == 3) {
            scoreViews[3].visibility = View.GONE
            findViewById<View>(R.id.player4_layout).visibility = View.GONE
        }
        for (index in 0 until playerCount) {
            checkboxes[index].setOnClickListener {
                pointsViews[index].setTextColor(if (checkboxes[index].isChecked) greenColor else defaultColor)
            }
        }
        progressBar.max = state.amountOfRounds
        progressBar.progress = state.round
        findViewById<View>(R.id.start_Button).setOnClickListener { startNextRound() }
        defaultColor = pointsViews[0].textColors.defaultColor
        render(state)
    }

    private fun render(state: ConfirmTicksUiState) {
        roundTextView.text = getString(R.string.round, state.round)
        state.players.forEachIndexed { index, player ->
            scoreViews[index].text = "${player.name}: ${player.score}"
            nameViews[index].text = player.name
            val trickWord =
                if (player.prediction == 1) getString(R.string.trick) else getString(R.string.tricks)
            tricksViews[index].text =
                String.format(Locale.getDefault(), "%d %s", player.prediction, trickWord)
            pointsViews[index].text = getString(R.string.points_added, player.pointsIfHit)
        }
    }

    private fun startNextRound() {
        val playerCount = viewModel.uiState().players.size
        val hits = (0 until playerCount).map { checkboxes[it].isChecked }
        val gameOver = viewModel.confirm(hits)
        val next = if (gameOver) ResultScreenActivity::class.java else DealCardsActivity::class.java
        startActivity(Intent(this, next))
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert ConfirmTicksActivity to Kotlin + MVVM"
```

### Task 6.7: ResultScreenActivity → Kotlin (findViewById-Schleife)

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt`

- [ ] **Step 1: Java-Datei entfernen** (enthält auch die Hilfsklasse `ScoreObj`, die entfällt)

Run: `git rm app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Space
import android.widget.TableRow
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.result.ResultViewModel

class ResultScreenActivity : AppCompatActivity() {

    private val viewModel: ResultViewModel by viewModels {
        viewModelFactory { initializer { ResultViewModel(gameRepository) } }
    }

    private companion object {
        const val MAX_ROUNDS = 20
        const val PLAYER4_SPACE_COUNT = 21
        const val HIGHLIGHT_TEXT_SIZE_SP = 30f
        const val COLOR_GOLD = "#ffd700"
        const val COLOR_SILVER = "#e6e6e6"
        const val COLOR_BRONZE = "#bf8970"
    }

    // scoreCells[player][round]: Spieler 0..3, Runde 0..19
    private lateinit var scoreCells: List<List<TextView>>
    private lateinit var roundRows: List<TableRow>
    private lateinit var player4Spaces: List<Space>

    @SuppressLint("DiscouragedApi")
    private fun viewIdByName(name: String): Int = resources.getIdentifier(name, "id", packageName)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result_screen)

        scoreCells = (1..4).map { player ->
            (1..MAX_ROUNDS).map { round ->
                findViewById<TextView>(viewIdByName("score_player${player}_round$round"))
            }
        }
        roundRows = (1..MAX_ROUNDS).map { findViewById<TableRow>(viewIdByName("round${it}Scores")) }
        player4Spaces = (1..PLAYER4_SPACE_COUNT).map { findViewById<Space>(viewIdByName("player4_space$it")) }

        val state = viewModel.uiState()

        hidePlayer4IfNeeded(state.playerCount)
        setRowVisibility(state.visibleRoundCount)
        fillScores(state.scoresByPlayer)
        setPlayerNames(state.playerCount, state.playerNames)
        highlightLastColumn(state.placementByPlayer, state.highlightColumnIndex)

        val continueButton = findViewById<Button>(R.id.continue_button)
        continueButton.setOnClickListener {
            viewModel.startSecondHalf()
            startActivity(Intent(this, DealCardsActivity::class.java))
        }
        if (state.isSecondHalf) {
            continueButton.visibility = View.GONE
            findViewById<Space>(R.id.lowerSpace).visibility = View.GONE
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* disable back */ }
        })
    }

    private fun hidePlayer4IfNeeded(playerCount: Int) {
        if (playerCount == 3) {
            scoreCells[3].forEach { it.visibility = View.GONE }
            player4Spaces.forEach { it.visibility = View.GONE }
            findViewById<TextView>(R.id.header_player4).visibility = View.GONE
        }
    }

    private fun setRowVisibility(visibleRoundCount: Int) {
        roundRows.forEach { it.visibility = View.GONE }
        for (i in 0 until visibleRoundCount) {
            roundRows[i].visibility = View.VISIBLE
        }
    }

    private fun fillScores(scoresByPlayer: List<List<Int>>) {
        scoresByPlayer.forEachIndexed { playerIndex, scores ->
            scores.forEachIndexed { roundIndex, score ->
                scoreCells[playerIndex][roundIndex].text = score.toString()
            }
        }
    }

    private fun setPlayerNames(playerCount: Int, names: List<String>) {
        findViewById<TextView>(R.id.header_player1).text = names[0]
        findViewById<TextView>(R.id.header_player2).text = names[1]
        findViewById<TextView>(R.id.header_player3).text = names[2]
        if (playerCount == 4) {
            findViewById<TextView>(R.id.header_player4).text = names[3]
        }
    }

    private fun highlightLastColumn(placementByPlayer: Map<Int, Int>, columnIndex: Int) {
        if (columnIndex < 0) return
        placementByPlayer.forEach { (playerIndex, place) ->
            val cell = scoreCells[playerIndex][columnIndex]
            cell.setTextColor(colorForPlace(place))
            cell.textSize = HIGHLIGHT_TEXT_SIZE_SP
        }
    }

    private fun colorForPlace(place: Int): Int = when (place) {
        1 -> Color.parseColor(COLOR_GOLD)
        2 -> Color.parseColor(COLOR_SILVER)
        3 -> Color.parseColor(COLOR_BRONZE)
        else -> scoreCells[0][1].textColors.defaultColor
    }
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(ui): convert ResultScreenActivity to Kotlin; collapse findViewById via id loop"
```

### Task 6.8: Basisklasse → Kotlin

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.java`
- Create: `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.kt`

- [ ] **Step 1: Java-Datei entfernen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.java`

- [ ] **Step 2: Kotlin-Datei anlegen**

```kotlin
package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

abstract class SpadesAppCompatActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initContentView()
        initializeUIComponents()
        setupUI()
        setupBackPressHandler()
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* disable back */ }
        })
    }

    protected abstract fun initializeUIComponents()

    protected abstract fun setupUI()

    protected abstract fun initContentView()
}
```

- [ ] **Step 3: Kompilieren**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` (alle fünf Subklassen sind nun Kotlin und überschreiben die `protected`-Methoden).

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: convert SpadesAppCompatActivity base class to Kotlin"
```

### Task 6.9: Alten Singleton + Languages-Enum entfernen

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/SpadesGame.java`
- Delete: `app/src/main/java/com/nwe/spadesscore/Languages.java`

- [ ] **Step 1: Sicherstellen, dass keine Referenzen mehr bestehen**

Run: `git grep -n "SpadesGame\|Languages" -- "app/src/main"`
Expected: keine Treffer mehr in `app/src/main` (außer evtl. in den zu löschenden Dateien selbst). Falls ein Treffer in produktivem Code auftaucht, zuerst dort auf Repository/`Language` umstellen.

- [ ] **Step 2: Dateien löschen**

Run: `git rm app/src/main/java/com/nwe/spadesscore/SpadesGame.java app/src/main/java/com/nwe/spadesscore/Languages.java`

- [ ] **Step 3: Voller Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` — vollständig Kotlin-basierter Backend-Code, kein Singleton mehr.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor: remove legacy SpadesGame singleton and Languages enum"
```

---

## Phase 7: Gesamt-Verifikation

### Task 7.1: Voller Build + alle Unit-Tests + Lint

**Files:** keine

- [ ] **Step 1: Clean Build**

Run: `.\gradlew.bat clean assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Alle Unit-Tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL` — alle Domain-Tests grün (GameStateTest, GameEngineTest, NameValidationTest) sowie der Template-`ExampleUnitTest`.

- [ ] **Step 3: Lint**

Run: `.\gradlew.bat lint`
Expected: `BUILD SUCCESSFUL`. Den Report unter `app/build/reports/lint-results-debug.html` auf neue **Errors** prüfen (Warnungen sind ok). Erwartet ist höchstens eine unterdrückte `DiscouragedApi`-Warnung in `ResultScreenActivity` (bereits via `@SuppressLint` behandelt).

### Task 7.2: Manueller 1:1-Verifikationsdurchlauf (Gerät/Emulator)

**Files:** keine

- [ ] **Step 1: App installieren**

Run: `.\gradlew.bat installDebug`
Expected: App installiert.

- [ ] **Step 2: 4-Spieler-Vollspiel auf Englisch durchspielen** und gegen das erwartete Verhalten prüfen:
  - MainActivity: 4 Spieler vorausgewählt, Englisch aktiv; Auswahl-Buttons animieren beim Umschalten (Color-Fill).
  - PlayerNamesActivity: **keine** vorausgefüllten Namen mehr (Debug-Prefill entfernt). Leeren Namen lassen → Dialog „Empty Name". Namen > 10 Zeichen → Dialog „Name too long".
  - DealCards: korrekter Dealer, Rundennummer, Kartenanzahl „1x", Fortschrittsbalken.
  - DeclareTricks: Spinner 0..Kartenanzahl; wenn Summe == Kartenanzahl → Start-Button deaktiviert, Warnfarbe, Shake-Animation; sonst aktiv.
  - ConfirmTicks: Häkchen färbt Punkte grün; Punkte = Vorhersage + 5; Stich/Stiche-Singular/Plural korrekt.
  - Scoring: Treffer = +Vorhersage+5, Fehlschlag unverändert — Werte mit altem Stand vergleichen.
  - Karten-Hochrampe 1.–8. Runde (1..8).
  - Halbzeit-Ergebnisbildschirm nach Runde 8: Medaillenfarben (Gold/Silber/Bronze) + größere Schrift in der letzten Spalte; „Weiter"-Button sichtbar.
  - 2. Halbzeit: Karten-Runterrampe (8..1); am Ende Endbildschirm **ohne** „Weiter"-Button.

- [ ] **Step 3: 3-Spieler-Spiel auf Deutsch durchspielen** und prüfen:
  - Sprache umschalten auf Deutsch → alle Texte deutsch (inkl. der Warn-Dialoge — neue Lokalisierung).
  - Spieler-4-Elemente überall ausgeblendet (Namenseingabe, Scores, Spinner, Ergebnistabelle).
  - 10 Runden hoch (1..10), 10 Runden runter; Ergebnistabelle zeigt bis zu 20 Runden.

- [ ] **Step 4: Persistenz prüfen (neue, gewollte Verhaltensänderung)**
  - Mitten im Spiel (z.B. DeclareTricks) den App-Prozess beenden (in Android Studio „Terminate Application" oder `adb shell am kill com.nwe.spadesscore`).
  - App über das zuletzt sichtbare Activity wiederherstellen lassen → Spielstand (Runde, Scores, Spielernamen) ist erhalten, kein Crash.

- [ ] **Step 5: Optional — instrumentierten Repository-Test laufen lassen** (falls Gerät verfügbar)

Run: `.\gradlew.bat connectedDebugAndroidTest`
Expected: PASS.

### Task 7.3: Branch abschließen

- [ ] **Step 1: Status prüfen**

Run: `git status`
Expected: sauberer Working Tree (alle Änderungen committed).

- [ ] **Step 2:** Mit der Skill `superpowers:finishing-a-development-branch` über Merge/PR entscheiden.

---

## Self-Review (vom Plan-Autor durchgeführt)

**1. Spec-Abdeckung** — jede Spec-Sektion hat zugeordnete Tasks:
- Zielarchitektur/Packages → Phasen 2–5 (domain/data/ui), dokumentierte Package-Abweichung oben.
- Domain (GameState/GameEngine/Rules/NameValidation) → Phase 2.
- Data (Room: 4 Entities, DAO, DB, Mapper, Repository) → Phase 3.
- DI (Application, AppContainer) → Phase 4.
- UI (ViewModels, StateFlow-/Snapshot-basierte UiStates, Präsentationstrennung, ResultScreen-Schleife) → Phasen 5–6.
- Cleanup (Compose raus, Room/KSP/Lifecycle rein, Debug-Prefill raus, Dialoge lokalisiert, Languages→Kotlin) → Phasen 1, 6.0, 6.3, 6.9.
- Tests (Domain-Unit-Tests; optionaler Room-Roundtrip) → Phasen 2, 3.6.
- Migrationsstrategie & 1:1-Verifikation → Phasen 6, 7.
- Drei bewusste Verhaltensänderungen → 6.3 (Prefill/Dialoge), 7.2 Step 4 (Persistenz).

**2. Platzhalter-Scan** — keine TBD/TODO; alle Code-Schritte enthalten vollständigen Code, alle Befehle haben erwartete Ausgaben.

**3. Typ-Konsistenz** — geprüft:
- `GameRepository`-Methoden (`setPlayerCount`, `setLanguage`, `startGame`, `setPredictions`, `confirmTricks`, `startSecondHalf`) werden in den ViewModels identisch aufgerufen.
- `GameEngine.startGame(playerNames, randomDealer, language, random)` Signatur stimmt zwischen Tests, Engine und Repository-Aufruf (`random` hat Default).
- `PlayerScore` (Package `ui.deal`) wird von DealCards **und** DeclareTricks genutzt (Import in `DeclareTricksUiState`).
- `viewModelFactory { initializer { … } }` (aus `androidx.lifecycle.viewmodel`) + `by viewModels` (aus `androidx.activity`) konsistent in allen sechs Activities; Deps `activity-ktx` + `lifecycle-viewmodel-ktx` in Phase 1 ergänzt.
- `Context.gameRepository`-Extension (`ui/repository.kt`) wird in allen Activity-Factories verwendet.

**Hinweis für den Ausführenden:** Die Template-Tests `ExampleUnitTest.kt` / `ExampleInstrumentedTest.kt` bleiben bestehen (harmlos). Sie können optional in Phase 7 entfernt werden, sind aber nicht Teil des kritischen Pfads.
