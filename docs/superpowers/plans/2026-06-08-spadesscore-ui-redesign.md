# SpadesScore UI-Redesign — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reskin the SpadesScore app so it looks **exactly 1:1** like the mockup `docs/mockups/spadesscore-ui-redesign.html` (Paper Light / Casino Noir, Space Grotesk + Inter), add a Settings screen with a theme switcher, and a new launcher icon — without touching MVVM/Room/GameEngine logic.

**Architecture:** Approach A — build a centralized design system first (color tokens for both palettes, theme attributes, fonts, shared styles, shape drawables, vector icons), then reskin each screen purely through `?attr/…` and shared styles. Dark/Light comes for free via the `values-night/` qualifier; a manual System/Light/Dark switch (SharedPreferences + `AppCompatDelegate`) overrides it.

**Tech Stack:** Kotlin, XML layouts + Material Components (no Compose), Room (unchanged), AndroidX AppCompat, downloadable Google Fonts, manual DI (`AppContainer`).

**Reference docs (read before starting):**
- Spec: `docs/superpowers/specs/2026-06-08-spadesscore-ui-redesign-design.md`
- Visual source of truth: `docs/mockups/spadesscore-ui-redesign.html` (Light screens 1–7, Dark screens 8–10)
- Launcher drafts (chosen B): `docs/mockups/launcher-icon-drafts.html`

**Branch:** Work continues on `ui-redesign` (already created).

**Build/verify commands** (Bash tool uses `./gradlew`; on Windows PowerShell use `.\gradlew.bat`):
- Build: `./gradlew assembleDebug`
- Lint: `./gradlew lint`
- Unit tests: `./gradlew testDebugUnitTest`

---

## File Structure

**New files**
- `app/src/main/res/font/space_grotesk.xml`, `space_grotesk_medium.xml`, `space_grotesk_semibold.xml`, `space_grotesk_bold.xml`, `inter_medium.xml`, `inter_semibold.xml`, `inter_bold.xml`
- `app/src/main/res/values/attrs.xml` — semantic theme attributes
- `app/src/main/res/drawable/` — `bg_card`, `bg_field`, `bg_field_focus`, `bg_chip`, `bg_chip_warn`, `bg_step`, `bg_segment`, `bg_segment_selected`, `bg_cta`, `bg_cta_disabled`, `bg_check`, `bg_check_on`, `bg_radio`, `bg_radio_on`, `bg_grid`, `bg_gear`, `bg_rank_badge`, `bg_rank_badge_lead`, `progress_track.xml`
- `app/src/main/res/drawable/` vectors — `ic_spade`, `ic_gear`, `ic_back`, `ic_crown`, `ic_theme_system`, `ic_sun`, `ic_moon`, `ic_github`, `ic_card_fan`
- `app/src/main/java/com/nwe/spadesscore/domain/model/ThemeMode.kt` (+ pure `themeModeFromStorage`)
- `app/src/main/java/com/nwe/spadesscore/data/ThemePreferences.kt`
- `app/src/main/java/com/nwe/spadesscore/ui/theme.kt` (ThemeMode→nightMode + `Context.themePreferences`)
- `app/src/main/java/com/nwe/spadesscore/ui/settings/SettingsUiState.kt`, `SettingsViewModel.kt`
- `app/src/main/java/com/nwe/spadesscore/SettingsActivity.kt`
- `app/src/main/res/layout/activity_settings.xml`
- `app/src/test/java/com/nwe/spadesscore/domain/ThemeModeTest.kt`

**Modified files**
- `res/values/colors.xml`, `res/values-night/colors.xml`, `res/values/themes.xml`, `res/values-night/themes.xml`
- All 7 layouts: `activity_main.xml`, `activity_player_names.xml`, `activity_deal_cards.xml`, `activity_declare_tricks.xml`, `activity_confirm_tricks.xml`, `activity_result_screen.xml`, `view_trick_spinner.xml`
- `MainActivity.kt` (gear→Settings, language buttons removed), `SpadesApplication.kt` (apply theme), `di/AppContainer.kt` (ThemePreferences), the 4 reskinned activities (view wiring for new elements)
- `res/values/strings.xml`, `res/values-de/strings.xml`
- Launcher icon: `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`, `drawable/ic_launcher_foreground.xml`, `values/ic_launcher_background.xml`, `mipmap-*/ic_launcher*.png`

**Removed (after they are unreferenced)**
- `drawable/spades_logo.png`, `ace.png`, `button_background.xml`, `option_button.xml`, `selector_option.xml`, `player_bg_rounded.xml`, `spinner_background.xml`, `spinner_button_background.xml`, `button_background_selector.xml`
- `font/patrick_hand.xml`, `patrick_hand_sc.xml`, `comic_neue.xml`, `comic_neue_light.xml`, `roboto.xml`

---

# Phase 1 — Design-System Foundation

## Task 1: Add Space Grotesk + Inter weight fonts

**Files:**
- Create: `app/src/main/res/font/space_grotesk.xml`, `space_grotesk_medium.xml`, `space_grotesk_semibold.xml`, `space_grotesk_bold.xml`, `inter_medium.xml`, `inter_semibold.xml`, `inter_bold.xml`

- [ ] **Step 1: Create the four Space Grotesk weight files**

`space_grotesk.xml` (weight 400):
```xml
<?xml version="1.0" encoding="utf-8"?>
<font-family xmlns:app="http://schemas.android.com/apk/res-auto"
    app:fontProviderAuthority="com.google.android.gms.fonts"
    app:fontProviderPackage="com.google.android.gms"
    app:fontProviderQuery="name=Space Grotesk&amp;weight=400"
    app:fontProviderCerts="@array/com_google_android_gms_fonts_certs">
</font-family>
```
Create `space_grotesk_medium.xml`, `space_grotesk_semibold.xml`, `space_grotesk_bold.xml` identically but with `weight=500`, `weight=600`, `weight=700` in the query.

- [ ] **Step 2: Create the three Inter weight files**

Same template with `app:fontProviderQuery="name=Inter&amp;weight=500"` → `inter_medium.xml`, `weight=600` → `inter_semibold.xml`, `weight=700` → `inter_bold.xml`. (Keep existing `inter.xml` = weight 400.)

