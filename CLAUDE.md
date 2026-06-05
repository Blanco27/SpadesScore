# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

SpadesScore is a single-module Android app for tracking scores in the card game Spades. It is **not** a card-playing app — it only records trick predictions and tallies points across rounds.

## Build / run / test

Use the Gradle wrapper (`./gradlew` on Unix, `.\gradlew.bat` on Windows). Min SDK 24, compile/target SDK 36, JVM target 11.

- Build debug APK: `.\gradlew.bat assembleDebug`
- Install on a connected device/emulator: `.\gradlew.bat installDebug`
- Unit tests (JVM): `.\gradlew.bat test`
- Single unit test: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.spadesscore.domain.SpadesEngineTest"` (the lifecycle `test` task rejects `--tests`; use `testDebugUnitTest` for a single class)
- Instrumented tests (needs device/emulator): `.\gradlew.bat connectedAndroidTest`
- Lint: `.\gradlew.bat lint`

Note: JVM unit tests cover the pure domain core (`SpadesEngineTest`) and the `GameViewModel` (`GameViewModelTest`). `androidTest` still holds only the Android Studio template stub.

## Architecture

**Language mix:** The UI is **Jetpack Compose** (Kotlin) — a single `MainActivity` hosting a `NavHost` (`setup` → `players` → `deal` → `declare` → `confirm` → `result`). Game rules live in the pure Kotlin domain core (`domain/GameState` + `domain/SpadesEngine`). `SpadesGame.java` is a now-unused legacy singleton kept only until Phase 4 removes it. The Compose theme lives in `ui/theme/`, the setup screen in `ui/setup/`, and the in-game screens + shared `GameViewModel` in `ui/game/`.

**State lives in one Activity-scoped `GameViewModel`** (`ui/game/GameViewModel.kt`). It holds `GameUiState` (player count, language, and the immutable `GameState`) as a `StateFlow`, and every action delegates directly to `SpadesEngine`. State survives configuration changes via the ViewModel; **there is no persistence** — process death loses everything (out of scope). The legacy `SpadesGame` singleton is no longer used.

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

**Shared host:** All screens are stateless `@Composable` functions under `ui/game/` (and `ui/setup/SetupScreen`), hosted by the single `MainActivity` NavHost and driven by the shared `GameViewModel`. There is no Activity base class anymore.

## Game rules encoded in `SpadesEngine` (important when touching scoring)

- Rounds per half = `floor(32 / playerCount)` (so 8 for 4 players, 10 for 3). `startSecondHalf()` doubles `amountOfRounds`.
- Cards dealt per round (`amountOfCards`): counts **up** in the first half (`currentRound`), counts **down** in the second half (`amountOfRounds - currentRound + 1`).
- Scoring (`confirmTricks`): if a player made their prediction, they gain `prediction + 5`; otherwise their score is unchanged (the previous total is re-appended so every round has a score entry).
- A random starting `currentPlayer` can be chosen at game start (`randomStartingPlayer`, surfaced as the "random dealer" checkbox); the dealer then rotates `(currentPlayer + 1) % playerCount` each round.

## 3-player vs 4-player handling

The Compose screens iterate `0 until playerCount`, so the 4th player simply isn't rendered for 3-player games — no `View.GONE` workarounds.

## ResultScreen

The result table is **data-driven** from `GameState.scores` (`ui/game/ResultScreen.kt`): one row per played round, one column per player, vertically scrollable. The last row is colored gold/silver/bronze via `SpadesEngine.rankingForLastRound`. No hardcoded round capacity.

## Localization

English/German via the `Languages` enum (held in `GameUiState`, changeable only on the setup screen). `MainActivity.applyLocale()`/`changeLanguage()` mutate the configuration and call `recreate()`. German strings are in `res/values-de/strings.xml`. User-facing copy goes through `getString(R.string.…)`, not literals — including the now-localized in-screen name validation.

