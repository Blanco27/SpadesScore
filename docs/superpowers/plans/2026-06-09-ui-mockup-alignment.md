# UI-Mockup-Feinabgleich Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die ausgelieferte App pixelgenau an `docs/mockups/spadesscore-ui-redesign.html` angleichen — zentrale Window-Insets-Behandlung plus Größen-/Abstands-/Ausrichtungs-Feinschliff pro Maske. Reine Theme-/Layout-Arbeit, keine Domain-/Daten-/Architektur-Änderung.

**Architecture:** Edge-to-Edge (erzwungen durch `targetSdk 36`) wird zentral über einen `ViewCompat.setOnApplyWindowInsetsListener` auf dem Activity-Content-Frame (`android.R.id.content`) abgefangen und als Padding gesetzt. Eine kleine geteilte Extension `View.applySystemBarInsetsAsPadding()` (`ui/insets.kt`) wird von der Basisklasse `SpadesAppCompatActivity` **und** von `ResultScreenActivity` (die nicht erbt) aufgerufen. Danach werden die bisher handgetunten Top-Paddings je Layout normalisiert und die im Mockup sichtbaren Detail-Abweichungen pro Maske korrigiert.

**Tech Stack:** Kotlin, XML-Layouts (kein Compose), AndroidX (`androidx.core` für `ViewCompat`/`WindowInsetsCompat` — bereits via `libs.androidx.core.ktx` im Projekt), Material Components. Build über Gradle-Wrapper (`.\gradlew.bat`).

---

## Testing-Ansatz (bitte vorab lesen)

Dies ist **reine Layout-/Theme-Arbeit** — es gibt keine sinnvollen JVM-Unit-Tests für Inset-Padding, Glyphen oder Abstände. Der automatisierte Gate je Batch ist daher:

1. `.\gradlew.bat assembleDebug` → `BUILD SUCCESSFUL`
2. `.\gradlew.bat lint` → keine neuen Fehler (Warnungen wie `DiscouragedApi` sind im Projekt bereits unterdrückt)
3. `.\gradlew.bat testDebugUnitTest` → die bestehenden `domain`-Tests bleiben **grün** (es wird keine Logik angefasst — dies ist nur ein Regressions-Sicherheitsnetz)
4. **Manuelle Sichtprüfung** am Gerät/Emulator gegen das Mockup (siehe „Manuelle Verifikation" je Batch)

Es wird **batch-weise** gearbeitet (Nutzer-Präferenz): ein Build-/Lint-/Commit-Checkpoint pro **Batch**, nicht pro Task. Innerhalb eines Batches werden mehrere Dateien bearbeitet, bevor gebaut wird.

## File Structure

Neu:
- `app/src/main/java/com/nwe/spadesscore/ui/insets.kt` — eine `View`-Extension, die System-Bar-Insets als Padding auf den Content-Frame legt. Einzige Verantwortung: Edge-to-Edge-Inset-Handling zentralisieren.

Geändert (Kotlin):
- `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.kt` — Helper nach `initContentView()` aufrufen.
- `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt` — Helper nach `setContentView(...)` aufrufen (erbt nicht von der Basisklasse).
- `app/src/main/java/com/nwe/spadesscore/MainActivity.kt` — `renderPlayerCount(...)`: Schatten der aktiven Segment-Pille via `elevation`.

Geändert (Layout/Drawable):
- `activity_main.xml`, `activity_player_names.xml`, `include_round_header.xml`, `activity_deal_cards.xml`, `ic_card_fan.xml`, `activity_confirm_tricks.xml`, `activity_declare_tricks.xml`, `activity_settings.xml`, `activity_result_screen.xml`

**Nicht verändert (bewusst):** `bg_segment_selected.xml` — es erfüllt die Anforderung „Surface-Füllung + abgerundete Ecken" bereits (`solid appSurface` + `corners 11dp`) und wird von `SettingsActivity.renderThemeSelection(...)` mitgenutzt; eine Änderung würde das Theme-Segment der Einstellungen ungewollt mitverändern. Der im Mockup sichtbare Schatten der aktiven Start-Pille wird stattdessen über `elevation` im Code erzeugt.

---

## Batch 1 — Window-Insets (Fundament, betrifft alle Masken)

Dieser Batch ist der „Pilot": die zentrale Inset-Logik wird gebaut und an zwei Masken (Start + Ergebnis) verifiziert, bevor in Batch 2 die Detail-Tweaks folgen.

### Task 1: Inset-Helper erstellen und einbinden

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/insets.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt`

- [ ] **Step 1: Helper-Datei anlegen**

Create `app/src/main/java/com/nwe/spadesscore/ui/insets.kt` mit exakt diesem Inhalt:

```kotlin
package com.nwe.spadesscore.ui

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Legt die System-Bar-Insets (Status- + Navigationsleiste, plus Display-Cutouts in
 * Landscape) als Padding auf diese View. Auf dem Activity-Content-Frame
 * (android.R.id.content) aufgerufen, der selbst kein Padding hat, bleibt das eigene
 * Padding jedes Layouts erhalten und wird nicht doppelt gezählt. targetSdk 36 erzwingt
 * Edge-to-Edge, daher app-weit notwendig.
 */
fun View.applySystemBarInsetsAsPadding() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
        insets
    }
}
```

- [ ] **Step 2: In der Basisklasse einbinden**

In `app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.kt` den Import ergänzen und den Aufruf direkt nach `initContentView()` einfügen.

Import (zu den bestehenden Imports hinzufügen):

```kotlin
import android.view.View
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
```

`onCreate` ändern von:

```kotlin
        applyPersistedLocale()
        initContentView()
        initializeUIComponents()