- [ ] **Step 3: Build to verify fonts resolve**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (font resources compile).

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/font/
git commit -m "feat(ui): add Space Grotesk + Inter weight fonts"
```

---

## Task 2: Color tokens for both palettes

**Files:**
- Modify: `app/src/main/res/values/colors.xml`
- Modify: `app/src/main/res/values-night/colors.xml`

- [ ] **Step 1: Add Paper Light semantic colors to `values/colors.xml`**

Append inside `<resources>` (keep existing colors for now; they are removed in Task 21):
```xml
<!-- Redesign tokens — Paper Light -->
<color name="bg">#F6F6F4</color>
<color name="surface">#FFFFFF</color>
<color name="ink">#17171A</color>
<color name="muted">#6B6B72</color>
<color name="accent">#4F46E5</color>
<color name="line">#E7E7E3</color>
<color name="track">#E7E7E3</color>
<color name="step_bg">#F1F1EF</color>
<color name="chip_bg">#EEEDFB</color>
<color name="cta_bg">#17171A</color>
<color name="cta_fg">#FFFFFF</color>
<color name="on_accent">#FFFFFF</color>
<color name="spade">#17171A</color>
<color name="ring">#244F46E5</color>
<color name="hit_bg">#0F4F46E5</color>
<color name="warn">#C0392B</color>
<color name="warn_bg">#1AC0392B</color>
```

- [ ] **Step 2: Add Casino Noir overrides to `values-night/colors.xml`**

Append the same names with dark values:
```xml
<!-- Redesign tokens — Casino Noir -->
<color name="bg">#0E0E10</color>
<color name="surface">#1A1A1E</color>
<color name="ink">#F4F4F5</color>
<color name="muted">#8A8A93</color>
<color name="accent">#E4B95B</color>
<color name="line">#2A2A30</color>
<color name="track">#26262C</color>
<color name="step_bg">#222228</color>
<color name="chip_bg">#211E16</color>
<color name="cta_bg">#E4B95B</color>
<color name="cta_fg">#15130C</color>
<color name="on_accent">#15130C</color>
<color name="spade">#E4B95B</color>
<color name="ring">#29E4B95B</color>
<color name="hit_bg">#17E4B95B</color>
<color name="warn">#E8736F</color>
<color name="warn_bg">#1FE8736F</color>
```

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/values/colors.xml app/src/main/res/values-night/colors.xml
git commit -m "feat(ui): add Paper Light + Casino Noir color tokens"
```

---

## Task 3: Theme attributes + attr→color mapping

**Files:**
- Create: `app/src/main/res/values/attrs.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`

- [ ] **Step 1: Declare semantic attributes in `attrs.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <attr name="appBg" format="color" />
    <attr name="appSurface" format="color" />
    <attr name="appInk" format="color" />
    <attr name="appMuted" format="color" />
    <attr name="appAccent" format="color" />
    <attr name="appLine" format="color" />
    <attr name="appTrack" format="color" />
    <attr name="appStepBg" format="color" />
    <attr name="appChipBg" format="color" />
    <attr name="appCtaBg" format="color" />
    <attr name="appCtaFg" format="color" />
    <attr name="appOnAccent" format="color" />
    <attr name="appSpade" format="color" />
    <attr name="appRing" format="color" />
    <attr name="appHitBg" format="color" />
    <attr name="appWarn" format="color" />
    <attr name="appWarnBg" format="color" />
</resources>
```

- [ ] **Step 2: Map attributes → colors in `values/themes.xml`**

Replace the body of `Theme.SpadesScore` (keep `parent="Theme.MaterialComponents.DayNight.NoActionBar"`) and add window/status-bar items. Because the `@color/*` names resolve per `-night/` qualifier, this single mapping covers both palettes:
```xml
<item name="appBg">@color/bg</item>
<item name="appSurface">@color/surface</item>
<item name="appInk">@color/ink</item>
<item name="appMuted">@color/muted</item>
<item name="appAccent">@color/accent</item>
<item name="appLine">@color/line</item>
<item name="appTrack">@color/track</item>
<item name="appStepBg">@color/step_bg</item>
<item name="appChipBg">@color/chip_bg</item>
<item name="appCtaBg">@color/cta_bg</item>
<item name="appCtaFg">@color/cta_fg</item>
<item name="appOnAccent">@color/on_accent</item>
<item name="appSpade">@color/spade</item>
<item name="appRing">@color/ring</item>
<item name="appHitBg">@color/hit_bg</item>
<item name="appWarn">@color/warn</item>
<item name="appWarnBg">@color/warn_bg</item>
<item name="android:windowBackground">@color/bg</item>
<item name="android:statusBarColor">@color/bg</item>
<item name="android:navigationBarColor">@color/bg</item>
<item name="android:windowLightStatusBar">true</item>
```

- [ ] **Step 3: Dark status-bar contrast in `values-night/themes.xml`**

In the night `Theme.SpadesScore`, add the same `appBg`/window items plus:
```xml
<item name="android:windowLightStatusBar">false</item>
```
(Night `@color/bg` is already dark, so only the light-status-bar flag flips.)

- [ ] **Step 4: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/values/attrs.xml app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml
git commit -m "feat(ui): map semantic theme attributes to color tokens"
```

---

## Task 4: TextAppearance styles

**Files:**
- Modify: `app/src/main/res/values/themes.xml` (append style block; or a new `res/values/type.xml`)

- [ ] **Step 1: Add the type scale styles**

Create `app/src/main/res/values/type.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="TextAppearance.Spades.Title" parent="">
        <item name="android:fontFamily">@font/space_grotesk_bold</item>
        <item name="android:textSize">40sp</item>
        <item name="android:letterSpacing">-0.02</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.Headline" parent="">
        <item name="android:fontFamily">@font/space_grotesk_bold</item>
        <item name="android:textSize">30sp</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.ScreenTitle" parent="">
        <item name="android:fontFamily">@font/space_grotesk_bold</item>
        <item name="android:textSize">24sp</item>
        <item name="android:letterSpacing">-0.01</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.Wordmark" parent="">
        <item name="android:fontFamily">@font/space_grotesk_bold</item>
        <item name="android:textSize">27sp</item>
        <item name="android:letterSpacing">0.04</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.Number" parent="">
        <item name="android:fontFamily">@font/space_grotesk_bold</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.PlayerName" parent="">
        <item name="android:fontFamily">@font/space_grotesk_medium</item>
        <item name="android:textSize">15sp</item>
        <item name="android:textColor">?attr/appInk</item>
    </style>
    <style name="TextAppearance.Spades.Label" parent="">
        <item name="android:fontFamily">@font/inter_bold</item>
        <item name="android:textSize">10.5sp</item>
        <item name="android:textAllCaps">true</item>
        <item name="android:letterSpacing">0.08</item>
        <item name="android:textColor">?attr/appMuted</item>
    </style>
    <style name="TextAppearance.Spades.Body" parent="">
        <item name="android:fontFamily">@font/inter_medium</item>
        <item name="android:textSize">12.5sp</item>
        <item name="android:textColor">?attr/appMuted</item>
    </style>
</resources>
```

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/values/type.xml
git commit -m "feat(ui): add Space Grotesk/Inter text appearance scale"
```

---

## Task 5: Shape drawables

**Files:**
- Create the drawables listed below under `app/src/main/res/drawable/`.

- [ ] **Step 1: Create card / field / focus**

`bg_card.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/appSurface" />
    <corners android:radius="16dp" />
    <stroke android:width="1dp" android:color="?attr/appLine" />
</shape>
```
`bg_field.xml`: identical but `android:radius="14dp"`.
`bg_field_focus.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/appSurface" />
    <corners android:radius="14dp" />
    <stroke android:width="2dp" android:color="?attr/appAccent" />
</shape>
```

- [ ] **Step 2: Create chip / chip-warn / step**

`bg_chip.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/appChipBg" />
    <corners android:radius="99dp" />
</shape>
```
`bg_chip_warn.xml`: same but `<solid android:color="?attr/appWarnBg" />`.
`bg_step.xml`: `<solid android:color="?attr/appStepBg" />` + `<corners android:radius="12dp" />`.

- [ ] **Step 3: Create segment / segment-selected**

