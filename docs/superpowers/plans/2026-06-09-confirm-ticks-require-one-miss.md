# Confirm-Ticks: Require One Miss — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent confirming a round where every visible player is marked as a hit — at least one player must remain a miss — by locking the "Next round" CTA, mirroring the existing DeclareTricks lock.

**Architecture:** Pure UI guard inside `ConfirmTicksActivity`, identical in feel to `DeclareTricksActivity`'s sum-lock. The two shared animation helpers (`animateFill`, shake) are first extracted into a small `ui/animations.kt` so both screens use one copy. No domain/engine/scoring changes.

**Tech Stack:** Kotlin, Android XML layouts, viewBinding-free `findViewById`, MaterialColors theme attrs, no Compose.

**Note on testing:** The lock logic lives in an `AppCompatActivity` and — exactly like the existing DeclareTricks lock — is not reachable from JVM unit tests. There is no domain function to TDD here (the spec deliberately keeps this inline, matching DeclareTricks). Verification is therefore: `testDebugUnitTest` stays green (no regression) + manual on-device check. No new unit test is added because there is no pure unit to test without contradicting the approved spec.

---

## File Structure

- **Create** `app/src/main/java/com/nwe/spadesscore/ui/animations.kt` — two shared view-animation helpers (`animateFill`, `View.shake()`).
- **Modify** `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt` — drop the two private animation methods, use the shared helpers.
- **Modify** `app/src/main/res/values/strings.xml` + `app/src/main/res/values-de/strings.xml` — add `confirm_all_hit_warning`.
- **Modify** `app/src/main/res/layout/activity_confirm_tricks.xml` — add a `warn_subtitle` TextView (gone by default) above the CTA.
- **Modify** `app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt` — resolve lock colors, add `updateConfirmLockState()`, drive the CTA lock.

---

### Task 1: Extract shared animation helpers

**Files:**
- Create: `app/src/main/java/com/nwe/spadesscore/ui/animations.kt`
- Modify: `app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt`

- [ ] **Step 1: Create `ui/animations.kt`**

```kotlin
package com.nwe.spadesscore.ui

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.View

/**
 * Animates the fill color of a view whose background is a [GradientDrawable]
 * (e.g. the CTA's bg_cta) from [fromColor] to [toColor] over 250 ms.
 */
fun animateFill(view: View, fromColor: Int, toColor: Int) {
    ValueAnimator.ofArgb(fromColor, toColor).apply {
        duration = 250
        addUpdateListener { animation ->
            (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
        }
        start()
    }
}

/** Horizontal shake — signals a locked/blocked CTA. */
fun View.shake() {
    ObjectAnimator.ofFloat(this, "translationX", 0f, 16f, -16f, 12f, -12f, 6f, -6f, 0f).apply {
        duration = 350
        start()
    }
}
```

- [ ] **Step 2: Remove the private helpers from `DeclareTricksActivity.kt`**

Delete the entire `// ── Animation helpers ──` section (the private `animateFill` and `shakeView` methods, currently lines ~163-180):

```kotlin
    // ── Animation helpers ──────────────────────────────────────────────────────

    private fun animateFill(view: View, fromColor: Int, toColor: Int) {
        ValueAnimator.ofArgb(fromColor, toColor).apply {
            duration = 250
            addUpdateListener { animation ->
                (view.background as GradientDrawable).setColor(animation.animatedValue as Int)
            }
            start()
        }
    }

    private fun shakeView(view: View) {
        ObjectAnimator.ofFloat(view, "translationX", 0f, 16f, -16f, 12f, -12f, 6f, -6f, 0f).apply {
            duration = 350
            start()
        }
    }
```

- [ ] **Step 3: Update the call site in `DeclareTricksActivity.updateCombinedTricksTextView()`**

Change `shakeView(startButton)` to `startButton.shake()`. The `animateFill(startButton, ...)` calls stay textually identical (now resolving to the top-level function). The locked branch becomes:

```kotlin
            if (lastTricksAreValid) {
                // Transition INTO locked: animate CTA background + shake.
                animateFill(startButton, colorActive, colorDeactive)
                startButton.shake()
            }
```

- [ ] **Step 4: Fix imports in `DeclareTricksActivity.kt`**

Remove the now-unused animation imports:

```kotlin
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
```

Add the shared-helper imports (alphabetical, next to the other `com.nwe.spadesscore.ui.*` imports):

