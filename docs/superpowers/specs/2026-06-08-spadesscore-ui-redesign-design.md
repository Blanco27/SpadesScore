# SpadesScore — UI-Redesign (Design-Spec)

- **Datum:** 2026-06-08
- **Status:** Genehmigt (Brainstorming abgeschlossen)
- **Quelle der Wahrheit (visuell):** `docs/mockups/spadesscore-ui-redesign.html`
- **Launcher-Icon-Entwürfe:** `docs/mockups/launcher-icon-drafts.html`

---

## 1. Ziel & harte Anforderung

Die App wird optisch komplett überarbeitet, sodass sie **exakt 1:1 wie das Mockup** `docs/mockups/spadesscore-ui-redesign.html` aussieht. Das Mockup ist die **verbindliche visuelle Vorlage** — Paletten, Typografie, Komponenten, Layouts, Abstände und die neue Settings-Struktur werden präzise übernommen.

**„1:1" heißt:** visuelle Treue, **nativ** umgesetzt mit XML-Layouts + Material Components (kein Compose, kein pixelgenauer HTML-Klon). Wo das Mockup eine HTML-Eigenheit hat (z. B. CSS-`box-shadow`), wird das native Android-Äquivalent (Elevation/Shadow-Drawable) so gewählt, dass das Ergebnis dem Mockup entspricht.

### Was unverändert bleibt (geringes Risiko)
- **Architektur:** MVVM, Room, `GameRepository`, `GameEngine`, `AppContainer`, ViewModels, Activity-Flow.
- **Spiel-Logik:** keine Änderung an Regeln, Scoring, Rundenanzahl, Persistenz, Process-Death-Verhalten.
- **Basis:** 100 % XML-Layouts + Material Components. Kein Compose, **keine** Material3-Migration.

### Was sich ändert
- **Theme:** `colors.xml` + `values-night/colors.xml`, `themes.xml`, neues `attrs.xml`.
- **Typografie:** Space Grotesk (neu) + Inter; zentrale `TextAppearance.Spades.*`-Styles. Patrick Hand / Comic Neue / Roboto raus.
- **Drawables:** neue Shape-Drawables (Card, Stepper, Chip, Segment, CTA, Check, Radio, Grid, Gear, Progress, Rang-Badge) und neue **Vektor-Icons**.
- **Layouts:** alle sechs `activity_*`-Layouts neu aufgebaut + **neu** `activity_settings.xml`.
- **Neuer Screen:** `SettingsActivity` + `SettingsViewModel` + `SettingsUiState`.
- **Theme-Schalter** (System/Hell/Dunkel) inkl. Persistenz.
- **Neues Launcher-Icon** (Variante **B · Paper Indigo**).

### Erfolgsmaßstab
1. Jeder der 7 Screens deckt sich in **Light & Dark** (und **EN & DE**) sichtbar mit dem Mockup.
2. Alle Domain-Unit-Tests bleiben grün (Beleg: rein visuelle Änderung).
3. `assembleDebug` + `lint` laufen sauber durch.
4. Theme-Wahl und Sprachwahl überleben App-Neustart; Room-Process-Death-Persistenz weiterhin intakt.

---

## 2. Umsetzungs-Strategie

**Ansatz A — Design-System zuerst, dann Screens reskinnen.** Erst die zentralen Tokens, Attribute, Fonts, Styles und Drawables bauen; danach jeden Screen rein über `?attr/…` und gemeinsame Styles neu aufbauen. Dadurch: eine Akzentlogik (ein Akzent pro Modus), Dark/Light „kostenlos" über den `-night/`-Qualifier, minimales Risiko.

---

## 3. Design-System

### 3.1 Farb-Token (beide Paletten)

Konkrete Hex-Werte als **semantische Farb-Ressourcen** in `res/values/colors.xml` (Paper Light) und `res/values-night/colors.xml` (Casino Noir). Layouts referenzieren ausschließlich **Theme-Attribute** (`res/values/attrs.xml`), die in `themes.xml` auf diese Farben gemappt werden. Da die Farb-Ressourcen über den `-night/`-Qualifier variieren, genügt **eine** `themes.xml` (Attribut → `@color/...`).

