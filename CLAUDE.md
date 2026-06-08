# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

SpadesScore is a single-module Android app for tracking scores in a Spades-style trick-prediction card game. The codebase is **100% Kotlin** and follows a layered **MVVM** architecture: a pure-Kotlin `domain` layer, a Room-backed `data` layer, manual DI, and per-screen ViewModels. The UI is still **XML layouts** driven by classic `AppCompatActivity` classes (`findViewById`/viewBinding) — there is **no Compose** anywhere in the app.

## Build & Test

Use the Gradle wrapper. On Windows/PowerShell use `.\gradlew.bat`; on the Bash tool use `./gradlew`.

```bash
./gradlew assembleDebug              # Build debug APK (output in app/build/outputs/apk/)
./gradlew installDebug               # Install debug build on a connected device/emulator
./gradlew test                       # Run JVM unit tests (testDebugUnitTest + testReleaseUnitTest)
./gradlew testDebugUnitTest          # Unit tests, debug variant only
./gradlew connectedAndroidTest       # Instrumented tests (needs a device/emulator)
./gradlew lint                       # Android lint

# Run a single unit test class or method:
./gradlew testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameEngineTest"
./gradlew testDebugUnitTest --tests "com.nwe.spadesscore.domain.GameEngineTest.method_name"
```

Tests: the `domain` layer has real unit-test coverage — `GameEngineTest`, `GameStateTest`, `NameValidationTest` (`app/src/test/`). The Room round-trip is covered by an instrumented test, `data/GameRepositoryTest` (`app/src/androidTest/`, needs a device/emulator). The default template tests (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) are still present but carry no real coverage.

Toolchain: AGP 8.13.1, Kotlin 2.2.21, KSP 2.2.21-2.0.4, Gradle 8.14.5, JDK 11 target, `compileSdk`/`targetSdk` 36, `minSdk` 24, `versionCode` 2 / `versionName` 1.1.0. Persistence is **Room 2.8.4** (KSP-processed) and coroutines 1.10.2. Dependency versions are centralized in `gradle/libs.versions.toml` (version catalog) and referenced as `libs.*` in `app/build.gradle.kts` — add/upgrade dependencies there, not with hardcoded coordinates.

## Architecture

Package layout under `com.nwe.spadesscore`:

- `domain/` — pure Kotlin, **no Android dependencies**. Models (`model/GameState`, `model/Player`, `model/Language`), rule constants (`GameRules`), stateless transitions (`GameEngine`), and `NameValidation`.
- `data/` — Room persistence (`data/db/*` entities + `GameDao` + `AppDatabase`), domain↔entity mapping (`data/mapper/GameMapper`), and the `GameRepository`.
- `di/` — `AppContainer` (manual DI), instantiated once in `SpadesApplication.onCreate()`.
- `ui/<screen>/` — one `ViewModel` + one `UiState` per screen, plus the shared `ui/repository.kt` and `ui/locale.kt` helpers. The six `*Activity` classes live in the root package.

### Central state: `GameRepository` (single source of truth)

`GameRepository` (`data/GameRepository.kt`) replaces the old in-memory `SpadesGame` singleton. It holds the game as a `StateFlow<GameState>` and **persists every mutation to Room**, so the game now survives process death — the headline behavior change of this architecture. Key details when editing it:

- The **initial state is loaded blocking** (`runBlocking` on IO) in the constructor so `state.value` is valid immediately — required so a deep activity recreated after process death (without going through `MainActivity`) sees the saved game right away.
- Persistence runs on an IO dispatcher capped to `limitedParallelism(1)`, so consecutive mutations are written **in order** (no reordering of concurrent writes). `lastPersistJob` exists only so tests can await a write deterministically.
- Mutators (`setPlayerCount`, `setLanguage`, `startGame`, `setPredictions`, `confirmTricks`, `startSecondHalf`) each compute a new `GameState` (delegating rule changes to `GameEngine`) and call `update()`.

`GameState` is an **immutable** `data class`; derived values (`amountOfCards`, `isGameOver`, `currentDealerName`) are computed properties. `GameEngine` is a stateless `object` whose functions take a `GameState` and return a new one.

### ViewModels and DI wiring

