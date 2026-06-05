# Phase 3: In-Game-Flow nach Compose – Design

**Status:** Approved (Brainstorming abgeschlossen)
**Datum:** 2026-06-05
**Vorgänger:** Phase 1 (Domain-Core), Phase 2 (Compose-Spine + Main/Setup-Screen)

## Ziel

Die fünf verbliebenen Legacy-Activity-Screens (PlayerNames, DealCards, DeclareTricks, ConfirmTicks, ResultScreen) nach Jetpack Compose migrieren und in **einen NavHost** überführen, sodass die App nach Phase 3 eine vollständige **Single-Activity-Compose-App** ist. Der `SetupScreen` aus Phase 2 wird das Startziel des NavHost (statt per `startActivity` zu überbrücken). Ein einziges, Activity-weites `GameViewModel` besitzt den Spielzustand und ruft die reine `SpadesEngine` direkt auf.

## Entscheidungen (aus dem Brainstorming)

- **Umfang:** Eine kohärente Phase 3 — alle 5 Screens + NavHost + gemeinsames ViewModel in einem Spec/Plan/Impl-Zyklus. Der Implementierungsplan wird in viele kleine Tasks zerlegt (subagent-driven).
- **State-Quelle:** Das `GameViewModel` besitzt `GameState` als `StateFlow` und ruft `SpadesEngine` direkt auf. Der `SpadesGame`-Singleton wird **nicht mehr als Quelle der Wahrheit** genutzt und bleibt **toter Code bis Phase 4**. Der Phase-2-`GameSetup`/`SpadesGameSetup`-Write-Through-Port **entfällt**.
- **Visuelle Richtung:** M3-Refresh für alle Screens (konsistent mit dem Phase-2-Setup-Screen: Spade-Grün-Palette, Patrick-Hand-Titel, M3-Karten/Komponenten). Verhalten/Regeln bleiben identisch.
- **Kleine Verbesserungen (alle gewählt):**
  1. Validierungstexte lokalisieren (EN+DE) statt hartkodiertem Englisch.
  2. Inline-Feldvalidierung (`isError`/`supportingText`) statt AlertDialog-Popups.
  3. DEBUG-Namens-Vorbefüllung (`DEBUG_DELETE_LATER`) nicht übernehmen — Felder starten leer.
  4. „Neues Spiel"-Aktion am finalen Result-Screen (heute eine Sackgasse).

## Nicht im Umfang (spätere Phasen)

- Entfernen des `SpadesGame`-Singletons und der ungenutzten Adapter-/String-Helfer → **Phase 4**.
- Umstellung der Lokalisierung auf `AppCompatDelegate.setApplicationLocales` (per-App-Locales) → **Phase 4**. Phase 3 behält den Phase-2-Ansatz (Locale anwenden + `recreate()`).
- Persistenz über Prozesstod hinaus (Room/DataStore) → bewusst **kein** Bestandteil. Nur Config-Change-Stabilität über das ViewModel.

---

## Architektur

### Navigation (ein NavHost)

`MainActivity` hostet einen `NavHost` (neue Dependency `androidx.navigation:navigation-compose`). Routen:

| Route | Screen | Bemerkung |
|-------|--------|-----------|
| `setup` | `SetupScreen` (Phase 2, angepasst) | **Startziel** |
| `players` | `PlayerNamesScreen` | |
| `deal` | `DealCardsScreen` | |
| `declare` | `DeclareTricksScreen` | |
| `confirm` | `ConfirmTicksScreen` | |
| `result` | `ResultScreen` | |

**Übergänge:**

- `setup` → `players`
- `players` → `deal` (nach `startGame`)
- `deal` → `declare`
- `declare` → `confirm` (nach `setPredictions`)
- `confirm` → `deal` (nächste Runde) **oder** `confirm` → `result` (Halbzeit/Spielende), abhängig von `game.showResultScreen`
- `result` (Halbzeit) → `deal` (nach `continueSecondHalf`)
- `result` (Spielende) → `setup` (nach `newGame`/Reset)

**Zurück-Verhalten (wie heute deaktiviert):** Ein No-op-`BackHandler(enabled = true) {}` auf oberster Ebene der Compose-Hierarchie verhindert, dass „Zurück" navigiert oder die App verlässt. Zusätzlich nutzt jeder Vorwärts-Übergang `popUpTo` so, dass kein sichtbarer Back-Stack entsteht (Wizard-Charakter bleibt erhalten).

**Navigation wird im NavHost gesteuert, nicht im ViewModel** — das ViewModel kennt keinen `NavController` (bleibt JVM-testbar). Die Composables rufen VM-Aktionen auf und lösen über übergebene `onNavigate…`-Callbacks die Navigation aus; Verzweigungen (confirm→deal/result, result→deal/setup) entscheiden die Callbacks anhand des resultierenden `GameUiState`.

### State – ein gemeinsames `GameViewModel`

Das Phase-2-`GameViewModel` (`ui/setup/`) wird zum **einzigen, Activity-weiten** Spiel-ViewModel ausgebaut (Ablageort z. B. `ui/game/GameViewModel.kt`; finaler Pfad im Plan). Es wird im NavHost einmal Activity-scoped bezogen und an alle Ziele übergeben.

