# SpadesScore – Phase 2: Compose-Architektur-Gerüst (Main-Screen)

**Datum:** 2026-06-05
**Status:** Design / Spec (Phase 2 der Kotlin-+-Compose-Modernisierung)
**Voraussetzung:** Phase 1 abgeschlossen (reiner Kotlin-Domain-Kern `SpadesEngine`/`GameState`, `SpadesGame` als dünner Adapter).
**Übergeordnetes Spec:** `docs/superpowers/specs/2026-06-05-kotlin-compose-modernisierung-design.md`

---

## 1. Ziel dieser Phase

Das **Architektur-Gerüst** für die Compose-UI etablieren und damit **einen** Screen (den Main-/Setup-Screen) end-to-end nach Jetpack Compose migrieren, während die übrigen fünf Screens als Legacy-Activities weiterlaufen. Am Ende der Phase ist der neue Compose-Stack (Single-Activity-Host + M3-Theme + ViewModel) bewiesen und die App durchgängig spielbar.

Risikoreduktion: Compose + ViewModel + M3-Theme zum Laufen bringen ist der eigentliche Architektur-Schritt. Die Navigation zwischen Compose-Screens (NavHost) ist Standard und kommt erst, wenn es ≥2 Compose-Ziele gibt (Phase 3).

## 2. Getroffene Entscheidungen (aus dem Brainstorming)

- **Strategie (Ansatz 1 – schlankes Gerüst):** Der neue Compose-Main-Screen überbrückt per `startActivity(PlayerNamesActivity)` in den bestehenden Legacy-Flow. **Kein NavHost** in dieser Phase.
- **UI-Layout (Variante A):** Logo oben, zwei beschriftete **SegmentedButton**-Gruppen (Spielerzahl 3/4, Sprache EN/DE), große gefüllte „Weiter"-Schaltfläche unten.
- **Theme-Palette:** **Spade-Grün als feste Marke** (kein Dynamic Color), Hell- **und** Dunkel-Schema.
- **Typografie:** **Patrick Hand** für Display/Headline (Logo, Titel), M3-Standardschrift für Fließtext & Buttons.
- **Config-Change-Stabilität:** über das überlebende `ViewModel` (kein `SavedStateHandle` nötig, da Prozesstod-Persistenz außerhalb des Scopes liegt).
- **Sprachwechsel:** vorerst der bestehende Mechanismus (Config-Update + `recreate()`); Umstellung auf `AppCompatDelegate` bleibt Phase 3.

## 3. Nicht-Ziele (bewusst ausgeschlossen)

- Kein `NavHost`/`navigation-compose` (Phase 3).
- Migration der übrigen Screens (PlayerNames, DealCards, DeclareTricks, ConfirmTicks, ResultScreen) – bleiben Legacy-Activities.
- Kein `AppCompatDelegate.setApplicationLocales` (Phase 3).
- Keine Prozesstod-Persistenz (`SavedStateHandle`/DataStore).
- Kein Redesign anderer Screens. Die temporäre visuelle Inkonsistenz (modernes M3-Main → Legacy-Look der Folge-Screens) ist während der inkrementellen Migration akzeptiert.

## 4. Architektur

Unidirektionaler Datenfluss für den Setup-Screen:

```
Compose SetupScreen ─event─▶ GameViewModel.select*() ─▶ SetupUiState (StateFlow)
        ▲                                   │
        │ recompose                         └─ write-through ▶ GameSetup ─▶ SpadesGame (Legacy-Bridge)
        └───────────── collectAsStateWithLifecycle ◀─────────┘
"Weiter" ─▶ onNext ─▶ MainActivity.startActivity(PlayerNamesActivity)
```

### 4.1 Schichten & Komponenten