| Theme-Attribut | Farb-Ressource | Light (Paper) | Dark (Noir) | Verwendung |
|---|---|---|---|---|
| `appBg` | `bg` | `#F6F6F4` | `#0E0E10` | Screen-Hintergrund |
| `appSurface` | `surface` | `#FFFFFF` | `#1A1A1E` | Karten, Felder, Grid |
| `appInk` | `ink` | `#17171A` | `#F4F4F5` | Haupttext, Titel |
| `appMuted` | `muted` | `#6B6B72` | `#8A8A93` | Sekundärtext, Labels |
| `appAccent` | `accent` | `#4F46E5` | `#E4B95B` | Werte, Progress, aktiv |
| `appLine` | `line` | `#E7E7E3` | `#2A2A30` | Haarlinie Karten |
| `appTrack` | `track` | `#E7E7E3` | `#26262C` | Progress-Track, Switch-off |
| `appStepBg` | `step_bg` | `#F1F1EF` | `#222228` | Stepper-Bg, Segment-Track, CTA-disabled, Rang-Badge |
| `appChipBg` | `chip_bg` | `#EEEDFB` | `#211E16` | „x/y Stiche"-Chip |
| `appCtaBg` | `cta_bg` | `#17171A` | `#E4B95B` | Primär-Button-Hintergrund |
| `appCtaFg` | `cta_fg` | `#FFFFFF` | `#15130C` | Primär-Button-Text |
| `appOnAccent` | `on_accent` | `#FFFFFF` | `#15130C` | Text/Icon auf Akzent (Check ✓) |
| `appSpade` | `spade` | `#17171A` | `#E4B95B` | Marken-♠ in der App |
| `appRing` | `ring` | `#244F46E5` | `#29E4B95B` | Fokus-Ring, Rang-1-Badge-Bg |
| `appHitBg` | `hit_bg` | `#0F4F46E5` | `#17E4B95B` | Treffer-Zeile (Bestätigen) |
| `appWarn` | `warn` | `#C0392B` | `#E8736F` | Sperr-Warnung Text |
| `appWarnBg` | `warn_bg` | `#1AC0392B` | `#1FE8736F` | Warn-Chip-Hintergrund |

> Alpha-Werte sind als `#AARRGGBB` notiert (Android-Format): `ring` = Akzent @14 %/16 %, `hit_bg` = Akzent @6 %/9 %, `warn_bg` = Warn @10 %/12 %.

**Wichtige Eigenheit (bewusst übernommen):** Der Primär-CTA ist im **Hellen Ink-schwarz** (`#17171A`, weißer Text), im **Dunklen Gold** (`#E4B95B`, dunkler Text) — er ist also im Light-Modus *nicht* gleich dem Akzent. Genau wie im Mockup.

**Status-Bar:** je Modus an `appBg` angeglichen (heller Status-Bar-Content im Dark-, dunkler im Light-Modus über `windowLightStatusBar`).

### 3.2 Typografie

- **Space Grotesk** (neu) als Downloadable Font, Gewichte 400/500/600/700.
- **Inter** (vorhanden) auf 400/500/600/700 sicherstellen.
- Pro Gewicht eine Font-Ressource (Muster wie bestehende `comic_neue`/`comic_neue_light`): z. B. `space_grotesk`, `space_grotesk_medium`, `space_grotesk_semibold`, `space_grotesk_bold`.

Zentrale `TextAppearance.Spades.*`-Styles (Werte aus dem Mockup):

| Style | Font | Größe / Eigenschaft | Verwendung |
|---|---|---|---|
| `Title` | Grotesk 700 | 40sp, letterSpacing −.02em | „Runde X" |
| `Headline` | Grotesk 700 | 30sp | „Endstand" |
| `ScreenTitle` | Grotesk 700 | 23–26sp | „Spielernamen", „Einstellungen" |
| `Wordmark` | Grotesk 700 | 27sp, letterSpacing +.04em | „SPADES" Start |
| `Number` | Grotesk 700 | variabel | Scores, Stepper-Wert, Kartenzahl, Endsumme |
| `PlayerName` | Grotesk 500 | 15sp | Spielernamen in Zeilen |
| `Label` | Inter 700 | 10.5sp, uppercase, letterSpacing +.08em | „ANZAHL SPIELER", Feld-Labels, Sektions-Labels |
| `Body` / `Meta` | Inter 500 | 12.5–13.5sp | Subtitles, Meta „3 Stiche · +8 Punkte" |

