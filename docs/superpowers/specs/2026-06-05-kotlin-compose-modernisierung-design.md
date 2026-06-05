# SpadesScore – Modernisierung zu Kotlin + Jetpack Compose

**Datum:** 2026-06-05
**Status:** Design / Übergreifendes Spec (Phase 1 ausgearbeitet, Phasen 2–4 als Roadmap)
**Fokus (vom Nutzer gewählt):** Code-Qualität + Modernisierung + vollständige Kotlin-Umstellung, volle Compose-UI, Config-Change-stabil (keine vollständige Persistenz)

---

## 1. Kontext & Ausgangslage

SpadesScore ist eine Single-Module-Android-App zum Mitschreiben der Punkte beim Kartenspiel Spades (kein Kartenspiel-Gameplay, nur Vorhersagen und Punkte-Tally über Runden).

Aktueller Zustand:

- **Sprachmix:** Activities und Spiellogik in **Java**; `TrickSpinner` und `ui/theme/*` in **Kotlin**. Das Compose-Theme-Gerüst (`Theme.kt`, `Color.kt`, `Type.kt`) ist Template-Überrest und **ungenutzt**; die echte UI sind klassische XML-Views.
- **Globaler Zustand im Singleton `SpadesGame.getInstance()`:** Spieleranzahl, Namen, Score-Historie (`LinkedList<LinkedList<Integer>>`), Vorhersagen, aktuelle Runde/Spieler, Halbzeit-Flags. **Keine Persistenz** – bei Prozesstod ist alles weg; Config-Changes (Rotation, Sprachwechsel via `recreate()`) hängen daran, dass der statische Singleton überlebt.
- **6 Activities + Intent-Navigation:** MainActivity → PlayerNames → DealCards → DeclareTricks → ConfirmTicks → (zurück zu DealCards | ResultScreen). Der Zurück-Button ist überall per No-op-Callback global deaktiviert.
- **`viewBinding = true` aktiviert, aber überall wird `findViewById` benutzt** – ungenutztes Potenzial.
- **`ResultScreenActivity`:** ~250 Zeilen reines `findViewById`-Boilerplate für **20 hartcodierte Runden × 4 Spieler** (`score_playerX_roundY`). Kapazität/Spieleranzahl ändern heißt XML **und** parallele `find*`-Methoden anfassen.
- **Duplizierung:** `animateFill` in `MainActivity` und `DeclareTricksActivity`; wiederholte Button-Styling-Logik (`select`/`deselect`); near-identische `find*TextViews`-Methoden.
- **Keine echten Tests** – `test`/`androidTest` enthalten nur Template-Stubs. Die gesamte Scoring-Logik ist ungetestet.
- **Altlasten:** `PlayerNamesActivity.DEBUG_DELETE_LATER()` füllt bei jedem Start Namen vor; zwei Dialoge in `PlayerNamesActivity` sind hartcodiert englisch (i18n-Lücke); `MainActivity.setLocale()` nutzt das deprecated `resources.updateConfiguration`.
- **3- vs. 4-Spieler:** per `View.GONE` gelöst, fragile Indizierung (z. B. `confirmTickPredictions` ignoriert bewusst den letzten Eintrag bei 3 Spielern; Result-Färbung indiziert über `getCurrentRound() - 2`).

## 2. Ziele & Nicht-Ziele

### Ziele
1. **Vollständige Kotlin-Umstellung** der gesamten App.
2. **Volle Jetpack-Compose-UI:** Ersatz der XML-Views/Multi-Activity-Struktur durch eine **Single-Activity-App mit Compose Navigation**.
3. **Saubere Schichtenarchitektur:** reiner, testbarer Domain-Kern + ViewModel + zustandslose Compose-UI, unidirektionaler Datenfluss.
4. **Config-Change-Stabilität** via ViewModel + `rememberSaveable`/`SavedStateHandle` – Rotation und Sprachwechsel verlieren nichts mehr.
5. **Unit-Test-Abdeckung** der Spiel-/Scoring-Regeln (JVM, ohne Android-Abhängigkeiten).
6. **Aufräumen:** toten Code, deprecated APIs und Duplizierung entfernen; DE/EN-Lokalisierung vervollständigen; das Compose-Theme-Gerüst tatsächlich nutzen.

### Nicht-Ziele (bewusst ausgeschlossen)
- **Vollständige Persistenz** (kein Room/DataStore). Ein laufendes Spiel überlebt **keinen** vollständigen Prozesstod. *(Nutzer-Entscheidung: nur Config-Change-stabil.)*
- **Neue Gameplay-Features** (Statistiken, Runde rückgängig, Spielstand speichern/laden, mehr als 4 Spieler).
- **Play-Store-Release-Reife** (Signing, Versionierung, Store-Assets) – separater Aufwand.
- Backend/Multiplayer/Netzwerk.