Each activity obtains its ViewModel via `by viewModels { viewModelFactory { initializer { SomeViewModel(gameRepository) } } }`. The `gameRepository` is reached through the `Context.gameRepository` extension (`ui/repository.kt`), which pulls it from `SpadesApplication.container` (`AppContainer`). ViewModels are thin: they read `repository.state.value` to build their screen's `UiState` and forward user actions to repository mutators. There is **no** Hilt/Dagger — DI is the single hand-written `AppContainer`.

### Activity flow

All screens are separate activities navigated linearly via explicit `Intent`s. The **back button is intentionally disabled** on every screen (see base class), so navigation is strictly forward, driven by `GameState` flags (`isSecondHalf`, `isGameOver`).

```
MainActivity            choose 3 or 4 players + language (EN/DE)
  -> PlayerNamesActivity   enter names, toggle random dealer, calls startGame()
  -> DealCardsActivity     shows who deals + card count for the round
  -> DeclareTricksActivity each player declares trick prediction (TrickSpinner)
  -> ConfirmTicksActivity  check off who hit their prediction; updates scores
       -> back to DealCardsActivity for the next round, OR
       -> ResultScreenActivity   at halftime (continue -> startSecondHalf) or game end
```

### Base class template-method pattern

`SpadesAppCompatActivity` (Kotlin, abstract) has an `onCreate` that calls, in order: `applyPersistedLocale()` → `initContentView()` → `initializeUIComponents()` → `setupUI()` → and installs the back-press disabler (via `onBackPressedDispatcher`). Concrete activities implement those three hooks rather than overriding `onCreate` directly. (`ResultScreenActivity` is the exception — it extends `AppCompatActivity` directly and does everything in `onCreate`.) When adding a screen, follow the template-method pattern.

### Game rules (live in `domain`)

- `amountOfRounds = floor(32 / playerCount)` (8 rounds for 4 players, 10 for 3; constant `GameRules.TOTAL_CARDS`). `GameEngine.startSecondHalf()` doubles it.
- Cards per round (`GameState.amountOfCards`): ramps **up** in the first half (`currentRound`), and **down** in the second half (`max(1, amountOfRounds - currentRound + 1)`).
- Trick-declaration rule, enforced in `DeclareTricksActivity`: the start button is **disabled while the combined predictions exactly equal the available tricks** (the table's bids must not sum to the number of tricks). The button animates/shakes on this transition.
- Scoring (`GameEngine.confirmTricks`): a player who hit their prediction gains `prediction + GameRules.HIT_BONUS` (5); otherwise the score is carried forward unchanged.
- Ranking: `GameEngine.placementByPlayerIndex` maps each player index to a placement (1 = highest score), reproducing the legacy tie order.
- Name validation (`NameValidation.validate`): empty takes precedence over too-long; max length is `GameRules.MAX_NAME_LENGTH` (10).
- 3-player mode is handled with `playerCount == 3` conditionals in the UI that hide the "player 4" views — when touching any score/UI logic, check both the 3- and 4-player paths.

### Localization

English/German via `values/strings.xml` and `values-de/strings.xml` (including the validation dialogs, which are localized). The chosen `Language` is a `domain` field, **persisted in Room**. `applyPersistedLocale()` (`ui/locale.kt`) applies it to the activity configuration **before the layout is inflated**, on every screen — so after process death a deep activity restores in the chosen language instead of falling back to the device locale. `MainActivity.setLocale()` additionally mutates the configuration and calls `recreate()` when the user toggles language. There is also `values-night/` for dark theme.

## Things to know before editing

- **No Compose.** `buildFeatures` only enables `viewBinding`; the old `ui/theme/*.kt` files and Compose dependencies were removed. The live UI is entirely XML layouts. Don't reintroduce Compose assuming it was the intended path.
- **`ResultScreenActivity` is hardcoded for up to 20 rounds** (`MAX_ROUNDS = 20`). It wires the score grid by name via `resources.getIdentifier(...)` loops (`score_playerN_roundM` cells, `roundNScores` rows, `player4_spaceN` spacers). Changing the max round count or the results table means editing both this activity and `activity_result_screen.xml` in lockstep. The `getIdentifier` use is intentional (suppressed `DiscouragedApi` lint warning).
- **`TrickSpinner`** (`TrickSpinner.kt`) is the one custom View — a +/- stepper backed by `ViewTrickSpinnerBinding`, clamped to `[0, maxAmount]`, with an `onValueChanged` callback.
- The game state survives process death (Room). To test the persistence path, kill the app mid-game and relaunch into the last activity — round/scores/names should restore without a crash.