`bg_segment.xml`: `<solid android:color="?attr/appStepBg" />` + `<corners android:radius="14dp" />`.
`bg_segment_selected.xml`: `<solid android:color="?attr/appSurface" />` + `<corners android:radius="11dp" />` + `<stroke android:width="1dp" android:color="?attr/appLine" />`.

- [ ] **Step 4: Create cta / cta-disabled**

`bg_cta.xml`: `<solid android:color="?attr/appCtaBg" />` + `<corners android:radius="15dp" />`.
`bg_cta_disabled.xml`: `<solid android:color="?attr/appStepBg" />` + `<corners android:radius="15dp" />`.

- [ ] **Step 5: Create check / check-on / radio / radio-on**

`bg_check.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="?attr/appSurface" />
    <corners android:radius="8dp" />
    <stroke android:width="2dp" android:color="?attr/appLine" />
</shape>
```
`bg_check_on.xml`: `<solid android:color="?attr/appAccent" />` + `<corners android:radius="8dp" />` + `<stroke android:width="2dp" android:color="?attr/appAccent" />`.
`bg_radio.xml`: `oval`, `<solid android:color="@android:color/transparent" />` + `<stroke android:width="2dp" android:color="?attr/appLine" />`.
`bg_radio_on.xml` (layer-list: accent ring + centered accent dot):
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="oval">
            <solid android:color="@android:color/transparent" />
            <stroke android:width="2dp" android:color="?attr/appAccent" />
        </shape>
    </item>
    <item android:width="11dp" android:height="11dp" android:gravity="center">
        <shape android:shape="oval">
            <solid android:color="?attr/appAccent" />
        </shape>
    </item>
</layer-list>
```

- [ ] **Step 6: Create grid / gear / rank badges**

`bg_grid.xml`: `<solid android:color="?attr/appSurface" />` + `<corners android:radius="18dp" />` + `<stroke android:width="1dp" android:color="?attr/appLine" />`.
`bg_gear.xml`: `oval`, `<solid android:color="?attr/appSurface" />` + `<stroke android:width="1dp" android:color="?attr/appLine" />`.
`bg_rank_badge.xml`: `<solid android:color="?attr/appStepBg" />` + `<corners android:radius="99dp" />`.
`bg_rank_badge_lead.xml`: `<solid android:color="?attr/appRing" />` + `<corners android:radius="99dp" />`.

- [ ] **Step 7: Create progress track drawable**

`progress_track.xml` (layer-list: track + clipped accent fill):
```xml
<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:id="@android:id/background">
        <shape android:shape="rectangle">
            <solid android:color="?attr/appTrack" />
            <corners android:radius="99dp" />
        </shape>
    </item>
    <item android:id="@android:id/progress">
        <clip>
            <shape android:shape="rectangle">
                <solid android:color="?attr/appAccent" />
                <corners android:radius="99dp" />
            </shape>
        </clip>
    </item>
</layer-list>
```

- [ ] **Step 8: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 9: Commit**
```bash
git add app/src/main/res/drawable/bg_*.xml app/src/main/res/drawable/progress_track.xml
git commit -m "feat(ui): add redesign shape drawables"
```

---

## Task 6: Vector icons

**Files:**
- Create under `app/src/main/res/drawable/`: `ic_spade.xml`, `ic_gear.xml`, `ic_back.xml`, `ic_crown.xml`, `ic_theme_system.xml`, `ic_sun.xml`, `ic_moon.xml`, `ic_github.xml`, `ic_card_fan.xml`

All icons use `#FF000000` for fill/stroke and are **tinted at the call site** via `android:tint="?attr/…"`. Pattern for a filled icon:

- [ ] **Step 1: `ic_spade.xml`** (filled)
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000"
        android:pathData="M12 3C12 3 4 9 4 14c0 2.8 2.2 4.6 4.6 4.6 1.2 0 2.3-.5 3-1.4-.2 2-.9 3.6-2.6 4.8h6c-1.7-1.2-2.4-2.8-2.6-4.8 .7 .9 1.8 1.4 3 1.4C17.8 18.6 20 16.8 20 14 20 9 12 3 12 3Z"/>
</vector>
```

- [ ] **Step 2: `ic_crown.xml`** (filled, from mockup)
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:fillColor="#FF000000" android:pathData="M3 7l4.5 4L12 4l4.5 7L21 7v11H3z"/>
</vector>
```

- [ ] **Step 3: Stroke icons** — `ic_gear`, `ic_back`, `ic_theme_system`, `ic_sun`, `ic_moon`

Stroke-icon template (use `android:strokeColor`, `android:strokeWidth="2"`, `android:strokeLineCap="round"`, `android:strokeLineJoin="round"`, no fill). Path data per icon:
- `ic_back`: two paths `M19 12H5` and `M12 19l-7-7 7-7`.
- `ic_gear`: `M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z` plus a `<path>` for the center circle `M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0z`.
- `ic_theme_system`: `M21 6a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2z` (screen) + `M8 20h8` + `M12 16v4`.
- `ic_sun`: circle `M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0z` + rays `M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4`.
- `ic_moon`: `M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z`.

Example (`ic_back.xml`):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">
    <path android:strokeColor="#FF000000" android:strokeWidth="2" android:strokeLineCap="round"
        android:strokeLineJoin="round" android:pathData="M19 12H5"/>
    <path android:strokeColor="#FF000000" android:strokeWidth="2" android:strokeLineCap="round"
        android:strokeLineJoin="round" android:pathData="M12 19l-7-7 7-7"/>
</vector>
```

- [ ] **Step 4: `ic_github.xml`** (filled, viewport 16)
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="16dp" android:height="16dp" android:viewportWidth="16" android:viewportHeight="16">
    <path android:fillColor="#FF000000" android:pathData="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z"/>
</vector>
```

- [ ] **Step 5: `ic_card_fan.xml`** — decorative 3-card fan

Reproduce the exact `<svg class="fan">` from `docs/mockups/spadesscore-ui-redesign.html` (mockup screen 3, the three `<g transform=…>` card groups). Use viewport `160x120`, three `<group>`s with `android:rotation` + `android:pivotX/pivotY` matching the SVG's `rotate(-15)`/`0`/`rotate(15)`, each containing a white rounded-rect path (corners ~8) with stroke `#FFE2E2DE`, plus the rank letter and suit glyph as `<path>`/text-as-path shapes. Card faces `#FFFFFFFF`; the heart card uses `#FFC0392B`, the club/spade cards `#FF17171A`. Static illustration — no theming needed (it sits on the screen background in both modes per the mockup).

- [ ] **Step 6: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**
```bash
git add app/src/main/res/drawable/ic_*.xml
git commit -m "feat(ui): add redesign vector icons (spade, gear, back, crown, theme, github, card fan)"
```

---

# Phase 2 — Theme Persistence

## Task 7: ThemeMode enum + pure storage mapping (TDD)

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/domain/model/ThemeMode.kt`
- Test: `app/src/test/java/com/nwe/spadesscore/domain/ThemeModeTest.kt`

- [ ] **Step 1: Write the failing test**

`ThemeModeTest.kt`:
```kotlin
package com.nwe.spadesscore.domain

