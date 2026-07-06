---
name: elsfm-agents
description: Dispatch parallel sub-agents for ELSFM troubleshooting — Android build debugger, iOS build debugger, and live-site monitor (plays music for 2 min and confirms it keeps playing). Use when debugging multi-platform issues.
version: 1.0.0
allowed-tools:
  - Bash
  - Agent
triggers:
  - elsfm-agents
  - elsfm parallel agents
  - debug elsfm platforms
  - monitor elsfm
  - elsfm sub agents
---

# /elsfm-agents — ELSFM Parallel Sub-Agents

Dispatches up to three sub-agents simultaneously. Each runs independently and reports back.

## Ask which agents to launch

Ask the user (multi-select, default = all three):
- A) Android build/runtime debugger
- B) iOS build/runtime debugger  
- C) Live-site monitor (plays music 2 min, confirms it keeps playing)

## Dispatch selected agents in parallel

### Agent A — Android Debugger

Prompt:
```
You are the ELSFM Android build debugger.

Check these files at /Users/siku/Documents/GitHub/elsfm-app/android/:

1. Run: grep -n "FOREGROUND_SERVICE\|ForegroundAudioService\|networkSecurityConfig" android/app/src/main/AndroidManifest.xml
   Expected: all three present

2. Check: ls android/app/src/main/java/com/elsfm/app/
   Expected: ElsfmBridge.java, ForegroundAudioService.java, MainActivity.java

3. Check build.gradle: grep "androidx.media" android/app/build.gradle
   Expected: androidx.media:media present

4. Check: grep -n "addJavascriptInterface\|startForegroundService" android/app/src/main/java/com/elsfm/app/MainActivity.java
   Expected: both present

Report: PASS (all checks green) or FAIL with file + line + fix for each issue.
```

### Agent B — iOS Debugger

Prompt:
```
You are the ELSFM iOS build debugger.

Check these files at /Users/siku/Documents/GitHub/elsfm-app/ios/:

1. UIBackgroundModes: grep -A3 "UIBackgroundModes" ios/App/App/Info.plist
   Expected: contains <string>audio</string>

2. NSExceptionDomains: grep -A10 "NSExceptionDomains" ios/App/App/Info.plist
   Expected: elsfm.com domain present

3. AVAudioSession: grep -n "AVFoundation\|setCategory\|playback" ios/App/App/AppDelegate.swift
   Expected: all three present

4. Podfile: ls ios/App/Pods/ 2>/dev/null && echo "Pods installed" || echo "Run: cd ios/App && pod install"

Report: PASS or FAIL with file + fix for each issue.
```

### Agent C — Live-Site Monitor

Prompt:
```
You are the ELSFM live-site monitor. Test that music keeps playing for 2 minutes.

The gstack browse binary is at: /Users/siku/.claude/skills/gstack/browse/dist/browse

First verify it exists:
  ls -la /Users/siku/.claude/skills/gstack/browse/dist/browse

If missing: report BLOCKED — user needs to build gstack browse first (run /qa).

Steps (use full binary path in every command):

1. /Users/siku/.claude/skills/gstack/browse/dist/browse goto https://www.elsfm.com
2. /Users/siku/.claude/skills/gstack/browse/dist/browse snapshot -i
   (find a play button @ref in the output)
3. /Users/siku/.claude/skills/gstack/browse/dist/browse click @e<N>
   (replace N with the play button ref)
4. /Users/siku/.claude/skills/gstack/browse/dist/browse js "navigator.mediaSession.playbackState"
   (expected: "playing")
5. sleep 120
6. /Users/siku/.claude/skills/gstack/browse/dist/browse js "navigator.mediaSession.playbackState"
   (still "playing"? This is the key check)
7. /Users/siku/.claude/skills/gstack/browse/dist/browse console --errors
8. /Users/siku/.claude/skills/gstack/browse/dist/browse screenshot /tmp/elsfm-monitor-result.png
   Then read /tmp/elsfm-monitor-result.png

Report: PASS (playing after 2 min, no errors) or FAIL with step number and details.
```

## Collect and summarise results

After all agents complete:
- Android: PASS / FAIL + issues list
- iOS: PASS / FAIL + issues list  
- Monitor: PASS / FAIL + details

If any agent FAILs, ask the user whether to re-dispatch that agent after they fix the issues.
