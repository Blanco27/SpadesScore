# Phase 2: Compose-Architektur-Gerüst (Main-Screen) – Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Den Compose-Stack etablieren (Single-Activity-Host + echtes M3-Theme + `GameViewModel`) und den Main-/Setup-Screen end-to-end nach Jetpack Compose migrieren, während die übrigen Screens Legacy-Activities bleiben und die App durchgängig spielbar bleibt.

**Architecture:** `MainActivity` wird zu einer Compose-`AppCompatActivity`, die ein zustandsloses `SetupScreen` rendert. `SetupScreen` liest seinen Zustand aus `GameViewModel` (`StateFlow<SetupUiState>`); das ViewModel schreibt Spielerzahl/Sprache über den Port `GameSetup` an den Legacy-Singleton `SpadesGame` durch. „Weiter" überbrückt per `startActivity` in den bestehenden `PlayerNamesActivity`-Flow (kein NavHost in dieser Phase).

**Tech Stack:** Kotlin, Jetpack Compose (Material 3, BOM 2025.11.01), `androidx.lifecycle` ViewModel + Compose-Integration, Google-Fonts-für-Compose (`ui-text-google-fonts`), JUnit4. Build über `.\gradlew.bat`.

---

## Wichtige Fakten (vorab verifiziert)

- App-Theme: `Theme.SpadesScore` (parent `Theme.MaterialComponents.DayNight.NoActionBar`) – **kein ActionBar**, AppCompat-kompatibel. `MainActivity` als `AppCompatActivity` mit diesem Theme funktioniert; Compose zeichnet die UI selbst.
- `R.drawable.spades_logo` (PNG) existiert.
- Vorhandene String-Keys (EN + DE): `number_of_players`, `language`, `three` ("3 Players"/"3 Spieler"), `four`, `english`, `deutsch`, `next` ("Start Game"/"Spiel starten"), `contentDescription` ("Logo").
- `@font/patrick_hand` ist ein **downloadbarer** Google-Font (Provider `com.google.android.gms.fonts`, Query "Patrick Hand"); das Cert-Array `@array/com_google_android_gms_fonts_certs` existiert. In Compose wird er über die `GoogleFont`-API geladen.
- `Languages` (Enum `ENGLISH`, `GERMAN`) und `SpadesGame` (Java-Adapter mit `getPlayerCount()/setPlayerCount(int)`, `getLanguages()/setLanguages(Languages)`) stammen aus Phase 1 und bleiben unverändert.

## File Structure

| Datei | Aktion | Verantwortung |
|------|--------|---------------|
| `gradle/libs.versions.toml` | ändern | Library-Einträge für `lifecycle-viewmodel-compose`, `lifecycle-runtime-compose`, `ui-text-google-fonts` |
| `app/build.gradle.kts` | ändern | obige drei Dependencies aufnehmen |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt` | ersetzen | Spade-Grün-Farbtokens (hell+dunkel) |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt` | ersetzen | Patrick-Hand-`FontFamily` (Google Fonts) + `AppTypography` |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt` | ersetzen | feste hell/dunkel-`ColorScheme`, kein Dynamic Color |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt` | neu | `GameSetup`-Interface + `SpadesGameSetup`-Impl |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt` | neu | `SetupUiState` + `GameViewModel` (+ `Factory`) |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt` | neu | zustandsloses `SetupScreen` + `SegmentedSelector` |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.kt` | neu | Compose-Host (ersetzt `MainActivity.java`) |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.java` | löschen | durch Compose-Variante ersetzt |
| `app/src/main/res/layout/activity_main.xml` | löschen | nicht mehr genutzt |
| `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt` | neu | JVM-Unit-Tests des ViewModels |
| `app/src/androidTest/java/com/nwe/spadesscore/ui/setup/SetupScreenTest.kt` | neu (optional) | Compose-UI-Smoke-Test |

---

## Task 1: Dependencies aufnehmen

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Versionskatalog ergänzen**

