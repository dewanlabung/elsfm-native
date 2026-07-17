---
name: elsfm-ship
description: |
  Release checklist for the ELSFM native Android app. Covers debug sideload via GitHub
  Releases, and the signed .aab path for Google Play. Run /elsfm-android-build first.
version: 2.0.0
allowed-tools:
  - Bash
  - Read
triggers:
  - elsfm-ship
  - ship elsfm
  - release elsfm
  - publish elsfm
  - submit elsfm
---

# /elsfm-ship — ELSFM Release Checklist

The ELSFM app is a **Kotlin native** Android app (not Capacitor).  
Project: `/Users/siku/Documents/GitHub/elsfm-native/`  
Repo: `https://github.com/dewanlabung/elsfm-native`

Run `/elsfm-android-build` first to produce the APK.

## Pre-flight check

```bash
ls -lh /Users/siku/Documents/GitHub/elsfm-native/app/build/outputs/apk/debug/app-debug.apk \
  && echo "APK: ✅" || echo "APK: ❌ — run /elsfm-android-build first"
git -C /Users/siku/Documents/GitHub/elsfm-native status --short | head -10
```

Stop if APK is missing.

## Step 1 — Commit and push

```bash
cd /Users/siku/Documents/GitHub/elsfm-native
git add <changed files>
git commit -m "feat: <description>"
git push origin main
```

## Step 2 — Create GitHub Release (dev/sideload)

```bash
NEXT_TAG="v1.0.X-<slug>"
gh release create "$NEXT_TAG" \
  --title "$NEXT_TAG — <Human title>" \
  --notes "## What's New
- feature 1
- feature 2

## Install
Download \`app-debug.apk\` and sideload on Android." \
  /Users/siku/Documents/GitHub/elsfm-native/app/build/outputs/apk/debug/app-debug.apk
```

Tag naming: `v1.0.X-<kebab-feature-slug>`  
Previous tags: v1.0.1 through v1.0.8 — increment X.

## Step 3 — Version bump in build.gradle (for Play Store)

Edit `/Users/siku/Documents/GitHub/elsfm-native/app/build.gradle.kts`:
- `versionCode` — increment by 1 (integer; Play Store rejects downgrades)
- `versionName` — set to human-readable string (e.g. `"1.0.8"`)

## Step 4 — Play Store signed .aab

1. Open Android Studio, open `elsfm-native/`
2. Build → Generate Signed Bundle / APK → **Android App Bundle (.aab)**
3. Use/create a keystore (keep safe — required for all future updates)
4. Build variant: **release**

Check output:
```bash
find /Users/siku/Documents/GitHub/elsfm-native -name "*.aab" 2>/dev/null
```

## Step 5 — Play Store checklist

```
[ ] App icon: 512×512 PNG, no alpha channel
[ ] Feature graphic: 1024×500 PNG
[ ] Screenshots: ≥2 phone screenshots
[ ] Short description: max 80 chars
[ ] Full description: max 4000 chars
[ ] Privacy policy URL
[ ] Data safety form: account info, listening history
[ ] Content rating: complete IARC questionnaire
[ ] minSdk 26 declared ✅
[ ] Media3/ExoPlayer foreground service declared in manifest ✅
```

## Done

Report:
- GitHub release URL
- .aab path if produced
- Any unchecked items
