---
name: update-readme-screenshots
description: Use when the README screenshots (docs/screenshots/) are stale or after UI changes to the start, player-names, deal, declare, confirm or result screens — regenerates them from the running app.
---

# Update README Screenshots

## Overview

Regenerates the six README screenshots by building the app, playing a scripted
4-player demo game on a running emulator, and capturing each screen. The
screenshots in `docs/screenshots/` therefore always reflect the live UI.

The demo data is realistic and deterministic (seeded): each round's tricks are
distributed across the players (one is a slightly stronger player), bids stay
within ±1 of the tricks won, the bid sum never equals the trick count (the game
forbids it), and a couple of rounds are "all-miss" rounds where nobody makes
their bid and no points are awarded.

## When to Use

- The app UI changed (layout, colors, strings, theme) and the README images are now out of date.
- `docs/screenshots/*.png` need refreshing for any reason.

## Steps

1. **Ensure an emulator/device is running.** Check with `adb devices`. If none,
   start one (the script will fail fast with a clear message otherwise):
   ```
   emulator -list-avds
   emulator -avd <name>          # wait until fully booted
   ```
2. **Run the capture script** from the repo root:
   ```
   pwsh .\.claude\skills\update-readme-screenshots\capture-screenshots.ps1
   ```
   It builds + installs the debug APK, drives the demo game, and overwrites the
   six PNGs in `docs/screenshots/`. Pass `-SkipBuild` to reuse the installed APK.
3. **Look at the output images** (`docs/screenshots/start.png`, `player-names.png`,
   `deal.png`, `declare.png`, `confirm.png`, `results.png`). A blank or wrong
   screen means a step failed — re-run; do not commit unverified images.
4. **Review the README** if the set of screens or filenames changed, then commit
   `docs/screenshots/` (and the README if edited).

## Notes

- Coordinates are resolved from `uiautomator` dumps (by resource-id), so the
  script is resolution-independent across devices.
- To change the demo (players, which rounds are all-miss, the screenshot round),
  edit `capture-screenshots.ps1`: `$names`, `$AllMiss`, `$SHOT_ROUND`.
- The script captures the **first half** (8 rounds) and uses the **half-time**
  table as `results.png`. To capture the final table instead, extend the loop
  through the second half (cards ramp down) before the `results` shot.