In `gradle/libs.versions.toml`, im Abschnitt `[libraries]` (z.B. nach der Zeile `androidx-material3 = ...`) diese drei Einträge hinzufügen:

```toml
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-lifecycle-runtime-compose = { group = "androidx.lifecycle", name = "lifecycle-runtime-compose", version.ref = "lifecycleRuntimeKtx" }
androidx-ui-text-google-fonts = { group = "androidx.compose.ui", name = "ui-text-google-fonts" }
```

- [ ] **Step 2: build.gradle.kts ergänzen**

In `app/build.gradle.kts`, im `dependencies { ... }`-Block (z.B. direkt nach `implementation(libs.androidx.material3)`), hinzufügen:

```kotlin
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.ui.text.google.fonts)
```

- [ ] **Step 3: Build verifizieren (Dependencies auflösen)**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Die neuen Artefakte werden aufgelöst; es gibt noch keine Nutzung, also keine Codeänderung nötig. Falls `ui-text-google-fonts` nicht auflöst, prüfen, dass die Compose-BOM (`platform(libs.androidx.compose.bom)`) im `dependencies`-Block vorhanden ist (sie ist es) – das Artefakt wird über die BOM versioniert.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "$(printf 'build: add compose viewmodel, lifecycle-runtime-compose, google-fonts deps\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 2: M3-Theme (Spade-Grün + Patrick Hand)

**Files:**
- Replace: `app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt`
- Replace: `app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt`
- Replace: `app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt`

> Kein Unit-Test (reine Compose-UI-Konfiguration); Verifikation per Build. `dynamicColor` wird entfernt.

- [ ] **Step 1: Color.kt ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt` mit:

```kotlin
package com.nwe.spadesscore.ui.theme

import androidx.compose.ui.graphics.Color

// Spade-green brand palette (Material 3 roles)

// Light
val LightPrimary = Color(0xFF386A20)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFB7F397)
val LightOnPrimaryContainer = Color(0xFF042100)
val LightSecondary = Color(0xFF55624C)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD9E7CB)
val LightOnSecondaryContainer = Color(0xFF131F0D)
val LightBackground = Color(0xFFFDFDF6)
val LightOnBackground = Color(0xFF1A1C18)
val LightSurface = Color(0xFFFDFDF6)
val LightOnSurface = Color(0xFF1A1C18)
val LightSurfaceVariant = Color(0xFFDFE4D7)
val LightOnSurfaceVariant = Color(0xFF43483E)
val LightOutline = Color(0xFF73796E)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

// Dark
val DarkPrimary = Color(0xFF9CD67D)
val DarkOnPrimary = Color(0xFF0B3900)
val DarkPrimaryContainer = Color(0xFF205107)
val DarkOnPrimaryContainer = Color(0xFFB7F397)
val DarkSecondary = Color(0xFFBDCBB0)
val DarkOnSecondary = Color(0xFF283420)
val DarkSecondaryContainer = Color(0xFF3E4A35)
val DarkOnSecondaryContainer = Color(0xFFD9E7CB)
val DarkBackground = Color(0xFF1A1C18)
val DarkOnBackground = Color(0xFFE2E3DC)
val DarkSurface = Color(0xFF1A1C18)
val DarkOnSurface = Color(0xFFE2E3DC)
val DarkSurfaceVariant = Color(0xFF43483E)
val DarkOnSurfaceVariant = Color(0xFFC3C8BB)
val DarkOutline = Color(0xFF8D9387)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)
```

- [ ] **Step 2: Type.kt ersetzen (Patrick Hand via Google Fonts)**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt` mit:

