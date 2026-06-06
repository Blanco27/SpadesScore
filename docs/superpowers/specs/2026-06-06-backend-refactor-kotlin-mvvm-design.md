# Backend-Refactor: Kotlin + MVVM + Room — Design

**Datum:** 2026-06-06
**Status:** Genehmigt (Design)
**Scope:** Reiner Backend-/Code-Refactor. UI, Layouts, Ressourcen, Animationen, Navigationsfluss und beobachtbares Verhalten bleiben 1:1 identisch (Ausnahmen siehe „Bewusste Verhaltensänderungen").

---

## Ziel & Leitplanken

SpadesScore ist eine Single-Modul-Android-App zum Scoren eines Spades-artigen Stich-Vorhersage-Spiels. Aktuell: überwiegend Java, ein statischer `SpadesGame`-Singleton als God-Object, keine Persistenz, keine Tests, vestigiale Compose-Reste.

**Ziel:** Vollständige Modernisierung des Backends auf idiomatisches Kotlin mit MVVM-Architektur und Room-Persistenz — bei **unverändertem UI und Verhalten**.

**Harte Leitplanken:**
- XML-Layouts, Ressourcen, Themes, Fonts, Farben, Animationen (Shake, Color-Fill) und der Navigationsfluss bleiben unangetastet.
- Eine Activity pro Screen + explizite `Intent`-Navigation bleibt erhalten (geringstes Risiko fürs 1:1-Verhalten).
- Der Back-Button bleibt auf allen Screens deaktiviert.
- Die Spielregeln/Scoring-Formeln bleiben mathematisch exakt identisch (durch Unit-Tests abgesichert).

**Getroffene Entscheidungen (Brainstorming):**
1. Architektur-Tiefe: **Volle MVVM-Modernisierung**.
2. Persistenz: **Volle Persistenz mit Room** — aber unsichtbar (laufendes Spiel speichern/wiederherstellen), **keine** neuen Screens/Features.
3. Verhaltenstreue: **Offensichtliche Bugs/Debug-Code fixen** (Debug-Prefill entfernen, Dialoge lokalisieren).
4. Tests: **Ja**, Unit-Tests für die reine Spiellogik.
5. Umsetzung: **Ansatz A** — Single-Modul, Schichten per Package, manuelles DI (`AppContainer`), `StateFlow`. Kein Hilt, kein Multi-Modul, keine Fragment-/Navigation-Component-Umstellung.

---

## Zielarchitektur & Package-Struktur

Single-Modul, klare Schichten per Package. Abhängigkeitsrichtung strikt `ui → domain ← data`. Die `domain`-Schicht importiert **weder `android.*` noch `R.*` noch Room**.

```
com.nwe.spadesscore
├── SpadesApplication.kt          // hält AppContainer (manuelles DI)
├── di/
│   └── AppContainer.kt           // baut Database + Repository + ViewModel-Factory
├── domain/                       // REINES Kotlin – kein android.*, kein R.*, kein Room
│   ├── model/   GameState, Player, Language, GameSettings
│   ├── GameEngine.kt             // pure Transitions (startGame, declare, confirm, secondHalf)
│   ├── GameRules.kt              // Konstanten: 32 Karten, +5 Bonus, max 10 Zeichen, max 20 Runden, Medaillen
│   └── NameValidation.kt         // Validierung → Result-Typ (Empty / TooLong / Ok)
├── data/
│   ├── GameRepository.kt         // StateFlow<GameState>, persistiert bei jeder Mutation
│   ├── db/      AppDatabase, GameDao, GameEntity, PlayerEntity, ScoreEntity, PredictionEntity
│   └── mapper/  Entity ↔ Domain
└── ui/
    ├── common/  SpadesAppCompatActivity (Kotlin, Template-Methode + Back-Disable)
    ├── main/    MainActivity + MainViewModel + MainUiState
    ├── names/   PlayerNamesActivity + ViewModel + UiState
    ├── deal/    DealCardsActivity + ViewModel + UiState
    ├── declare/ DeclareTricksActivity + ViewModel + UiState
    ├── confirm/ ConfirmTicksActivity + ViewModel + UiState
    ├── result/  ResultScreenActivity + ViewModel + UiState
    └── widget/  TrickSpinner.kt (bleibt, leicht idiomatischer)
```

Der bisherige statische `SpadesGame`-Singleton **entfällt vollständig**. Seine Rolle übernehmen:
- `GameRepository` — Zustand (Single Source of Truth) + Persistenz
- die `ViewModel`s — Überleben von Config-Changes (z.B. Rotation), Aufbereitung des `UiState`

---

## Domain-Layer (reine Kotlin-Spiellogik)

Unveränderliches `GameState` + pure Transitionsfunktionen. Hier liegt die **gesamte** Spielregel-Logik; hier setzen die Unit-Tests an. Kein `Context`, kein `R.string`. Der bisherige Smell `getCombinedTrickPredictionString(DeclareTricksActivity, …)` (eine Activity als Parameter im Model) verschwindet komplett.

```kotlin
enum class Language { ENGLISH, GERMAN }

// scores: kumulativ, startet mit [0]
data class Player(val name: String, val scores: List<Int>)

data class GameState(
    val playerCount: Int,
    val players: List<Player>,
    val currentRound: Int,
    val dealerIndex: Int,
    val amountOfRounds: Int,
    val isSecondHalf: Boolean,
    val predictions: List<Int>,        // laufende Runde (überlebt Prozess-Tod)
    val language: Language,
) {
    val amountOfCards: Int get() =
        if (!isSecondHalf) currentRound else maxOf(1, amountOfRounds - currentRound + 1)
    val isGameOver: Boolean get() = currentRound > amountOfRounds
}
```

`GameEngine`: pure Funktionen, jede gibt ein neues `GameState` zurück. Dieselben Formeln wie heute, abgesichert durch Tests:
- `amountOfRounds = floor(32 / playerCount)` → 8 bei 4 Spielern, 10 bei 3 Spielern. `startSecondHalf` verdoppelt diesen Wert.
- **Scoring:** Treffer → `letzterKumulativwert + prediction + 5`; Fehlschlag → letzter Kumulativwert unverändert übernehmen (Score wird fortgeschrieben).
- **Kartenanzahl:** 1. Halbzeit Hochrampe (`currentRound`), 2. Halbzeit Runterrampe (`max(1, amountOfRounds - currentRound + 1)`).
- **Dealer-Rotation:** `(dealerIndex + 1) % playerCount`; Zufallsdealer setzt zufälligen Startindex `[0, playerCount)`.
- **3- vs 4-Spieler:** Iteration über `players` (Größe 3 oder 4) statt verstreuter `playerCount == 3`-Sonderfälle.

`GameRules`: zentrale Konstanten (32 Karten gesamt, +5 Bonus, max. Namenslänge 10, max. 20 Runden für die Ergebnistabelle, Medaillenfarben Gold `#ffd700` / Silber `#e6e6e6` / Bronze `#bf8970`, Hervorhebungs-Textgröße 30f).

`NameValidation`: pure Validierung, gibt ein Result zurück (`Empty` / `TooLong` / `Ok`), das die UI auf den jeweiligen Dialog mappt.

### Belegte invariante Formeln (Referenz für Tests)
- 4 Spieler: `amountOfRounds` 8 → nach Halbzeit 16. Karten Runde 1–8: 1..8; Runde 9–16: 8..1.
- 3 Spieler: `amountOfRounds` 10 → nach Halbzeit 20. Karten Runde 1–10: 1..10; Runde 11–20: 10..1.
- Die Ergebnistabelle ist auf max. 20 Runden ausgelegt (entspricht dem 3-Spieler-Vollspiel).

---

## Data-Layer (Room — unsichtbar)

Ein laufendes Spiel wird relational persistiert und beim Start wiederhergestellt — **ohne** neue Screens oder Features. `GameRepository` ist die einzige Quelle der Wahrheit und exponiert `StateFlow<GameState>`. Jede Mutation läuft durch die `GameEngine` und wird anschließend sofort in Room geschrieben (auf einem IO-Dispatcher).

**Schema:**
- `GameEntity` (id, playerCount, currentRound, dealerIndex, amountOfRounds, isSecondHalf, language)
- `PlayerEntity` (gameId, index, name)
- `ScoreEntity` (gameId, playerIndex, roundIndex, cumulativeScore)
- `PredictionEntity` (gameId, playerIndex, value) — damit auch mitten in einer Runde nichts verloren geht

Es gibt zu jedem Zeitpunkt **ein** aktives Spiel (single in-progress game). Mapper übersetzen Entity ↔ Domain.

**Fehlerbehandlung:** DB-Zugriffe gekapselt; Restore defensiv — ein korrupter/unvollständiger Stand führt zu „kein Spiel" (sauberer Fallback) statt zu einem Crash.

---

## UI-Layer (ViewModels + StateFlow)

Jeder Screen besteht aus: dünner Activity (Kotlin, viewBinding) + ViewModel + `UiState`. Die Activity collectet `StateFlow<UiState>` und rendert; Nutzer-Aktionen (Button-Klicks) → ViewModel → `GameEngine` → `GameRepository` → neuer State → neues `UiState`.

**Kernprinzipien:**
- **String-Formatierung wandert aus der Domain in die UI.** `UiState` trägt Rohwerte (z.B. `cardAmount: Int`, `predictions: List<Int>`); die Activity baut die lokalisierten Strings mit `getString(R.string…)`. Domain bleibt Android-frei.
- **Navigation 1:1:** weiterhin eine Activity pro Screen + explizite `Intent`s, gesteuert über `isGameOver`/`isSecondHalf` aus dem State. Back-Button bleibt deaktiviert.
- **Sprachwechsel:** Mechanik bleibt verhaltensgleich (`setLocale()` mutiert die Konfiguration + `recreate()`); die gewählte Sprache wird zusätzlich persistiert.
- **3- vs 4-Spieler:** Sichtbarkeits-Toggling der Spieler-4-Views bleibt (UI 1:1), aber datengetrieben über die Listengröße statt über `playerCount == 3`-Streuung.

**`ResultScreenActivity` (Sonderfall):**
- Das XML mit 20 Runden bleibt unangetastet (UI 1:1).
- Die ~80 `findViewById`-Aufrufe werden zu einer Schleife zusammengefasst, die die systematischen IDs (`score_playerN_roundM`, `player4_spaceN`, `roundNScores`) auflöst.
- `ResultViewModel` liefert ein `ResultUiState`: Zell-Matrix (Score-Werte), sichtbare Zeilen, Medaillen-Platzierung + Textgröße pro Spieler. Die Activity malt nur noch.
- Medaillenfarben und Textgröße 30f bleiben identisch.

`TrickSpinner.kt` bleibt erhalten (einzige bestehende Kotlin-View), wird nur leicht idiomatischer; Verhalten (Clamp `[0, maxAmount]`, `onValueChanged`-Callback) unverändert.

---

## Cleanup & Build-Konfiguration

**Vestigiales Compose entfernen** (wird zur Laufzeit nie genutzt — kein UI-Effekt):
- `kotlin-compose`-Plugin, `buildFeatures.compose`
- Deps: `activity-compose`, `compose-bom`, alle `ui*`-Artefakte, `material3`
- Dateien: `ui/theme/Color.kt`, `Theme.kt`, `Type.kt`
- `viewBinding` bleibt aktiv.

**Neue Dependencies** (im Version-Catalog `gradle/libs.versions.toml`, referenziert als `libs.*`):
- `room-runtime`, `room-ktx`, `room-compiler` (via **KSP**-Plugin)
- `lifecycle-viewmodel-ktx`

**Bug-/Debug-Fixes (freigegeben):**
- `PlayerNamesActivity.DEBUG_DELETE_LATER()` entfernen (Prefill „Player 1..4").
- Die 4 hartkodierten englischen Dialog-Strings (`"Please give each player a name"`, `"Empty Name"`, `"Names can only be a maximum of 10 characters long"`, `"Name too long"`) nach `values/strings.xml` + `values-de/strings.xml` auslagern und lokalisieren.

**Sonstiges:**
- `Languages.java` → Kotlin-Enum `Language` in `domain/model`.
- `SpadesApplication` anlegen und im Manifest via `android:name` registrieren (aktuell nicht vorhanden).

---

## Tests

JVM-Unit-Tests (kein Android, im `test/`-Source-Set) für die Domain — sichern, dass das Scoring **exakt** gleich bleibt:
- `amountOfRounds`: 8 (4P) / 10 (3P); Verdopplung bei Halbzeit.
- `amountOfCards`: Hochrampe in der 1. Halbzeit, Runterrampe in der 2. Halbzeit (inkl. `max(1, …)`-Randfall).
- Scoring: Treffer = `+prediction+5`, Fehlschlag = unverändert; kumulativ über mehrere Runden.
- Dealer-Rotation & Zufallsdealer (gültiger Index-Range), 3- vs 4-Spieler-Pfad.
- `isGameOver`-/Halbzeit-Übergänge.
- `NameValidation` (leer / >10 Zeichen / ok).
- Optional: Repository-Roundtrip mit In-Memory-Room (im `androidTest`-Source-Set).

Die bestehenden Template-Tests (`ExampleUnitTest.kt`, `ExampleInstrumentedTest.kt`) werden ersetzt/ergänzt.

---

## Migrationsstrategie (inkrementell, jeder Schritt build- & lauffähig)

1. **Build-Setup:** Compose-Stack entfernen, Room/KSP/lifecycle-viewmodel hinzufügen.
2. **Domain + Unit-Tests (TDD):** reine Logik zuerst, Tests grün.
3. **Data:** Room (Entities, DAO, Database) + Repository + Mapper.
4. **DI:** `SpadesApplication` + `AppContainer` + ViewModel-Factory; Manifest-Registrierung.
5. **ViewModels** pro Screen.
6. **Activities Java→Kotlin** einzeln umstellen, an ViewModel verdrahten, String-Formatierung in die UI ziehen — **nach jedem Screen 1:1 verifizieren**.
7. **Aufräumen:** Singleton, `Languages.java`, Debug-Code und Compose-Reste löschen; Voll-Build + alle Tests + manueller Smoke-Test jedes Screens.

---

## Bewusste Verhaltensänderungen (einzige Abweichungen von „1:1")

Diese sind explizit freigegeben und stellen Verbesserungen dar, kein UI-Redesign:
1. **Persistenz:** Ein laufendes Spiel überlebt nun Prozess-Tod/App-Neustart (vorher: Totalverlust).
2. **Kein Debug-Prefill** mehr in den Namensfeldern (vorher: „Player 1..4" vorausgefüllt).
3. **Lokalisierte Warn-Dialoge:** im Deutsch-Modus erscheinen die Validierungs-Dialoge jetzt auf Deutsch (vorher: immer Englisch).

Alles andere — Layouts, Farben, Animationen, Texte, Navigationsfluss, Scoring-Ergebnisse — bleibt exakt wie zuvor.

---

## 1:1-Verifikation

Nach jedem umgestellten Screen visueller Abgleich gegen den Ist-Zustand:
- Layout & Positionierung, Farben, Texte in **EN und DE**
- Animationen: Shake + Color-Fill (Buttons), Color-Fill (Spielerauswahl/Sprache)
- **3-Spieler- und 4-Spieler-Pfad**
- Halbzeit-Übergang (Ergebnisbildschirm → 2. Halbzeit, „Weiter"-Button-Sichtbarkeit)
- Endbildschirm: Medaillenfarben + Hervorhebung des letzten Scores
- Scoring-Ergebnisse identisch zu einem Referenzdurchlauf des alten Stands