## 3. Zielarchitektur

Drei Schichten, unidirektionaler Datenfluss:

```
UI-Event ─▶ GameViewModel.method() ─▶ SpadesEngine (pure) ─▶ neuer GameState
   ▲                                                              │
   └──────────── Compose recomposes ◀── StateFlow<GameUiState> ◀──┘
```

### 3.1 Domain (reines Kotlin, keine Android-Imports)
- **`GameState`** – immutable `data class`: `playerCount`, `playerNames`, `currentRound`, `currentPlayer`, `amountOfRounds`, `secondHalf`, `showResultScreen`, `scores: List<List<Int>>`, `tickPredictions: List<Int>`, `language`.
- **`SpadesEngine`** – reine Funktionen, die jeweils einen neuen `GameState` zurückgeben: `startGame(...)`, `setTickPredictions(...)`, `confirmTricks(...)`, `startSecondHalf(...)`, `setRandomDealer(...)` sowie abgeleitete Werte (`amountOfCards`, `rankingForLastRound`, Rundentitel …).
- **Einzige Quelle der Wahrheit für die Spielregeln**, 100 % per JUnit testbar.

### 3.2 Präsentation
- **`GameViewModel`** (`androidx.lifecycle`): hält den aktuellen `GameState`, exponiert `StateFlow<GameUiState>`. Überlebt Config-Changes. Setup-Parameter (Spieleranzahl, Sprache, Namen) über `SavedStateHandle`, damit der Setup-Schritt einen Prozesstod übersteht (das laufende Spiel selbst nicht – siehe Scope).

### 3.3 UI (Compose)
- Eine **`MainActivity`** → `setContent { SpadesScoreTheme { SpadesApp() } }`.
- **`SpadesApp`** hostet einen `NavHost` mit typisierten Routen. Jeder Screen ist ein **zustandsloses Composable** (bekommt State + Callbacks); ein dünner Wrapper verbindet ihn mit dem ViewModel.
- **Material-3-Theme** durch Wiederverwendung/Bereinigung von `ui/theme` (endlich genutzt), inkl. der Custom-Fonts (Patrick Hand etc.) in der Typography.
- **Navigation:** Routen Setup → PlayerNames → Deal → Declare → Confirm → (Loop zu Deal | Result) → (Continue → Deal für 2. Halbzeit). „Zurück deaktivieren" wird zu bewusstem Nav-Graph-Design (`popUpTo` / kein Zurück in eine bestätigte Runde) + gezieltem `BackHandler` statt globalem No-op.
- **Lokalisierung:** Per-App-Locale über `AppCompatDelegate.setApplicationLocales(LocaleListCompat…)`; alle Texte über `stringResource`; hartcodierte englische Dialoge beheben.

## 4. Strategie: Strangler / inkrementell (Approach A)

Domain-first mit Tests als Sicherheitsnetz, Compose-Gerüst danebenbauen, Screens einzeln migrieren, zuletzt den Launcher umstellen und Altlasten löschen. **Die App baut & läuft an jeder Phasengrenze.**

## 5. Phasenzerlegung

Jede Phase bekommt später ihr **eigenes** Spec → Plan → Implementierung. Dieses Dokument fixiert Zielarchitektur + Zerlegung und arbeitet **Phase 1** aus.

| Phase | Inhalt | Ergebnis |
|------|--------|----------|
| **1 – Getesteter Kotlin-Domain-Kern** | Spiellogik aus dem Java-Singleton in reine Kotlin-Engine + immutable State herauslösen; volle JUnit-Abdeckung; Activities delegieren an die Engine. | App verhält sich **identisch**, Regeln sind durch Tests fixiert. |
| **2 – Architektur-Gerüst in Compose** | Single Activity, Compose-Nav-Skelett, M3-Theme, `GameViewModel`; **ein** Screen (Setup/Main) end-to-end nach Compose. | Der neue Stack ist bewiesen und lauffähig. |
| **3 – Restliche Screens nach Compose** | PlayerNames, DealCards, DeclareTricks (+ `TrickSpinner`→Composable), ConfirmTicks, **ResultScreen als datengetriebene Tabelle** (killt das Boilerplate). Launcher auf neuen Flow umstellen. | Komplette Compose-UI. |
| **4 – Altlasten entfernen & Politur** | Alte Activities/XML/ungenutzte Dependencies/`viewBinding`-Flag löschen; Lokalisierung abschließen; optionale Compose-UI-Tests. | Schlanker, moderner Stand. |

## 6. Phase 1 im Detail – Getesteter Kotlin-Domain-Kern

