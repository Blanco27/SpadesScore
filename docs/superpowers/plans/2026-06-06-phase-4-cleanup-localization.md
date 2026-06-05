# Phase 4: Final Cleanup & AppCompatDelegate Localization — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the dead `SpadesGame.java`/`Languages.java` legacy, switch localization to `AppCompatDelegate` per-app locales (persisted, single source of truth), migrate the base theme off `Theme.MaterialComponents` to drop `com.google.android.material` (+ other dead deps and the `viewBinding` flag), and close the three Phase-3 review nits.

**Architecture:** Language becomes owned by `AppCompatDelegate` (autoStoreLocales) instead of `GameViewModel`; the setup selector reads/writes BCP-47 tags. `GameUiState` shrinks to `(playerCount, game)`. The XML theme keeps only a window background matching the Compose surface; the real palette stays in the Compose `SpadesScoreTheme`. Each task is build-green and the app stays playable.

**Tech Stack:** Kotlin, Jetpack Compose (M3), `androidx.appcompat` 1.7.1 (`AppCompatDelegate.setApplicationLocales` + `autoStoreLocales`), `androidx.core` (`LocaleListCompat`), JUnit4. Build on Windows via `.\gradlew.bat`. Single test class: `.\gradlew.bat testDebugUnitTest --tests "FQN"` (the lifecycle `test` task rejects `--tests`; plain `.\gradlew.bat test` runs the full suite).

**Branch note:** This plan builds on Phase 3 (branch `modernization/phase-3-compose-flow`). Execute it on a branch based off that code (branch from it, or after the Phase-3 PR merges to `master`), so the deletions/edits apply against the already-migrated Compose app.

---

## File Structure

| File | Action | Responsibility |
|------|--------|----------------|
| `app/src/main/java/com/nwe/spadesscore/SpadesGame.java` | delete | dead legacy singleton |
| `app/src/main/java/com/nwe/spadesscore/Languages.java` | delete | obsolete Java enum (replaced by BCP-47 tags) |
| `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt` | modify | drop `language` from state + dead methods + pure `startGame` |
| `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt` | modify | drop language/validity tests |
| `app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt` | modify | localize card-count string |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt` | modify | tag-based language selector |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.kt` | modify | AppCompatDelegate locale wiring |
| `app/src/main/AndroidManifest.xml` | modify | locales service + `localeConfig` |
| `app/src/main/res/xml/locales_config.xml` | create | declared app locales en/de |
| `app/src/main/res/values/strings.xml` | modify | new `amount_of_cards` (EN) |
| `app/src/main/res/values-de/strings.xml` | modify | new `amount_of_cards` (DE) |
| `app/src/main/res/values/themes.xml` | modify | reparent to AppCompat, window bg only |
| `app/src/main/res/values-night/themes.xml` | delete | night theme no longer differs |
| `app/src/main/res/values/colors.xml` | modify | add `window_background` (light) |
| `app/src/main/res/values-night/colors.xml` | create | `window_background` (dark) |
| `app/build.gradle.kts` | modify | drop `viewBinding` + 4 deps |
| `gradle/libs.versions.toml` | modify | drop 4 versions + 4 libraries |
| `CLAUDE.md` | modify | doc the new localization + singleton removal |

---

## Task 1: Delete the dead `SpadesGame.java` singleton

`SpadesGame` has no production caller (verified: only its own definition + one KDoc mention). `Languages.java` is **not** deleted here — live Compose code still references it until Task 3.

**Files:**
- Delete: `app/src/main/java/com/nwe/spadesscore/SpadesGame.java`

- [ ] **Step 1: Confirm there are no references**

Run a search for `SpadesGame` across the app sources:

Run: `.\gradlew.bat` is not needed — use the repo grep. Expected: the only hits are `SpadesGame.java` itself and the KDoc line `* die reine [SpadesEngine] direkt – kein SpadesGame-Singleton...` in `ui/game/GameViewModel.kt`. If any **other** file references `SpadesGame`, STOP and report — do not delete.

- [ ] **Step 2: Delete the file**

```bash
git rm app/src/main/java/com/nwe/spadesscore/SpadesGame.java
```

- [ ] **Step 3: Build + test**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.
Run: `.\gradlew.bat test`
Expected: PASS (unchanged green).

- [ ] **Step 4: Commit**

```bash
git commit -m "$(printf 'refactor: remove dead SpadesGame legacy singleton\n\nNo production caller remained after the Phase 3 Compose migration; game\nstate lives in GameViewModel + SpadesEngine.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: Phase-3 follow-up nits

Three small fixes: make `GameViewModel.startGame`'s `update` lambda pure, delete the two unused `GameViewModel` helpers (+ their test), and route the DealCards card-count literal through a string resource.

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`
- Modify: `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt`

