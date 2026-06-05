# Phase 3: In-Game-Flow nach Compose – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die fünf verbliebenen Legacy-Activity-Screens nach Jetpack Compose migrieren und in einen NavHost überführen, sodass die App eine vollständige Single-Activity-Compose-App wird; ein gemeinsames `GameViewModel` besitzt `GameState` und ruft `SpadesEngine` direkt.

**Architecture:** `MainActivity` hostet einen `NavHost` mit dem Phase-2-`SetupScreen` als Startziel und fünf neuen Compose-Screens. Alle teilen ein Activity-weites `GameViewModel` (`StateFlow<GameUiState>`), dessen Aktionen direkt an die reine `SpadesEngine` delegieren. Navigation wird im NavHost gesteuert (das VM kennt keinen `NavController`); Zurück bleibt per No-op-`BackHandler` deaktiviert. Der `SpadesGame`-Singleton wird nicht mehr genutzt (toter Code bis Phase 4).

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, BOM 2025.11.01), `androidx.navigation:navigation-compose`, `androidx.lifecycle` ViewModel/StateFlow, JUnit4. Build über `.\gradlew.bat`. Wichtig: Unit-Tests laufen über `.\gradlew.bat testDebugUnitTest --tests "..."` (das Lifecycle-Task `test` lehnt `--tests` ab; ohne `--tests` ist `test` ok).

---

## Geteilte Verträge (von allen Tasks genutzt – exakt einhalten)

```kotlin
// Paket aller neuen Spiel-Klassen:
package com.nwe.spadesscore.ui.game

// State:
data class GameUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH,   // com.nwe.spadesscore.Languages
    val game: GameState? = null                    // com.nwe.spadesscore.domain.GameState
)

// ViewModel-API:
class GameViewModel(random: java.util.Random = java.util.Random()) : ViewModel()
  val uiState: StateFlow<GameUiState>
  fun selectPlayerCount(count: Int)
  fun selectLanguage(language: Languages)
  fun startGame(names: List<String>, randomDealer: Boolean)
  fun setPredictions(predictions: List<Int>)
  fun confirmRound(made: List<Boolean>)
  fun continueSecondHalf()
  fun newGame()
  fun amountOfCards(): Int
  fun isPredictionSumValid(sum: Int): Boolean
  companion object { val Factory: ViewModelProvider.Factory }

// Screen-Signaturen (alle zustandslos gegenüber dem VM; transiente Eingaben via remember):
@Composable fun SetupScreen(playerCount: Int, language: Languages, onSelectPlayerCount: (Int)->Unit, onSelectLanguage: (Languages)->Unit, onNext: ()->Unit, modifier: Modifier = Modifier)  // in ui.setup
@Composable fun Stepper(value: Int, onValueChange: (Int)->Unit, max: Int, modifier: Modifier = Modifier, min: Int = 0)
@Composable fun PlayerNamesScreen(playerCount: Int, onStart: (names: List<String>, randomDealer: Boolean)->Unit, modifier: Modifier = Modifier)
@Composable fun DealCardsScreen(game: GameState, onNext: ()->Unit, modifier: Modifier = Modifier)
@Composable fun DeclareTricksScreen(game: GameState, onConfirm: (predictions: List<Int>)->Unit, modifier: Modifier = Modifier)
@Composable fun ConfirmTricksScreen(game: GameState, onDone: (made: List<Boolean>)->Unit, modifier: Modifier = Modifier)
@Composable fun ResultScreen(game: GameState, onContinue: ()->Unit, onNewGame: ()->Unit, modifier: Modifier = Modifier)
```