import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.domain.model.themeModeFromStorage
import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {
    @Test fun defaultsToSystemForNull() {
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage(null))
    }
    @Test fun defaultsToSystemForGarbage() {
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage("nope"))
    }
    @Test fun parsesEachKnownName() {
        assertEquals(ThemeMode.LIGHT, themeModeFromStorage("LIGHT"))
        assertEquals(ThemeMode.DARK, themeModeFromStorage("DARK"))
        assertEquals(ThemeMode.SYSTEM, themeModeFromStorage("SYSTEM"))
    }
    @Test fun roundTripsThroughName() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, themeModeFromStorage(mode.name))
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.nwe.spadesscore.domain.ThemeModeTest"`
Expected: FAIL — `ThemeMode` / `themeModeFromStorage` unresolved.

- [ ] **Step 3: Write minimal implementation**

`ThemeMode.kt`:
```kotlin
package com.nwe.spadesscore.domain.model

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Pure parse of a persisted theme-mode string; unknown/null -> SYSTEM. */
fun themeModeFromStorage(value: String?): ThemeMode =
    ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.nwe.spadesscore.domain.ThemeModeTest"`
Expected: PASS (4 tests).

- [ ] **Step 5: Commit**
```bash
git add app/src/main/java/com/nwe/spadesscore/domain/model/ThemeMode.kt app/src/test/java/com/nwe/spadesscore/domain/ThemeModeTest.kt
git commit -m "feat(theme): add ThemeMode enum + pure storage parsing (TDD)"
```

---

## Task 8: ThemePreferences + theme helper + DI wiring + apply on startup

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/data/ThemePreferences.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/theme.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/di/AppContainer.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/SpadesApplication.kt`

- [ ] **Step 1: ThemePreferences (SharedPreferences wrapper)**

`ThemePreferences.kt`:
```kotlin
package com.nwe.spadesscore.data

import android.content.Context
import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.domain.model.themeModeFromStorage