Keine hartcodierten Schriftgrößen/-farben mehr direkt in Layouts, wo ein Style passt.

### 3.3 Komponenten-Styles & Shape-Drawables

Ersetzen die Alt-Drawables (`button_background.xml`, `option_button.xml`, `selector_option.xml`, `player_bg_rounded.xml`, `spinner_background.xml`, `spinner_button_background.xml`, `button_background_selector.xml`):

| Drawable | Beschreibung |
|---|---|
| `bg_card` | `surface`, 1dp `line`-Stroke, 16dp Radius, dezente Elevation/Schatten |
| `bg_field` / `bg_field_focus` | Eingabefeld; Fokus = `accent`-Rand + `ring`-Glow (3dp) |
| `bg_chip` / `bg_chip_warn` | Pille; Normal `chip_bg`/`accent`-Text, Warn `warn_bg`/`warn`-Text |
| `bg_step` | Stepper-Hintergrund (`step_bg`, 12dp Radius) |
| `bg_segment` / `bg_segment_selected` | Segment-Track (`step_bg`) + ausgewähltes Feld (`surface` + Schatten, `accent`-Text) |
| `bg_cta` / `bg_cta_disabled` | Primär-Button (`cta_bg`/`cta_fg`) + gesperrt (`step_bg`/`muted`) |
| `bg_check` / `bg_check_on` | Runde Checkbox 27dp, 8dp Radius; an = `accent`-Fill + ✓ in `on_accent` |
| `bg_radio` / `bg_radio_on` | Radio 21dp; an = `accent`-Rand + `accent`-Punkt |
| `bg_grid` | Ergebnis-Grid-Karte (`surface`, `line`, 18dp Radius) |
| `bg_gear` | Gear-Button auf Start (38dp Kreis, `surface` + `line` + Schatten) |
| `bg_rank_badge` / `bg_rank_badge_lead` | Rang-Pille (`step_bg`/`muted`) + Platz 1 (`ring`/`accent`) |
| `progress_track` / `progress_fill` | Progress-Bar (Track `track`, Fill `accent`, 6dp, 99dp Radius) |

Material-Switch (Zufalls-Dealer) und der `TrickSpinner` werden auf die Tokens umgestylt (Switch: Track `accent`/`track`, Thumb weiß; Spinner: `step_bg`-Bg, `accent`-Wert, `muted`-±).

### 3.4 Vektor-Assets (VectorDrawable)

Ersetzen `spades_logo.png` und `ace.png`. Alle einfarbig über `?attr/…` tönbar (`android:tint`).

| Asset | Verwendung | Hinweis |
|---|---|---|
| `ic_spade` | Marken-♠ (Start-Hero, Brand-Zeile, Settings-Footer) | Pfad siehe unten |
| `ic_gear` | ⚙ Start → Settings | Lucide-Stil (Mockup-SVG) |
| `ic_back` | Zurück-Pfeil Settings-AppBar | |
| `ic_crown` | Rang-1-Badge im Ergebnis | Pfad `M3 7l4.5 4L12 4l4.5 7L21 7v11H3z` |
| `ic_theme_system` / `ic_sun` / `ic_moon` | Theme-Segment-Icons | aus Mockup-SVGs |
| `ic_github` | About-Link | GitHub-Mark |
| `ic_card_fan` | 3-Karten-Fächer (Austeilen) | dekorativ; reproduziert das Mockup-SVG (Q♣ / K♥ / A♠), Karten in `surface`/`line`, Symbole Ink/`warn` |

**`ic_spade`-Pfad** (viewBox `0 0 24 24`), identisch zu den Icon-Entwürfen:

```
M12 3C12 3 4 9 4 14c0 2.8 2.2 4.6 4.6 4.6 1.2 0 2.3-.5 3-1.4-.2 2-.9 3.6-2.6 4.8h6c-1.7-1.2-2.4-2.8-2.6-4.8.7.9 1.8 1.4 3 1.4C17.8 18.6 20 16.8 20 14 20 9 12 3 12 3Z
```

---

## 4. Screens (1:1 zum Mockup)

Logik der Activities/ViewModels bleibt; nur die View-Verdrahtung (IDs, Bindings) passt sich dem neuen Layout an. 3-/4-Spieler-Pfad jeweils prüfen.

### 4.1 Start (`MainActivity` / `activity_main.xml`)
- **⚙-Gear oben rechts** (`bg_gear` + `ic_gear`) → startet `SettingsActivity`.
- Zentrierter Hero: großes `ic_spade` (`appSpade`), Wortmarke **„SPADES"** (`Wordmark`), Tagline (`Body`, `muted`).
- Label **„ANZAHL SPIELER"** + **Segment-Control [3 Spieler | 4 Spieler]** (`bg_segment`); ausgewählt = `bg_segment_selected`.
- Primär-CTA **„Spiel starten"**.
- **Entfällt hier:** die EN/DE-Sprachbuttons. Die Sprach-Logik (`setLocale()` + `recreate()`) wandert nach `SettingsActivity`. `applyPersistedLocale()` bleibt.

### 4.2 Spielernamen (`PlayerNamesActivity` / `activity_player_names.xml`)
- Screen-Titel „Spielernamen" (`ScreenTitle`, links).
- 3–4 **Feld-Karten** (`bg_field`): Label „SPIELER N" (`Label`) + `EditText` (Grotesk 500, 16sp). Fokus → `bg_field_focus`.
- **Switch-Zeile** „Zufälliger erster Dealer" (Material-Switch auf Token).
- Primär-CTA „Spiel starten".
- 3-Spieler-Modus blendet Feld 4 aus.

### 4.3 Karten austeilen (`DealCardsActivity` / `activity_deal_cards.xml`)
- **Brand-Zeile:** `♠ Spades` links, „Runde X / Y" rechts.
- **Score-Zeile:** 3–4 Spalten Name + Score (`Number`).
- Titel „Runde X" (Akzent-Ziffer via `Title em`-Äquivalent), **Progress-Bar** (Anteil `currentRound/amountOfRounds`).
- Subtitle „… teilt aus".
- `ic_card_fan` + Zähler **„5 Karten pro Spieler"** (große `Number`-Zahl in `accent`).
- Primär-CTA „Weiter".

### 4.4 Stiche ansagen (`DeclareTricksActivity` / `activity_declare_tricks.xml`)
- Brand-/Score-Zeile, Titel, Progress, Subtitle „Stiche ansagen".
- Pro Spieler eine **Karten-Zeile** (`bg_card`): Name links, **Stepper** (`TrickSpinner`, restyled) rechts.
- **Chip „X / Y Stiche"** (`bg_chip`).
- **Sperr-Zustand** (bestehende Logik: Summe der Vorhersagen == verfügbare Stiche):
  - Chip → `bg_chip_warn`, Wert in `warn`.
  - Warn-Subtitle „Summe muss von Y abweichen" (`warn`).
  - Primär-CTA → `bg_cta_disabled`, deaktiviert.
  - Die bestehende Shake-/Animations-Transition bleibt.
- Primär-CTA „Stiche bestätigen".

### 4.5 Stiche bestätigen (`ConfirmTicksActivity` / `activity_confirm_tricks.xml`)
- Brand-/Score-Zeile, Titel, Progress.
- Pro Spieler: Name + Meta **„X Stiche · +N Punkte"** + **runde Checkbox** (`bg_check`/`bg_check_on`).
- **Treffer-Zeile:** Hintergrund `appHitBg`, Punkte in `accent`.
- Primär-CTA „Nächste Runde".