**Layout-Muster für alle Spiel-Screens** (vermeidet das „weight in verticalScroll"-Problem): äußere `Column(fillMaxSize)` → scrollbarer Inhalt in `Column(Modifier.weight(1f).verticalScroll(...))` → fixierter Button darunter (ohne weight).

**Routen:** `setup` (Start) · `players` · `deal` · `declare` · `confirm` · `result`.

## File Structure

| Datei | Aktion | Verantwortung |
|------|--------|---------------|
| `gradle/libs.versions.toml` | ändern | `navigation`-Version + `androidx-navigation-compose` |
| `app/build.gradle.kts` | ändern | `implementation(libs.androidx.navigation.compose)` |
| `app/src/main/res/values/strings.xml` | ändern | neue Keys (EN) |
| `app/src/main/res/values-de/strings.xml` | ändern | neue Keys (DE) |
| `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt` | neu | `GameUiState` + `GameViewModel` (Engine-direkt) |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt` | löschen | nach `ui/game` verschoben |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt` | löschen | Port entfällt |
| `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt` | neu | JVM-Tests des neuen VM |
| `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt` | löschen | ersetzt |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt` | ändern | Signatur auf Primitive |
| `app/src/main/java/com/nwe/spadesscore/ui/game/Stepper.kt` | neu | wiederverwendbarer −/+ Stepper |
| `app/src/main/java/com/nwe/spadesscore/ui/game/PreviewData.kt` | neu | `previewGameState()` für @Preview |
| `app/src/main/java/com/nwe/spadesscore/ui/game/PlayerNamesScreen.kt` | neu | Namen + Inline-Validierung + Geber |
| `app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt` | neu | Runde/Geber/Karten/Scores |
| `app/src/main/java/com/nwe/spadesscore/ui/game/DeclareTricksScreen.kt` | neu | Stepper + Summen-Validierung |
| `app/src/main/java/com/nwe/spadesscore/ui/game/ConfirmTricksScreen.kt` | neu | erfüllt?-Checkboxen + Punkte |
| `app/src/main/java/com/nwe/spadesscore/ui/game/ResultScreen.kt` | neu | datengetriebene Tabelle + Medaillen |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.kt` | ändern (2×) | erst VM-Anpassung, dann NavHost |
| `app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.java` u. a. | löschen | nach Migration |
| `app/src/main/res/layout/*.xml` (6 Stück) | löschen | nach Migration |
| `app/src/main/AndroidManifest.xml` | ändern | 5 `<activity>`-Einträge entfernen |
| `CLAUDE.md` | ändern | Architektur-Doku aktualisieren |

---

## Task 1: Navigation-Dependency + neue String-Ressourcen

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Version + Library im Katalog**

In `gradle/libs.versions.toml` unter `[versions]` (nach `constraintlayout = "2.2.1"`) ergänzen:

```toml
navigationCompose = "2.9.0"
```

Unter `[libraries]` (nach `androidx-constraintlayout = ...`) ergänzen:

```toml
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
```

- [ ] **Step 2: Dependency in build.gradle.kts**

In `app/build.gradle.kts` im `dependencies { ... }` direkt nach `implementation(libs.androidx.material3)` ergänzen:

```kotlin
    implementation(libs.androidx.navigation.compose)
```

- [ ] **Step 3: Neue Strings (EN)**

In `app/src/main/res/values/strings.xml` vor `</resources>` ergänzen:

```xml
    <string name="name_empty_error">Please enter a name</string>
    <string name="name_too_long_error">Maximum 10 characters</string>
    <string name="new_game">New Game</string>
    <string name="result_title">Result</string>
```

- [ ] **Step 4: Neue Strings (DE)**

In `app/src/main/res/values-de/strings.xml` vor `</resources>` ergänzen:

```xml
    <string name="name_empty_error">Bitte einen Namen eingeben</string>
    <string name="name_too_long_error">Maximal 10 Zeichen</string>
    <string name="new_game">Neues Spiel</string>
    <string name="result_title">Ergebnis</string>
```

- [ ] **Step 5: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. **Falls** `navigation-compose:2.9.0` nicht auflöst: die Version in `[versions] navigationCompose` auf die neueste stabile `androidx.navigation:navigation-compose`-Version (2.8.x oder 2.9.x) setzen, die Gradle akzeptiert, und erneut bauen. Keine weitere Codeänderung nötig (noch ungenutzt).

- [ ] **Step 6: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "$(printf 'build: add navigation-compose dep and phase-3 string resources\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: `GameViewModel` auf Engine-direkt umbauen (TDD) + Konsumenten anpassen

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`
- Create: `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`
- Delete: `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt`
- Delete: `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt`
- Delete: `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`

- [ ] **Step 1: Failing test schreiben**

Erstelle `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`:

```kotlin
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
```

- [ ] **Step 2: Test laufen lassen – muss fehlschlagen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.ui.game.GameViewModelTest"`
Expected: Kompilierfehler – `unresolved reference: GameViewModel` / `GameUiState`.

- [ ] **Step 3: Neues `GameViewModel` implementieren**

Erstelle `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`:

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.Random

/** UI-State für den gesamten Spielfluss. `game` ist null vor [startGame]. */
data class GameUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH,
    val game: GameState? = null
)

/**
 * Activity-weites ViewModel des gesamten Spiels. Besitzt den unveränderlichen [GameState] und ruft
 * die reine [SpadesEngine] direkt – kein SpadesGame-Singleton. Überlebt Config-Changes als ViewModel
 * (keine Prozesstod-Persistenz; bewusst außerhalb des Umfangs). [random] ist für deterministische
 * Tests injizierbar.
 */