- [ ] **Step 1: Make `startGame` pure and remove the dead helpers**

In `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`, replace the current `startGame` (expression-body form that draws the RNG inside the `update` lambda):

```kotlin
    fun startGame(names: List<String>, randomDealer: Boolean) = _uiState.update {
        val start = if (randomDealer) SpadesEngine.randomStartingPlayer(it.playerCount, random) else 0
        it.copy(game = SpadesEngine.newGame(it.playerCount, names, start))
    }
```

with the block form that computes `start` (including the RNG draw) **before** `update`:

```kotlin
    fun startGame(names: List<String>, randomDealer: Boolean) {
        val start = if (randomDealer) SpadesEngine.randomStartingPlayer(_uiState.value.playerCount, random) else 0
        _uiState.update { it.copy(game = SpadesEngine.newGame(it.playerCount, names, start)) }
    }
```

Then delete these two unused methods (and their preceding KDoc comment) entirely:

```kotlin
    fun amountOfCards(): Int = SpadesEngine.amountOfCards(requireGame(_uiState.value.game))

    /** Ungültig, wenn die Summe der Ansagen der möglichen Stichzahl entspricht (heutige Regel). */
    fun isPredictionSumValid(sum: Int): Boolean = sum != amountOfCards()
```

Leave `private fun requireGame(...)` in place — it is still used by `setPredictions`/`confirmRound`/`continueSecondHalf`.

- [ ] **Step 2: Delete the orphaned test**

In `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`, delete the entire test that exercises the removed method:

```kotlin
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
```

`assertFalse`/`assertTrue` remain used by other tests (`confirmRound_lastRoundOfHalf_flagsResultScreen`, `continueSecondHalf_...`, `startGame_randomDealer_...`), so leave their imports.

- [ ] **Step 3: Add the `amount_of_cards` string (EN + DE)**

In `app/src/main/res/values/strings.xml`, before `</resources>`:

```xml
    <string name="amount_of_cards">%dx</string>
```

In `app/src/main/res/values-de/strings.xml`, before `</resources>`:

```xml
    <string name="amount_of_cards">%dx</string>
```

- [ ] **Step 4: Use the string in DealCardsScreen**

In `app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt`, replace the hardcoded suffix:

```kotlin
            Text(
                "${SpadesEngine.amountOfCards(game)}x",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
```

with the localized form:

```kotlin
            Text(
                stringResource(R.string.amount_of_cards, SpadesEngine.amountOfCards(game)),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
```

(`stringResource` and `R` are already imported in this file.)

- [ ] **Step 5: Build + test**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.ui.game.GameViewModelTest"`
Expected: PASS (one fewer test than before; no reference to the removed methods).
Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml app/src/main/java/com/nwe/spadesscore/ui/game/DealCardsScreen.kt
git commit -m "$(printf 'refactor(game): pure startGame update, drop dead VM helpers, localize card count\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: Localization → AppCompatDelegate per-app locales

The coupled change: `GameUiState` drops `language`, `GameViewModel` drops `selectLanguage`, `SetupScreen` takes a `languageTag: String`, `MainActivity` reads/writes the locale via `AppCompatDelegate`, the manifest gains the autoStore service + `localeConfig`, and `Languages.java` is deleted. All in one commit to keep the build green.

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`
- Modify: `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Create: `app/src/main/res/xml/locales_config.xml`
- Delete: `app/src/main/java/com/nwe/spadesscore/Languages.java`

- [ ] **Step 1: Shrink `GameUiState` and drop `selectLanguage` in the ViewModel**

In `app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt`:

Remove the import:

```kotlin
import com.nwe.spadesscore.Languages
```

Replace the `GameUiState` data class:

```kotlin
/** UI-State für den gesamten Spielfluss. `game` ist null vor [startGame]. */
data class GameUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH,
    val game: GameState? = null
)
```

with:

```kotlin
/** UI-State für den gesamten Spielfluss. `game` ist null vor [startGame]. Die Sprache lebt
 *  nicht hier, sondern in [androidx.appcompat.app.AppCompatDelegate] (per-app locales). */
data class GameUiState(
    val playerCount: Int = 4,
    val game: GameState? = null
)
```

Delete the `selectLanguage` method:

```kotlin
    fun selectLanguage(language: Languages) = _uiState.update { it.copy(language = language) }
```

Update the class KDoc line that mentions the now-deleted singleton — replace:

