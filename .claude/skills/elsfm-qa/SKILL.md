---
name: elsfm-qa
description: QA-test the live elsfm.com music player using gstack browse. Verifies play/pause/skip controls work, checks JS console for errors, and produces annotated screenshots as evidence. Run before submitting a release.
version: 1.0.0
allowed-tools:
  - Bash
  - Read
triggers:
  - elsfm-qa
  - qa elsfm
  - test elsfm player
  - check elsfm site
  - verify elsfm music
---

# /elsfm-qa — ELSFM Live Site QA

Tests the music player on https://www.elsfm.com using gstack's headless browser.

## Setup browse binary

```bash
B="$HOME/.claude/skills/gstack/browse/dist/browse"
if [ ! -x "$B" ]; then
  echo "NEEDS_SETUP: gstack browse not built. Run /qa first (or cd ~/.claude/skills/gstack && ./setup) then retry."
  exit 1
fi
echo "Browse binary: $B"
```

## Step 1 — Navigate to site

```bash
$B goto https://www.elsfm.com
$B screenshot /tmp/elsfm-homepage.png
```

Read `/tmp/elsfm-homepage.png` to show the homepage.

## Step 2 — Inspect player controls

```bash
$B snapshot -i -a -o /tmp/elsfm-player-annotated.png
```

Read `/tmp/elsfm-player-annotated.png`. Identify the play button @ref from the interactive elements list.

## Step 3 — Play a track

```bash
$B click @e<N>
```
Replace `<N>` with the play button ref from Step 2.

Wait 3 seconds:
```bash
sleep 3
```

Verify audio started:
```bash
$B js "navigator.mediaSession && navigator.mediaSession.metadata !== null"
```
Expected: `true`

## Step 4 — Read track metadata

```bash
$B js "JSON.stringify({title: navigator.mediaSession.metadata?.title, artist: navigator.mediaSession.metadata?.artist})"
```

Log result. Both `title` and `artist` should be non-empty strings.

## Step 5 — Verify playback state

```bash
$B js "navigator.mediaSession.playbackState"
```
Expected: `"playing"`

## Step 6 — Check for JS errors

```bash
$B console --errors
```

Log all results. JS errors referencing the player or audio are FAIL. Framework warnings are acceptable.

## Step 7 — Check network requests

```bash
$B network
```

Look for failed requests (4xx/5xx) to audio stream URLs. Any audio stream failures = FAIL.

## Step 8 — Final screenshot

```bash
$B screenshot /tmp/elsfm-player-playing.png
```

Read `/tmp/elsfm-player-playing.png` to show the player in active state.

## Step 9 — Report

Print one of:

**✅ PASS** — player loaded, `playbackState` is `"playing"`, MediaSession metadata has title and artist, no JS errors, no failed audio requests.

**❌ FAIL** — list each failing check with details and screenshot path.

Include the homepage and player screenshots in your response regardless of result.