class GameViewModel(private val random: Random = Random()) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    fun selectPlayerCount(count: Int) = _uiState.update { it.copy(playerCount = count) }

    fun selectLanguage(language: Languages) = _uiState.update { it.copy(language = language) }

    fun startGame(names: List<String>, randomDealer: Boolean) {
        val count = _uiState.value.playerCount
        val start = if (randomDealer) SpadesEngine.randomStartingPlayer(count, random) else 0
        _uiState.update { it.copy(game = SpadesEngine.newGame(count, names, start)) }
    }

    fun setPredictions(predictions: List<Int>) =
        _uiState.update { it.copy(game = SpadesEngine.setTickPredictions(requireGame(it.game), predictions)) }

    fun confirmRound(made: List<Boolean>) =
        _uiState.update { it.copy(game = SpadesEngine.confirmTricks(requireGame(it.game), made)) }

    fun continueSecondHalf() =
        _uiState.update { it.copy(game = SpadesEngine.startSecondHalf(requireGame(it.game))) }

    fun newGame() = _uiState.update { it.copy(game = null) }

    fun amountOfCards(): Int = SpadesEngine.amountOfCards(requireGame(_uiState.value.game))

    /** Ungültig, wenn die Summe der Ansagen der möglichen Stichzahl entspricht (heutige Regel). */
    fun isPredictionSumValid(sum: Int): Boolean = sum != amountOfCards()

    private fun requireGame(game: GameState?): GameState =
        requireNotNull(game) { "No game in progress" }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel() }
        }
    }
}
```

- [ ] **Step 4: Test laufen lassen – muss bestehen**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.ui.game.GameViewModelTest"`
Expected: PASS (10 Tests).

- [ ] **Step 5: Alte VM/Port/Test entfernen**

```bash
git rm app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt
```

- [ ] **Step 6: `SetupScreen` auf Primitive umstellen**

Ersetze in `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt` die Funktion `SetupScreen` und die `@Preview` (der private `SegmentedSelector` bleibt unverändert). Neue Signatur/Body:

```kotlin
@Composable
fun SetupScreen(
    playerCount: Int,
    language: Languages,
    onSelectPlayerCount: (Int) -> Unit,
    onSelectLanguage: (Languages) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.spades_logo),
            contentDescription = stringResource(R.string.contentDescription),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
        Spacer(Modifier.weight(1f))

        SegmentedSelector(
            label = stringResource(R.string.number_of_players),
            options = listOf(
                3 to stringResource(R.string.three),
                4 to stringResource(R.string.four)
            ),
            selected = playerCount,
            onSelect = onSelectPlayerCount
        )
        Spacer(Modifier.height(24.dp))
        SegmentedSelector(
            label = stringResource(R.string.language),
            options = listOf(
                Languages.ENGLISH to stringResource(R.string.english),
                Languages.GERMAN to stringResource(R.string.deutsch)
            ),
            selected = language,
            onSelect = onSelectLanguage
        )

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text(
                text = stringResource(R.string.next),
                style = MaterialTheme.typography.titleMedium
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}
```

Und die Preview am Dateiende:

```kotlin
@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    SpadesScoreTheme {
        SetupScreen(
            playerCount = 4,
            language = Languages.ENGLISH,
            onSelectPlayerCount = {},
            onSelectLanguage = {},
            onNext = {}
        )
    }
}
```

- [ ] **Step 7: `MainActivity` an neues VM anpassen (noch Bridge zu Legacy)**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`:

```kotlin
package com.nwe.spadesscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nwe.spadesscore.ui.game.GameViewModel
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme
import java.util.Locale

/**
 * Launcher. Hostet aktuell noch nur den [SetupScreen] (NavHost folgt in einem späteren Task) und
 * überbrückt „Weiter" vorerst per startActivity in den Legacy-Flow. Sprache lebt im
 * [GameViewModel]; Locale wird vor setContent angewandt, Wechsel via recreate().
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLocale(localeCodeFor(gameViewModel.uiState.value.language))

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                val state by gameViewModel.uiState.collectAsStateWithLifecycle()
                SetupScreen(
                    playerCount = state.playerCount,
                    language = state.language,
                    onSelectPlayerCount = gameViewModel::selectPlayerCount,
                    onSelectLanguage = { changeLanguage(it) },
                    onNext = { startActivity(Intent(this, PlayerNamesActivity::class.java)) }
                )
            }
        }
    }

    private fun changeLanguage(language: Languages) {
        if (gameViewModel.uiState.value.language == language) return
        gameViewModel.selectLanguage(language)
        applyLocale(localeCodeFor(language))
        recreate()
    }

    private fun localeCodeFor(language: Languages?): String =
        if (language == Languages.GERMAN) "de" else "en"

    @Suppress("DEPRECATION")
    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}
```

- [ ] **Step 8: Voller Build + Test**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` (keine Referenzen mehr auf `SetupUiState`/`GameSetup`).
Run: `.\gradlew.bat test`
Expected: PASS – `SpadesEngineTest` + neue `GameViewModelTest` (10) + Template-Test.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt app/src/main/java/com/nwe/spadesscore/MainActivity.kt
git commit -m "$(printf 'feat(game): engine-backed GameViewModel; drop GameSetup port\n\nThe shared GameViewModel now owns GameState and calls SpadesEngine directly.\nSetupScreen takes primitives; MainActivity sources language from the VM.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: `Stepper`-Composable + Preview-Daten

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/Stepper.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/PreviewData.kt`

- [ ] **Step 1: Stepper.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Zustandsloser −/Wert/+ Stepper; klemmt auf [min]..[max]. Ersetzt die alte TrickSpinner-View. */
@Composable
fun Stepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    max: Int,
    modifier: Modifier = Modifier,
    min: Int = 0
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedIconButton(onClick = { if (value > min) onValueChange(value - 1) }, enabled = value > min) {
            Text("−", style = MaterialTheme.typography.titleLarge)
        }
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 40.dp).padding(horizontal = 8.dp)
        )
        OutlinedIconButton(onClick = { if (value < max) onValueChange(value + 1) }, enabled = value < max) {
            Text("+", style = MaterialTheme.typography.titleLarge)
        }
    }
}
```

- [ ] **Step 2: PreviewData.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import com.nwe.spadesscore.domain.GameState

/** Beispielzustand nur für @Preview – nicht in Produktionslogik verwenden. */
internal fun previewGameState(playerCount: Int = 4): GameState {
    val names = listOf("Anna", "Ben", "Cleo", "Dan").take(playerCount)
    return GameState(
        playerCount = playerCount,
        playerNames = names,
        currentRound = 3,
        currentPlayer = 1,
        amountOfRounds = 8,
        secondHalf = false,
        showResultScreen = false,
        scores = List(playerCount) { p -> listOf(0, 5 + p, 10 + p, 12 + p) },
        tickPredictions = List(playerCount) { 1 }
    )
}
```