```

zu:

```kotlin
        applyPersistedLocale()
        initContentView()
        findViewById<View>(android.R.id.content).applySystemBarInsetsAsPadding()
        initializeUIComponents()
```

- [ ] **Step 3: In `ResultScreenActivity` einbinden (erbt nicht von der Basisklasse)**

In `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt` den Import ergänzen:

```kotlin
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
```

(Der Import `android.view.View` ist dort bereits vorhanden.)

In `onCreate` den Aufruf direkt nach `setContentView(...)` einfügen. Von:

```kotlin
        applyPersistedLocale()
        setContentView(R.layout.activity_result_screen)

        // ── Original getIdentifier loops — preserved exactly ──────────
```

zu:

```kotlin
        applyPersistedLocale()
        setContentView(R.layout.activity_result_screen)
        findViewById<View>(android.R.id.content).applySystemBarInsetsAsPadding()

        // ── Original getIdentifier loops — preserved exactly ──────────
```

> **Hinweis:** `SettingsActivity` erbt ebenfalls nicht von `SpadesAppCompatActivity`, ist aber **nicht** Teil dieses Tasks — ihr Root-Padding wird in Task 8 normalisiert und der Inset-Aufruf dort ergänzt (gehört thematisch zur Settings-Maske). Falls der Pilot zeigt, dass Settings ohne Insets überlappt, kann der Aufruf vorgezogen werden; Standard ist Task 8.

### Batch-1-Gate

- [ ] **Step 1: Build**

Run (PowerShell): `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Lint**

Run: `.\gradlew.bat lint`
Expected: keine neuen Fehler.

- [ ] **Step 3: Manuelle Verifikation (Pilot)**

App installieren (`.\gradlew.bat installDebug`) und auf einem Gerät mit Gesten-Navigation prüfen:
- **Start:** Zahnrad oben rechts überlappt nicht mehr Uhr/Akku; CTA „Spiel starten" sitzt nicht unter der Navigationsleiste.
- **Ergebnis:** Titel überlappt nicht die Statusleiste; „Weiter"-Button nicht unter der Navigationsleiste.

