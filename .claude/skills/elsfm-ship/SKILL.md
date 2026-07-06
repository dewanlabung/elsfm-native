---
name: elsfm-ship
description: Guided release build checklist for ELSFM — produces signed .aab for Google Play and .ipa for App Store, with store-listing reminders and AdSense policy warning.
version: 1.0.0
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

Walks through the full release process for Android (Google Play) and iOS (App Store).

## Pre-flight check

```bash
[ -d /Users/siku/Documents/GitHub/elsfm-app/android ] && echo "Android: ✅" || echo "Android: ❌ MISSING — run /elsfm-build first"
[ -d /Users/siku/Documents/GitHub/elsfm-app/ios ] && echo "iOS: ✅" || echo "iOS: ❌ MISSING — run /elsfm-build first"
cd /Users/siku/Documents/GitHub/elsfm-app && npx cap sync 2>&1 | tail -5
```

Stop if any platform is missing.

## Step 1 — Version bump

Ask: "What version number for this release? (e.g. 1.0.0)"

**Android** — edit `/Users/siku/Documents/GitHub/elsfm-app/android/app/build.gradle`:
- Increment `versionCode` by 1
- Set `versionName` to the user's answer

**iOS** — edit `ios/App/App/Info.plist`:
- `CFBundleShortVersionString` = user's version
- `CFBundleVersion` = increment by 1

## Step 2 — Android: signed release build

Instruct the user:
1. Open Android Studio: `cd /Users/siku/Documents/GitHub/elsfm-app && npx cap open android`
2. Build → Generate Signed Bundle / APK → choose **Android App Bundle (.aab)**
3. Create or select your keystore (keep it safe — you need it for every future update)
4. Build variant: **release**

Check if .aab was produced:
```bash
find /Users/siku/Documents/GitHub/elsfm-app/android -name "*.aab" 2>/dev/null
```

If found: print the path. If not: remind user to complete the build in Android Studio.

## Step 3 — iOS: archive

Instruct the user (requires full Xcode and Apple Developer account):
1. Install CocoaPods if not present: `sudo gem install cocoapods`
2. `cd /Users/siku/Documents/GitHub/elsfm-app/ios/App && pod install`
3. Open: `cd /Users/siku/Documents/GitHub/elsfm-app && npx cap open ios` — opens `App.xcworkspace`
4. Set scheme **App**, destination **Any iOS Device (arm64)**
5. Product → Archive
6. Distribute App → App Store Connect → Upload

## Step 4 — Store listing checklist

Print and confirm each item:

```
Android (Google Play Console — play.google.com/console):
[ ] App icon: 512×512 PNG, no alpha channel
[ ] Feature graphic: 1024×500 PNG
[ ] Screenshots: at least 2 phone screenshots (min 320px, max 3840px on longest side)
[ ] Short description: max 80 characters
[ ] Full description: max 4000 characters
[ ] Privacy policy URL (publicly accessible)
[ ] Data safety form: declare data collected (account info, listening history, device ID)
[ ] Content rating: complete IARC questionnaire in Play Console
[ ] ⚠️  AdSense policy: web AdSense inside a native app violates Google Play policy.
    Options:
    A) Serve https://app.elsfm.com (ad-free subdomain) as server.url in capacitor.config.json
    B) Inject CSS to hide .adsbygoogle elements inside the WebView via Capacitor plugin

iOS (App Store Connect — appstoreconnect.apple.com):
[ ] App icon: 1024×1024 PNG (no alpha, no rounded corners — Apple adds them)
[ ] Screenshots: iPhone 6.5" (required) and 5.5" (required)
[ ] Privacy policy URL
[ ] Background audio entitlement: UIBackgroundModes:audio is declared in Info.plist ✅
[ ] Age rating: complete questionnaire
[ ] $99/yr Apple Developer account active
```

## Step 5 — Submit

- Android: upload `.aab` to Play Console → Production track → Review and submit (review takes 1-3 days)
- iOS: submit build via Xcode Organizer → App Store Connect review takes 1-7 days

## Done

Report:
- Android .aab path (if found)
- iOS archive status
- Any unchecked items from the checklist