```kotlin
 * die reine [SpadesEngine] direkt – kein SpadesGame-Singleton. Überlebt Config-Changes als ViewModel
```

with:

```kotlin
 * die reine [SpadesEngine] direkt. Überlebt Config-Changes als ViewModel
```

- [ ] **Step 2: Update the ViewModel tests**

In `app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt`:

Remove the import:

```kotlin
import com.nwe.spadesscore.Languages
```

Replace the initial-state test:

```kotlin
    @Test
    fun initialState_isDefault() {
        assertEquals(GameUiState(playerCount = 4, language = Languages.ENGLISH, game = null), vm().uiState.value)
    }
```

with:

```kotlin
    @Test
    fun initialState_isDefault() {
        assertEquals(GameUiState(playerCount = 4, game = null), vm().uiState.value)
    }
```

Delete the language test entirely:

```kotlin
    @Test
    fun selectLanguage_updatesState() {
        val vm = vm()
        vm.selectLanguage(Languages.GERMAN)
        assertEquals(Languages.GERMAN, vm.uiState.value.language)
    }
```

- [ ] **Step 3: Make the SetupScreen language selector tag-based**

In `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt`:

Remove the import:

```kotlin
import com.nwe.spadesscore.Languages
```

Replace the entire public `SetupScreen` function:

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

with (params `language`→`languageTag: String`, `onSelectLanguage: (String) -> Unit`, and the language `SegmentedSelector` using tag strings):

```kotlin
@Composable
fun SetupScreen(
    playerCount: Int,
    languageTag: String,
    onSelectPlayerCount: (Int) -> Unit,
    onSelectLanguage: (String) -> Unit,
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
                "en" to stringResource(R.string.english),
                "de" to stringResource(R.string.deutsch)
            ),
            selected = languageTag,
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

Then replace the preview:

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

with:

```kotlin
@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    SpadesScoreTheme {
        SetupScreen(
            playerCount = 4,
            languageTag = "en",
            onSelectPlayerCount = {},
            onSelectLanguage = {},
            onNext = {}
        )
    }
}
```

The private generic `SegmentedSelector` composable is unchanged (it already works with any `T`, here `String`). If reading the file reveals it is **not** generic (e.g. it is typed to `Languages`), STOP and report — the plan assumes a generic `SegmentedSelector<T>`.

- [ ] **Step 4: Rewrite `MainActivity` to drive locale via AppCompatDelegate**

Replace the **entire** contents of `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`:

```kotlin
package com.nwe.spadesscore

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.core.os.LocaleListCompat
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
 * [GameViewModel] hält den Zustand. Zurück ist im gesamten Flow deaktiviert. Die Sprache wird über
 * [AppCompatDelegate] per-app locales gesetzt (autoStoreLocales persistiert sie; AppCompat ruft
 * recreate() selbst auf), nur auf dem Setup-Screen änderbar.
 */
class MainActivity : AppCompatActivity() {

