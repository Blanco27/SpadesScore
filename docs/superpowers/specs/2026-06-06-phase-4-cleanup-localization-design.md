# Phase 4 — Final Cleanup & AppCompatDelegate Localization (Design)

**Status:** approved 2026-06-06

## Goal

Final step of the SpadesScore modernization. After Phase 3 the app is a full single-Activity Compose app, but it still carries dead legacy weight and uses a deprecated locale-switching mechanism. Phase 4:

1. Removes the now-dead `SpadesGame.java` legacy singleton (and the obsolete Java `Languages` enum).
2. Replaces manual `resources.updateConfiguration()` + `recreate()` locale switching with **`AppCompatDelegate` per-app locales** as the single source of truth (persisted across restarts, integrated with the Android 13+ system per-app-language screen).
3. Migrates the base XML theme off `Theme.MaterialComponents` so `com.google.android.material` can be dropped, and removes other dead build config / dependencies.
4. Clears the three Phase-3 review follow-up nits.

**Behavior is unchanged except (intended):** the chosen UI language now persists across process death and is settable from the system per-app-language settings (Android 13+).

## Context (current state, on branch `modernization/phase-3-compose-flow`)

- `SpadesGame.java` — fully dead: no production caller (only its own definition + one KDoc mention in `GameViewModel`). Adapts `SpadesEngine`/`GameState`; obsolete.
- `Languages.java` — Java enum `{ ENGLISH, GERMAN }`. Used only by `GameUiState.language`, `GameViewModel.selectLanguage`, `SetupScreen`, and `MainActivity` locale plumbing.
- Localization today: `MainActivity.applyLocale()` (deprecated `resources.updateConfiguration`) + `changeLanguage()` (manual `recreate()`), with language held as `GameUiState.language` in `GameViewModel`. No `res/xml/locales_config.xml`, no AppCompat locale service.
- Base XML theme `Theme.SpadesScore` (in `values/themes.xml` **and** `values-night/themes.xml`) parents `Theme.MaterialComponents.DayNight.NoActionBar` and sets only vestigial template colors (`colorPrimary*`/`colorSecondary*` → cyan/teal) + `android:statusBarColor`. The real palette is the Compose `SpadesScoreTheme` (spade-green M3). `colorPrimaryVariant`/`colorSecondaryVariant` are MaterialComponents-only attributes.
- Compose surface colors: light `#FDFDF6`, dark `#1A1C18` (`ui/theme/Color.kt`).
- Dead build weight: `viewBinding = true` (no `ViewBinding` usage remains); `implementation` of `androidx.recyclerview`, `androidx.fragment`, `androidx.constraintlayout`, and `com.google.android.material` (the last only kept alive by the base theme).
- `appcompat = "1.7.1"` — already supports `AppCompatDelegate.setApplicationLocales` / `autoStoreLocales` (added in 1.6.0). No bump needed.
- Phase-3 nits: `GameViewModel.amountOfCards()`/`isPredictionSumValid()` are unused (screens re-derive from `game`); `startGame` calls the RNG inside the `_uiState.update {}` lambda; `DealCardsScreen` hardcodes the `"…x"` suffix.

## Decisions (settled during brainstorming)

1. **Scope:** full cleanup including the theme migration (drop `com.google.android.material`).
2. **Localization source of truth:** `AppCompatDelegate` alone. `language` leaves `GameViewModel`/`GameUiState`; the setup selector reads/writes via `AppCompatDelegate`. Language persists across restarts (desirable, free).
3. **Language representation:** BCP-47 tag strings (`"en"`/`"de"`) directly; delete `Languages.java`.
4. **Localization mechanism:** `autoStoreLocales=true` via the AndroidX `AppLocalesMetadataHolderService` manifest entry (AppCompat persists the choice itself; no custom storage).

---

## A. Localization → AppCompatDelegate per-app locales

### Manifest (`app/src/main/AndroidManifest.xml`)
- Add `android:localeConfig="@xml/locales_config"` to `<application>`.
- Add inside `<application>`:
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

### New `app/src/main/res/xml/locales_config.xml`
```xml
<?xml version="1.0" encoding="utf-8"?>
<locale-config xmlns:android="http://schemas.android.com/apk/res/android">
    <locale android:name="en" />
    <locale android:name="de" />
</locale-config>
```

