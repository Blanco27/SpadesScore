# UI-Feinabgleich an das Redesign-Mockup

**Datum:** 2026-06-08
**Quelle der Wahrheit:** `docs/mockups/spadesscore-ui-redesign.html`
**Ziel:** Die ausgelieferte App 1:1 näher an das Mockup bringen. Reine Theme-/Layout-Arbeit — keine Architektur-, Domain- oder Datenänderungen. Kein Compose.

## Kontext

Das Redesign wurde bereits umgesetzt (auf `master` gemergt). Beim Vergleich der laufenden App mit dem Mockup fallen noch sichtbare Abweichungen auf, die größtenteils auf zwei Ursachen zurückgehen:

1. **Fehlende Window-Insets-Behandlung.** Bei `targetSdk 36` erzwingt Android Edge-to-Edge. Es gibt nirgends Inset-Padding, daher zeichnet jeder Screen unter Status- und Navigationsleiste. Symptome: Zahnrad/Überschriften/Header „zu hoch" (überlappen Uhr/Akku), CTAs „zu weit unten" (unter der Gesten-Navigationsleiste).
2. **Kleinere Feinheiten** in Größen, Abständen und Ausrichtung gegenüber dem Mockup.

Alle Entscheidungen unten wurden mit dem Nutzer abgestimmt.

## Änderungen

### A. Window-Insets (zentral, betrifft alle Masken)