```kotlin
package com.nwe.spadesscore.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.nwe.spadesscore.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val patrickHand = GoogleFont("Patrick Hand")

/** Playful brand font used for display/headline styles; body stays on the M3 default. */
val PatrickHandFamily = FontFamily(
    Font(googleFont = patrickHand, fontProvider = googleFontProvider, weight = FontWeight.Normal)
)

private val default = Typography()

val AppTypography = default.copy(
    displayLarge = default.displayLarge.copy(fontFamily = PatrickHandFamily),
    displayMedium = default.displayMedium.copy(fontFamily = PatrickHandFamily),
    displaySmall = default.displaySmall.copy(fontFamily = PatrickHandFamily),
    headlineLarge = default.headlineLarge.copy(fontFamily = PatrickHandFamily),
    headlineMedium = default.headlineMedium.copy(fontFamily = PatrickHandFamily),
    headlineSmall = default.headlineSmall.copy(fontFamily = PatrickHandFamily),
    titleLarge = default.titleLarge.copy(fontFamily = PatrickHandFamily)
)
```

- [ ] **Step 3: Theme.kt ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt` mit:

```kotlin
package com.nwe.spadesscore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    error = LightError,
    onError = LightOnError
)

private val DarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    error = DarkError,
    onError = DarkOnError
)

@Composable
fun SpadesScoreTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
```

- [ ] **Step 4: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. (Das Theme wird noch nicht von einer Activity genutzt, kompiliert aber.)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt
git commit -m "$(printf 'feat(theme): real M3 theme (spade-green palette, Patrick Hand display font)\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 3: `GameSetup`-Port

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt`

> Dünner Port, über den das ViewModel Spielerzahl/Sprache an den Legacy-Singleton durchschreibt und Startwerte liest. Entkoppelt das ViewModel von `SpadesGame` → JVM-testbar. Verifikation per Build.

- [ ] **Step 1: GameSetup.kt erstellen**

Erstelle `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt`:

```kotlin
package com.nwe.spadesscore.ui.setup

import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.SpadesGame

/**
 * Port through which the setup screen reads the initial player count / language and writes
 * the user's choices. Keeps [GameViewModel] decoupled from the [SpadesGame] singleton, so the
 * ViewModel is unit-testable on the JVM with a fake. Bridge for the legacy Activity flow until
 * the singleton is removed in a later phase.
 */
interface GameSetup {
    fun playerCount(): Int
    fun language(): Languages?
    fun setPlayerCount(count: Int)
    fun setLanguage(language: Languages)
}

/** Real implementation delegating to the in-memory [SpadesGame] singleton. */
class SpadesGameSetup(
    private val game: SpadesGame = SpadesGame.getInstance()
) : GameSetup {
    override fun playerCount(): Int = game.playerCount
    override fun language(): Languages? = game.languages
    override fun setPlayerCount(count: Int) {
        game.setPlayerCount(count)
    }
    override fun setLanguage(language: Languages) {
        game.setLanguages(language)
    }
}
```

(Hinweis: `game.playerCount` / `game.languages` rufen die Java-Getter `getPlayerCount()` / `getLanguages()` auf; `setPlayerCount` / `setLanguages` sind die Java-Setter.)

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt
git commit -m "$(printf 'feat(setup): add GameSetup port over the SpadesGame singleton\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 4: `GameViewModel` + `SetupUiState` (TDD)

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt`

- [ ] **Step 1: Failing test schreiben**

Erstelle `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt`:

```kotlin
package com.nwe.spadesscore.ui.setup

import com.nwe.spadesscore.Languages
import org.junit.Assert.assertEquals
import org.junit.Test

/** In-memory fake of the setup port; records write-throughs so we can assert them. */
private class FakeGameSetup(
    private var playerCount: Int = 4,
    private var language: Languages? = null
) : GameSetup {
    val setPlayerCountCalls = mutableListOf<Int>()
    val setLanguageCalls = mutableListOf<Languages>()
    override fun playerCount(): Int = playerCount
    override fun language(): Languages? = language
    override fun setPlayerCount(count: Int) {
        playerCount = count
        setPlayerCountCalls += count
    }
    override fun setLanguage(language: Languages) {
        this.language = language
        setLanguageCalls += language
    }
}

class GameViewModelTest {

