<#
  capture-screenshots.ps1
  Builds + installs SpadesScore, plays a deterministic 4-player demo game on a
  running emulator/device, captures the six README screenshots and writes them
  to docs/screenshots/.

  Requires: a running emulator/device (adb), the Android SDK (ANDROID_HOME or the
  default %LOCALAPPDATA%\Android\Sdk), and the Gradle wrapper in the repo root.

  Usage:
    pwsh .\.claude\skills\update-readme-screenshots\capture-screenshots.ps1
    pwsh .\...\capture-screenshots.ps1 -SkipBuild     # reuse the installed APK
#>
param([switch]$SkipBuild)
$ErrorActionPreference = "Stop"

# ---------------------------------------------------------------- paths / setup
$RepoRoot = (Resolve-Path "$PSScriptRoot\..\..\..").Path
$ShotsOut = Join-Path $RepoRoot "docs\screenshots"
$Tmp      = Join-Path $env:TEMP "spades-shots"
$Pkg      = "com.nwe.spadesscore"
$Sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME }
       elseif ($env:ANDROID_SDK_ROOT) { $env:ANDROID_SDK_ROOT }
       else { "$env:LOCALAPPDATA\Android\Sdk" }
$AdbExe = Join-Path $Sdk "platform-tools\adb.exe"

if (-not (Test-Path $AdbExe)) { throw "adb not found at $AdbExe — set ANDROID_HOME." }
New-Item -ItemType Directory -Force -Path $Tmp, $ShotsOut | Out-Null

$dev = & $AdbExe devices | Select-String "\sdevice$"
if (-not $dev) { throw "No emulator/device attached. Start one (e.g. 'emulator -avd <name>') and retry." }

# --------------------------------------------------------------------- helpers
function Get-UI {
    & $AdbExe shell uiautomator dump /sdcard/ui.xml *> $null
    $f = Join-Path $Tmp "ui.xml"
    & $AdbExe pull /sdcard/ui.xml $f *> $null
    [xml](Get-Content $f -Raw)
}
# Centers of every node whose resource-id matches $idRegex, sorted top-to-bottom.
function Centers([xml]$xml, [string]$idRegex) {
    $list = @()
    foreach ($n in $xml.SelectNodes("//node[@resource-id]")) {
        if ($n.GetAttribute("resource-id") -match $idRegex) {
            if ($n.GetAttribute("bounds") -match '\[(\d+),(\d+)\]\[(\d+),(\d+)\]') {
                $list += [pscustomobject]@{
                    x = [int]((([int]$Matches[1]) + ([int]$Matches[3])) / 2)
                    y = [int]((([int]$Matches[2]) + ([int]$Matches[4])) / 2)
                }
            }
        }
    }
    , ($list | Sort-Object y)
}
function TapXY([int]$x, [int]$y, [int]$ms = 550) { & $AdbExe shell input tap $x $y; Start-Sleep -Milliseconds $ms }
# Poll the hierarchy until $idRegex appears (handles cold start / screen transitions).
function Find-Centers([string]$idRegex, [int]$tries = 10, [int]$ms = 500) {
    for ($a = 0; $a -lt $tries; $a++) {
        $c = Centers (Get-UI) $idRegex
        if ($c.Count -gt 0) { return $c }
        Start-Sleep -Milliseconds $ms
    }
    return @()
}
function TapId([string]$id, [int]$ms = 700) {
    $c = Find-Centers ([regex]::Escape("${Pkg}:id/$id") + '$')
    if ($c.Count -eq 0) { throw "resource-id not found: $id (on $(CurAct))" }
    TapXY $c[0].x $c[0].y $ms
}
function Shot([string]$name) {
    & $AdbExe shell screencap -p /sdcard/shot.png *> $null
    & $AdbExe pull /sdcard/shot.png (Join-Path $Tmp "$name.png") *> $null
    Write-Host "  shot: $name"
}
function CurAct {
    (((& $AdbExe shell dumpsys activity activities 2>$null | Select-String "ResumedActivity") |
        Select-Object -First 1) -replace '.*com\.nwe\.spadesscore/', '' -replace '\}.*', '').Trim()
}

# ------------------------------------------------- deterministic demo-data model
# Seeded LCG -> reproducible games, so screenshots are stable across runs.
$script:Seed = 20240607
function Rand([int]$mod) {
    $script:Seed = [int]((([int64]$script:Seed * 1103515245 + 12345) -band 0x7fffffff))
    [int]($script:Seed % $mod)
}
# Rounds where nobody makes their bid -> no points awarded, scores carried forward.
$AllMiss = @(3, 6)
# On a normal round exactly one player misses; this rotates the misser fairly
# across Ben/Clara/David (Anna, the stronger player, always makes her bid) so
# every player scores regularly and no score column flat-lines.
$MissOrder = @(1, 2, 3, 1, 2, 3)
$script:NormalIdx = 0