Falls hier alles passt, ist der zentrale Inset-Mechanismus bestätigt und Batch 2 kann die restlichen Masken normalisieren.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/insets.kt app/src/main/java/com/nwe/spadesscore/SpadesAppCompatActivity.kt app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt
git commit -m "feat(ui): apply system-bar insets as padding on all screens"
```

---

## Batch 2 — Maskenweiser Feinabgleich

Jede Task bearbeitet **eine** Maske vollständig: Root-Padding-Normalisierung (gemäß Inset-Tabelle der Spec) **plus** die maskenspezifischen Tweaks **plus** die CTA-Vereinheitlichung (60dp/16sp). So wird jede Layout-Datei nur einmal angefasst. Build/Lint/Commit erst am **Batch-2-Gate**.

**CTA-Standard (Spec G), gilt für Start, Namen, Austeilen, Ansagen, Bestätigen — NICHT Ergebnis/Settings:**
- `android:layout_height="56dp"` → `60dp`
- `android:textSize="15sp"` → `16sp`

### Task 2: Start (`activity_main.xml`, `MainActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`

- [ ] **Step 1: Root-Padding normalisieren**

In `activity_main.xml` am Wurzel-`LinearLayout`:

```xml
android:paddingTop="16dp"
```
→
```xml
android:paddingTop="8dp"
```
(`paddingStart`/`paddingEnd` 24dp und `paddingBottom` 24dp bleiben.)

- [ ] **Step 2: Pik-Logo vergrößern (62dp → 80dp)**

Am Hero-`ImageView` (das mit `android:src="@drawable/ic_spade"`):

```xml
android:layout_width="62dp"
android:layout_height="62dp"
```
→
```xml
android:layout_width="80dp"
android:layout_height="80dp"
```

- [ ] **Step 3: Segment-Optionen luftiger (12dp → 15dp)**

An **beiden** Segment-`TextView`s (`@id/btn3Players` und `@id/btn4Players`):

```xml
android:paddingTop="12dp"
android:paddingBottom="12dp"
```
→
```xml
android:paddingTop="15dp"
android:paddingBottom="15dp"
```

- [ ] **Step 4: CTA vereinheitlichen (60dp / 16sp)**

Am `@id/start_game_button`:

```xml
android:layout_height="56dp"
```
→
```xml
android:layout_height="60dp"
```
und
```xml
android:textSize="15sp"
```
→
```xml
android:textSize="16sp"
```

- [ ] **Step 5: Schatten der aktiven Pille im Code (elevation)**

In `MainActivity.kt` die Funktion `renderPlayerCount` so ersetzen, dass die aktive Pille eine `elevation` von 4dp bekommt und die inaktive 0dp. Ersetze die gesamte Funktion:

```kotlin
    private fun renderPlayerCount(count: Int) {
        val accentColor = MaterialColors.getColor(btn3Players, R.attr.appAccent)
        val mutedColor = MaterialColors.getColor(btn3Players, R.attr.appMuted)
        val activeElevation = 4f * resources.displayMetrics.density

        val (active, inactive) = if (count == 3) btn3Players to btn4Players
                                 else btn4Players to btn3Players

        active.setBackgroundResource(R.drawable.bg_segment_selected)
        active.setTextColor(accentColor)
        active.elevation = activeElevation

        inactive.setBackgroundResource(0)
        inactive.setTextColor(mutedColor)
        inactive.elevation = 0f
    }