### `GameViewModel` / `GameUiState` (`ui/game/GameViewModel.kt`)
- `GameUiState` becomes `data class GameUiState(val playerCount: Int = 4, val game: GameState? = null)` — `language` removed.
- Remove `fun selectLanguage(...)` and the `import com.nwe.spadesscore.Languages`.
- Update the class KDoc line that references the SpadesGame singleton (cosmetic; the singleton no longer exists after this phase).

### `MainActivity` (`MainActivity.kt`)
- Remove `applyLocale`, `localeCodeFor`, `changeLanguage`, and `import java.util.Locale`. AppCompat (autoStore) applies/restores the locale; no manual config mutation in `onCreate`.
- Add a helper:
  ```kotlin
  private fun setLanguage(tag: String) {
      AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
      // AppCompat recreates the Activity itself.
  }
  ```
- Compute the currently-effective tag for the selector (read once per composition; a language change triggers Activity recreation, which re-reads it):
  ```kotlin
  private fun currentLanguageTag(): String {
      val appLocales = AppCompatDelegate.getApplicationLocales()
      val lang = if (!appLocales.isEmpty) appLocales[0]?.language
                 else resources.configuration.locales[0].language
      return if (lang == "de") "de" else "en"
  }
  ```
- Wire the setup destination with `languageTag = currentLanguageTag()` and `onSelectLanguage = { setLanguage(it) }`.
- `imports`: add `androidx.appcompat.app.AppCompatDelegate`, `androidx.core.os.LocaleListCompat`.

### `SetupScreen` (`ui/setup/SetupScreen.kt`)
- Signature changes:
  - `language: Languages` → `languageTag: String`
  - `onSelectLanguage: (Languages) -> Unit` → `onSelectLanguage: (String) -> Unit`
- Selector options use tag strings:
  ```kotlin
  SegmentedSelector(
      label = stringResource(R.string.language),
      options = listOf(
          "en" to stringResource(R.string.english),
          "de" to stringResource(R.string.deutsch)
      ),
      selected = languageTag,
      onSelect = onSelectLanguage
  )
  ```
- Remove the now-unused `import com.nwe.spadesscore.Languages`. The private generic `SegmentedSelector` is unchanged (works with `String`).
- Update the `@Preview` to pass `languageTag = "en"` and `onSelectLanguage = {}`.

## B. Remove dead legacy

- Delete `app/src/main/java/com/nwe/spadesscore/SpadesGame.java`.
- Delete `app/src/main/java/com/nwe/spadesscore/Languages.java`.
- After deletion, confirm via grep that nothing references `SpadesGame` or `Languages` (the only remaining `Languages` users — `GameViewModel`, `SetupScreen`, `MainActivity` — are updated in Section A).

## C. Theme migration + dependency cleanup

### `app/src/main/res/values/themes.xml`
```xml
<resources xmlns:tools="http://schemas.android.com/tools">
    <!-- Base application theme. Real palette lives in the Compose SpadesScoreTheme;
         this only governs the window/splash frame before Compose composes. -->
    <style name="Theme.SpadesScore" parent="Theme.AppCompat.DayNight.NoActionBar">
        <item name="android:windowBackground">@color/window_background</item>
    </style>
</resources>
```
(Drop all `colorPrimary*`/`colorSecondary*` items and `android:statusBarColor` — edge-to-edge + Compose manage bar appearance; `statusBarColor` is deprecated at API 35.)

### Delete `app/src/main/res/values-night/themes.xml`
The DayNight parent plus a night-specific `window_background` color cover the dark case; no separate night theme needed.

### Colors
- `app/src/main/res/values/colors.xml`: add `<color name="window_background">#FDFDF6</color>` (= Compose `LightBackground`/`LightSurface`).
- New `app/src/main/res/values-night/colors.xml`:
  ```xml
  <?xml version="1.0" encoding="utf-8"?>
  <resources>
      <color name="window_background">#1A1C18</color>
  </resources>
  ```
  (= Compose `DarkBackground`/`DarkSurface`.)
- The existing unused legacy color entries in `colors.xml` are **left as-is** (pruning them is out of scope).