    @Test
    fun initialState_seedsFromSetup() {
        val vm = GameViewModel(FakeGameSetup(playerCount = 3, language = Languages.GERMAN))

        assertEquals(SetupUiState(playerCount = 3, language = Languages.GERMAN), vm.uiState.value)
    }

    @Test
    fun initialState_defaultsLanguageToEnglishWhenNull() {
        val vm = GameViewModel(FakeGameSetup(playerCount = 4, language = null))

        assertEquals(Languages.ENGLISH, vm.uiState.value.language)
    }

    @Test
    fun selectPlayerCount_updatesStateAndWritesThrough() {
        val setup = FakeGameSetup(playerCount = 4)
        val vm = GameViewModel(setup)

        vm.selectPlayerCount(3)

        assertEquals(3, vm.uiState.value.playerCount)
        assertEquals(listOf(3), setup.setPlayerCountCalls)
    }

    @Test
    fun selectLanguage_updatesStateAndWritesThrough() {
        val setup = FakeGameSetup(language = Languages.ENGLISH)
        val vm = GameViewModel(setup)

        vm.selectLanguage(Languages.GERMAN)

        assertEquals(Languages.GERMAN, vm.uiState.value.language)
        assertEquals(listOf(Languages.GERMAN), setup.setLanguageCalls)
    }
}
```

- [ ] **Step 2: Test laufen lassen – muss fehlschlagen**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.ui.setup.GameViewModelTest"`
Expected: Kompilierfehler – `unresolved reference: GameViewModel` / `SetupUiState`.

- [ ] **Step 3: GameViewModel implementieren**

Erstelle `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt`:

```kotlin
package com.nwe.spadesscore.ui.setup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.spadesscore.Languages
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** UI state for the Main/Setup screen. */
data class SetupUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH
)

/**
 * Holds the setup screen state and writes every choice through to [GameSetup] so the legacy
 * Activity flow sees the values. Survives configuration changes by virtue of being a ViewModel
 * (no SavedStateHandle needed; process-death persistence is out of scope for this phase).
 */
class GameViewModel(private val setup: GameSetup) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SetupUiState(
            playerCount = setup.playerCount(),
            language = setup.language() ?: Languages.ENGLISH
        )
    )
    val uiState: StateFlow<SetupUiState> = _uiState.asStateFlow()

    fun selectPlayerCount(count: Int) {
        setup.setPlayerCount(count)
        _uiState.update { it.copy(playerCount = count) }
    }

    fun selectLanguage(language: Languages) {
        setup.setLanguage(language)
        _uiState.update { it.copy(language = language) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { GameViewModel(SpadesGameSetup()) }
        }
    }
}
```

- [ ] **Step 4: Test laufen lassen – muss bestehen**