```

> Begründung: `bg_segment_selected.xml` bleibt unverändert (es wird vom Settings-Theme-Segment mitgenutzt); der im Mockup sichtbare Schatten (`box-shadow:0 2px 8px`) entsteht über `elevation` auf der aktiven View. Die Drawable hat bereits `solid appSurface` + `corners 11dp`, sodass die Elevation eine saubere abgerundete Schattenkante wirft.

### Task 3: Spielernamen (`activity_player_names.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_player_names.xml`

- [ ] **Step 1: Root-Padding normalisieren (32dp → 16dp)**

Am Wurzel-`LinearLayout`:

```xml
android:paddingTop="32dp"
```
→
```xml
android:paddingTop="16dp"
```

- [ ] **Step 2: Screen-Titel 24sp → 26sp (inline-Override)**

Am Titel-`TextView` (`android:text="@string/set_player_names"`) ein inline `textSize` ergänzen, **direkt unter** der `textAppearance`-Zeile:

```xml
android:textAppearance="@style/TextAppearance.Spades.ScreenTitle"
```
→
```xml
android:textAppearance="@style/TextAppearance.Spades.ScreenTitle"
android:textSize="26sp"
```

> Wichtig: **nicht** `TextAppearance.Spades.ScreenTitle` selbst ändern — der Settings-Titel nutzt denselben Style und soll bei 24sp bleiben.

- [ ] **Step 3: Feld-Karten luftiger (alle vier Karten)**

In **allen vier** Feld-Karten (`@id/player1_card` … `@id/player4_card`) jeweils:

```xml
android:paddingTop="10dp"
android:paddingBottom="10dp"
android:layout_marginBottom="10dp"
```
→
```xml
android:paddingTop="14dp"
android:paddingBottom="14dp"
android:layout_marginBottom="14dp"
```

(Die `paddingStart`/`paddingEnd` 14dp bleiben; die inneren `EditText`-Paddings von 2dp bleiben unberührt.)

- [ ] **Step 4: CTA vereinheitlichen (60dp / 16sp)**

Am `@id/start_game_button`:

```xml
android:layout_height="56dp"
```
→ `60dp`, und
```xml
android:textSize="15sp"
```
→ `16sp`.

### Task 4: Geteilter Runden-Header (`include_round_header.xml`)

**Files:**
- Modify: `app/src/main/res/layout/include_round_header.xml`

- [ ] **Step 1: Punkte-Spalten zentrieren**

An **allen vier** Spalten-`LinearLayout`s (`@id/score_col_1` … `@id/score_col_4`) das Attribut `android:gravity="center_horizontal"` ergänzen. Beispiel `score_col_1`:

```xml
        <LinearLayout
            android:id="@+id/score_col_1"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical">
```
→
```xml
        <LinearLayout
            android:id="@+id/score_col_1"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:orientation="vertical"
            android:gravity="center_horizontal">
```

Identisch für `score_col_2`, `score_col_3`, `score_col_4`. Entspricht `.scorerow div{text-align:center}`.

- [ ] **Step 2: Runden-Überschrift zentrieren**

Am `@id/round_title`:

```xml
    <TextView
        android:id="@+id/round_title"
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:textAppearance="@style/TextAppearance.Spades.Title"
        android:layout_marginTop="22dp" />
```
→
```xml
    <TextView
        android:id="@+id/round_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:gravity="center"
        android:textAppearance="@style/TextAppearance.Spades.Title"
        android:layout_marginTop="22dp" />
```

Entspricht `.title{text-align:center}`. Die Accent-Tönung der Ziffer (Spannable in `RoundHeader.kt`) bleibt unverändert — `gravity` ändert nur die Ausrichtung, nicht den Span.

### Task 5: Karten austeilen (`activity_deal_cards.xml`, `ic_card_fan.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_deal_cards.xml`
- Modify: `app/src/main/res/drawable/ic_card_fan.xml`

- [ ] **Step 1: Root-Padding normalisieren (24dp ringsum → top16/horiz24/bottom24)**

Am Wurzel-`LinearLayout` das einzelne `android:padding="24dp"` durch getrennte Werte ersetzen:

```xml
    android:orientation="vertical"
    android:padding="24dp"
    android:background="?attr/appBg"
```
→
```xml
    android:orientation="vertical"
    android:paddingStart="24dp"
    android:paddingEnd="24dp"
    android:paddingTop="16dp"
    android:paddingBottom="24dp"
    android:background="?attr/appBg"