/** Persists the global UI theme choice (not game state). */
class ThemePreferences(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences("spades_ui_prefs", Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() = themeModeFromStorage(prefs.getString(KEY_THEME, null))
        set(value) { prefs.edit().putString(KEY_THEME, value.name).apply() }

    private companion object { const val KEY_THEME = "theme_mode" }
}
```

- [ ] **Step 2: theme.kt helper (mode→nightMode + Context extension)**

`ui/theme.kt`:
```kotlin
package com.nwe.spadesscore.ui

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.nwe.spadesscore.SpadesApplication
import com.nwe.spadesscore.data.ThemePreferences
import com.nwe.spadesscore.domain.model.ThemeMode

val Context.themePreferences: ThemePreferences
    get() = (applicationContext as SpadesApplication).container.themePreferences

fun ThemeMode.toNightMode(): Int = when (this) {
    ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
    ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
}

/** Apply a theme choice globally and persist it. */
fun Context.applyThemeMode(mode: ThemeMode) {
    themePreferences.themeMode = mode
    AppCompatDelegate.setDefaultNightMode(mode.toNightMode())
}
```

- [ ] **Step 3: Expose ThemePreferences from AppContainer**

In `AppContainer.kt` add (constructor already receives `context`):
```kotlin
val themePreferences: ThemePreferences = ThemePreferences(context)
```
(Add `import com.nwe.spadesscore.data.ThemePreferences`.)

- [ ] **Step 4: Apply persisted theme at startup**

In `SpadesApplication.onCreate()`, after `container = AppContainer(this)`:
```kotlin
AppCompatDelegate.setDefaultNightMode(container.themePreferences.themeMode.toNightMode())
```
(Add imports `androidx.appcompat.app.AppCompatDelegate` and `com.nwe.spadesscore.ui.toNightMode`.)

- [ ] **Step 5: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**
```bash
git add app/src/main/java/com/nwe/spadesscore/data/ThemePreferences.kt app/src/main/java/com/nwe/spadesscore/ui/theme.kt app/src/main/java/com/nwe/spadesscore/di/AppContainer.kt app/src/main/java/com/nwe/spadesscore/SpadesApplication.kt
git commit -m "feat(theme): persist theme choice and apply on startup"
```

---

# Phase 3 — Settings Screen

## Task 9: Settings strings (EN/DE)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add new EN strings** to `values/strings.xml`:
```xml
<string name="tagline">Call your tricks. Keep the score.</string>
<string name="settings_title">Settings</string>
<string name="settings_appearance">Appearance</string>
<string name="theme_system">System</string>
<string name="theme_light">Light</string>
<string name="theme_dark">Dark</string>
<string name="cards_per_player">Cards per player</string>
<string name="declare_tricks_subtitle">Call tricks</string>
<string name="deal_subtitle">%s deals</string>
<string name="tricks_sum_warning">Sum must differ from %d</string>
<string name="round_progress">Round %1$d / %2$d</string>
<string name="final_score">Final score</string>
<string name="rounds_played">%d rounds played</string>
<string name="source_on_github">Source on GitHub</string>
<string name="open_source_built_with_love">Open source · built with ❤</string>
<string name="next_round">Next round</string>
<string name="continue_label">Continue</string>
<string name="wordmark">SPADES</string>
<string name="github_url" translatable="false">https://github.com/Blanco27/SpadesScore</string>
```

- [ ] **Step 2: Add matching DE strings** to `values-de/strings.xml`:
```xml
<string name="tagline">Stiche ansagen. Punkte behalten.</string>
<string name="settings_title">Einstellungen</string>
<string name="settings_appearance">Darstellung</string>
<string name="theme_system">System</string>
<string name="theme_light">Hell</string>
<string name="theme_dark">Dunkel</string>
<string name="cards_per_player">Karten pro Spieler</string>
<string name="declare_tricks_subtitle">Stiche ansagen</string>
<string name="deal_subtitle">%s teilt aus</string>
<string name="tricks_sum_warning">Summe muss von %d abweichen</string>
<string name="round_progress">Runde %1$d / %2$d</string>
<string name="final_score">Endstand</string>
<string name="rounds_played">%d Runden gespielt</string>
<string name="source_on_github">Quellcode auf GitHub</string>
<string name="open_source_built_with_love">Open Source · mit ❤ gebaut</string>
<string name="next_round">Nächste Runde</string>
<string name="continue_label">Weiter</string>
<string name="wordmark">SPADES</string>
```
(`github_url` is `translatable="false"`, defined once in `values/`.)

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(i18n): add redesign + settings strings (EN/DE)"
```

---

## Task 10: SettingsUiState + SettingsViewModel

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/settings/SettingsUiState.kt`
- Create: `app/src/main/java/com/nwe/spadesscore/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: SettingsUiState**
```kotlin
package com.nwe.spadesscore.ui.settings

import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode

data class SettingsUiState(
    val language: Language,
    val themeMode: ThemeMode,
)
```

- [ ] **Step 2: SettingsViewModel**
```kotlin
package com.nwe.spadesscore.ui.settings

import androidx.lifecycle.ViewModel
import com.nwe.spadesscore.data.GameRepository
import com.nwe.spadesscore.data.ThemePreferences
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode

class SettingsViewModel(
    private val repository: GameRepository,
    private val themePreferences: ThemePreferences,
) : ViewModel() {

    fun uiState(): SettingsUiState = SettingsUiState(
        language = repository.state.value.language,
        themeMode = themePreferences.themeMode,
    )

    fun setLanguage(language: Language) = repository.setLanguage(language)

    fun currentLanguage(): Language = repository.state.value.language
}
```
(Theme persistence is applied via `Context.applyThemeMode` in the Activity so the night mode is set globally; the ViewModel only exposes current values + forwards language.)

- [ ] **Step 3: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nwe/spadesscore/ui/settings/
git commit -m "feat(settings): add SettingsViewModel + UiState"
```

---

## Task 11: activity_settings.xml

**Files:**
- Create: `app/src/main/res/layout/activity_settings.xml`

Target = mockup screen 7. Root `ScrollView` (bg `?attr/appBg`) → vertical `LinearLayout`, padded 17dp/15dp.

- [ ] **Step 1: Build the layout**

Structure (use the styles/drawables from Phase 1; tint icons via `app:tint`/`android:tint`):
- **AppBar row** (`LinearLayout` horizontal, center-vertical): `ImageButton @id/back_button` (`ic_back`, tint `?attr/appInk`, background `?selectableItemBackgroundBorderless`) + `TextView` `@string/settings_title`, `TextAppearance.Spades.ScreenTitle`.
- **Section label** `@string/settings_appearance`, `TextAppearance.Spades.Label`, margins `16dp/4dp`.
- **Theme segment** (`LinearLayout` horizontal, background `@drawable/bg_segment`, padding 5dp, `@id/theme_segment`): three equal-weight vertical option cells `@id/theme_system`, `@id/theme_light`, `@id/theme_dark`, each an `ImageView` (`ic_theme_system`/`ic_sun`/`ic_moon`) + `TextView` (`@string/theme_system`/`theme_light`/`theme_dark`, `TextAppearance.Spades.PlayerName`). Selected cell gets `@drawable/bg_segment_selected` + accent tint (set in Activity).
- **Section label** `@string/language`.
- **Language panel** (`LinearLayout` vertical, `@drawable/bg_card`): two rows `@id/lang_en`, `@id/lang_de`, each horizontal: a tag `TextView` ("EN"/"DE", Grotesk bold, `@drawable/bg_step` background), a name `TextView` ("English"/"Deutsch", `TextAppearance.Spades.PlayerName`, weight 1), and a radio `View @id/radio_en`/`@id/radio_de` (21dp, `@drawable/bg_radio`). Add a 1dp `?attr/appLine` divider between rows.
- **Quiet footer** (`LinearLayout` vertical, center-horizontal, top margin large): `ImageView` `ic_spade` (tint `?attr/appSpade`, ~46dp on a dark rounded tile — use `@drawable/bg_card`-style or a dedicated rounded bg), `TextView` "SpadesScore" + version (filled in Activity from `BuildConfig.VERSION_NAME`), a GitHub link row (`@id/github_link`: `ic_github` tint `?attr/appAccent` + `@string/source_on_github` in accent), and `@string/open_source_built_with_love` (`TextAppearance.Spades.Body`).

- [ ] **Step 2: Build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/layout/activity_settings.xml
git commit -m "feat(settings): add activity_settings layout"
```

---

## Task 12: SettingsActivity

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/SettingsActivity.kt`
- Modify: `app/src/main/AndroidManifest.xml` (register the activity)

- [ ] **Step 1: Register in manifest**

Add inside `<application>`:
```xml
<activity android:name=".SettingsActivity" android:exported="false" />
```

- [ ] **Step 2: Implement SettingsActivity** (extends `AppCompatActivity` directly — back allowed, like `ResultScreenActivity`)
```kotlin
package com.nwe.spadesscore

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.activity.viewModels
import com.nwe.spadesscore.domain.model.Language
import com.nwe.spadesscore.domain.model.ThemeMode
import com.nwe.spadesscore.ui.applyPersistedLocale
import com.nwe.spadesscore.ui.applyThemeMode
import com.nwe.spadesscore.ui.gameRepository
import com.nwe.spadesscore.ui.settings.SettingsViewModel
import com.nwe.spadesscore.ui.themePreferences
import java.util.Locale

class SettingsActivity : AppCompatActivity() {

    private val viewModel: SettingsViewModel by viewModels {
        viewModelFactory { initializer { SettingsViewModel(gameRepository, themePreferences) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        applyPersistedLocale()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageView>(R.id.back_button).setOnClickListener { finish() }

        bindThemeSegment()
        bindLanguageRows()
        bindFooter()
    }

    private fun bindThemeSegment() {
        val map = mapOf(
            R.id.theme_system to ThemeMode.SYSTEM,
            R.id.theme_light to ThemeMode.LIGHT,
            R.id.theme_dark to ThemeMode.DARK,
        )
        renderThemeSelection(viewModel.uiState().themeMode)
        map.forEach { (id, mode) ->
            findViewById<android.view.View>(id).setOnClickListener {
                applyThemeMode(mode)   // persists + setDefaultNightMode -> triggers recreate
            }
        }
    }

    private fun renderThemeSelection(selected: ThemeMode) {
        val ids = mapOf(
            ThemeMode.SYSTEM to R.id.theme_system,
            ThemeMode.LIGHT to R.id.theme_light,
            ThemeMode.DARK to R.id.theme_dark,
        )
        ids.forEach { (mode, id) ->
            val cell = findViewById<android.view.View>(id)
            cell.background = if (mode == selected)
                ContextCompat.getDrawable(this, R.drawable.bg_segment_selected) else null
        }
    }

    private fun bindLanguageRows() {
        renderLanguageSelection(viewModel.currentLanguage())
        findViewById<android.view.View>(R.id.lang_en).setOnClickListener { switchLanguage(Language.ENGLISH, "en") }
        findViewById<android.view.View>(R.id.lang_de).setOnClickListener { switchLanguage(Language.GERMAN, "de") }
    }

    private fun renderLanguageSelection(language: Language) {
        findViewById<android.view.View>(R.id.radio_en).isSelected = language == Language.ENGLISH
        findViewById<android.view.View>(R.id.radio_de).isSelected = language == Language.GERMAN
        findViewById<android.view.View>(R.id.radio_en).setBackgroundResource(
            if (language == Language.ENGLISH) R.drawable.bg_radio_on else R.drawable.bg_radio)
        findViewById<android.view.View>(R.id.radio_de).setBackgroundResource(
            if (language == Language.GERMAN) R.drawable.bg_radio_on else R.drawable.bg_radio)
    }

    private fun switchLanguage(language: Language, code: String) {
        if (viewModel.currentLanguage() == language) return
        viewModel.setLanguage(language)
        val locale = Locale(code)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
        recreate()
    }

    private fun bindFooter() {
        findViewById<TextView>(R.id.version_label).text =
            getString(R.string.app_name) + "  v" + BuildConfig.VERSION_NAME
        findViewById<android.view.View>(R.id.github_link).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.github_url))))
        }
    }
}
```
> `bg_radio_on` (Task 5) already draws the accent ring + centered dot. `version_label` must exist in `activity_settings.xml` (Task 11) with `@id/version_label` — add it if missing.

- [ ] **Step 3: Build + run smoke check**

Run: `./gradlew assembleDebug` → BUILD SUCCESSFUL.
Manual: launch app, tap ⚙ (added in Task 13) → Settings opens; toggling System/Light/Dark recolors the whole app and survives relaunch; switching language recreates in the chosen language; GitHub link opens browser; back returns to Start.

- [ ] **Step 4: Commit**
```bash
git add app/src/main/java/com/nwe/spadesscore/SettingsActivity.kt app/src/main/AndroidManifest.xml
git commit -m "feat(settings): add SettingsActivity (theme switch, language, about)"
```

---

# Phase 4 — Reskin Screens

> For every screen task: rebuild the layout to match its mockup screen using Phase-1 styles/attrs/drawables, keep the existing view IDs the Activity references (or update the Activity in lockstep when adding IDs), then **build + lint + visually compare to the named mockup screen in `docs/mockups/spadesscore-ui-redesign.html`**, then commit. The data needed by every new decorative element (round, totals, scores, names, predictions, placements) is already present in each screen's `UiState` (verified).

## Task 13: Start screen (`activity_main.xml` + `MainActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_main.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/MainActivity.kt`

Target = mockup screens 1 (light) / 8 (dark).

- [ ] **Step 1: Rebuild `activity_main.xml`**

Root `LinearLayout` vertical, bg `?attr/appBg`, padded. Contents top→bottom:
- **Start bar:** `ImageButton @id/settings_gear`, `@drawable/bg_gear` background, `ic_gear` (tint `?attr/appMuted`), aligned end.
- Spacer (weight).
- **Hero:** `ImageView` `ic_spade` (tint `?attr/appSpade`, ~62dp) + `TextView @string/wordmark` (`TextAppearance.Spades.Wordmark`) + `TextView @string/tagline` (`TextAppearance.Spades.Body`), all centered.
- Spacer (weight).
- **Label** `@string/number_of_players` (`TextAppearance.Spades.Label`).
- **Segment** `LinearLayout` horizontal, `@drawable/bg_segment`, padding 4dp: two equal-weight `TextView`s `@id/btn3Players` / `@id/btn4Players` (`@string/three`/`@string/four`, Grotesk semibold), selected gets `@drawable/bg_segment_selected` + accent text.
- Spacer (weight).
- **CTA** `Button @id/start_game_button`, `@drawable/bg_cta`, text `@string/next`, `textColor ?attr/appCtaFg`, Grotesk semibold; remove `app:backgroundTint`.
- **Remove** the language buttons + their `LinearLayout`.

- [ ] **Step 2: Update `MainActivity.kt`**

- Remove `btnLanguageEnglish`/`btnLanguageGerman` fields, their `findViewById`s, their click listeners, and the `setLocale`/`recreate` block in `onCreate` and the language branch in `setupUI`.
- Replace the `GradientDrawable.setColor` player-count animation with **segment selection**: on 3/4 tap call `viewModel.setPlayerCount(n)` and swap each cell's background between `@drawable/bg_segment_selected` (selected, text `?attr/appAccent`) and `null`/transparent (text `?attr/appMuted`). Use `setBackgroundResource(...)` + `setTextColor(MaterialColors.getColor(view, R.attr.appAccent/appMuted))`.
- Add `findViewById<ImageButton>(R.id.settings_gear).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }`.
- Keep `start_game_button` → `PlayerNamesActivity`.

- [ ] **Step 3: Build + lint**

Run: `./gradlew assembleDebug && ./gradlew lint` → SUCCESSFUL, no new errors.

- [ ] **Step 4: Visual check**

Launch; compare Start to mockup screens 1 & 8 (Light/Dark). Verify gear, hero ♠, wordmark, tagline, 3/4 segment, ink/gold CTA.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_main.xml app/src/main/java/com/nwe/spadesscore/MainActivity.kt
git commit -m "feat(ui): reskin Start screen (gear, hero, segment, CTA); move language to Settings"
```