**Ziel:** ein reiner, immutabler, vollständig getesteter Kotlin-Spielkern, der das **heutige Verhalten exakt** reproduziert, während die bestehenden Java-Activities an ihn delegieren (App bleibt lauffähig, **keine sichtbare Änderung**).

### 6.1 Komponenten
- **`GameState`** (data class) wie in 3.1.
- **`SpadesEngine`** mit reinen Übergängen:
  - `startGame(playerCount, names, randomDealer, rng)` → initialer State.
  - `setTickPredictions(state, predictions)` → State mit Vorhersagen.
  - `confirmTricks(state, made: List<Boolean>)` → State mit aktualisierten Scores, Runde/Spieler weitergeschaltet, ggf. `showResultScreen`.
  - `startSecondHalf(state)` → `amountOfRounds` verdoppelt, `secondHalf = true`.
  - abgeleitet: `amountOfCards(state)`, `rankingForLastRound(state)` u. a.
- **RNG injizierbar/seedbar** (statt `Math.random()`), damit der Zufalls-Dealer deterministisch testbar ist.

### 6.2 Exakt zu erhaltende Regeln (Tests pinnen sie fest)
- Runden/Halbzeit = `floor(32 / playerCount)` (8 bei 4 Spielern, 10 bei 3); `startSecondHalf` verdoppelt das.
- Karten pro Runde: 1. Halbzeit = `currentRound`; 2. Halbzeit = `max(1, amountOfRounds - currentRound + 1)`.
- Scoring: Vorhersage getroffen → `vorher + prediction + 5`; verfehlt → vorheriger Stand erneut angehängt (jede Runde hat einen Eintrag).
- 3-Spieler: der 4. Eintrag wird ignoriert – sauber neu ausgedrückt als „nur die ersten `playerCount` Spieler".
- Dealer-Rotation `(currentPlayer + 1) % playerCount` je Runde; Zufalls-Dealer wählt zufälligen Start.
- `showResultScreen`, sobald `currentRound > amountOfRounds`.

### 6.3 Integration
Der bestehende `SpadesGame`-Singleton wird zum **dünnen Adapter**, der einen `GameState` hält und an die Engine delegiert (gleiche Getter/Setter-Signaturen → Activities bleiben unangetastet, keine UI-Änderung in Phase 1).

### 6.4 Tests (JUnit, JVM)
- Runden/Halbzeit für 3p/4p; Verdopplung in der 2. Halbzeit.
- Kartenzahl steigt (1. Halbzeit) / fällt (2. Halbzeit), Untergrenze 1.
- Scoring getroffen/verfehlt; kumulierte Summen; jede Runde erzeugt einen Eintrag.
- 3p vs. 4p (4. Spieler ignoriert).
- Dealer-Rotation & seedbarer Zufalls-Dealer.
- Result-Ranking-Reihenfolge & Gleichstand (Gold/Silber/Bronze-Logik).
- Vollspiel-Simulation (3p und 4p) bis zum Result-Screen.

### 6.5 Definition of Done (Phase 1)
- Reine Kotlin-Engine + `GameState`, **keine** Android-Imports.
- Mindestens die obigen Testfälle grün; Verhalten deckt sich mit der aktuellen App (manuell stichprobenartig geprüft).
- App baut & läuft identisch (Activities delegieren an die Engine).
- Für den Nutzer **keine** sichtbare Verhaltensänderung.

## 7. Teststrategie (gesamt)
- **Domain:** umfassende JVM-Unit-Tests – das zentrale Sicherheitsnetz.
- **ViewModel:** gezielte Unit-Tests (Test-Dispatcher) wo sinnvoll.
- **UI:** optionaler Compose-UI-Smoke-Test des Happy-Path (ein volles 3p-Spiel) in Phase 4.

## 8. Risiken & offene Punkte
- **Verhaltensparität:** Phase-1-Tests pinnen die heutigen Regeln, *bevor* die UI angefasst wird. Für bestehende Eigenheiten (Result-Färbung via `currentRound-2`, 3p-`length-1`-Ignore) gilt per Default „Verhalten erhalten"; jede bewusste Korrektur wird in Phase 1 notiert.
- **ResultScreen-Kapazität:** aktuelles XML unterstützt max. 20 Runden (3p verdoppelt = 20, 4p verdoppelt = 16); die datengetriebene Compose-Tabelle hebt diese Grenze natürlich auf.
- **Lokalisierungs-Mechanismus:** AppCompat-Per-App-Locales vs. config-basiert – Entscheidung in Phase 3.
- **Compose-Navigation-API:** String-Routen vs. type-safe Routen – Entscheidung in Phase 2.

## 9. Nicht Teil dieses Specs
Detail-Designs der Phasen 2–4 (jeweils eigene Specs). Dieses Dokument fixiert Zielarchitektur, Zerlegung und die umsetzbare Phase 1.