```

- [ ] **Step 2: Zahl/Label-Grundlinie korrigieren**

Am Label-`TextView` „Karten pro Spieler" (`android:text="@string/cards_per_player"`) das `android:layout_gravity="bottom"` **entfernen**, damit `baselineAligned="true"` greift:

```xml
                android:layout_marginStart="7dp"
                android:layout_gravity="bottom"
                android:text="@string/cards_per_player"
```
→
```xml
                android:layout_marginStart="7dp"
                android:text="@string/cards_per_player"
```

Entspricht `.count{align-items:baseline}`: große Zahl ragt oben heraus, beide unten bündig.

- [ ] **Step 3: Karten-Block tiefer setzen (oberen Spacer gewichten)**

Am **oberen** Spacer (Kommentar `<!-- Top spacer -->`, der zwischen `dealer_subtitle` und dem Illustrations-Block liegt):

```xml
    <!-- Top spacer -->
    <Space
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
```
→
```xml
    <!-- Top spacer (slightly weighted so the card block sits lower / more centred) -->
    <Space
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1.25" />
```

(Der untere Spacer bleibt bei `layout_weight="1"`. Bewusst leichtgewichtig, keine Entkopplung vom geteilten Header.)

- [ ] **Step 4: CTA vereinheitlichen (60dp / 16sp)**

Am `@id/deal_next_button`:

```xml
android:layout_height="56dp"
```
→ `60dp`, und
```xml
android:textSize="15sp"
```
→ `16sp`.

- [ ] **Step 5: Rang-Balken durch Q/K/A-Glyphen ersetzen**

In `ic_card_fan.xml` gibt es drei „Rank mark"-Pfade, alle mit identischem `pathData="M7,7 L14,7 L14,10 L7,10 Z"`. Jeder wird durch einen gestrichelten (stroke-basierten) Buchstaben ersetzt — Q (Karte 1), K (Karte 2), A (Karte 3). Reihenfolge im File: Q-Karte zuerst, dann K-Karte, dann A-Karte.

**Karte 1 (Q, dunkel):** ersetze

```xml
            <!-- Rank mark (Q) — small bar at top-left, dark -->
            <path
                android:fillColor="#FF17171A"
                android:pathData="M7,7 L14,7 L14,10 L7,10 Z"/>
```
durch
```xml
            <!-- Rank glyph Q — stroked letter, dark -->
            <path
                android:fillColor="#00000000"
                android:strokeColor="#FF17171A"
                android:strokeWidth="2"
                android:strokeLineCap="round"
                android:strokeLineJoin="round"
                android:pathData="M12,9.5 A3.2,5 0 0 1 12,19.5 A3.2,5 0 0 1 12,9.5 Z M13,16.5 L16,20"/>
```

**Karte 2 (K, rot):** ersetze

```xml
        <!-- Rank mark (K) — small bar at top-left, red -->
        <path
            android:fillColor="#FFC0392B"
            android:pathData="M7,7 L14,7 L14,10 L7,10 Z"/>
```
durch
```xml
        <!-- Rank glyph K — stroked letter, red -->
        <path
            android:fillColor="#00000000"
            android:strokeColor="#FFC0392B"
            android:strokeWidth="2"
            android:strokeLineCap="round"
            android:strokeLineJoin="round"
            android:pathData="M8.5,9 L8.5,20 M15,9 L8.5,14.5 L15,20"/>
```

**Karte 3 (A, dunkel):** ersetze

```xml
            <!-- Rank mark (A) — small bar at top-left, dark -->
            <path
                android:fillColor="#FF17171A"
                android:pathData="M7,7 L14,7 L14,10 L7,10 Z"/>
```
durch
```xml
            <!-- Rank glyph A — stroked letter, dark -->
            <path
                android:fillColor="#00000000"
                android:strokeColor="#FF17171A"
                android:strokeWidth="2"
                android:strokeLineCap="round"
                android:strokeLineJoin="round"
                android:pathData="M8.5,20 L12,9 L15.5,20 M9.7,16 L14.3,16"/>