```kotlin
import com.nwe.spadesscore.ui.animateFill
import com.nwe.spadesscore.ui.shake
```

Keep `import android.graphics.drawable.GradientDrawable` — it is still used in `setupUI()` (`(startButton.background as GradientDrawable).setColor(colorActive)`).

- [ ] **Step 5: Build to verify the refactor compiles and behaves the same**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL (no behavior change; DeclareTricks lock identical).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ui/animations.kt app/src/main/java/com/nwe/spadesscore/DeclareTricksActivity.kt
git commit -m "refactor(ui): extract shared CTA animation helpers"
```

---

### Task 2: Add the warning string (EN + DE)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add the English string**

In `app/src/main/res/values/strings.xml`, after the `tricks_sum_warning` line, add:

```xml
    <string name="confirm_all_hit_warning">At least one player must miss</string>
```

- [ ] **Step 2: Add the German string**

In `app/src/main/res/values-de/strings.xml`, after the `tricks_sum_warning` line, add:

```xml
    <string name="confirm_all_hit_warning">Mindestens einer muss daneben liegen</string>
```

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(i18n): add confirm-all-hit warning string"
```

---

### Task 3: Add the warn subtitle to the confirm layout

**Files:**
- Modify: `app/src/main/res/layout/activity_confirm_tricks.xml`

- [ ] **Step 1: Insert the `warn_subtitle` TextView between the flex `Space` and the CTA `Button`**

Find the existing flex spacer + button block (currently lines ~283-301):

```xml
    <!-- Flex spacer pushes CTA to bottom -->
    <Space
        android:layout_width="match_parent"
        android:layout_height="0dp"
        android:layout_weight="1"
        android:minHeight="16dp" />

    <!-- CTA button -->
    <Button
        android:id="@+id/start_Button"
```

Insert the warn subtitle **before** the `<Space ...>` (so it sits directly under the player rows, matching the DeclareTricks order rows → warn → spacer → CTA):

```xml
    <!-- ── Warn subtitle: shown when every visible player is marked as a hit ── -->
    <TextView
        android:id="@+id/warn_subtitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginTop="12dp"
        android:gravity="center"
        android:textAppearance="@style/TextAppearance.Spades.Body"
        android:textColor="?attr/appWarn"
        android:visibility="gone"
        tools:text="At least one player must miss" />

    <!-- Flex spacer pushes CTA to bottom -->
    <Space
```

- [ ] **Step 2: Build to verify the layout inflates**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/layout/activity_confirm_tricks.xml
git commit -m "feat(ui): add warn subtitle to confirm-ticks layout"
```

---

### Task 4: Drive the CTA lock in ConfirmTicksActivity

**Files:**
- Modify: `app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt`

- [ ] **Step 1: Add fields, imports, and lock-color resolution**

Add imports (next to the existing imports):

```kotlin
import android.widget.Button
import androidx.core.content.ContextCompat
import com.nwe.spadesscore.ui.animateFill
import com.nwe.spadesscore.ui.shake
```

Add a `warnSubtitle`/`startButton` reference and lock-color fields to the class body (alongside the existing view-list `lateinit` fields and `hitStates`):

```kotlin
    private lateinit var startButton: Button
    private lateinit var warnSubtitle: TextView

    // Colors resolved from theme in initializeUIComponents(); used for lock/unlock transitions.
    private var colorActive = 0      // appCtaBg  — CTA background when enabled
    private var colorDeactive = 0    // appStepBg — CTA background when locked
    private var ctaFgColor = 0       // appCtaFg  — CTA text color when enabled
    private var ctaMutedColor = 0    // appMuted  — CTA text color when locked

    /** True while the current selection is allowed (at least one visible player is a miss). */
    private var lastStateValid = true
```

- [ ] **Step 2: Resolve views + colors in `initializeUIComponents()`**

At the end of `initializeUIComponents()` (after `checkViews = listOf(...)`), add:

```kotlin
        startButton = findViewById(R.id.start_Button)
        warnSubtitle = findViewById(R.id.warn_subtitle)

        colorActive   = MaterialColors.getColor(startButton, R.attr.appCtaBg)
        colorDeactive = MaterialColors.getColor(startButton, R.attr.appStepBg)
        ctaFgColor    = MaterialColors.getColor(startButton, R.attr.appCtaFg)
        ctaMutedColor = MaterialColors.getColor(startButton, R.attr.appMuted)