**State:**

```kotlin
data class GameUiState(
    val playerCount: Int = 4,
    val language: Languages = Languages.ENGLISH,
    val game: GameState? = null   // null vor startGame; danach der unveränderliche Domain-State
)
```

Exponiert als `val uiState: StateFlow<GameUiState>`.

**Aktionen (alle delegieren an die reine `SpadesEngine` und ersetzen `_uiState`):**

- `selectPlayerCount(count: Int)` / `selectLanguage(language: Languages)` — wie Phase 2 (Setup-Screen).
- `startGame(names: List<String>, randomDealer: Boolean)` — ermittelt Startspieler (bei `randomDealer` via `SpadesEngine.randomStartingPlayer(playerCount, Random)`, sonst 0), dann `SpadesEngine.newGame(playerCount, names, start)` → `game`.
- `setPredictions(predictions: List<Int>)` — `SpadesEngine.setTickPredictions(game, predictions)`.
- `confirmRound(made: List<Boolean>)` — `SpadesEngine.confirmTricks(game, made)`; der neue `game.showResultScreen` steuert die Navigation.
- `continueSecondHalf()` — `SpadesEngine.startSecondHalf(game)`.
- `newGame()` — Reset: `game = null` (Sprache bleibt; `playerCount` bleibt für den Setup-Screen wählbar).

**Reine Hilfen** (delegiert/getestet, keine Android-Abhängigkeit):

- `amountOfCards()` → `SpadesEngine.amountOfCards(game)`.
- `ranking()` → `SpadesEngine.rankingForLastRound(game)`.
- `isPredictionSumValid(sum: Int): Boolean` = `sum != amountOfCards()` — exakt die heutige Regel (ungültig, wenn die Summe der Ansagen der möglichen Stichzahl **entspricht**).

**Wegfall des Ports:** `GameSetup` und `SpadesGameSetup` werden gelöscht; der `Factory` konstruiert künftig `GameViewModel()` ohne Singleton-Abhängigkeit (das VM nutzt das `SpadesEngine`-Objekt direkt und ist damit ohne Fake JVM-testbar). Der Phase-2-`SetupScreen` und `GameViewModelTest` werden auf das erweiterte VM/`GameUiState` angepasst (`SetupUiState` entfällt zugunsten von `GameUiState`).

**Sprache & Locale:** Die gewählte Sprache lebt im VM. `MainActivity` beobachtet sie und wendet Locale + `recreate()` an (wie Phase 2). Da Sprache nur auf dem Start-Ziel `setup` geändert wird und das VM (Activity-scoped) `recreate()` überlebt, geht kein Spielzustand verloren; der NavHost startet nach `recreate()` neu beim `setup`-Ziel, was vor Spielbeginn unkritisch ist.

---

## Screens (Compose, M3-Refresh)

Alle Texte über `stringResource`. Spieler werden als M3-Karten/Listenzeilen dargestellt. Für `playerCount == 3` wird der 4. Spieler-Slot schlicht **nicht gerendert** (kein `View.GONE`-Workaround mehr) — die Composables iterieren über `0 until playerCount`.

### `PlayerNamesScreen`
- `playerCount` M3-`OutlinedTextField`s (3 oder 4), **leer beim Start** (kein Debug-Prefill).
- **Inline-Validierung:** leer → Fehlertext „Bitte einen Namen eingeben"; > 10 Zeichen → „Maximal 10 Zeichen". Über `isError` + `supportingText`, lokalisiert (neue String-Keys). „Spiel starten" ist nur bei gültigen Namen aktiv (oder zeigt die Fehler beim Versuch).
- Geber-Auswahl (zufälliger Geber) als M3-`Checkbox` (entspricht der heutigen `random_dealer_checkBox`; der jüngste Commit hat bewusst von Switch auf CheckBox umgestellt).
- Aktion: `vm.startGame(names, randomDealer)` → Navigation `deal`.

### `DealCardsScreen`
- Patrick-Hand-Titel mit Rundennummer (`R.string.round`), `LinearProgressIndicator` (`currentRound` / `amountOfRounds`).
- „<Spieler> muss geben" (`R.string.player_must_deal_cards` mit `currentPlayerName`), Kartenanzahl „Nx" (`amountOfCards()`).
- Aktuelle Punktestände aller Spieler (Karten/Zeilen).
- Button → `declare`.

### `DeclareTricksScreen`
- Pro Spieler: Name + aktueller Punktestand + **`Stepper`** (neuer Composable, ersetzt `TrickSpinner`).
- Summen-Status-Banner: grün „ok" wenn `isPredictionSumValid`, rot/Fehlerfarbe + Hinweis wenn ungültig (Summe == mögliche Stiche). „Weiter" nur aktiv bei gültiger Summe.
- Aktion: `vm.setPredictions(values)` → Navigation `confirm`.