- [ ] **Step 3: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/Stepper.kt app/src/main/java/com/nwe/spadesscore/ui/game/PreviewData.kt
git commit -m "$(printf 'feat(game): reusable Stepper composable + preview fixture\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: `PlayerNamesScreen`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/PlayerNamesScreen.kt`

- [ ] **Step 1: PlayerNamesScreen.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun PlayerNamesScreen(
    playerCount: Int,
    onStart: (names: List<String>, randomDealer: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val names = remember(playerCount) { mutableStateListOf<String>().apply { repeat(playerCount) { add("") } } }
    val errors = remember(playerCount) { mutableStateListOf<Int?>().apply { repeat(playerCount) { add(null) } } }
    var randomDealer by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.set_player_names), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            for (i in 0 until playerCount) {
                OutlinedTextField(
                    value = names[i],
                    onValueChange = { names[i] = it; errors[i] = null },
                    label = { Text(stringResource(playerLabelRes(i))) },
                    singleLine = true,
                    isError = errors[i] != null,
                    supportingText = { errors[i]?.let { Text(stringResource(it)) } },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Checkbox(checked = randomDealer, onCheckedChange = { randomDealer = it })
                Text(stringResource(R.string.random_first_dealer))
            }
        }
        Button(
            onClick = {
                var ok = true
                for (i in 0 until playerCount) {
                    val name = names[i].trim()
                    when {
                        name.isEmpty() -> { errors[i] = R.string.name_empty_error; ok = false }
                        name.length > 10 -> { errors[i] = R.string.name_too_long_error; ok = false }
                        else -> errors[i] = null
                    }
                }
                if (ok) onStart((0 until playerCount).map { names[it].trim() }, randomDealer)
            },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.next), style = MaterialTheme.typography.titleMedium)
        }
    }
}

private fun playerLabelRes(index: Int): Int = when (index) {
    0 -> R.string.player1_name
    1 -> R.string.player2_name
    2 -> R.string.player3_name
    else -> R.string.player4_name
}