Run: `.\gradlew.bat test --tests "com.nwe.spadesscore.ui.setup.GameViewModelTest"`
Expected: PASS (4 Tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt
git commit -m "$(printf 'feat(setup): add GameViewModel with write-through to GameSetup\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 5: `SetupScreen` + `SegmentedSelector`

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt`

> Zustandsloses Composable (Layout A). Verifikation per Build; eine `@Preview` erleichtert die visuelle Kontrolle in der IDE.

- [ ] **Step 1: SetupScreen.kt erstellen**

Erstelle `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt`:

```kotlin
package com.nwe.spadesscore.ui.setup

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.nwe.spadesscore.Languages
import com.nwe.spadesscore.R
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme

@Composable
fun SetupScreen(
    state: SetupUiState,
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
            selected = state.playerCount,
            onSelect = onSelectPlayerCount
        )
        Spacer(Modifier.height(24.dp))
        SegmentedSelector(
            label = stringResource(R.string.language),
            options = listOf(
                Languages.ENGLISH to stringResource(R.string.english),
                Languages.GERMAN to stringResource(R.string.deutsch)
            ),
            selected = state.language,
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

@Composable
private fun <T> SegmentedSelector(
    label: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, (value, text) ->
                SegmentedButton(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    label = { Text(text) }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SetupScreenPreview() {
    SpadesScoreTheme {
        SetupScreen(
            state = SetupUiState(playerCount = 4, language = Languages.ENGLISH),
            onSelectPlayerCount = {},
            onSelectLanguage = {},
            onNext = {}
        )
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Falls `SingleChoiceSegmentedButtonRow`/`SegmentedButton` nicht aufgelöst werden, bestätigen, dass `androidx.compose.material3:material3` (über die BOM) eingebunden ist (ist es) – die Segmented-Button-APIs sind in der genutzten BOM stabil.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt
git commit -m "$(printf 'feat(setup): add SetupScreen with M3 segmented selectors (layout A)\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 6: `MainActivity` auf Compose umstellen

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`
- Delete: `app/src/main/java/com/nwe/spadesscore/MainActivity.java`
- Delete: `app/src/main/res/layout/activity_main.xml`

> Ersetzt den alten View-basierten Launcher. Die alte `animateFill`/`select`/`deselect`/`setLocale`-Logik entfällt; Auswahlvisuals übernimmt der M3-SegmentedButton. Verhalten (Sprachwechsel, Übergang zu PlayerNames) bleibt erhalten.

- [ ] **Step 1: Alte Java-Activity und XML löschen**

```bash
git rm app/src/main/java/com/nwe/spadesscore/MainActivity.java app/src/main/res/layout/activity_main.xml
```

- [ ] **Step 2: MainActivity.kt erstellen**

Erstelle `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`:

```kotlin
package com.nwe.spadesscore

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nwe.spadesscore.ui.setup.GameViewModel
import com.nwe.spadesscore.ui.setup.SetupScreen
import com.nwe.spadesscore.ui.theme.SpadesScoreTheme
import java.util.Locale

/**
 * Launcher screen, now in Compose. Hosts the stateless [SetupScreen] wired to [GameViewModel].
 * Player count / language flow through the ViewModel into the SpadesGame singleton; "Next"
 * bridges into the still-legacy [PlayerNamesActivity]. Language changes apply a locale and
 * recreate the activity, matching the previous behaviour.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val game = SpadesGame.getInstance()
        if (game.languages == null) {
            game.setLanguages(Languages.ENGLISH)
        }
        applyLocale(localeCodeFor(game.languages))

        enableEdgeToEdge()
        setContent {
            SpadesScoreTheme {
                val viewModel: GameViewModel = viewModel(factory = GameViewModel.Factory)
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                SetupScreen(
                    state = state,
                    onSelectPlayerCount = viewModel::selectPlayerCount,
                    onSelectLanguage = { language -> changeLanguage(viewModel, language) },
                    onNext = { startActivity(Intent(this, PlayerNamesActivity::class.java)) }
                )
            }
        }
    }

    private fun changeLanguage(viewModel: GameViewModel, language: Languages) {
        if (viewModel.uiState.value.language == language) return
        viewModel.selectLanguage(language)
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

- [ ] **Step 3: Build verifizieren**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. Es darf keine doppelte `MainActivity` mehr geben (die `.java` ist gelöscht). `AndroidManifest.xml` bleibt unverändert (Launcher zeigt weiter auf `.MainActivity`).

- [ ] **Step 4: Volle Unit-Test-Suite**

Run: `.\gradlew.bat test`
Expected: PASS – Phase-1-Engine-Tests + `GameViewModelTest` (+ Template-Test) grün.

- [ ] **Step 5: Manueller Smoke-Test (empfohlen)**

Run: `.\gradlew.bat installDebug` auf Gerät/Emulator. Prüfen:
- App startet auf dem neuen Compose-Setup-Screen (Logo, zwei Segmented-Selektoren, „Weiter").
- Spielerzahl 3/4 und Sprache EN/DE lassen sich umschalten; Sprachwechsel ändert die App-Sprache (Activity wird neu erstellt).
- „Weiter" führt in den bestehenden `PlayerNamesActivity`-Flow; ein vollständiges Spiel (3p und 4p) ist durchgängig spielbar und endet korrekt im Result-Screen.
- (Falls Patrick Hand nicht sofort erscheint: der downloadbare Font lädt asynchron über Google Play Services – Fallback-Schrift ist akzeptabel, identisch zum bisherigen Verhalten.)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/MainActivity.kt
git commit -m "$(printf 'feat: migrate Main/Setup screen to Compose single-activity host\n\nReplaces the View-based MainActivity with a Compose AppCompatActivity that\nhosts SetupScreen + GameViewModel. Player count/language flow through to the\nSpadesGame singleton; Next bridges to the legacy PlayerNamesActivity.\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Task 7 (optional): Compose-UI-Smoke-Test für `SetupScreen`

**Files:**
- Create: `app/src/androidTest/java/com/nwe/spadesscore/ui/setup/SetupScreenTest.kt`

> Benötigt ein Gerät/Emulator (`connectedAndroidTest`). Prüft Rendering + Interaktion des zustandslosen Screens.

- [ ] **Step 1: Test schreiben**

Erstelle `app/src/androidTest/java/com/nwe/spadesscore/ui/setup/SetupScreenTest.kt`:

```kotlin
package com.nwe.spadesscore.ui.setup

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.nwe.spadesscore.Languages
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SetupScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun next_invokesCallback() {
        var nextClicks = 0
        composeRule.setContent {
            SetupScreen(
                state = SetupUiState(playerCount = 4, language = Languages.ENGLISH),
                onSelectPlayerCount = {},
                onSelectLanguage = {},
                onNext = { nextClicks++ }
            )
        }

        composeRule.onNodeWithText("Start Game").assertIsDisplayed()
        composeRule.onNodeWithText("Start Game").performClick()

        assertEquals(1, nextClicks)
    }

    @Test
    fun playerCountSelection_invokesCallbackWithValue() {
        val selected = mutableListOf<Int>()
        composeRule.setContent {
            SetupScreen(
                state = SetupUiState(playerCount = 4, language = Languages.ENGLISH),
                onSelectPlayerCount = { selected += it },
                onSelectLanguage = {},
                onNext = {}
            )
        }

        composeRule.onNodeWithText("3 Players").performClick()

        assertTrue(selected.contains(3))
    }
}
```

- [ ] **Step 2: Test laufen lassen (Gerät/Emulator nötig)**

Run: `.\gradlew.bat connectedDebugAndroidTest --tests "com.nwe.spadesscore.ui.setup.SetupScreenTest"`
Expected: PASS (2 Tests). (Texte „Start Game"/„3 Players" entsprechen `R.string.next`/`R.string.three` in der Standard-Locale.)

- [ ] **Step 3: Commit**

```bash
git add app/src/androidTest/java/com/nwe/spadesscore/ui/setup/SetupScreenTest.kt
git commit -m "$(printf 'test(setup): add Compose UI smoke test for SetupScreen\n\nCo-Authored-By: Claude Opus 4.8 (1M context) <noreply@anthropic.com>')"
```

---

## Definition of Done (Phase 2)

- [ ] `MainActivity.kt` (Compose) ersetzt `MainActivity.java`; `activity_main.xml` entfernt; App startet auf dem Compose-Setup-Screen.
- [ ] M3-Theme mit fester Spade-Grün-Palette (hell+dunkel) und Patrick-Hand-Display-Schrift aktiv; kein Dynamic Color.
- [ ] `GameViewModel` hält den Setup-State als `StateFlow` und schreibt über `GameSetup` durch; `GameViewModelTest` grün (4 Tests).
- [ ] Spielerzahl-/Sprachauswahl funktionieren wie zuvor; „Weiter" führt in den unveränderten Legacy-Flow; vollständiges Spiel (3p und 4p) spielbar.
- [ ] `.\gradlew.bat test` grün; `.\gradlew.bat assembleDebug` erfolgreich.
- [ ] Keine andere Activity inhaltlich verändert.