### `ConfirmTicksScreen`
- Pro Spieler: Name + aktueller Punktestand + Ansage (`prediction` Trick/Tricks) + Punkte-Vorschau (`prediction + 5`) + **„erfüllt?"-Checkbox**; aktivierte Checkbox hebt die Punkte-Vorschau farblich hervor.
- Aktion: `vm.confirmRound(made)`; danach Navigation `deal` (nächste Runde) **oder** `result`, abhängig von `game.showResultScreen`.

### `ResultScreen`
- **Datengetriebene** Score-Tabelle: Spalte je Spieler, Zeile je gespielter Runde, Werte aus `game.scores` (kumulative Totale; der einleitende `0`-Eintrag wird übersprungen). Vertikal scrollbar (`LazyColumn`/scrollbare `Column`) — ersetzt die 20 hartkodierten Zeilen.
- Die **letzte** Zeile wird nach `ranking()` (= `SpadesEngine.rankingForLastRound`) eingefärbt: Platz 1/2/3 → Gold `#FFD700` / Silber `#E6E6E6` / Bronze `#BF8970`, leicht vergrößert (wie heute).
- **Halbzeit:** Button „Weiter" → `vm.continueSecondHalf()` → `deal`.
- **Spielende (zweite Hälfte):** Button „Neues Spiel" → `vm.newGame()` → `setup`.

### `Stepper` (wiederverwendbarer Composable)
- Ersetzt die `TrickSpinner`-View + `view_trick_spinner.xml`. „−" / Wert / „+"; klemmt auf `0..max`; `onValueChange`-Callback. Reines, zustandsloses Composable (Wert wird gehoben).

---

## Löschungen & Ressourcen

**Nach erfolgreicher Migration entfernt:**
- `PlayerNamesActivity.java`, `DealCardsActivity.java`, `DeclareTricksActivity.java`, `ConfirmTicksActivity.java`, `ResultScreenActivity.java`
- `SpadesAppCompatActivity.java` (Activity-Basisklasse — nicht mehr nötig)
- `TrickSpinner.kt`, `res/layout/view_trick_spinner.xml`
- `res/layout/activity_player_names.xml`, `activity_deal_cards.xml`, `activity_declare_tricks.xml`, `activity_confirm_tricks.xml`, `activity_result_screen.xml`
- `GameSetup.kt` (Interface + `SpadesGameSetup`)
- Die 5 `<activity>`-Einträge der Spiel-Screens in `AndroidManifest.xml` (nur `.MainActivity` bleibt; Launcher-Intent-Filter unverändert).

**Bleibt (toter Code bis Phase 4):** `SpadesGame.java`.

**Neue String-Ressourcen (EN + DE):** Validierung (leerer Name, Name zu lang), „Neues Spiel", sowie alle Button-/Label-Texte, die bisher nur im XML standen und in Compose über `stringResource` benötigt werden. Bestehende Keys werden wiederverwendet, wo vorhanden.

---

## Tests

- **`GameViewModelTest` (JVM, erweitert):** `startGame` seedet `game` korrekt (Spielerzahl, Namen, Startspieler; Zufallsgeber mit injizierter/deterministischer RNG-Strategie); `setPredictions`; `confirmRound` hängt Scores an, schaltet die Runde weiter und flaggt `showResultScreen` korrekt; `continueSecondHalf`; `newGame` setzt `game` auf `null` zurück; `isPredictionSumValid` (gültig/ungültig an der Grenze `sum == amountOfCards`). Setup-Aktionen (`selectPlayerCount`/`selectLanguage`) bleiben getestet.
- Die Spielregeln selbst sind bereits über `SpadesEngineTest` (Phase 1) abgedeckt; die VM-Tests prüfen die Orchestrierung und abgeleiteten Hilfen.
- **Compose-UI-Tests:** optional (wie Phase 2; benötigen Gerät/Emulator), nicht verbindlich für die Definition of Done.

---

## Definition of Done

- [ ] Alle 5 Spiel-Screens sind Compose-Ziele in einem NavHost in `MainActivity`; der `SetupScreen` ist das Startziel (kein `startActivity`-Bridge mehr).
- [ ] Ein gemeinsames, Activity-weites `GameViewModel` besitzt `GameState` als `StateFlow` und ruft `SpadesEngine` direkt; `GameSetup`/`SpadesGameSetup` sind entfernt.
- [ ] M3-Refresh konsistent mit Phase 2; `playerCount == 3` rendert den 4. Slot nicht.
- [ ] Validierung lokalisiert + inline; kein Debug-Prefill; „Neues Spiel" am Spielende.
- [ ] Alte Activities/Basisklasse/`TrickSpinner`/Layout-XMLs gelöscht; die zugehörigen `<activity>`-Manifest-Einträge entfernt; `SpadesGame.java` bleibt (Phase 4).
- [ ] Zurück bleibt im gesamten Flow deaktiviert.
- [ ] `.\gradlew.bat test` grün (inkl. erweiterter `GameViewModelTest`); `.\gradlew.bat assembleDebug` erfolgreich.
- [ ] Ein vollständiges Spiel (3p und 4p), inkl. Halbzeit-Result, zweiter Hälfte und „Neues Spiel", ist durchgängig spielbar.
