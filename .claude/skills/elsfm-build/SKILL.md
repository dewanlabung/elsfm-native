---
name: elsfm-build
description: Scaffold or re-sync the elsfm-app/ Capacitor project (Android + iOS) for ELSFM.com. Creates the hybrid native shell that wraps https://www.elsfm.com. Run on a new machine or after config changes.
version: 1.0.0
allowed-tools:
  - Bash
  - Read
  - Write
  - Edit
triggers:
  - elsfm-build
  - build elsfm app
  - scaffold elsfm
  - setup capacitor elsfm
  - setup elsfm mobile
---

# /elsfm-build — ELSFM Capacitor App Builder

Scaffolds or re-syncs the `elsfm-app/` Capacitor project that wraps https://www.elsfm.com.

**App ID:** `com.elsfm.app` (permanent — do not change after first Play Store publish)

## Step 0 — Prerequisites

```bash
node --version 2>/dev/null && echo "Node: OK" || echo "MISSING: install Node.js from nodejs.org"
npx cap --version 2>/dev/null && echo "Capacitor CLI: OK" || echo "Capacitor CLI will be installed with npm install"
xcode-select -p 2>/dev/null && echo "Xcode tools: OK" || echo "Xcode: not found — iOS builds require Xcode from the Mac App Store"
```

Stop and tell the user what to install if Node.js is missing.

## Step 1 — Check if elsfm-app already exists

```bash
[ -d /Users/siku/Documents/GitHub/elsfm-app ] && echo "EXISTS" || echo "NEW"
```

- If **EXISTS**: jump to Step 6 (re-sync only)
- If **NEW**: proceed from Step 2

## Step 2 — Create project structure (new only)

```bash
mkdir -p /Users/siku/Documents/GitHub/elsfm-app/www
```

Create `/Users/siku/Documents/GitHub/elsfm-app/package.json`:
```json
{
  "name": "elsfm-app",
  "version": "1.0.0",
  "description": "ELSFM music streaming native app",
  "scripts": {
    "sync": "npx cap sync",
    "open:android": "npx cap open android",
    "open:ios": "npx cap open ios"
  },
  "dependencies": {
    "@capacitor/core": "^6.0.0",
    "@capacitor/background-runner": "^2.0.0"
  },
  "devDependencies": {
    "@capacitor/cli": "^6.0.0",
    "@capacitor/android": "^6.0.0",
    "@capacitor/ios": "^6.0.0"
  }
}
```

Create `/Users/siku/Documents/GitHub/elsfm-app/capacitor.config.json`:
```json
{
  "appId": "com.elsfm.app",
  "appName": "ELSFM",
  "webDir": "www",
  "server": {
    "url": "https://www.elsfm.com",
    "cleartext": false,
    "androidScheme": "https"
  },
  "plugins": {
    "BackgroundRunner": {
      "label": "com.elsfm.background.task",
      "src": "background.js",
      "event": "keepAlive",
      "repeat": true,
      "interval": 15,
      "autoStart": false
    }
  }
}
```

Create `/Users/siku/Documents/GitHub/elsfm-app/www/index.html` (placeholder — never served in server mode):
```html
<!DOCTYPE html><html><head><title>ELSFM</title></head><body></body></html>
```

Create `/Users/siku/Documents/GitHub/elsfm-app/www/background.js`:
```javascript
addEventListener('keepAlive', (resolve) => { resolve(); });
```

## Step 3 — Install dependencies (new only)

```bash
cd /Users/siku/Documents/GitHub/elsfm-app && npm install
```

## Step 4 — Init Capacitor and add platforms (new only)

```bash
cd /Users/siku/Documents/GitHub/elsfm-app && npx cap add android && npx cap add ios
```

## Step 5 — Apply native patches (new only)

### Android: AndroidManifest.xml

Open `/Users/siku/Documents/GitHub/elsfm-app/android/app/src/main/AndroidManifest.xml`.

Add these permissions inside `<manifest>` before `<application>`:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

Add `android:networkSecurityConfig="@xml/network_security_config"` to `<application>`.

Add inside `<application>`:
```xml
<service
    android:name=".ForegroundAudioService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false" />
```

### Android: network_security_config.xml

Create `/Users/siku/Documents/GitHub/elsfm-app/android/app/src/main/res/xml/network_security_config.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">elsfm.com</domain>
    </domain-config>
</network-security-config>
```

### Android: ElsfmBridge.java

Create `/Users/siku/Documents/GitHub/elsfm-app/android/app/src/main/java/com/elsfm/app/ElsfmBridge.java`:
```java
package com.elsfm.app;
import android.webkit.JavascriptInterface;
public class ElsfmBridge {
    private final TrackUpdateListener listener;
    public interface TrackUpdateListener {
        void onTrackUpdate(String title, String artist, String album);
    }
    public ElsfmBridge(TrackUpdateListener listener) { this.listener = listener; }
    @JavascriptInterface
    public void updateTrack(String title, String artist, String album) {
        if (listener != null) listener.onTrackUpdate(title, artist, album);
    }
}
```

### Android: ForegroundAudioService.java

Create `/Users/siku/Documents/GitHub/elsfm-app/android/app/src/main/java/com/elsfm/app/ForegroundAudioService.java` with the full implementation (foreground service + MediaSessionCompat + evaluateJavascript polling + implements ElsfmBridge.TrackUpdateListener). Reference the existing file at that path for the complete source — it will exist after `cap add android` + native patching.

### Android: build.gradle

Add to dependencies in `/Users/siku/Documents/GitHub/elsfm-app/android/app/build.gradle`:
```groovy
implementation 'androidx.media:media:1.7.0'
```

### iOS: Info.plist

Add to `/Users/siku/Documents/GitHub/elsfm-app/ios/App/App/Info.plist` before `</dict></plist>`:
```xml
<key>UIBackgroundModes</key>
<array><string>audio</string></array>
<key>NSAppTransportSecurity</key>
<dict>
    <key>NSAllowsArbitraryLoads</key><false/>
    <key>NSExceptionDomains</key>
    <dict>
        <key>elsfm.com</key>
        <dict>
            <key>NSIncludesSubdomains</key><true/>
            <key>NSExceptionAllowsInsecureHTTPLoads</key><false/>
        </dict>
    </dict>
</dict>
```

### iOS: AppDelegate.swift

Add `import AVFoundation` and inside `didFinishLaunchingWithOptions`:
```swift
do {
    try AVAudioSession.sharedInstance().setCategory(.playback, mode: .default, options: [])
    try AVAudioSession.sharedInstance().setActive(true)
} catch { print("AVAudioSession: \(error)") }
```

## Step 6 — Sync

```bash
cd /Users/siku/Documents/GitHub/elsfm-app && npx cap sync
```

## Step 7 — Open (optional)

Ask: "Open Android Studio or Xcode?"

- Android: `cd /Users/siku/Documents/GitHub/elsfm-app && npx cap open android`
- iOS: `cd /Users/siku/Documents/GitHub/elsfm-app && npx cap open ios`
- iOS note: run `cd ios/App && pod install` first if CocoaPods is installed

## Done

Report:
- elsfm-app/ status (new or re-synced)
- Next steps: run `/elsfm-qa` to test, then `/elsfm-ship` when ready to publish