- **`MainActivity` (Kotlin/Compose, ersetzt die Java-Variante, bleibt Launcher):** `AppCompatActivity`, die in `onCreate` die Locale anwendet und `setContent { SpadesScoreTheme { SetupScreen(...) } }` aufruft. Hält keinen Spielzustand; verdrahtet ViewModel ↔ Screen, behandelt Sprachwechsel (Locale + `recreate()`) und die `startActivity`-Brücke zu `PlayerNamesActivity`.
- **`GameViewModel` (`androidx.lifecycle.ViewModel`):** Quelle der Wahrheit für den Setup-UI-State. Exponiert `StateFlow<SetupUiState>`; Methoden `selectPlayerCount(Int)`, `selectLanguage(Languages)`. Schreibt jede Änderung über ein `GameSetup`-Port an den Legacy-Singleton durch. Überlebt Config-Changes von Haus aus → Config-Change-Stabilität ohne `SavedStateHandle`.
- **`SetupUiState` (data class):** `playerCount: Int`, `language: Languages`.
- **`GameSetup` (Interface) + `SpadesGameSetup` (Impl):** dünner Port, über den das ViewModel Spielerzahl/Sprache an `SpadesGame.getInstance()` durchschreibt und initiale Werte liest. Entkoppelt das ViewModel vom Singleton → das ViewModel ist als reiner JVM-Unit-Test prüfbar (Fake-Port), ohne Android. Stepping Stone dahin, dass das ViewModel in Phase 3 den Zustand vollständig selbst besitzt.
- **`SetupScreen` (zustandsloses Composable):** `SetupScreen(state, onSelectPlayerCount, onSelectLanguage, onNext)`. Enthält Logo (`painterResource(R.drawable.spades_logo)`), zwei `SegmentedSelector`, „Weiter"-`Button`. Alle Texte via `stringResource` (vorhandene `R.string`-Keys: `number_of_players`, `language`, `three`, `four`, `english`, `deutsch`, `next`, `contentDescription`).
- **`SegmentedSelector` (wiederverwendbares Composable):** beschriftete `SingleChoiceSegmentedButtonRow` (M3) über eine Optionsliste; ersetzt die bisherige hand­animierte Umschalt-Logik (`animateFill`/`select`/`deselect`) durch M3-Standard-Auswahlvisuals.
- **`ui/theme/*` (überarbeitet):** echtes M3-Theme statt Template – feste Spade-Grün-`ColorScheme` (hell+dunkel), Patrick-Hand-`FontFamily` in den Display/Headline-Styles, `dynamicColor` entfernt.

### 4.2 Datei-Struktur