---

## Task 14: Player names (`activity_player_names.xml` + `PlayerNamesActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_player_names.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.kt` (only if IDs change)

Target = mockup screen 2.

- [ ] **Step 1: Rebuild the layout**

Root vertical `LinearLayout`, bg `?attr/appBg`, padded. Contents:
- `TextView` `@string/set_player_names` (`TextAppearance.Spades.ScreenTitle`, left).
- 4 **field cards** (`@drawable/bg_field`), each a vertical `LinearLayout`: a `TextView` label "Spieler N" (`TextAppearance.Spades.Label`) + the existing `EditText` (keep its current id, e.g. `@id/etPlayer1…4`; Grotesk medium 16sp, transparent background, no underline). Preserve the existing EditText IDs so the Activity keeps working. Keep the 4th field's id and have the Activity hide it when `playerCount == 3` (existing behavior).
- **Switch row** (`@id` of existing switch preserved): `TextView` "Zufälliger erster Dealer" (Grotesk medium) + `SwitchMaterial`/`Switch` styled with thumb white + track `?attr/appAccent`/`?attr/appTrack`.
- Spacer.
- **CTA** `@id/start_game_button` (keep id), `@drawable/bg_cta`, `@string/next`.
- Apply focus styling: set `bg_field_focus` on focus via an `OnFocusChangeListener` (add in Activity) — optional polish to match the focused field in the mockup.

- [ ] **Step 2: Reconcile IDs**

Open `PlayerNamesActivity.kt`; if any referenced id changed, update it. Prefer preserving original ids to minimize churn. Keep the 3-player hide-player-4 logic.

- [ ] **Step 3: Build + lint** → SUCCESSFUL.

- [ ] **Step 4: Visual check** vs mockup screen 2 (fields, focus ring, switch, CTA). Test 3- and 4-player.

- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_player_names.xml app/src/main/java/com/nwe/spadesscore/PlayerNamesActivity.kt
git commit -m "feat(ui): reskin Player names screen (field cards, switch, CTA)"
```

---

## Task 15: TrickSpinner restyle (`view_trick_spinner.xml`)

**Files:**
- Modify: `app/src/main/res/layout/view_trick_spinner.xml`

Target = stepper in mockup screen 4 (`.step2`).

- [ ] **Step 1: Restyle the stepper**

Keep the three child ids `btnMinus`, `tvValue`, `btnPlus` (referenced by `TrickSpinner.kt` via `ViewTrickSpinnerBinding`). Wrap them in a horizontal container with `@drawable/bg_step` background, padding 6dp/13dp: `btnMinus` "−" and `btnPlus` "+" as Grotesk-semibold `TextView`/`Button` in `?attr/appMuted`; `tvValue` centered, `TextAppearance.Spades.Number` size 18sp in `?attr/appAccent`. Remove old spinner background drawables usage.

- [ ] **Step 2: Build** → SUCCESSFUL (binding ids unchanged).

- [ ] **Step 3: Commit**
```bash
git add app/src/main/res/layout/view_trick_spinner.xml
git commit -m "feat(ui): restyle TrickSpinner stepper to step_bg/accent"
```

---

## Task 16: Deal cards (`activity_deal_cards.xml` + `DealCardsActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_deal_cards.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/DealCardsActivity.kt`

Target = mockup screen 3. Data from `DealCardsUiState` (round, dealerName, cardAmount, players[name,score], amountOfRounds).

- [ ] **Step 1: Rebuild the layout**

Root vertical `LinearLayout`, bg `?attr/appBg`, padded. Contents:
- **Brand row** (horizontal, space-between): `♠ Spades` (`ic_spade` tint `?attr/appSpade` + `@string/app_name`, Grotesk bold) and `@id/round_progress` `TextView` (`TextAppearance.Spades.Body`).
- **Score row** (horizontal, 4 equal columns) `@id/score_row`: per player a vertical Name (Inter, `?attr/appMuted`) + Score (`TextAppearance.Spades.Number` 13sp). Build the columns in the layout with ids `@id/score_name_1..4` / `@id/score_value_1..4`, or inflate dynamically in the Activity.
- Spacer.
- **Title** `@id/round_title` (`TextAppearance.Spades.Title`) — "Runde N" with the number in `?attr/appAccent` (use a `SpannableString` in the Activity, or two TextViews).
- **Progress** `ProgressBar` `@id/round_progress_bar` style `?android:attr/progressBarStyleHorizontal`, `progressDrawable=@drawable/progress_track`, height 6dp; `max=amountOfRounds`, `progress=round`.
- **Subtitle** `@id/dealer_subtitle` (`TextAppearance.Spades.Body`, centered) = `getString(R.string.deal_subtitle, dealerName)`.
- Spacer.
- **Deal illustration**: `ImageView` `ic_card_fan` + `@id/card_count` (`TextAppearance.Spades.Number` 40sp, `?attr/appAccent`) + `@string/cards_per_player` (`TextAppearance.Spades.Body`).
- Spacer.
- **CTA** `@id/deal_next_button` (keep existing id if present), `@drawable/bg_cta`, text `@string/continue_label` ("Weiter"/"Continue", per mockup).

- [ ] **Step 2: Wire `DealCardsActivity.kt`**

Bind: `round_progress` = `getString(R.string.round_progress, state.round, state.amountOfRounds)`; populate score columns from `state.players`; `round_title` from `state.round`; `round_progress_bar.max/progress`; `dealer_subtitle`; `card_count` = `state.cardAmount`. Hide the 4th score column when `players.size == 3`. Preserve the existing next-button navigation.

- [ ] **Step 3: Build + lint** → SUCCESSFUL.
- [ ] **Step 4: Visual check** vs mockup screen 3 (3- and 4-player).
- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_deal_cards.xml app/src/main/java/com/nwe/spadesscore/DealCardsActivity.kt
git commit -m "feat(ui): reskin Deal screen (brand/score row, progress, card fan)"
```

---

## Task 17: Declare tricks (`activity_declare_tricks.xml` + `DeclareTricksActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_declare_tricks.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt`

Target = mockup screens 4 (light) / 9 (dark, locked). Data from `DeclareTricksUiState`.

- [ ] **Step 1: Rebuild the layout**

Same header block as Deal (brand row, score row, title, progress) + subtitle `@string/declare_tricks_subtitle`. Then per player a **card row** (`@drawable/bg_card`, horizontal): name (`TextAppearance.Spades.PlayerName`, weight 1) + a `TrickSpinner`. Below the rows: a **chip** `@id/sum_chip` (`@drawable/bg_chip`, centered, text via `@string/combined_trick_prediction` → "%d/%d %s") + a `@id/warn_subtitle` `TextView` (`@string/tricks_sum_warning`, `?attr/appWarn`, hidden by default). Then **CTA** `@id/confirm_button`.

- [ ] **Step 2: Extend the lock logic in `DeclareTricksActivity.kt`**

The activity already disables the start/confirm button while `sum == cardAmount`. Extend that same condition to:
- Set chip background `@drawable/bg_chip_warn` and chip text color `?attr/appWarn` when locked, else `@drawable/bg_chip` and `?attr/appAccent`.
- Show `@id/warn_subtitle` (text `getString(R.string.tricks_sum_warning, cardAmount)`) when locked, else `GONE`.
- Set CTA background `@drawable/bg_cta_disabled` + disabled when locked, `@drawable/bg_cta` + enabled otherwise.
Keep the existing shake/animation transition.

- [ ] **Step 3: Build + lint** → SUCCESSFUL.
- [ ] **Step 4: Visual check** vs mockup screens 4 & 9 — verify the locked state (warn chip + warn subtitle + disabled CTA) by setting bids to sum == tricks.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_declare_tricks.xml app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt
git commit -m "feat(ui): reskin Declare screen with chip + locked warn state"
```

---

## Task 18: Confirm tricks (`activity_confirm_tricks.xml` + `ConfirmTicksActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_confirm_tricks.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt`

Target = mockup screen 5. Data from `ConfirmTicksUiState` (players: name, score, prediction, pointsIfHit).

- [ ] **Step 1: Rebuild the layout**

Header block (brand, score, title, progress). Then per player a **card row** (`@drawable/bg_card`, horizontal): left a vertical (name `TextAppearance.Spades.PlayerName` + meta `TextAppearance.Spades.Body` = `"<prediction> Stiche · +<pointsIfHit> Punkte"` using `@string/tricks`/`@string/points_added`), right a **check** `TextView` (27dp, `gravity=center`, text `"✓"`, `textColor=?attr/appOnAccent`, background `@drawable/bg_check`). When checked: row background `?attr/appHitBg`, check background `@drawable/bg_check_on` and its `"✓"` shown (set text empty/invisible when unchecked), points text `?attr/appAccent`. Then **CTA** `@id/next_round_button`, text `@string/next_round`.

- [ ] **Step 2: Wire `ConfirmTicksActivity.kt`**

Populate rows from `state.players`; toggle the hit state on tap (the activity already tracks who hit — reuse that; just swap backgrounds/check drawable and points color). Keep the existing scoring/navigation (to Deal or Result).

- [ ] **Step 3: Build + lint** → SUCCESSFUL.
- [ ] **Step 4: Visual check** vs mockup screen 5 (hit vs miss rows).
- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_confirm_tricks.xml app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt
git commit -m "feat(ui): reskin Confirm screen (meta rows, round check, hit highlight)"
```

---

## Task 19: Result screen (`activity_result_screen.xml` + `ResultScreenActivity.kt`)

**Files:**
- Modify: `app/src/main/res/layout/activity_result_screen.xml`
- Modify: `app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt`

Target = mockup screens 6 (light) / 10 (dark). Data from `ResultUiState` (playerNames, scoresByPlayer, visibleRoundCount, placementByPlayer). **Keep `MAX_ROUNDS = 20` and the `resources.getIdentifier(...)` wiring** — only restyle cells and add rank badges.

- [ ] **Step 1: Restyle the grid layout**

Wrap the existing score grid in a **grid card** (`@drawable/bg_grid`). Header row = player names (Grotesk semibold, `?attr/appInk`). Round rows = `score_playerN_roundM` cells (Grotesk medium, `?attr/appMuted`; last visible row `?attr/appInk`). Add a **totals row** with large `TextAppearance.Spades.Number` (27sp) per player and, beneath each total, a **rank badge** container `@id/rank_badge_1..4` (`@drawable/bg_rank_badge`, text "N." Grotesk bold). Keep all existing cell ids and the `getIdentifier` loops. Add header `@id/result_title` (`@string/final_score`, `TextAppearance.Spades.Headline`) + `@id/result_subtitle` (`@string/rounds_played`). CTA `@id/result_next_button` text `@string/continue_label`.