    private val gameViewModel: GameViewModel by viewModels { GameViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                            languageTag = currentLanguageTag(),
                            onSelectPlayerCount = gameViewModel::selectPlayerCount,
                            onSelectLanguage = { setLanguage(it) },
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

    /** Setzt die App-Sprache; AppCompat persistiert (autoStoreLocales) und ruft recreate() selbst. */
    private fun setLanguage(tag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    /** Aktuell wirksamer Sprach-Tag für den Selektor: app-locale, sonst die aufgelöste Config-Locale. */
    private fun currentLanguageTag(): String {
        val appLocales = AppCompatDelegate.getApplicationLocales()
        val locale = if (appLocales.size() > 0) appLocales.get(0) else resources.configuration.locales.get(0)
        return if (locale?.language == "de") "de" else "en"
    }
}

/** Vorwärts-Navigation ohne sichtbaren Back-Stack (Wizard). */
private fun NavHostController.navTo(route: String) = navigate(route) {
    popUpTo(graph.id) { inclusive = true }
    launchSingleTop = true
}
```

- [ ] **Step 5: Add the locales service + `localeConfig` to the manifest**

In `app/src/main/AndroidManifest.xml`, add `android:localeConfig="@xml/locales_config"` to the `<application>` element (next to the existing `android:theme=...` line):

```xml
        android:localeConfig="@xml/locales_config"
```

And add the autoStore service inside `<application>`, right after the `</activity>` close tag and before the `preloaded_fonts` `<meta-data>`:

```xml
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <meta-data
                android:name="autoStoreLocales"
                android:value="true" />
        </service>
```

- [ ] **Step 6: Create `locales_config.xml`**

Create `app/src/main/res/xml/locales_config.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="de" />
</locale-config>
```

- [ ] **Step 7: Delete the obsolete `Languages.java`**

All references are now gone (SpadesGame deleted in Task 1; GameViewModel/SetupScreen/MainActivity/tests updated above):

```bash
git rm app/src/main/java/com/nwe/spadesscore/Languages.java
```

Verify nothing references `Languages` anymore (search the app sources). Expected: no hits. If any remain, fix them before building.

- [ ] **Step 8: Build + test**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL` (no references to `Languages` or `applyLocale`/`changeLanguage`).
Run: `.\gradlew.bat test`
Expected: PASS (`GameViewModelTest` now without the two language/validity tests; `SpadesEngineTest`; template test).

- [ ] **Step 9: Manual smoke (recommended)**

Run: `.\gradlew.bat installDebug` on a device/emulator. Verify:
- Setup shows the language toggle reflecting the current language; switching EN↔DE re-renders all strings.
- Kill and relaunch the app → the chosen language persists.
- Android 13+: Settings → Apps → SpadesScore → Language lists English + Deutsch.
- A full game still plays; back gesture stays inert.

- [ ] **Step 10: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/game/GameViewModel.kt app/src/test/java/com/nwe/spadesscore/ui/game/GameViewModelTest.kt app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt app/src/main/java/com/nwe/spadesscore/MainActivity.kt app/src/main/AndroidManifest.xml app/src/main/res/xml/locales_config.xml
git commit -m "$(printf 'feat: localize via AppCompatDelegate per-app locales; drop Languages enum\n\nLanguage now lives in AppCompatDelegate (autoStoreLocales + locales_config) as the\nsingle source of truth and persists across restarts. GameUiState no longer carries\nlanguage; MainActivity reads/writes the locale tag and the setup selector is tag-based.\nDeletes the obsolete Java Languages enum.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

(The `git rm` from Step 7 already staged the `Languages.java` deletion, so it is included.)

---

## Task 4: Theme migration + dependency cleanup

Reparent the base theme off `Theme.MaterialComponents`, set a window background that matches the Compose surface, delete the redundant night theme, and remove the now-unused `viewBinding` flag and the four legacy View dependencies. All in one commit (reparenting the theme and dropping `com.google.android.material` must land together).

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Delete: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/res/values/colors.xml`
- Create: `app/src/main/res/values-night/colors.xml`
- Modify: `app/build.gradle.kts`
- Modify: `gradle/libs.versions.toml`

- [ ] **Step 1: Reparent the base theme**

Replace the **entire** contents of `app/src/main/res/values/themes.xml`:

```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme. The real palette lives in the Compose SpadesScoreTheme;
         this only governs the window/splash frame before Compose composes. -->
    <style name="Theme.SpadesScore" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/window_background</item>
    </style>
</resources>
```

- [ ] **Step 2: Delete the redundant night theme**

```bash
git rm app/src/main/res/values-night/themes.xml
```

(The DayNight parent + a night-specific `window_background` cover dark mode.)

- [ ] **Step 3: Add the light window-background color**

In `app/src/main/res/values/colors.xml`, add before `</resources>`:

```xml
    <color name="window_background">#FDFDF6</color>
```

(= Compose `LightBackground`/`LightSurface`.) Leave the existing legacy colors untouched.

- [ ] **Step 4: Create the dark window-background color**

Create `app/src/main/res/values-night/colors.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="window_background">#1A1C18</color>
</resources>
```

(= Compose `DarkBackground`/`DarkSurface`.)

- [ ] **Step 5: Drop `viewBinding` and the legacy View dependencies in `build.gradle.kts`**

In `app/build.gradle.kts`, in the `buildFeatures { … }` block, remove this line (keep `compose = true`):

```kotlin
        viewBinding = true
```

In the `dependencies { … }` block, remove these four lines:

```kotlin
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.constraintlayout)
```

- [ ] **Step 6: Remove the catalog entries**

In `gradle/libs.versions.toml`, under `[versions]` remove:

```toml
material = "1.13.0"
recyclerview = "1.4.0"
fragment = "1.8.9"
constraintlayout = "2.2.1"
```

Under `[libraries]` remove:

```toml
material = { group = "com.google.android.material", name = "material", version.ref = "material" }
androidx-recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
androidx-fragment = { group = "androidx.fragment", name = "fragment", version.ref = "fragment" }
androidx-constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
```

(Keep `appcompat`, `navigationCompose`, and all Compose entries.)

- [ ] **Step 7: Build + test**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. If it fails to resolve `Theme.AppCompat.DayNight.NoActionBar`, ensure `androidx.appcompat` is still a dependency (it is). If it fails over a removed dependency still being referenced, find the referencing file and report rather than re-adding blindly.
Run: `.\gradlew.bat test`
Expected: PASS.

- [ ] **Step 8: Manual theme check (recommended)**

Run: `.\gradlew.bat installDebug`. Launch in light and in dark mode; confirm there is no jarring window-background flash before the Compose UI appears (light ≈ `#FDFDF6`, dark ≈ `#1A1C18`), and the app looks identical to before.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml app/build.gradle.kts gradle/libs.versions.toml
git commit -m "$(printf 'refactor: drop MaterialComponents base theme and dead View deps\n\nReparents Theme.SpadesScore to Theme.AppCompat.DayNight.NoActionBar with a window\nbackground matching the Compose surface, removes the redundant night theme, and drops\ncom.google.android.material, recyclerview, fragment, constraintlayout, and the unused\nviewBinding flag.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

(The `git rm` from Step 2 already staged the night-theme deletion.)

---

## Task 5: Update `CLAUDE.md`

Reflect the singleton removal and the new localization mechanism.

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Update the Architecture "Language mix" sentence about SpadesGame**

In `CLAUDE.md`, in the **Architecture** section, replace the sentence:

```markdown
`SpadesGame.java` is a now-unused legacy singleton kept only until Phase 4 removes it.
```

with:

```markdown
The legacy `SpadesGame.java` singleton and `Languages.java` enum have been removed.
```

- [ ] **Step 2: Update the Localization section**

Replace the entire **Localization** section paragraph:

```markdown
English/German via the `Languages` enum (held in `GameUiState`, changeable only on the setup screen). `MainActivity.applyLocale()`/`changeLanguage()` mutate the configuration and call `recreate()`. German strings are in `res/values-de/strings.xml`. User-facing copy goes through `getString(R.string.…)`, not literals — including the now-localized in-screen name validation.
```

with:

```markdown
English/German via **`AppCompatDelegate` per-app locales** (BCP-47 tags `en`/`de`). `MainActivity.setLanguage(tag)` calls `AppCompatDelegate.setApplicationLocales(...)`; `autoStoreLocales` (manifest service + `res/xml/locales_config.xml`) persists the choice across restarts and AppCompat handles the `recreate()`. Language is the single source of truth in AppCompatDelegate (not in `GameUiState`) and is changeable only on the setup screen. German strings are in `res/values-de/strings.xml`. User-facing copy goes through `getString(R.string.…)`, not literals.
```

- [ ] **Step 3: Verify no other stale mentions**

Search `CLAUDE.md` for `SpadesGame`, `Languages`, `applyLocale`, `changeLanguage`, `viewBinding`. Expected after the edits above: no remaining mentions of `applyLocale`/`changeLanguage`/`viewBinding`; `SpadesGame` only appears (if at all) in historical/"State lives in…" context that already reads "no longer used" — if any sentence still implies `SpadesGame` is present-and-kept, fix it to past tense (removed). Fix any stale line you find within this section's scope.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "$(printf 'docs: update CLAUDE.md for removed singleton and AppCompatDelegate locales\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Definition of Done (Phase 4)

- [ ] `SpadesGame.java` and `Languages.java` deleted; no references remain anywhere.
- [ ] Localization runs through `AppCompatDelegate` (`autoStoreLocales` service + `res/xml/locales_config.xml`); `GameUiState` is `(playerCount, game)` with no `language`; the setup selector is tag-based; language persists across restarts.
- [ ] Base theme parents `Theme.AppCompat.DayNight.NoActionBar`; `values-night/themes.xml` removed; `window_background` matches the Compose surface in light (`#FDFDF6`) + dark (`#1A1C18`).
- [ ] `com.google.android.material`, `recyclerview`, `fragment`, `constraintlayout`, and `viewBinding = true` removed from `build.gradle.kts` and the version catalog.
- [ ] Three Phase-3 nits resolved: pure `startGame` `update` lambda; dead `amountOfCards()`/`isPredictionSumValid()` (and their test) removed; DealCards card count via `R.string.amount_of_cards`.
- [ ] `.\gradlew.bat test` green; `.\gradlew.bat assembleDebug` `BUILD SUCCESSFUL`.
- [ ] Manual smoke passes: EN↔DE switch + persistence across relaunch; light/dark window background has no flash; a full game (3p + 4p) is playable.
- [ ] `CLAUDE.md` reflects the removed singleton and the AppCompatDelegate localization.
