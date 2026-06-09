# Spec: Confirm-Ticks — mindestens ein Spieler muss daneben liegen

**Datum:** 2026-06-09
**Screen:** `ConfirmTicksActivity` (`activity_confirm_tricks.xml`)
**Status:** Genehmigt zur Umsetzung

## Problem

Auf dem Confirm-Screen kann der Nutzer aktuell **alle** Spieler als Treffer abhaken
und „Nächste Runde" drücken. Das widerspricht der Spielregel.

`DeclareTricksActivity` sperrt die Ansage bereits, solange die Summe der Vorhersagen
exakt der verfügbaren Stichzahl entspricht (`combined != possible`,
`R.string.tricks_sum_warning`). Weil die tatsächlich gewonnenen Stiche immer in
Summe der verfügbaren Stichzahl entsprechen, die Vorhersagen aber nie, ist es
**mathematisch unmöglich, dass alle Spieler ihre Ansage treffen**. Der
Confirm-Screen muss diesen unmöglichen Zustand verhindern.

## Regel

Mindestens ein sichtbarer Spieler muss als **Miss** (nicht abgehakt) bleiben.
Anders gesagt: die „Nächste Runde"-Aktion ist gesperrt, **genau dann wenn alle
sichtbaren Spieler abgehakt sind**.

- Sichtbare Spieler = 3 oder 4 (Reihe 4 ist im 3-Spieler-Modus `GONE` und zählt nicht).
- Alle anderen Kombinationen (inkl. „alle Miss") sind gültig.
- Startzustand: alle unmarkiert ⇒ gültig ⇒ CTA aktiv.

## Verhalten (UI)

Das Lock-Pattern von `DeclareTricksActivity` wird 1:1 gespiegelt, damit beide
Sperr-Screens sich identisch anfühlen:

| Zustand | CTA | Text | Warnzeile | Übergang |
|---|---|---|---|---|
| gültig (mind. ein Miss) | aktiv, `appCtaBg` | `appCtaFg` | versteckt (`GONE`) | beim Entsperren: Farb-Rückanimation |
| gesperrt (alle Treffer) | deaktiviert, `appStepBg` | `appMuted` | sichtbar, `appWarn` | beim Sperren: Farbanimation + Shake |

- Neue Warn-Zeile (`warn_subtitle`) wird im Layout über dem CTA ergänzt,
  standardmäßig `visibility="gone"`, zentriert, Textfarbe `?attr/appWarn`,
  Textstil analog `declare`-Warnung.
- Die Sperrprüfung läuft nach jedem `toggleHit(...)` **und** einmal nach dem
  initialen Render.

## Lokalisierung

Neuer String, in beiden Sprachdateien:

- `values/strings.xml`: `confirm_all_hit_warning` = „At least one player must miss"
- `values-de/strings.xml`: `confirm_all_hit_warning` = „Mindestens einer muss daneben liegen"

## Umsetzungsschritte (Touch-Points)

1. **`ui/animations.kt` (neu):** `animateFill(view, fromColor, toColor)` und
   `View.shake()` aus `DeclareTricksActivity` extrahieren (gemeinsamer Helfer,
   da nun zwei Konsumenten). `DeclareTricksActivity` auf die extrahierten
   Funktionen umstellen (Verhalten unverändert).
2. **`activity_confirm_tricks.xml`:** `warn_subtitle`-`TextView` (gone) zwischen
   dem Flex-`Space` und dem `start_Button` einfügen.
3. **`strings.xml` + `values-de/strings.xml`:** `confirm_all_hit_warning` ergänzen.
4. **`ConfirmTicksActivity.kt`:**
   - Lock-Farben in `initializeUIComponents()` auflösen
     (`appCtaBg`, `appStepBg`, `appCtaFg`, `appMuted`, `appWarn`).
   - `warn_subtitle` referenzieren; CTA-Anfangszustand in `setupUI()` setzen.
   - `updateConfirmLockState()` ergänzen (Logik analog
     `updateCombinedTricksTextView`): Lock = „alle sichtbaren Spieler Treffer".
     Treiber für CTA-`isEnabled`, CTA-Farbe (animiert), CTA-Textfarbe,
     Warnzeile-Sichtbarkeit, Shake beim Übergang; `lastStateValid` merken.
   - Aufruf in `toggleHit(...)` und nach initialem `render(...)`.

## Nicht im Scope (YAGNI)

- Keine zusätzliche Domain-Validierung: Die Sperre ist — wie bei
  `DeclareTricks` — ein reiner UI-Guard inline in der Activity. `start_Button`
  verlässt sich ausschließlich auf `isEnabled`; ein zusätzlicher Guard in
  `startNextRound()` ist nicht nötig (Klick ist im gesperrten Zustand inaktiv).
- Keine Änderung an `GameEngine.confirmTricks` oder am Scoring.

## Verifikation

- Build + bestehende Unit-Tests grün (`testDebugUnitTest`).
- Manuell: 4-Spieler-Runde, alle abhaken ⇒ CTA gesperrt + Warnung + Shake;
  einen Haken lösen ⇒ CTA aktiv. Gleiches im 3-Spieler-Modus mit 3 Reihen.