| Datei | Aktion | Verantwortung |
|------|--------|---------------|
| `gradle/libs.versions.toml` | ändern | `lifecycle-viewmodel-compose` und `lifecycle-runtime-compose` als Library-Einträge ergänzen (Version an `lifecycleRuntimeKtx` 2.10.0 ausgerichtet) |
| `app/build.gradle.kts` | ändern | Dependencies `androidx.lifecycle:lifecycle-viewmodel-compose` (für `viewModel()`) und `androidx.lifecycle:lifecycle-runtime-compose` (für `collectAsStateWithLifecycle`) aufnehmen |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Color.kt` | ändern | Spade-Grün-Farbtokens (hell+dunkel) |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Theme.kt` | ändern | feste hell/dunkel-`ColorScheme`, kein Dynamic Color |
| `app/src/main/java/com/nwe/spadesscore/ui/theme/Type.kt` | ändern | Patrick Hand für Display/Headline, M3-Default für Body |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/SetupScreen.kt` | neu | zustandsloses `SetupScreen` + `SegmentedSelector` (+ `@Preview`) |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameViewModel.kt` | neu | `SetupUiState` + `GameViewModel` |
| `app/src/main/java/com/nwe/spadesscore/ui/setup/GameSetup.kt` | neu | `GameSetup`-Interface + `SpadesGameSetup`-Impl |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.kt` | neu | Compose-Host (ersetzt `MainActivity.java`) |
| `app/src/main/java/com/nwe/spadesscore/MainActivity.java` | löschen | durch Kotlin-Compose-Variante ersetzt |
| `app/src/main/res/layout/activity_main.xml` | löschen | nicht mehr genutzt |
| `app/src/test/java/com/nwe/spadesscore/ui/setup/GameViewModelTest.kt` | neu | JVM-Unit-Tests des ViewModels (Fake-`GameSetup`) |
| `app/src/androidTest/java/com/nwe/spadesscore/ui/setup/SetupScreenTest.kt` | neu (optional) | Compose-UI-Smoke-Test des Setup-Screens |

`AndroidManifest.xml` bleibt unverändert – der Launcher zeigt weiter auf `.MainActivity` (gleicher Klassenname/Package).

## 5. Verhalten & Details

- **Spracheinstellung:** `MainActivity.onCreate` liest die Sprache aus `GameSetup` (Default Englisch, falls keine gesetzt – wie heute), wendet die Locale **vor** `setContent` an, und ruft danach `setContent`. Der Sprach-Callback aktualisiert das ViewModel und ruft `applyLocale(code)` + `recreate()`. Das ViewModel überlebt `recreate()`; die Sprache liegt zusätzlich im Singleton, sodass `onCreate` sie nach der Neuerstellung wieder liest. (Verhalten identisch zum heutigen `MainActivity.setLocale`.)
- **Spielerzahl/Sprache write-through:** Bei jeder Auswahl schreibt das ViewModel `playerCount`/`language` über `GameSetup` in `SpadesGame`. Beim Tippen auf „Weiter" hat der Singleton bereits die korrekten Werte; `PlayerNamesActivity` liest sie unverändert.
- **Zurück-Taste:** Der Main-Screen ist die Wurzel – Standard-Back beendet die App (kein No-op-Handler nötig). Die Legacy-Screens behalten ihren No-op-Back.
- **Edge-to-Edge:** `enableEdgeToEdge()` in `MainActivity` für einen modernen randlosen Look (von `activity-compose` bereitgestellt); Inhalte respektieren `WindowInsets`.
- **Activity-Theme:** sicherstellen, dass das in `AndroidManifest`/`themes.xml` gesetzte Theme der Launcher-Activity **kein ActionBar** zeigt (Compose zeichnet die UI selbst). Die bestehenden Activities laufen bereits ohne ActionBar – während der Umsetzung verifizieren.

## 6. Teststrategie

- **`GameViewModelTest` (JVM, JUnit4) – verbindlich:** mit einem **Fake-`GameSetup`** prüfen:
  - Initialzustand entspricht den vom Port gelieferten Startwerten (Default 4 Spieler, Englisch).
  - `selectPlayerCount(3)` setzt `SetupUiState.playerCount = 3` **und** ruft `GameSetup.setPlayerCount(3)`.
  - `selectLanguage(GERMAN)` setzt `SetupUiState.language = GERMAN` **und** ruft `GameSetup.setLanguage(GERMAN)`.
  - Mehrfachauswahl bleibt konsistent (letzter Wert gewinnt).
- **`SetupScreenTest` (androidTest, Compose-UI) – optional/empfohlen:** Screen rendert; Tippen auf „3" wählt 3; Tippen auf „Weiter" löst den `onNext`-Callback aus.

Hinweis: Die `GameSetup`-Entkopplung dient genau dieser JVM-Testbarkeit – das ViewModel berührt kein Android und keinen `Context`.

## 7. Definition of Done

- [ ] `MainActivity.kt` (Compose) ersetzt `MainActivity.java`; `activity_main.xml` entfernt; App startet auf dem neuen Compose-Setup-Screen.
- [ ] M3-Theme mit fester Spade-Grün-Palette (hell+dunkel) und Patrick-Hand-Display-Schrift aktiv; kein Dynamic Color.
- [ ] `GameViewModel` hält den Setup-State als `StateFlow` und schreibt über `GameSetup` durch; `GameViewModelTest` grün.
- [ ] Spielerzahl- und Sprachauswahl funktionieren wie zuvor; „Weiter" führt in den unveränderten Legacy-Flow; ein vollständiges Spiel (3p und 4p) ist durchgängig spielbar.
- [ ] Sprachwechsel (EN↔DE) wirkt app-weit wie bisher.
- [ ] `.\gradlew.bat test` grün (Phase-1-Tests + `GameViewModelTest`); `.\gradlew.bat assembleDebug` erfolgreich.
- [ ] Keine andere Activity inhaltlich verändert (außer der Main-Ersetzung).

## 8. Risiken & offene Punkte

- **Activity-Theme/ActionBar:** Falls das App-Theme eine ActionBar zeigt, erscheint sie über dem Compose-Inhalt → in der Umsetzung `themes.xml` prüfen und ggf. ein `NoActionBar`-Theme verwenden.
- **`recreate()`-Flicker beim Sprachwechsel:** entspricht dem heutigen Verhalten; die saubere Lösung (`AppCompatDelegate`) ist bewusst Phase 3.
- **`SegmentedButton`-Verfügbarkeit:** in der genutzten Compose-BOM (2025.11.01) stabil enthalten – während der Umsetzung verifizieren.
- **Doppelte Quelle der Wahrheit (ViewModel + Singleton):** bewusst akzeptiert, solange der Legacy-Flow den Singleton liest. Das write-through hält beide synchron; in Phase 3 entfällt der Singleton.

## 9. Nicht Teil dieses Specs

Phasen 3–4 (NavHost + Migration der übrigen Screens; Altlasten/Lokalisierungs-Umstellung) – jeweils eigene Specs.