```

Die großen Suit-Glyphen (♣/♥/♠) im Karten-Zentrum bleiben **unverändert**. Koordinaten der Buchstaben liegen im lokalen 50×72-Kartenraum (oben links), kollidieren also nicht mit den Suits. Rein dekorativ, nicht gethemt — Legibilität bei Step „Manuelle Verifikation" am Gerät prüfen und ggf. fein-justieren (Spec erlaubt Feinschliff).

### Task 6: Stiche bestätigen (`activity_confirm_tricks.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_confirm_tricks.xml`

Diese Maske wird in einen `ScrollView` gewickelt (Überlaufschutz bei 4 Spielern auf kleinen Displays), analog zu `activity_declare_tricks.xml`. Die Treffer-Visuals (`ConfirmTicksActivity.applyHitVisuals`) bleiben unberührt — es ändern sich nur IDs-frei die Container.

- [ ] **Step 1: Wurzel-Tag durch `ScrollView` ersetzen**

Ersetze den öffnenden Wurzel-`LinearLayout`:

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/appBg"
    android:orientation="vertical"
    android:padding="24dp"
    tools:context=".ConfirmTicksActivity">

    <!-- Shared header: brand row + per-player score row + round title + progress bar -->
```
durch
```xml
<ScrollView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    xmlns:tools="http://schemas.android.com/tools"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:background="?attr/appBg"
    android:fillViewport="true"
    android:paddingStart="24dp"
    android:paddingEnd="24dp"
    android:paddingTop="16dp"
    android:paddingBottom="24dp"
    android:clipToPadding="false"
    tools:context=".ConfirmTicksActivity">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="vertical">

    <!-- Shared header: brand row + per-player score row + round title + progress bar -->
```

- [ ] **Step 2: Flex-Spacer `minHeight` geben**

Am Flex-Spacer (Kommentar `<!-- Flex spacer pushes CTA to bottom -->`):

```xml
    <Space
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1" />
```
→
```xml
    <Space
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:minHeight="16dp" />
```

- [ ] **Step 3: CTA vereinheitlichen + Wurzel schließen**

Ersetze den abschließenden CTA-Button samt schließendem `</LinearLayout>`:

```xml
    <!-- CTA button -->
    <Button
        android:id="@+id/start_Button"
        android:layout_width="match_parent"
        android:layout_height="56dp"
        android:background="@drawable/bg_cta"
        android:fontFamily="@font/space_grotesk_semibold"
        android:text="@string/next_round"
        android:textAllCaps="false"
        android:textColor="?attr/appCtaFg"
        android:textSize="15sp"
        app:backgroundTint="@null" />

</LinearLayout>
```
durch
```xml
    <!-- CTA button -->
    <Button
        android:id="@+id/start_Button"
        android:layout_width="match_parent"
        android:layout_height="60dp"
        android:background="@drawable/bg_cta"
        android:fontFamily="@font/space_grotesk_semibold"
        android:text="@string/next_round"
        android:textAllCaps="false"
        android:textColor="?attr/appCtaFg"
        android:textSize="16sp"
        app:backgroundTint="@null" />

    </LinearLayout>

</ScrollView>
```

- [ ] **Step 4: Spieler-Zeilen luftiger (alle vier Zeilen)**

In **allen vier** Zeilen-`LinearLayout`s (`@id/player1_layout` … `@id/player4_layout`) jeweils:

```xml
        android:layout_marginTop="10dp"
        ...
        android:padding="14dp">
```
→
```xml
        android:layout_marginTop="12dp"
        ...
        android:padding="16dp">
```

Konkret pro Zeile: `android:layout_marginTop="10dp"` → `12dp` und `android:padding="14dp"` → `16dp`.

### Task 7: Stiche ansagen (`activity_declare_tricks.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_declare_tricks.xml`

- [ ] **Step 1: Root-Padding normalisieren (64dp → 24dp)**

Am Wurzel-`ScrollView`:

```xml
    android:paddingTop="64dp"
```
→
```xml
    android:paddingTop="24dp"
```

(`paddingStart`/`paddingEnd` 24dp, `paddingBottom` 24dp und `clipToPadding="false"` bleiben.)

- [ ] **Step 2: CTA vereinheitlichen (60dp / 16sp)**

Am `@id/start_Button`:

```xml
android:layout_height="56dp"
```
→ `60dp`, und
```xml
android:textSize="15sp"
```
→ `16sp`.

### Task 8: Einstellungen (`activity_settings.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_settings.xml`

> Settings hat keinen CTA — nur Root-Padding-Normalisierung. `SettingsActivity` erbt nicht von `SpadesAppCompatActivity`, ruft den Inset-Helper aber noch nicht auf. Da der Inset-Helper in Batch 1 existiert, wird er hier ergänzt (analog `ResultScreenActivity`).

- [ ] **Step 1: Inset-Helper in `SettingsActivity` aufrufen**

In `app/src/main/java/com/nwe/spadesscore/SettingsActivity.kt` Import ergänzen:

```kotlin
import com.nwe.spadesscore.ui.applySystemBarInsetsAsPadding
```

In `onCreate` direkt nach `setContentView(...)`:

```kotlin
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
```
→
```kotlin
        setContentView(R.layout.activity_settings)
        findViewById<android.view.View>(android.R.id.content).applySystemBarInsetsAsPadding()

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }
```

- [ ] **Step 2: Root-Padding normalisieren**

Am inneren `LinearLayout` des `ScrollView` (das mit `paddingStart="17dp"`):

```xml
        android:paddingStart="17dp"
        android:paddingEnd="17dp"
        android:paddingTop="15dp"
        android:paddingBottom="15dp">
```
→
```xml
        android:paddingStart="17dp"
        android:paddingEnd="17dp"
        android:paddingTop="8dp"
        android:paddingBottom="15dp">
```

(Nur `paddingTop` 15dp → 8dp gemäß Inset-Tabelle; horizontale 17dp und bottom 15dp bleiben.)

### Task 9: Ergebnis (`activity_result_screen.xml`)

**Files:**
- Modify: `app/src/main/res/layout/activity_result_screen.xml`

