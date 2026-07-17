---
name: elsfm-features
description: |
  Complete feature inventory for the ELSFM native Android app. Lists every shipped
  feature with its module location, key implementation file(s), and notes on design
  decisions. Use as context when adding new features or debugging existing ones.
version: 1.0.0
allowed-tools:
  - Read
  - Grep
triggers:
  - elsfm-features
  - what features exist
  - feature list
  - what has been built
---

# ELSFM Native Android — Feature Inventory

App: Kotlin/Compose native Android music streaming client for https://www.elsfm.com  
Repo: https://github.com/dewanlabung/elsfm-native  
Latest release: v1.0.8-smart-features-artist-ui

---

## Authentication (`feature/auth`)

| Feature | File | Notes |
|---------|------|-------|
| Login screen | `LoginScreen.kt` | Email + password, Sanctum token auth, OTP support |
| Signup screen | `SignupScreen.kt` | Registration with email verification |
| Password reset | `PasswordResetScreen.kt` | Two states: form + success; both use 80dp icon |
| Auth hero image | `feature/auth/src/main/res/drawable/auth_hero.jpg` | 80dp `ContentScale.Fit` — icon-sized, not banner; also copied to `app/src/main/res/drawable/` |
| OTP verification | `OtpVerificationScreen.kt` | 6-digit OTP input, resend countdown |

**Key decision:** Each Compose module has its own `R` class. Resources must be in `feature/auth/src/main/res/drawable/`, not `app/res/drawable/`.

---

## Player / Playback (`feature/player`, `core/media`)

| Feature | File | Notes |
|---------|------|-------|
| Now Playing screen | `NowPlayingScreen.kt` | Full-bleed album art, seek bar, speed/volume sliders |
| Sleep timer | `SleepTimer.kt` | Countdown in minutes, auto-pause on expiry |
| Playback speed | `PlayerViewModel.kt` | 0.5×–2.0× via `PlaybackParameters` |
| Volume control | `PlayerViewModel.kt` | 0–1 float, reflected in ExoPlayer |
| Queue management | `Media3PlayerController.kt` | Current queue, add-to-queue, jump-to-item |
| Shuffle / Repeat | `Media3PlayerController.kt` | OFF / ALL / ONE cycle |
| State persistence | `PlaybackStateStore.kt` | DataStore JSON; restored paused on next launch |
| Position ticker | `Media3PlayerController.kt` | 500ms poll for seek-bar updates |
| **Shake to skip** | `ShakeDetector.kt` | Accelerometer, 2.5g threshold, 1s cooldown; wired in `PlaybackService` |
| **Headphone unplug pause** | `PlaybackService.kt` | ExoPlayer `setHandleAudioBecomingNoisy(true)` — built-in, no extra code |
| **Phone call pause/resume** | `PlaybackService.kt` | `AudioAttributes` `handleAudioFocus=true` — built-in |
| **Headphone reconnect resume** | `HeadsetEventMonitor.kt` | `ACTION_HEADSET_PLUG` sticky broadcast; `state=1` triggers play if was-playing |
| **Auto-play recommendations** | `LocalRecommendationEngine.kt`, `RecentTracksStore.kt` | On `STATE_ENDED`, appends up to 5 tracks from local history that aren't already in queue |
| Equalizer | `PlaybackService.kt` | `Equalizer` attached to `audioSessionId`, released on destroy |

### Smart Features detail

- **ShakeDetector** — `SensorManager.TYPE_ACCELEROMETER`, gForce = `sqrt(x²+y²+z²) / GRAVITY_EARTH`, threshold 2.5g, 1s cooldown. Calls `seekToNextMediaItem()` + `play()`.
- **HeadsetEventMonitor** — registered in `PlaybackService.onCreate()`, unregistered in `onDestroy()`. `wasPlayingBeforeUnplug` flag is checked on reconnect; `runCatching` in `stop()` handles double-unregister safely.
- **RecentTracksStore** — SharedPreferences JSON (no Room migration needed), max 50 tracks, dedup by id, prepend-then-trim pattern.
- **LocalRecommendationEngine** — filters `recentTracksStore.getTracks()` by `excludeIds`, `distinctBy { id }`, `take(limit=5)`.

---

## Artist Profile (`feature/artist`)