```

- [ ] **Step 3: Set the CTA initial appearance and drop the local `findViewById` in `setupUI()`**

In `setupUI()`, replace the final line:

```kotlin
        findViewById<View>(R.id.start_Button).setOnClickListener { startNextRound() }
```

with (set initial fill + click on the cached field, then evaluate the lock once):

```kotlin
        (startButton.background as GradientDrawable).setColor(colorActive)
        startButton.setTextColor(ctaFgColor)
        startButton.setOnClickListener { startNextRound() }

        updateConfirmLockState()
```

Add the import for `GradientDrawable` is already present (`import android.graphics.drawable.GradientDrawable`).

- [ ] **Step 4: Re-evaluate the lock after every toggle**

In `toggleHit(index)`, append a call to the new updater so the CTA reacts to each tap:

```kotlin
    private fun toggleHit(index: Int) {
        hitStates[index] = !hitStates[index]
        applyHitVisuals(index, isHit = hitStates[index])
        updateConfirmLockState()
    }
```

- [ ] **Step 5: Add `updateConfirmLockState()`**

Add this method (place it after `toggleHit`, mirroring DeclareTricks' `updateCombinedTricksTextView`):

```kotlin
    /**
     * Locks the "Next round" CTA when EVERY visible player is marked as a hit:
     * the trick-declaration rule guarantees not everyone can hit, so at least one
     * player must remain a miss. Drives CTA enabled-state, background (animated),
     * text color, the warn subtitle, and a shake on the transition into locked.
     */
    private fun updateConfirmLockState() {
        val playerCount = viewModel.uiState().players.size
        val allHit = (0 until playerCount).all { hitStates[it] }
        val isValid = !allHit

        if (!isValid) {
            // ── LOCKED: everyone is a hit — impossible, force at least one miss ──
            warnSubtitle.text = getString(R.string.confirm_all_hit_warning)
            warnSubtitle.visibility = View.VISIBLE
            startButton.isEnabled = false
            startButton.setTextColor(ctaMutedColor)
            if (lastStateValid) {
                animateFill(startButton, colorActive, colorDeactive)
                startButton.shake()
            }
        } else {
            // ── OK: at least one miss — round can be confirmed ──
            warnSubtitle.visibility = View.GONE
            startButton.isEnabled = true
            startButton.setTextColor(ctaFgColor)
            if (!lastStateValid) {
                animateFill(startButton, colorDeactive, colorActive)
            }
        }
        lastStateValid = isValid
    }
```

- [ ] **Step 6: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/nwe/spadesscore/ConfirmTicksActivity.kt
git commit -m "feat(ui): lock confirm-ticks CTA until at least one player misses"
```

---

### Task 5: Verify (build + unit tests + manual)

**Files:** none (verification only)

- [ ] **Step 1: Run the unit tests (no regression)**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL — `GameEngineTest`, `GameStateTest`, `NameValidationTest`, `ThemeModeTest` all pass.

- [ ] **Step 2: Run lint**

Run: `.\gradlew.bat lint`
Expected: no new errors introduced by the changes.

- [ ] **Step 3: Manual check (4-player)**

Start a 4-player game, reach Confirm-Ticks. Check off all 4 players:
Expected: CTA greys out + shakes, warn subtitle "At least one player must miss" appears, button not clickable. Uncheck one: CTA animates back to active, warn subtitle hides, button clickable.

- [ ] **Step 4: Manual check (3-player)**

Repeat with a 3-player game (only 3 rows visible): checking all 3 must lock the CTA; player-4 row is `GONE` and must not affect the lock.

---

## Self-Review

- **Spec coverage:** Rule (all visible hit ⇒ locked) → Task 4 Step 5. Lock visuals/parity with DeclareTricks → Task 4 Steps 3+5 (+ extracted helpers Task 1). Warn subtitle → Task 3. EN/DE string → Task 2. 3-player handling → Task 4 Step 5 uses `players.size`; manual check Task 5 Step 4. "No domain change" → respected (no engine/test edits). All spec sections covered.
- **Placeholder scan:** No TBD/TODO; every code step shows full code; commands have expected output.
- **Type consistency:** `updateConfirmLockState()` named consistently across Steps 3-5; fields `colorActive/colorDeactive/ctaFgColor/ctaMutedColor/lastStateValid` declared in Step 1 and used in Step 5; `startButton`/`warnSubtitle` declared Step 1, assigned Step 2. `animateFill`/`shake` signatures match the Task 1 definitions.