> Inset-Aufruf wurde bereits in Task 1/Batch 1 gesetzt. Hier nur Root-Padding. Kein CTA-Bump (der „Weiter"-Button ist nicht Teil der CTA-Vereinheitlichung gemäß Spec G).

- [ ] **Step 1: Root-Padding normalisieren (top40→16, horiz20, bottom28)**

Am inneren `LinearLayout` des `ScrollView`:

```xml
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:paddingTop="40dp"
        android:paddingBottom="28dp">
```
→
```xml
        android:orientation="vertical"
        android:paddingStart="20dp"
        android:paddingEnd="20dp"
        android:paddingTop="16dp"
        android:paddingBottom="28dp">
```

(Nur `paddingTop` 40dp → 16dp; horizontale 20dp und bottom 28dp bleiben.)

### Batch-2-Gate

- [ ] **Step 1: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 2: Lint**

Run: `.\gradlew.bat lint`
Expected: keine neuen Fehler.

- [ ] **Step 3: Domain-Tests grün (Regressions-Netz)**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, alle Tests bestehen (keine Logik geändert).

- [ ] **Step 4: Manuelle Sichtprüfung gegen das Mockup**

`.\gradlew.bat installDebug`, dann je Maske prüfen:
- **Alle Masken:** kein Überlappen mit Status-/Navigationsleiste; CTA überall gleich groß (60dp/16sp).
- **Start:** Logo größer; aktive Spielerzahl-Pille mit Schatten + Accent-Text; Segment luftiger.
- **Namen:** Titel größer; Feld-Karten luftiger.
- **Header (Deal/Declare/Confirm):** Punkte-Spalten zentriert; „Runde N" zentriert (Ziffer weiterhin Accent-getönt).
- **Austeilen:** Zahl/Label-Grundlinie sauber; Q/K/A-Karten lesbar; Block sitzt mittiger.
- **Bestätigen:** Zeilen luftiger; bei 4 Spielern auf kleinem Display scrollbar, CTA nicht abgeschnitten.
- **Ergebnis/Settings:** Kopfbereich nicht zu hoch.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/res/layout/activity_player_names.xml app/src/main/res/layout/include_round_header.xml app/src/main/res/layout/activity_deal_cards.xml app/src/main/res/drawable/ic_card_fan.xml app/src/main/res/layout/activity_confirm_tricks.xml app/src/main/res/layout/activity_declare_tricks.xml app/src/main/res/layout/activity_settings.xml app/src/main/res/layout/activity_result_screen.xml app/src/main/java/com/nwe/spadesscore/MainActivity.kt app/src/main/java/com/nwe/spadesscore/SettingsActivity.kt
git commit -m "feat(ui): align screens to mockup (insets padding, sizes, centering, Q/K/A cards, CTA)"
```

---

## Nach Abschluss

- [ ] README-Screenshots ggf. aktualisieren via `update-readme-screenshots`-Skill (Start, Namen, Deal, Declare, Confirm, Result haben sich sichtbar geändert).
- [ ] Branch-Abschluss über `superpowers:finishing-a-development-branch` (Merge/PR-Entscheidung) — die Vor-Arbeit liegt unverändert auf `master`, origin ist laut Notiz noch nicht gepusht.

---

## Self-Review (gegen die Spec geprüft)

**Spec-Abdeckung:**
- A. Window-Insets zentral → Task 1 (Helper + Basisklasse + ResultScreen) + Inset-Aufruf für Settings in Task 8. Padding-Tabelle: main (Task 2), player_names (Task 3), deal (Task 5), confirm (Task 6), declare (Task 7), settings (Task 8), result (Task 9). ✓ alle sieben Zeilen abgedeckt.
- B. Start: Logo 80dp, Segment 15dp, aktive Pille Surface+Schatten (elevation), CTA 60/16 → Task 2. ✓ (`bg_segment_selected.xml` bewusst unverändert, Begründung dokumentiert.)
- C. Namen: Feld-Padding/Margin 14, Titel 26sp inline, CTA → Task 3. ✓
- D. Header: Spalten zentriert, Titel zentriert → Task 4. ✓
- E. Deal: Label-`layout_gravity` entfernt, Spacer gewichtet, Q/K/A-Glyphen, CTA → Task 5. ✓
- F. Confirm: Zeilen 16/12, ScrollView-Wrap, CTA → Task 6. ✓
- G. CTA-Vereinheitlichung 60/16 → in Tasks 2, 3, 5, 6, 7 enthalten; Ergebnis/Settings bewusst ausgenommen. ✓ (Optionale gemeinsame `Widget.Spades.Cta`-Style-Klasse: laut Spec „nicht erforderlich" → weggelassen, DRY-Hinweis statt Pflicht.)

**Platzhalter-Scan:** keine TBD/TODO/„appropriate handling"; jeder Code-Step zeigt konkreten Code bzw. exakte alt→neu-Attribute.

**Typ-/Namens-Konsistenz:** `applySystemBarInsetsAsPadding()` identisch in `insets.kt`, Basisklasse, `ResultScreenActivity`, `SettingsActivity`. `renderPlayerCount` ersetzt die bestehende Funktion vollständig (gleiche Signatur). Layout-IDs (`start_game_button`, `start_Button`, `deal_next_button`, `score_col_1..4`, `round_title`, `card_count`) gegen die gelesenen Dateien verifiziert.

**Offene Punkte für die manuelle Phase:** Legibilität der Q/K/A-Glyphen (gestrichelte Pfade sind Richtwerte) und exakte Spacer-Gewichtung im Deal-Screen — beide laut Spec „Feinschliff am Gerät erlaubt".