# Returns @{ pred=int[4]; hits=bool[4] } for a round with $c cards.
# Tricks are distributed across players (Anna slightly favoured -> a strong
# player), bids stay within +-1 of the tricks won, and the bid sum never equals
# $c (the game forbids it). On an all-miss round everyone is off by one.
function Get-RoundData([int]$c, [int]$round) {
    $n = 4
    $actual = @(0, 0, 0, 0)
    $w = @(3, 2, 2, 2)                      # Anna (index 0) wins tricks more often
    $tw = ($w | Measure-Object -Sum).Sum
    for ($t = 0; $t -lt $c; $t++) {
        $r = Rand $tw; $acc = 0; $win = 0
        for ($p = 0; $p -lt $n; $p++) { $acc += $w[$p]; if ($r -lt $acc) { $win = $p; break } }
        $actual[$win]++
    }
    $pred = @($actual[0], $actual[1], $actual[2], $actual[3])
    $hits = @($true, $true, $true, $true)

    if ($AllMiss -contains $round) {
        for ($i = 0; $i -lt $n; $i++) {
            if ($actual[$i] -lt $c) { $pred[$i] = $actual[$i] + 1 } else { $pred[$i] = $actual[$i] - 1 }
            $hits[$i] = $false
        }
    }
    else {
        # Exactly one player misses, rotating fairly across rounds (see $MissOrder).
        $m = $MissOrder[$script:NormalIdx % $MissOrder.Count]; $script:NormalIdx++
        if ($actual[$m] -lt $c) { $pred[$m] = $actual[$m] + 1 } else { $pred[$m] = $actual[$m] - 1 }
        $hits[$m] = $false
    }
    @{ pred = $pred; hits = $hits }
}

# ------------------------------------------------------------- build + install
if (-not $SkipBuild) {
    Write-Host "Building debug APK..."
    Push-Location $RepoRoot
    try { $out = & .\gradlew.bat assembleDebug 2>&1 } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { $out | Select-Object -Last 30 | Write-Host; throw "gradle assembleDebug failed" }
    & $AdbExe install -r (Join-Path $RepoRoot "app\build\outputs\apk\debug\app-debug.apk") *> $null
    Write-Host "Installed."
}

# --------------------------------------------------------------- drive the app
& $AdbExe shell pm clear $Pkg *> $null
& $AdbExe shell am start -n "$Pkg/.MainActivity" *> $null
Start-Sleep -Seconds 3

# MainActivity: 4 players (English is the default game language; the EN/DE
# toggle now lives in SettingsActivity behind the gear icon, not on this screen)
[void](TapId "btn4Players")
Start-Sleep -Milliseconds 400
Shot "start"
[void](TapId "start_game_button" 1200)

# PlayerNames
$names = @("Anna", "Ben", "Clara", "David")
for ($i = 1; $i -le 4; $i++) {
    $c = Find-Centers ([regex]::Escape("${Pkg}:id/player${i}_name_input") + '$')
    TapXY $c[0].x $c[0].y 250
    & $AdbExe shell input text $names[$i - 1]; Start-Sleep -Milliseconds 250
}
& $AdbExe shell input keyevent 4 | Out-Null   # hide keyboard
Start-Sleep -Milliseconds 500
Shot "player-names"
[void](TapId "start_game_button" 1200)

# 8 first-half rounds; capture deal/declare/confirm mid-game, results at half-time
$SHOT_ROUND = 5
for ($r = 1; $r -le 8; $r++) {
    Write-Host "Round $r ($(CurAct))"
    if ($r -eq $SHOT_ROUND) { Shot "deal" }
    [void](TapId "deal_next_button" 850)                   # DONE -> Declare

    $data = Get-RoundData $r $r                            # first half: cards == round
    $plus = Find-Centers ':id/btnPlus$'                    # 4 steppers, top-to-bottom
    for ($i = 0; $i -lt 4; $i++) {
        for ($t = 0; $t -lt $data.pred[$i]; $t++) { TapXY $plus[$i].x $plus[$i].y 180 }
    }
    if ($r -eq $SHOT_ROUND) { Start-Sleep -Milliseconds 300; Shot "declare" }
    [void](TapId "start_Button" 850)                       # CONFIRM TICKS -> Confirm

    for ($i = 0; $i -lt 4; $i++) { if ($data.hits[$i]) { [void](TapId "player$($i+1)_check" 250) } }
    if ($r -eq $SHOT_ROUND) { Start-Sleep -Milliseconds 300; Shot "confirm" }
    [void](TapId "start_Button" 950)                       # START NEXT ROUND
}

Start-Sleep -Milliseconds 700
if ((CurAct) -notlike "*ResultScreen*") { throw "expected ResultScreen at half-time, got $(CurAct)" }
Shot "results"

# ------------------------------------------------------------- publish results
Copy-Item (Join-Path $Tmp "start.png")        (Join-Path $ShotsOut "start.png")        -Force
Copy-Item (Join-Path $Tmp "player-names.png") (Join-Path $ShotsOut "player-names.png") -Force
Copy-Item (Join-Path $Tmp "deal.png")         (Join-Path $ShotsOut "deal.png")         -Force
Copy-Item (Join-Path $Tmp "declare.png")      (Join-Path $ShotsOut "declare.png")      -Force
Copy-Item (Join-Path $Tmp "confirm.png")      (Join-Path $ShotsOut "confirm.png")      -Force
Copy-Item (Join-Path $Tmp "results.png")      (Join-Path $ShotsOut "results.png")      -Force
Write-Host "`nUpdated 6 screenshots in $ShotsOut"