- In `SpadesAppCompatActivity` nach `initContentView()` einen `ViewCompat.setOnApplyWindowInsetsListener` auf den Activity-Content (`android.R.id.content`) setzen, der `WindowInsetsCompat.Type.systemBars()` als **Padding** auf den Content-Frame anwendet (top, bottom, left, right — letztere für Display-Cutouts/Landscape). Der Frame hat selbst kein Padding, daher bleibt das eigene Padding jedes Layouts erhalten und wird nicht doppelt gezählt.
- `ResultScreenActivity` erbt nicht von der Basisklasse → dort die **gleiche** Inset-Logik nach `setContentView(...)` einsetzen (am besten als kleine, gemeinsam genutzte Hilfsfunktion, z. B. `View.applySystemBarInsetsAsPadding()` in `ui/`).
- Die bisher handgetunten Top-Paddings (die die Statusleiste „umgehen" sollten) auf einheitliche Werte normalisieren, da der Frame jetzt die Bar-Höhe beisteuert:

  | Layout | paddingTop alt | paddingTop neu | Rest |
  |---|---|---|---|
  | `activity_main.xml` | 16dp | 8dp | horiz. 24dp, bottom 24dp |
  | `activity_player_names.xml` | 32dp | 16dp | horiz. 24dp, bottom 24dp |
  | `activity_deal_cards.xml` | 24dp (padding) | 16dp | horiz. 24dp, bottom 24dp |
  | `activity_confirm_tricks.xml` | 24dp (padding) | 16dp | horiz. 24dp, bottom 24dp |
  | `activity_declare_tricks.xml` | 64dp | 24dp | horiz. 24dp, bottom 24dp |
  | `activity_settings.xml` | 15dp | 8dp | horiz. 17dp, bottom 15dp |
  | `activity_result_screen.xml` | 40dp | 16dp | horiz. 20dp, bottom 28dp |

  *(Werte sind Richtwerte; Feinschliff beim Umsetzen am Gerät erlaubt.)*

### B. Start (`activity_main.xml`, `MainActivity.kt`, `bg_segment_selected.xml`)

- Pik-Logo `ImageView` **62dp → 80dp** (Breite & Höhe).
- Segment-Optionen (`btn3Players`/`btn4Players`): vertikales Padding **12dp → 15dp**.
- Aktive Auswahl-Pille klarer wie im Mockup: weiße/Surface-Fläche + Schatten. `bg_segment_selected` so anpassen, dass sie eine Surface-Füllung mit abgerundeten Ecken zeigt; Schatten über `android:elevation` (z. B. ~4dp) auf dem aktiven Button (in `renderPlayerCount(...)` setzen, beim inaktiven wieder entfernen).
- CTA „Spiel starten": Höhe **56dp → 60dp**, Textgröße **15sp → 16sp**.

### C. Spielernamen (`activity_player_names.xml`)

- Alle vier Feld-Karten: vertikales Padding **10dp → 14dp**, `layout_marginBottom` **10dp → 14dp**.
- Screen-Titel **24sp → 26sp** (inline `textSize`-Override am Titel-`TextView`; die geteilte Style-Klasse `TextAppearance.Spades.ScreenTitle` **nicht** ändern, da sie auch der Einstellungs-Titel nutzt, der im Mockup 23px hat).
- CTA: 60dp/16sp (siehe G).

### D. Geteilter Runden-Header (`include_round_header.xml`)

- **Punkte-Spalten zentrieren:** Name- und Wert-`TextView`s je Spalte zentriert (Spalten-`LinearLayout` `gravity="center_horizontal"` bzw. `TextView`s auf zentriert), statt linksbündig. Entspricht `.scorerow div{text-align:center}`.
- **Runden-Überschrift zentrieren:** `round_title` von `wrap_content` (linksbündig) auf `match_parent` + `gravity="center"`. Entspricht `.title{text-align:center}`. Die Accent-Tönung der Ziffer (Spannable in `RoundHeader.kt`) bleibt unverändert.

### E. Karten austeilen (`activity_deal_cards.xml`, `ic_card_fan.xml`)

- **Zahl/Label-Ausrichtung:** Am Label-`TextView` „Karten pro Spieler" das `layout_gravity="bottom"` **entfernen**, sodass die saubere Grundlinien-Ausrichtung (`baselineAligned="true"`) greift — große Zahl ragt oben heraus, beide unten bündig, exakt wie `.count{align-items:baseline}`.
- **D3 (leichtgewichtig):** Der „Runde X"-Titel bleibt im geteilten Header. Nur die Abstände im Austeilen-Screen feinjustieren, damit der Karten-Block etwas tiefer/mittiger sitzt und der obere Bereich nicht kopflastig wirkt (z. B. oberen Spacer leicht gewichten/Abstand erhöhen). Bewusst **keine** Entkopplung vom geteilten Header (geringes Risiko).
- **Karten-Icon:** Die drei „Rang-Balken" durch echte Glyphen **Q / K / A** als Vektor-Pfade ersetzen (Q♣ dunkel, K♥ rot, A♠ dunkel), passend zum Mockup-SVG. Rein dekorativ, nicht gethemt.
- CTA: 60dp/16sp (siehe G).

### F. Stiche bestätigen (`activity_confirm_tricks.xml`)

- Spieler-Zeilen luftiger: Zeilen-`padding` **14dp → 16dp**, `layout_marginTop` **10dp → 12dp**.
- Gegen Überlauf bei 4 Spielern auf kleinen Displays in einen `ScrollView` (`fillViewport="true"`, `clipToPadding="false"`) wickeln — analog zu `activity_declare_tricks.xml`: innerer `LinearLayout`, Flex-Spacer mit `minHeight` schiebt den CTA nach unten. Die Treffer-Visuals (Code in `ConfirmTicksActivity.applyHitVisuals`) bleiben unberührt.
- CTA: 60dp/16sp (siehe G).

### G. CTA-Vereinheitlichung (alle Masken)

- Der vergrößerte CTA (**Höhe 60dp, Text 16sp**) wird auf **allen** Masken angewandt: Start, Namen, Austeilen, Ansagen, Bestätigen — wie im Mockup, wo alle CTAs identisch aussehen. Umsetzung pro Layout-`Button` (bestehendem Muster folgend). *Optional/cleanup:* gemeinsame `Widget.Spades.Cta`-Style-Klasse, falls gewünscht — nicht erforderlich.

## Nicht im Scope

- Architektur (MVVM, Room, Repository, GameEngine), Domain-Logik, Persistenz.
- Compose (bleibt entfernt).
- Tastatur-/IME-Inset-Verhalten der Namens-Maske (separat, falls je nötig).
- Spiellogik, Strings/Lokalisierung, Farben/Paletten (bereits korrekt).

## Verifikation

- Build: `.\gradlew.bat assembleDebug` (und `lint`).
- Manuell am Gerät/Emulator je Maske gegen das Mockup prüfen: kein Überlappen mit Status-/Navigationsleiste; Logo-/Button-/Feld-Größen; zentrierte Punkte & Runden-Titel; Zahl/Label-Grundlinie; Q/K/A-Karten; luftigere Bestätigen-Maske; CTA-Größe überall gleich.
- Bestehende Unit-Tests (`domain`) müssen grün bleiben (keine Logikänderung). README-Screenshots ggf. via Skill aktualisieren.
