---
name: elsfm-android-build
description: |
  Build, install, and release the ELSFM native Android app (Kotlin/Compose/Media3).
  Covers Gradle build commands, the C2-compiler crash workaround, ADB device install,
  GitHub release creation with APK attachment, and the module/package structure.
version: 2.0.0
allowed-tools:
  - Bash
  - Read
  - Write
  - Edit
triggers:
  - elsfm-android-build
  - build elsfm
  - assemble apk
  - install apk
  - release elsfm
---

# /elsfm-android-build — ELSFM Native Android Build

Builds and ships the Kotlin-native ELSFM Android app at
`/Users/siku/Documents/GitHub/elsfm-native/`.

**App ID:** `com.elsfm.mobile`  
**Min SDK:** 26 (Android 8.0)  
**Compile SDK:** 35  
**Git repo:** `https://github.com/dewanlabung/elsfm-native`

---

## Module Layout

```
app/                    # Shell module: Hilt entry-point, MainActivity, NavGraph
core/
  common/              # DispatcherProvider, extension fns
  database/            # Room DB (schema v6), DAOs, FollowStateRepository
  designsystem/        # Shared Compose theme, colors, typography
  media/               # PlaybackService (Media3), ShakeDetector, HeadsetEventMonitor,
  │                    # RecentTracksStore, LocalRecommendationEngine
  model/               # Data classes: Track, Album, Artist, Playlist, User …
  network/             # Ktor ApiClient, all Api interfaces, ApiResult sealed type
feature/
  artist/              # ArtistDetailScreen + ViewModel + State (6-tab profile)
  auth/                # LoginScreen, SignupScreen, PasswordResetScreen (80dp icon)
  channels/
  downloads/
  home/
  library/
  player/              # NowPlayingScreen, PlayerViewModel, Media3PlayerController
  playlists/
  profile/
  search/
  settings/
```

---

## Step 1 — Gradle Build

> **IMPORTANT — JVM C2 crash workaround:** Gradle on this machine crashes with
> `SIGSEGV (0xb) at pc=0x…` when the C2 JIT compiler is active. Always export
> this env var before any `./gradlew` call:

```bash
export GRADLE_OPTS="-XX:TieredStopAtLevel=1 -Xmx2g -XX:+UseG1GC"
cd /Users/siku/Documents/GitHub/elsfm-native
./gradlew assembleDebug --no-daemon
```

- `--no-daemon` avoids accumulating crashed daemon processes.
- `-XX:TieredStopAtLevel=1` disables the C2 JIT tier that causes the SIGSEGV.
- Output APK: `app/build/outputs/apk/debug/app-debug.apk`

### Check APK exists

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

---

## Step 2 — Install to USB Device

ADB is at `/Users/siku/Library/Android/sdk/platform-tools/adb`.

```bash
ADB=/Users/siku/Library/Android/sdk/platform-tools/adb
$ADB devices                              # verify device is listed
$ADB -s RF9XA017X8P install -r app/build/outputs/apk/debug/app-debug.apk
```

- `-r` = reinstall over existing without losing data.
- Device serial: `RF9XA017X8P` (Samsung Galaxy, USB debug must be enabled).
- If device not listed, check "USB Debugging" in Developer Options.

---

## Step 3 — Commit Changes

```bash
git add <files>
git commit -m "$(cat <<'EOF'
feat: <short description>

- bullet 1
- bullet 2
EOF
)"
git push origin main
```

Commit types: `feat`, `fix`, `refactor`, `perf`, `chore`

---

## Step 4 — GitHub Release with APK

```bash
gh release create v1.0.X-<tag> \
  --title "v1.0.X — <Title>" \
  --notes "$(cat <<'EOF'
## What's New
- feature 1
- feature 2

## Install
Download `app-debug.apk` and sideload.
EOF
)" \
  app/build/outputs/apk/debug/app-debug.apk
```

Release naming convention: `v1.0.X-<kebab-slug>` (e.g. `v1.0.8-smart-features-artist-ui`)

---

## Troubleshooting

| Symptom | Fix |
|---------|-----|
| `SIGSEGV in C2 compiler` | Add `export GRADLE_OPTS="-XX:TieredStopAtLevel=1 -Xmx2g -XX:+UseG1GC"` |
| `Unresolved reference 'drawable'` in module | Resource must be in **that module's** `res/drawable/`, not `app/res/drawable/` |
| `Smart cast impossible` from cross-module property | Copy to local `val` first: `val x = obj.prop` then use `x` |
| `Class BaseController not found` (backend) | Extend `Common\Core\BaseController`, not `Controller` |
| ADB: `device not found` | Ensure USB debugging on, cable plugged, run `$ADB kill-server && $ADB start-server` |