### 4.6 Ergebnis (`ResultScreenActivity` / `activity_result_screen.xml`)
- Kopf: „Endstand" (`Headline`) + „N Runden gespielt" (`Body`).
- **Grid-Karte** (`bg_grid`): Header-Zeile (Namen), Runden-Zeilen (kumulierte Scores), **Summen-Zeile** mit großen `Number`-Zahlen + **Rang-Badges**. Platz 1: Zahl in `accent` + `ic_crown`-Badge (`bg_rank_badge_lead`); übrige `bg_rank_badge`.
- Primär-CTA „Weiter".
- **Wichtig:** die hardcodierte `MAX_ROUNDS = 20`-Logik und die `resources.getIdentifier(...)`-Verdrahtung (`score_playerN_roundM`, `roundNScores`, `player4_spaceN`) **bleibt**. Es werden nur die Zellen umgestylt und die Rang-Badge-Views ergänzt. Activity + `activity_result_screen.xml` weiterhin im Gleichschritt ändern.

### 4.7 Einstellungen (NEU)

**Dateien:** `SettingsActivity`, `activity_settings.xml`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsUiState.kt`.

- **AppBar:** Zurück-Pfeil (`ic_back`) + „Einstellungen" (`ScreenTitle`).
- **Sektion „Darstellung":** Segment-Control **[System | Hell | Dunkel]** mit Icons (`ic_theme_system`/`ic_sun`/`ic_moon`). Auswahl → `AppCompatDelegate.setDefaultNightMode(...)` + persistiert (siehe §5).
- **Sektion „Sprache":** Panel (`bg_card`) mit zwei Radio-Zeilen — „EN · English" / „DE · Deutsch" (`bg_radio`/`bg_radio_on`). Auswahl → `Language` in Room setzen (bestehender Mutator) + `recreate()`.
- **Quiet-Footer:** `ic_spade`-Mark, „SpadesScore" + Version aus **`BuildConfig.VERSION_NAME`** (1.1.0), GitHub-Link (`ic_github`) → `Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Blanco27/SpadesScore"))`, Zeile „Open Source · mit ❤ gebaut".

**ViewModel:** `SettingsViewModel(gameRepository, themePreferences)` baut `SettingsUiState(language, themeMode)` aus `repository.state.value` + `themePreferences`; leitet Aktionen weiter (`setLanguage`, `setThemeMode`).

**Navigations-Ausnahme:** Die App-Regel „Back deaktiviert" gilt dem Vorwärts-Spielfluss. `SettingsActivity` ist ein Seiten-Abstecher vom Start → **Back ist hier erlaubt** (Zurück-Pfeil und Hardware-Back rufen `finish()` → zurück zum Start). **Umsetzung (festgelegt):** `SettingsActivity` erbt — analog zu `ResultScreenActivity` — **direkt von `AppCompatActivity`** (nicht von `SpadesAppCompatActivity`), ruft als Erstes `applyPersistedLocale()` auf und installiert **bewusst keinen** Back-Press-Disabler. Dokumentierte Ausnahme wie `ResultScreenActivity`.

---

## 5. Theme-Persistenz