@Preview(showBackground = true)
@Composable
private fun PlayerNamesScreenPreview() {
    SpadesScoreTheme {
        PlayerNamesScreen(playerCount = 4, onStart = { _, _ -> })
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/PlayerNamesScreen.kt
git commit -m "$(printf 'feat(game): PlayerNamesScreen with localized inline validation\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: `DealCardsScreen`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt`

- [ ] **Step 1: DealCardsScreen.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun DealCardsScreen(
    game: GameState,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.round, game.currentRound), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { game.currentRound.toFloat() / game.amountOfRounds },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.player_must_deal_cards, game.playerNames[game.currentPlayer]),
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "${SpadesEngine.amountOfCards(game)}x",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(24.dp))
            for (i in 0 until game.playerCount) {
                ScoreCard(name = game.playerNames[i], score = game.scores[i].last())
            }
        }
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text(stringResource(R.string.start), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun ScoreCard(name: String, score: Int, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(score.toString(), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DealCardsScreenPreview() {
    SpadesScoreTheme {
        DealCardsScreen(game = previewGameState(), onNext = {})
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt
git commit -m "$(printf 'feat(game): DealCardsScreen\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: `DeclareTricksScreen`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/DeclareTricksScreen.kt`

- [ ] **Step 1: DeclareTricksScreen.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun DeclareTricksScreen(
    game: GameState,
    onConfirm: (predictions: List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val predictions = remember(game) { mutableStateListOf<Int>().apply { repeat(game.playerCount) { add(0) } } }
    val possible = SpadesEngine.amountOfCards(game)
    val sum = predictions.sum()
    val valid = sum != possible
    val tricksWord = stringResource(if (possible == 1) R.string.trick else R.string.tricks)

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.round, game.currentRound), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { game.currentRound.toFloat() / game.amountOfRounds },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            for (i in 0 until game.playerCount) {
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(game.playerNames[i], style = MaterialTheme.typography.titleMedium)
                            Text(
                                game.scores[i].last().toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Stepper(value = predictions[i], onValueChange = { predictions[i] = it }, max = possible)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                color = if (valid) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.combined_trick_prediction, sum, possible, tricksWord),
                    modifier = Modifier.padding(12.dp),
                    color = if (valid) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        Button(
            onClick = { onConfirm(predictions.toList()) },
            enabled = valid,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.confirm_ticks), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DeclareTricksScreenPreview() {
    SpadesScoreTheme {
        DeclareTricksScreen(game = previewGameState(), onConfirm = {})
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/DeclareTricksScreen.kt
git commit -m "$(printf 'feat(game): DeclareTricksScreen with stepper and sum validation\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7: `ConfirmTricksScreen`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/ConfirmTricksScreen.kt`

- [ ] **Step 1: ConfirmTricksScreen.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun ConfirmTricksScreen(
    game: GameState,
    onDone: (made: List<Boolean>) -> Unit,
    modifier: Modifier = Modifier
) {
    val made = remember(game) { mutableStateListOf<Boolean>().apply { repeat(game.playerCount) { add(false) } } }

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(stringResource(R.string.round, game.currentRound), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { game.currentRound.toFloat() / game.amountOfRounds },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            for (i in 0 until game.playerCount) {
                val prediction = game.tickPredictions[i]
                val tricksWord = stringResource(if (prediction == 1) R.string.trick else R.string.tricks)
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(game.playerNames[i], style = MaterialTheme.typography.titleMedium)
                            Text(
                                "$prediction $tricksWord",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                stringResource(R.string.points_added, prediction + 5),
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (made[i]) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Checkbox(checked = made[i], onCheckedChange = { made[i] = it })
                    }
                }
            }
        }
        Button(
            onClick = { onDone(made.toList()) },
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text(stringResource(R.string.done), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfirmTricksScreenPreview() {
    SpadesScoreTheme {
        ConfirmTricksScreen(game = previewGameState(), onDone = {})
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/ConfirmTricksScreen.kt
git commit -m "$(printf 'feat(game): ConfirmTricksScreen with per-player made checkboxes\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 8: `ResultScreen`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/game/ResultScreen.kt`

- [ ] **Step 1: ResultScreen.kt erstellen**

```kotlin
package com.nwe.spadesscore.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.R
import com.nwe.spadesscore.domain.GameState
import com.nwe.spadesscore.domain.SpadesEngine
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun ResultScreen(
    game: GameState,
    onContinue: () -> Unit,
    onNewGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val roundsPlayed = game.scores[0].size - 1
    val ranking = SpadesEngine.rankingForLastRound(game) // Indizes höchster Score zuerst
    val isFinal = game.secondHalf

    Column(
        modifier = modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp)
    ) {
        Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(16.dp))

        // Kopfzeile: Spielernamen
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(32.dp))
            for (p in 0 until game.playerCount) {
                Text(
                    game.playerNames[p],
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            for (round in 1..roundsPlayed) {
                val isLast = round == roundsPlayed
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        round.toString(),
                        modifier = Modifier.width(32.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    for (p in 0 until game.playerCount) {
                        val value = game.scores[p][round]
                        val place = ranking.indexOf(p)
                        if (isLast && place < 3) {
                            Box(
                                modifier = Modifier.weight(1f).padding(horizontal = 2.dp)
                                    .background(medalColor(place), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    value.toString(),
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF1A1C18)
                                )
                            }
                        } else {
                            Text(
                                value.toString(),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                style = if (isLast) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
                                color = LocalContentColor.current
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        if (isFinal) {
            Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.new_game), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text(stringResource(R.string.continue_game), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Gold/Silber/Bronze als gut lesbarer Hintergrund-Chip (Verfeinerung der Spec-Farben fürs helle Theme). */
private fun medalColor(place: Int): Color = when (place) {
    0 -> Color(0xFFFFD700) // Gold
    1 -> Color(0xFFC0C0C0) // Silber
    else -> Color(0xFFCD7F32) // Bronze
}

@Preview(showBackground = true)
@Composable
private fun ResultScreenPreview() {
    SpadesScoreTheme {
        ResultScreen(game = previewGameState(), onContinue = {}, onNewGame = {})
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/ResultScreen.kt
git commit -m "$(printf 'feat(game): data-driven ResultScreen with medal-colored final row\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 9: NavHost in `MainActivity` verdrahten

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`

> Ersetzt den `setContent`-Body durch einen NavHost, der alle sechs Routen verdrahtet und die Verzweigungen (confirm→deal/result, result→deal/setup) anhand des `GameUiState` steuert. Zurück wird per No-op-`BackHandler` deaktiviert. `onCreate`-Locale-Logik und die `changeLanguage`/`localeCodeFor`/`applyLocale`-Helfer bleiben.

- [ ] **Step 1: MainActivity.kt ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`:

```kotlin
package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.nwe.spadesscore.ui.game.ConfirmTricksScreen
import com.nwe.spadesscore.ui.game.DealCardsScreen
import com.nwe.spadesscore.ui.game.DeclareTricksScreen
import com.nwe.spadesscore.ui.game.GameViewModel
import com.nwe.spadesscore.ui.game.PlayerNamesScreen
import com.nwe.spadesscore.ui.game.ResultScreen
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme
import java.util.Locale

private object Routes {
    const val SETUP = "setup"
    const val PLAYERS = "players"
    const val DEAL = "deal"
    const val DECLARE = "declare"
    const val CONFIRM = "confirm"
    const val RESULT = "result"
}

/**
 * Single-Activity-Host. Ein NavHost trägt Setup + alle Spiel-Screens; ein gemeinsames
 * [GameViewModel] hält den Zustand. Zurück ist im gesamten Flow deaktiviert. Sprache lebt im VM;
 * Locale wird vor setContent angewandt, Wechsel via recreate().
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyLocale(localeCodeFor(gameViewModel.uiState.value.language))

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                BackHandler(enabled = true) { /* Zurück im gesamten Flow deaktiviert */ }
                val navController = rememberNavController()
                val state by gameViewModel.uiState.collectAsStateWithLifecycle()

                NavHost(navController = navController, startDestination = Routes.SETUP) {
                    composable(Routes.SETUP) {
                        SetupScreen(
                            playerCount = state.playerCount,
                            language = state.language,
                            onSelectPlayerCount = gameViewModel::selectPlayerCount,
                            onSelectLanguage = { changeLanguage(it) },
                            onNext = { navController.navTo(Routes.PLAYERS) }
                        )
                    }
                    composable(Routes.PLAYERS) {
                        PlayerNamesScreen(
                            playerCount = state.playerCount,
                            onStart = { names, randomDealer ->
                                gameViewModel.startGame(names, randomDealer)
                                navController.navTo(Routes.DEAL)
                            }
                        )
                    }
                    composable(Routes.DEAL) {
                        state.game?.let { game ->
                            DealCardsScreen(game = game, onNext = { navController.navTo(Routes.DECLARE) })
                        }
                    }
                    composable(Routes.DECLARE) {
                        state.game?.let { game ->
                            DeclareTricksScreen(
                                game = game,
                                onConfirm = { predictions ->
                                    gameViewModel.setPredictions(predictions)
                                    navController.navTo(Routes.CONFIRM)
                                }
                            )
                        }
                    }
                    composable(Routes.CONFIRM) {
                        state.game?.let { game ->
                            ConfirmTricksScreen(
                                game = game,
                                onDone = { made ->
                                    gameViewModel.confirmRound(made)
                                    val showResult = gameViewModel.uiState.value.game?.showResultScreen == true
                                    navController.navTo(if (showResult) Routes.RESULT else Routes.DEAL)
                                }
                            )
                        }
                    }
                    composable(Routes.RESULT) {
                        state.game?.let { game ->
                            ResultScreen(
                                game = game,
                                onContinue = {
                                    gameViewModel.continueSecondHalf()
                                    navController.navTo(Routes.DEAL)
                                },
                                onNewGame = {
                                    gameViewModel.newGame()
                                    navController.navTo(Routes.SETUP)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeLanguage(language: Languages) {
        if (gameViewModel.uiState.value.language == language) return
        gameViewModel.selectLanguage(language)
        applyLocale(localeCodeFor(language))
        recreate()
    }

    private fun localeCodeFor(language: Languages?): String =
        if (language == Languages.GERMAN) "de" else "en"

    @Suppress("DEPRECATION")
    private fun applyLocale(code: String) {
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

/** Vorwärts-Navigation ohne sichtbaren Back-Stack (Wizard). */
private fun NavHostController.navTo(route: String) = navigate(route) {
    popUpTo(graph.startDestinationId) { inclusive = true }
    launchSingleTop = true
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Die App startet jetzt vollständig in Compose; die Legacy-Activities sind nicht mehr erreichbar (werden in Task 10 gelöscht).

- [ ] **Step 3: Voller Test**

Run: `.\gradlew.bat test`
Expected: PASS (unverändert grün).

- [ ] **Step 4: Manueller Smoke-Test (empfohlen)**

Run: `.\gradlew.bat installDebug` auf Gerät/Emulator. Prüfen:
- Setup → „Weiter" → Namen (leer lassen → Inline-Fehler; gültige Namen) → Spiel startet.
- Deal → Declare (Summe == mögliche Stiche ⇒ Banner rot, „Weiter" deaktiviert; gültige Summe ⇒ grün) → Confirm (Häkchen) → nächste Runde.
- Vollständiges 4p- und 3p-Spiel inkl. Halbzeit-Result („Weiter") und Spielende-Result („Neues Spiel" → zurück zu Setup).
- Zurück-Geste/-Taste tut nirgends etwas.
- Sprache auf Setup umschaltbar (DE/EN), App-Sprache ändert sich.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/MainActivity.kt
git commit -m "$(printf 'feat: single-activity NavHost wiring the full Compose game flow\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 10: Legacy-Activities, Basisklasse, TrickSpinner, Layouts & Manifest entfernen

**Files:**
- Delete: 5 Activity-`.java`, `SpadesAppCompatActivity.java`, `TrickSpinner.kt`, 6 Layout-XMLs
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Dateien löschen**

```bash
git rm \
  app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.java \
  app/src/main/java/com/nwe/spadesscore/DealCardsActivity.java \
  app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.java \
  app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.java \
  app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.java \
  app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.java \
  app/src/main/java/com/nwe/spadesscore/TrickSpinner.kt \
  app/src/main/res/layout/activity_player_names.xml \
  app/src/main/res/layout/activity_deal_cards.xml \
  app/src/main/res/layout/activity_declare_tricks.xml \
  app/src/main/res/layout/activity_confirm_tricks.xml \
  app/src/main/res/layout/activity_result_screen.xml \
  app/src/main/res/layout/view_trick_spinner.xml
```

- [ ] **Step 2: Verbliebene Referenz in `SpadesGame.java` entfernen**

`SpadesGame.java` bleibt (toter Code bis Phase 4), referenziert aber die soeben gelöschte `DeclareTricksActivity` in einer Methoden-Signatur und würde sonst den Build brechen. Öffne `app/src/main/java/com/nwe/spadesscore/SpadesGame.java` und ändere die Methode `getCombinedTrickPredictionString`, sodass der Parameter ein `android.content.Context` statt `DeclareTricksActivity` ist (`Context` ist bereits importiert). Ersetze:

```java
    public String getCombinedTrickPredictionString(DeclareTricksActivity declareTricksActivity, int tricks1, int tricks2, int tricks3, int tricks4) {
        final int combinedTricks = tricks1 + tricks2 + tricks3 + (playerCount == 4 ? tricks4 : 0);
        final int possibleTricks = getAmountOfCards();
        return String.format(Locale.getDefault(), declareTricksActivity.getString(R.string.combined_trick_prediction), combinedTricks, possibleTricks, declareTricksActivity.getString(R.string.tricks));
    }
```

durch:

```java
    public String getCombinedTrickPredictionString(Context context, int tricks1, int tricks2, int tricks3, int tricks4) {
        final int combinedTricks = tricks1 + tricks2 + tricks3 + (playerCount == 4 ? tricks4 : 0);
        final int possibleTricks = getAmountOfCards();
        return String.format(Locale.getDefault(), context.getString(R.string.combined_trick_prediction), combinedTricks, possibleTricks, context.getString(R.string.tricks));
    }
```

(Prüfe per Grep, dass keine andere Datei `getCombinedTrickPredictionString` oder andere gelöschte Activity-Typen referenziert — es sollte keine geben.)

- [ ] **Step 3: Manifest bereinigen**

In `app/src/main/AndroidManifest.xml` die fünf Spiel-Activity-Einträge entfernen (die `.MainActivity` mit Launcher-Intent-Filter und der `preloaded_fonts`-`meta-data`-Eintrag bleiben). Lösche genau diese fünf Zeilen:

```xml
        <activity android:name=".PlayerNamesActivity" />
        <activity android:name=".ResultScreenActivity" />
        <activity android:name=".DealCardsActivity" />
        <activity android:name=".DeclareTricksActivity" />
        <activity android:name=".ConfirmTicksActivity" />
```

- [ ] **Step 4: Build + Test verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Falls der Build über eine verbliebene Referenz auf eine gelöschte Klasse/ein gelöschtes Layout (z. B. `ViewTrickSpinnerBinding`, `R.layout.activity_*`) klagt: prüfen, dass keine andere Datei darauf verweist (es sollte keine geben) und melden statt zu raten.
Run: `.\gradlew.bat test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/com/nwe/spadesscore/ app/src/main/res/layout/ app/src/main/AndroidManifest.xml
git commit -m "$(printf 'refactor: remove legacy view-based screens now replaced by Compose\n\nDeletes the 5 game Activities, the SpadesAppCompatActivity base, TrickSpinner +\nits layout, the 6 game layout XMLs, and their manifest entries. Fixes the lone\nSpadesGame reference to the removed DeclareTricksActivity; SpadesGame stays as\ndead code until Phase 4.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 11: CLAUDE.md-Architekturdoku aktualisieren

**Files:**
- Modify: `CLAUDE.md`

> Die Doku beschreibt noch den Activity-/View-Stand. Nach Phase 3 ist die App Single-Activity-Compose. Aktualisiere die betroffenen Abschnitte sachlich.

- [ ] **Step 1: Abschnitt „Architecture" aktualisieren**

Ersetze in `CLAUDE.md` den Satz, der die Sprachen-Mischung beschreibt, sinngemäß so, dass er den neuen Stand trifft. Konkret ersetze den Absatz unter **Architecture**, der mit „**Language mix:**" beginnt, durch:

```markdown
**Language mix:** The UI is **Jetpack Compose** (Kotlin) — a single `MainActivity` hosting a `NavHost` (`setup` → `players` → `deal` → `declare` → `confirm` → `result`). Game rules live in the pure Kotlin domain core (`domain/GameState` + `domain/SpadesEngine`). `SpadesGame.java` is a now-unused legacy singleton kept only until Phase 4 removes it. The Compose theme lives in `ui/theme/`, the setup screen in `ui/setup/`, and the in-game screens + shared `GameViewModel` in `ui/game/`.
```

- [ ] **Step 2: Abschnitt „State lives in one in-memory singleton" aktualisieren**

Ersetze den Absatz, der mit „**State lives in one in-memory singleton: `SpadesGame`**" beginnt, durch:

```markdown
**State lives in one Activity-scoped `GameViewModel`** (`ui/game/GameViewModel.kt`). It holds `GameUiState` (player count, language, and the immutable `GameState`) as a `StateFlow`, and every action delegates directly to `SpadesEngine`. State survives configuration changes via the ViewModel; **there is no persistence** — process death loses everything (out of scope). The legacy `SpadesGame` singleton is no longer used.
```

- [ ] **Step 3: Veraltete Abschnitte ersetzen**

Ersetze den gesamten Abschnitt **Activity flow** (die Überschrift, der Einleitungssatz und der Code-Block mit der Pfeil-Kette) durch:

```markdown
**Screen flow** (Compose routes in one `NavHost`, forward-only, back disabled):

```
setup (player count 3/4, language EN/DE)
  → players (names, "random dealer" checkbox) → startGame()
    → deal (who deals, how many cards)
      → declare (each player picks a trick prediction via Stepper)
        → confirm (checkbox per player: did they make it?) → confirmRound()
          → back to deal for next round
          → or result when the half/game ends
            → "Continue" → startSecondHalf() and back to deal
            → at game end "New Game" → reset and back to setup
```
```

Ersetze außerdem den Abschnitt **Shared activity template** (Überschrift + Absatz) durch:

```markdown
**Shared host:** All screens are stateless `@Composable` functions under `ui/game/` (and `ui/setup/SetupScreen`), hosted by the single `MainActivity` NavHost and driven by the shared `GameViewModel`. There is no Activity base class anymore.
```

Ersetze den Abschnitt **ResultScreenActivity caveat** (Überschrift + Absatz) durch:

```markdown
## ResultScreen

The result table is **data-driven** from `GameState.scores` (`ui/game/ResultScreen.kt`): one row per played round, one column per player, vertically scrollable. The last row is colored gold/silver/bronze via `SpadesEngine.rankingForLastRound`. No hardcoded round capacity.
```

Ersetze den Abschnitt **3-player vs 4-player handling** (Überschrift + Absatz) durch:

```markdown
## 3-player vs 4-player handling

The Compose screens iterate `0 until playerCount`, so the 4th player simply isn't rendered for 3-player games — no `View.GONE` workarounds.
```

Entferne den Abschnitt **Known scaffolding to clean up** (Überschrift + Absatz über `DEBUG_DELETE_LATER`) vollständig — das Debug-Prefill existiert nicht mehr.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "$(printf 'docs: update CLAUDE.md for the Compose single-activity architecture\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Definition of Done (Phase 3)

- [ ] Alle 5 Spiel-Screens sind Compose-Ziele in einem NavHost in `MainActivity`; `SetupScreen` ist das Startziel (kein `startActivity`-Bridge mehr).
- [ ] Ein gemeinsames `GameViewModel` besitzt `GameState` als `StateFlow` und ruft `SpadesEngine` direkt; `GameSetup`/`SpadesGameSetup`/`SetupUiState` sind entfernt.
- [ ] M3-Refresh konsistent mit Phase 2; `playerCount == 3` rendert den 4. Slot nicht.
- [ ] Validierung lokalisiert + inline; kein Debug-Prefill; „Neues Spiel" am Spielende.
- [ ] Legacy-Activities/Basisklasse/`TrickSpinner`/Layout-XMLs gelöscht; die 5 `<activity>`-Manifest-Einträge entfernt; `SpadesGame.java` bleibt (Phase 4).
- [ ] Zurück bleibt im gesamten Flow deaktiviert.
- [ ] `.\gradlew.bat test` grün (inkl. `GameViewModelTest`, 10 Tests); `.\gradlew.bat assembleDebug` erfolgreich.
- [ ] Ein vollständiges Spiel (3p und 4p), inkl. Halbzeit-Result, zweiter Hälfte und „Neues Spiel", ist durchgängig spielbar.
- [ ] `CLAUDE.md` spiegelt die neue Architektur wider.