### `app/build.gradle.kts`
- In `buildFeatures { … }` remove `viewBinding = true` (keep `compose = true`).
- In `dependencies { … }` remove:
  - `implementation(libs.material)`
  - `implementation(libs.androidx.recyclerview)`
  - `implementation(libs.androidx.fragment)`
  - `implementation(libs.androidx.constraintlayout)`

### `gradle/libs.versions.toml`
- Remove `[versions]` keys: `material`, `recyclerview`, `fragment`, `constraintlayout`.
- Remove `[libraries]` entries: `material`, `androidx-recyclerview`, `androidx-fragment`, `androidx-constraintlayout`.
- (`appcompat = "1.7.1"` stays; no bump.)

## D. Phase-3 follow-up nits

### `GameViewModel.kt`
- Delete the unused `fun amountOfCards()` and `fun isPredictionSumValid(sum)`. The "sum == possible cards is invalid" rule remains inline in `DeclareTricksScreen` (`val valid = sum != possible`), where it is already used. (No new domain function — YAGNI.)
- `startGame`: compute `start` (including the RNG draw) **before** the `update` so the lambda stays a pure function of `it`:
  ```kotlin
  fun startGame(names: List<String>, randomDealer: Boolean) {
      val start = if (randomDealer) SpadesEngine.randomStartingPlayer(_uiState.value.playerCount, random) else 0
      _uiState.update { it.copy(game = SpadesEngine.newGame(it.playerCount, names, start)) }
  }
  ```

### `DealCardsScreen.kt` + strings
- Add string `amount_of_cards` = `"%dx"` in both `values/strings.xml` and `values-de/strings.xml`.
- Replace the literal `"${SpadesEngine.amountOfCards(game)}x"` with `stringResource(R.string.amount_of_cards, SpadesEngine.amountOfCards(game))`.

## Testing

- **`GameViewModelTest`:** update `initialState_isDefault` to `GameUiState(playerCount = 4, game = null)` (no `language`); delete `selectLanguage_updatesState` and `isPredictionSumValid_falseWhenSumEqualsPossibleCards`. Remaining tests (~9: initial, selectPlayerCount, startGame fixed/random/3-player, confirmRound appends/last-round, continueSecondHalf, newGame) stay green.
- **AppCompatDelegate locale behavior** is not JVM-unit-testable (Android framework). Covered by a manual smoke test:
  - Switch EN↔DE on setup → strings + selector update.
  - Kill and relaunch the app → the chosen language persists.
  - Android 13+: the system "App languages" screen lists English + Deutsch.
  - A full game still plays in the selected language.
- **Build gates:** `.\gradlew.bat test` green; `.\gradlew.bat assembleDebug` `BUILD SUCCESSFUL`. Manual check of the light + dark window background (no color flash before Compose).

## Out of scope

- Process-death persistence of **game** state (never — explicit project constraint).
- Pruning the many unused legacy color entries in `colors.xml` (only `window_background` is added).
- Adding further languages beyond EN/DE.

## Risks & mitigations

- **Theme reparent** could alter the pre-Compose window/splash frame → mitigated by setting `windowBackground` to the exact Compose surface color for light and dark; verify both.
- **Dependency removal** — `androidx.fragment` is still pulled transitively by AppCompat, so `AppCompatActivity` keeps working; the build verifies no other consumer remains.

## Sequencing

Phase 4 builds on Phase 3 (branch `modernization/phase-3-compose-flow`, pushed, PR open, not yet merged to `master`). The Phase 4 branch must base off Phase 3's code (branch from it, or start after the Phase-3 PR merges) so the deletions and edits apply against the already-migrated Compose app.

## Definition of Done

- `SpadesGame.java` and `Languages.java` deleted; no references remain.
- Localization runs through `AppCompatDelegate` (`autoStoreLocales` + `locales_config.xml`); `GameUiState` has no `language`; language persists across restarts.
- Base theme parents `Theme.AppCompat.DayNight.NoActionBar`; `values-night/themes.xml` removed; `window_background` matches the Compose surface in light + dark.
- `com.google.android.material`, `recyclerview`, `fragment`, `constraintlayout`, and `viewBinding = true` removed from the build; catalog entries removed.
- Three Phase-3 nits resolved (dead VM methods gone, pure `startGame` update, localized card-count string).
- `.\gradlew.bat test` + `assembleDebug` green; manual locale smoke test passes.