- **`ThemeMode`**-Enum: `SYSTEM`, `LIGHT`, `DARK` (Mapping → `AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM` / `_NO` / `_YES`).
- **`ThemePreferences`** (neue Klasse, **SharedPreferences**) — speichert/liest die Wahl. Wird über `AppContainer` bereitgestellt (manuelle DI, kein Hilt).
- **`SpadesApplication.onCreate()`** liest die Wahl **synchron** und ruft **vor** dem Start jeder Activity `AppCompatDelegate.setDefaultNightMode(...)` auf → gilt global app-weit, überlebt Process Death, greift auf allen Screens.
- **Bewusst nicht in Room:** Theme ist eine globale UI-Präferenz, kein Spielzustand. **Sprache bleibt in Room** (bestehendes `Language`-Feld in `GameState`) — unverändert.
- Light/Dark-Ressourcen kommen weiterhin über `values/` + `values-night/`; der manuelle Schalter erzwingt nur, **welcher** Qualifier gilt (oder „System").

---

## 6. Launcher-Icon — Variante B · Paper Indigo

**Gewählt:** helle „Paper"-Kachel mit Indigo-♠ (passend zur Light-Palette / Default-Identität). Entwurf gesichert in `docs/mockups/launcher-icon-drafts.html` (Varianten A/C/D dort dokumentiert, falls später gewechselt wird).

**Adaptive Icon** (`mipmap-anydpi-v26/ic_launcher.xml` + `ic_launcher_round.xml`):
- **Background-Layer** (`ic_launcher_background`): Paper-Fläche `#F6F6F4` (optional dezenter Verlauf `#FFFFFF → #ECECEA`). Ersetzt das bestehende `values/ic_launcher_background.xml`.
- **Foreground-Layer** (`ic_launcher_foreground`): Indigo-♠ (`#4F46E5`), `ic_spade`-Pfad, zentriert in der Safe-Zone (Glyph ca. 50–55 % der 108dp-Fläche).
- **Legacy/Fallback** (`mipmap-*/ic_launcher.png`, `ic_launcher_round.png`): aus den beiden Layern gerendert (vorab erzeugte PNGs in den Dichten).
- **Optional (Android 13+ themed icon):** Monochrom-Layer (`<monochrome>` = `ic_spade`) für getönte Themed Icons. Kann in einem Folge-Schritt ergänzt werden.

> Hinweis: Die Haarlinie aus der Vorschau ist dekorativ; beim adaptiven Icon übernimmt die Launcher-Maske den Rand. Auf rein weißen Homescreens wirkt das Icon bewusst ruhig (Paper-Light-Identität) — der Indigo-♠ trägt den Kontrast.

---

## 7. Lokalisierung

Konvention bleibt: **`values/` = Englisch**, **`values-de/` = Deutsch** (die Mockup-Texte sind die DE-Variante). EN/DE bleibt vollständig; **keine** hartcodierten Strings in Layouts.

**Neue String-Keys** (jeweils EN / DE):

| Key | EN | DE |
|---|---|---|
| `tagline` | Call your tricks. Keep the score. | Stiche ansagen. Punkte behalten. |
| `settings_title` | Settings | Einstellungen |
| `settings_appearance` | Appearance | Darstellung |
| `theme_system` | System | System |
| `theme_light` | Light | Hell |
| `theme_dark` | Dark | Dunkel |
| `cards_per_player` | Cards per player | Karten pro Spieler |
| `declare_tricks` | Call tricks | Stiche ansagen |
| `deal_subtitle` | %s deals | %s teilt aus |
| `tricks_sum_warning` | Sum must differ from %d | Summe muss von %d abweichen |
| `round_progress` | Round %1$d / %2$d | Runde %1$d / %2$d |
| `final_score` | Final score | Endstand |
| `rounds_played` | %d rounds played | %d Runden gespielt |
| `source_on_github` | Source on GitHub | Quellcode auf GitHub |
| `open_source_built_with_love` | Open source · built with ❤ | Open Source · mit ❤ gebaut |
| `next_round` | Next round | Nächste Runde |
| `continue_label` | Continue | Weiter |

> Vorhandene Keys werden wiederverwendet, wo passend: `round`, `tricks`/`trick`, `points_added`, `combined_trick_prediction` (Chip „%d/%d %s"), `random_first_dealer`, `english`, `deutsch`, `language`, `next` („Spiel starten"/„Start Game"). Beim Umbau String-Audit machen und ungenutzte Keys entfernen.

---

## 8. Testing & Verifikation

1. **Domain-Unit-Tests** (`GameEngineTest`, `GameStateTest`, `NameValidationTest`) bleiben unverändert grün — Regressions-Wächter, dass keine Logik geändert wurde.
2. **Build-Gate:** `./gradlew assembleDebug` + `./gradlew lint` ohne Fehler.
3. **1:1-Fidelity-Gate (Hauptmaßstab):** alle 7 Screens in **Light & Dark** und **EN & DE** rendern und direkt gegen das Mockup vergleichen (Skills `run` / `update-readme-screenshots` für Screenshots). Pro Screen: Palette, Typo, Komponenten, Abstände, CTA-Farbe (Ink hell / Gold dunkel), Sperr-/Treffer-/Rang-Zustände prüfen.
4. **Verhalten neu prüfen:** Theme-Wahl überlebt Neustart; Sprachwechsel aus Settings persistiert + `recreate()`; Process-Death-Persistenz (Room) weiterhin intakt (Spiel mitten im Lauf killen, Deep-Activity in gewählter Sprache + Theme wiederherstellen).
5. **Optional:** kleiner Unit-Test für `ThemePreferences` (Default = SYSTEM, read/write).

---

## 9. Scope-Grenzen (bewusst draußen)

- **Kein Compose, keine Material3-Migration, keine Spiel-Logik-Änderung.**
- Keine Änderung an Rundenzahl/Scoring/Flow.
- Keine neuen Sprachen über EN/DE hinaus.
- Themed-Icon-Monochrom-Layer ist optional (Folge-Schritt).

---

## 10. Konkrete Änderungs-/Datei-Liste

**Theme & Ressourcen**
- `res/values/colors.xml` — Paper-Light-Token (semantische Namen), Alt-Farben entfernen, soweit ungenutzt.
- `res/values-night/colors.xml` — Casino-Noir-Token.
- `res/values/attrs.xml` — **neu**, deklariert `appBg`, `appSurface`, `appInk`, `appMuted`, `appAccent`, `appLine`, `appTrack`, `appStepBg`, `appChipBg`, `appCtaBg`, `appCtaFg`, `appOnAccent`, `appSpade`, `appRing`, `appHitBg`, `appWarn`, `appWarnBg`.
- `res/values/themes.xml` (+ ggf. `values-night/themes.xml` nur für nicht-farbliche Attribute) — Attribut→Farbe-Mapping, Status-Bar, `TextAppearance.Spades.*`-Styles, Komponenten-Styles.
- `res/font/space_grotesk*.xml` — **neu**; `inter.xml` Gewichte sicherstellen.

**Drawables**
- **Neu:** `bg_card`, `bg_field`, `bg_field_focus`, `bg_chip`, `bg_chip_warn`, `bg_step`, `bg_segment`, `bg_segment_selected`, `bg_cta`, `bg_cta_disabled`, `bg_check`, `bg_check_on`, `bg_radio`, `bg_radio_on`, `bg_grid`, `bg_gear`, `bg_rank_badge`, `bg_rank_badge_lead`, `progress_track`, `progress_fill`.
- **Neu (Vektor):** `ic_spade`, `ic_gear`, `ic_back`, `ic_crown`, `ic_theme_system`, `ic_sun`, `ic_moon`, `ic_github`, `ic_card_fan`.
- **Entfernen (nach Umbau, wenn unreferenziert):** `spades_logo.png`, `ace.png`, `button_background.xml`, `option_button.xml`, `selector_option.xml`, `player_bg_rounded.xml`, `spinner_background.xml`, `spinner_button_background.xml`, `button_background_selector.xml`, Alt-Fonts (`patrick_hand*`, `comic_neue*`, `roboto`).

**Launcher-Icon**
- `res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `res/drawable/ic_launcher_foreground.xml`, `res/values/ic_launcher_background.xml` (→ Paper), Legacy-PNGs in `mipmap-*`.

**Layouts**
- Neu aufgebaut: `activity_main.xml`, `activity_player_names.xml`, `activity_deal_cards.xml`, `activity_declare_tricks.xml`, `activity_confirm_tricks.xml`, `activity_result_screen.xml`, `view_trick_spinner.xml`.
- **Neu:** `activity_settings.xml`.

**Kotlin**
- **Neu:** `SettingsActivity.kt`, `ui/settings/SettingsViewModel.kt`, `ui/settings/SettingsUiState.kt`, `ThemePreferences` (+ `ThemeMode`).
- **Geändert:** `MainActivity.kt` (Gear→Settings, Sprachbuttons raus), `SpadesApplication.kt` (`setDefaultNightMode` beim Start), `di/AppContainer.kt` (ThemePreferences verdrahten), betroffene Activities/Bindings an neue View-IDs anpassen.

**Strings**
- `res/values/strings.xml` + `res/values-de/strings.xml` — neue Keys (§7), Audit, Tagline.

---

## 11. Offene Punkte für die Implementierungs-Planung
- Reihenfolge/Phasierung (Design-System → Screen-für-Screen) wird in der `writing-plans`-Phase festgelegt.
- Exakte Safe-Zone-Skalierung des Launcher-♠ am Gerät verifizieren.