- [ ] **Step 2: Wire rank badges in `ResultScreenActivity.kt`**

For each player index, read `placementByPlayer[index]`; set badge text to `"$placement."`. For placement 1, use `@drawable/bg_rank_badge_lead`, accent total color (`?attr/appAccent`), and show the `ic_crown` inside the badge (`ImageView` `@id/crown_1..4`, visible only for the leader). Others use `@drawable/bg_rank_badge` and `?attr/appInk` totals. Keep the existing `MAX_ROUNDS`/`getIdentifier` logic and the halftime/continue + game-over navigation.

- [ ] **Step 3: Build + lint** → SUCCESSFUL.
- [ ] **Step 4: Visual check** vs mockup screens 6 & 10 — totals, rank badges, crown on leader, 3- and 4-player, and the second-half (20-round) layout.
- [ ] **Step 5: Commit**
```bash
git add app/src/main/res/layout/activity_result_screen.xml app/src/main/java/com/nwe/spadesscore/ResultScreenActivity.kt
git commit -m "feat(ui): reskin Result screen (grid card, totals, rank badges + crown)"
```

---

# Phase 5 — Launcher Icon (Variante B · Paper Indigo)

## Task 20: Adaptive launcher icon

**Files:**
- Modify: `app/src/main/res/values/ic_launcher_background.xml` (→ Paper)
- Create/Modify: `app/src/main/res/drawable/ic_launcher_foreground.xml` (Indigo ♠)
- Verify: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`
- Replace: legacy `mipmap-*/ic_launcher.png`, `ic_launcher_round.png`

- [ ] **Step 1: Set the background color to Paper**

`values/ic_launcher_background.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="ic_launcher_background">#F6F6F4</color>
</resources>
```

- [ ] **Step 2: Foreground = Indigo spade centered in the safe zone**

`drawable/ic_launcher_foreground.xml` (108dp viewport; spade ~ half the canvas, centered):
```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp" android:height="108dp" android:viewportWidth="108" android:viewportHeight="108">
    <group android:scaleX="2.6" android:scaleY="2.6" android:translateX="22" android:translateY="22">
        <path android:fillColor="#4F46E5"
            android:pathData="M12 3C12 3 4 9 4 14c0 2.8 2.2 4.6 4.6 4.6 1.2 0 2.3-.5 3-1.4-.2 2-.9 3.6-2.6 4.8h6c-1.7-1.2-2.4-2.8-2.6-4.8 .7 .9 1.8 1.4 3 1.4C17.8 18.6 20 16.8 20 14 20 9 12 3 12 3Z"/>
    </group>
</vector>
```
(Adjust `scale`/`translate` so the glyph sits within the 72dp safe zone — verify on device, mockup draft B.)

- [ ] **Step 3: Verify the adaptive XML references both layers**

`mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` should be:
```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```
(Optional: add `<monochrome android:drawable="@drawable/ic_spade" />` for Android-13 themed icons.)

- [ ] **Step 4: Regenerate legacy PNGs**

Replace the `mipmap-mdpi…xxxhdpi/ic_launcher.png` and `ic_launcher_round.png` with renders of the paper-tile + indigo-♠ (use Android Studio's Image Asset Studio, or pre-rendered PNGs at 48/72/96/144/192 px). Ensure all densities are updated.

- [ ] **Step 5: Build + install + eyeball**

Run: `./gradlew assembleDebug` → SUCCESSFUL. Install and confirm the home-screen icon = paper tile + indigo ♠ (draft B).

- [ ] **Step 6: Commit**
```bash
git add app/src/main/res/values/ic_launcher_background.xml app/src/main/res/drawable/ic_launcher_foreground.xml app/src/main/res/mipmap-anydpi-v26/ app/src/main/res/mipmap-*/
git commit -m "feat(ui): new launcher icon (Paper Indigo spade)"
```

---

# Phase 6 — Cleanup & Final Verification

## Task 21: Remove unused legacy assets

**Files:**
- Delete unreferenced drawables/fonts/colors (see list).

- [ ] **Step 1: Confirm each is unreferenced**

Run (Bash) `git grep` for each name to confirm zero references in `res/`/`java/`:
```bash
for n in spades_logo ace button_background option_button selector_option player_bg_rounded spinner_background spinner_button_background button_background_selector patrick_hand patrick_hand_sc comic_neue comic_neue_light roboto; do echo "== $n =="; git grep -n "$n" -- app/src/main || echo "  (none)"; done
```

- [ ] **Step 2: Delete the ones with no references**

Remove the corresponding files under `res/drawable/` and `res/font/`. Also remove now-unused legacy color names from `colors.xml`/`values-night/colors.xml` (e.g. `background_button_*`, `text_header`, `spinnerText`, `cyan*`, etc.) **only if** `git grep` shows no references.

- [ ] **Step 3: Build + lint**

Run: `./gradlew assembleDebug && ./gradlew lint`
Expected: BUILD SUCCESSFUL, no unresolved-resource errors.

- [ ] **Step 4: Commit**
```bash
git add -A
git commit -m "chore(ui): remove unused legacy drawables/fonts/colors"
```

---

## Task 22: Full verification pass (1:1 fidelity gate)

**Files:** none (verification only).

- [ ] **Step 1: Run the full test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS — `GameEngineTest`, `GameStateTest`, `NameValidationTest`, `ThemeModeTest` all green (proves logic untouched).

- [ ] **Step 2: Build + lint clean**

Run: `./gradlew assembleDebug && ./gradlew lint` → SUCCESSFUL.

- [ ] **Step 3: Visual 1:1 gate**

Use the `run` / `update-readme-screenshots` skill to capture each screen and compare to `docs/mockups/spadesscore-ui-redesign.html`:
- Screens 1–7 in **Light**, then in **Dark** (toggle via Settings) — verify palette, typography, components, spacing, CTA color (Ink light / Gold dark), locked/hit/rank states.
- Repeat key screens in **EN** and **DE**.

- [ ] **Step 4: Behavior checks**

- Theme System/Light/Dark persists across a full app kill + relaunch.
- Language switch from Settings persists + recreates correctly.
- Process-death persistence: kill the app mid-game, relaunch into the deep activity — round/scores/names restore in the chosen language + theme, no crash.

- [ ] **Step 5: Record results**

If any screen deviates from the mockup, note it and open a follow-up fix task (loop back to the relevant Phase-4 task). When all pass, the redesign meets the 1:1 gate.

- [ ] **Step 6: Finish the branch**

Use the `superpowers:finishing-a-development-branch` skill to decide merge/PR. (Do not auto-merge; present options.)

---

## Notes for the implementer
- Tint vector icons at the call site (`android:tint`/`app:tint` → `?attr/app…`); the vectors themselves use placeholder black.
- `MaterialColors.getColor(view, R.attr.appAccent)` resolves a theme attr to a color int in code (Material Components dependency already present).
- Don't reintroduce Compose. Don't change game rules, scoring, round counts, or Room schema.
- Keep `values/` = English, `values-de/` = German; never hardcode user-facing strings in layouts.