| Feature | File | Notes |
|---------|------|-------|
| Artist detail screen | `ArtistDetailScreen.kt` | Horizontal header (avatar left, info right); action row; 6 tabs |
| Artist header | `ArtistHeader()` composable | 100dp circular avatar + name, verified badge, location, 2-line bio |
| Social link icons | `SocialLinkBadge()` composable | Colored 28dp circle with platform initial; `Intent(ACTION_VIEW, Uri.parse(...))` on tap — no brand icon dependency |
| Follow / Following button | `ArtistDetailScreen.kt` | `OutlinedButton` + `FavoriteBorder` when not following; filled green `0xFF1DB954` + `Favorite` when following |
| Plays + likes stats | Action row | `artist.plays` (String) and `artist.likesCount` (Int) — local val to avoid cross-module smart cast error |
| Discography tab | `DiscographyTab()` | Numbered popular songs, show top-5 with "Show more" toggle |
| Popular track row | `PopularTrackRow()` | Index number + 44dp album art + name/artist + heart + duration |
| Albums tab | `AlbumsTab()` | 2-column chunked grid using `AlbumCard` |
| Similar Artists / About / Tracks / Followers tabs | inline composables | Followers loaded lazily on first tab visit |
| 6-tab scroll | `ScrollableTabRow(edgePadding = 0.dp)` | Avoids overflow crash; edge padding 0 for flush alignment |
| Follow user from Followers tab | `ArtistDetailViewModel.toggleFollowUser()` | User-to-user follow via `UserApi`, distinct from artist follow |
| Share artist URL | `ArtistDetailViewModel.buildArtistShareUrl()` | Mirrors web client's slugify logic, no backend endpoint needed |

### `ArtistTab` enum
```kotlin
enum class ArtistTab { DISCOGRAPHY, SIMILAR_ARTISTS, ABOUT, TRACKS, ALBUMS, FOLLOWERS }
```

---

## Library (`feature/library`)

| Feature | File | Notes |
|---------|------|-------|
| Liked tracks | `LikedTracksScreen.kt` | List with play/unlike actions |
| Albums grid | 2-column grid | Album art + name + artist |
| Artists list | `ArtistsScreen.kt` | Avatar + name, tap → ArtistDetailScreen |
| Playlists | `PlaylistsScreen.kt` | User playlists, create/delete |
| Downloads | `DownloadsScreen.kt` | Locally cached tracks/albums, 2-column album grid |

---

## Search (`feature/search`)

| Feature | File | Notes |
|---------|------|-------|
| Search screen | `SearchScreen.kt` | `GET /search?q=` multi-entity results |
| Recent searches | Local state | Cleared on navigation away |

---

## Home / Channels (`feature/home`, `feature/channels`)

| Feature | File | Notes |
|---------|------|-------|
| Home feed | `HomeScreen.kt` | Featured channels, new releases, trending |
| Channel detail | `ChannelDetailScreen.kt` | Track listing from `GET /channel/{id}` |

---

## Playlists (`feature/playlists`)

| Feature | File | Notes |
|---------|------|-------|
| Create playlist | `CreatePlaylistDialog.kt` | Name + description |
| Add/remove tracks | `PlaylistDetailScreen.kt` | Long-press context menu on tracks |
| Reorder tracks | drag handles | Via `PlaylistApi` PATCH |

---

## Profile / Settings (`feature/profile`, `feature/settings`)

| Feature | File | Notes |
|---------|------|-------|
| Profile screen | `ProfileScreen.kt` | Avatar, display name, stats |
| Change password | `ChangePasswordScreen.kt` | Current + new + confirm fields |
| Sign out | `SettingsScreen.kt` | Clears token + stops player |
| Theme toggle | `SettingsScreen.kt` | Light / Dark / System |

---

## Backend API (`core/network`)

Base URL: `https://www.elsfm.com/api/v1`  
Auth: `Authorization: Bearer {token}` (Sanctum)

Key endpoints used:
```
POST  /auth/login           # → {token, user}
POST  /auth/register
POST  /auth/send-otp
POST  /auth/verify-otp
POST  /auth/password/email
GET   /tracks
GET   /artists/{id}
GET   /artists/{id}/tracks
GET   /artists/{id}/albums
GET   /artists/{id}/followers
POST  /users/{id}/follow
POST  /users/{id}/unfollow
GET   /playlists
POST  /playlists
POST  /playlists/{id}/tracks
GET   /search?q=
GET   /channel
GET   /channel/{id}
POST  /tracks/{id}/plays
```

---

## Database (`core/database`)

Room schema version: **6**  
Tables: `followed_artists`, `download_tasks`, `cached_tracks`  
Do NOT bump schema version without writing a `Migration` object.

---

## Release History

| Tag | Features |
|-----|----------|
| v1.0.1 | Initial Kotlin native app scaffold |
| v1.0.2 | Auth (login/signup/OTP/password reset) |
| v1.0.3 | Player (Media3, queue, speed, sleep timer) |
| v1.0.4 | Home, Search, Channels |
| v1.0.5 | Library (liked, albums, artists, playlists) |
| v1.0.6 | Downloads, profile/settings, theme |
| v1.0.7 | Playlists clickable, universal track context menu |
| v1.0.8 | Smart features (shake-skip, headset monitor, auto-recommend), artist profile redesign (6-tab, horizontal header, social badges, follow button), auth icon resize (80dp) |

---

## Next Feature Ideas (Phase 4)

- Lyrics screen (synced if backend supports it)
- Push notifications (Firebase)
- Offline playback improvements
- Artist radio / smart mix based on local history
- Lock-screen / widget controls (already partially via `MediaSession`)
