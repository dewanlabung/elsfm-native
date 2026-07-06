# Phase 4: Advanced Features & UI Polish — Design Spec

**Date:** 2026-07-05  
**Phase:** 4 of 4  
**Scope:** Discovery, recommendations, user profile, downloads, notifications, and UI improvements to match Flutter app visual language  
**Status:** Design phase

---

## Overview

Phase 3 delivered core library & search navigation. Phase 4 extends with **intelligent discovery, personalized recommendations, user profile management, offline support, and visual polish** to match the Flutter app's design language and user experience.

---

## Goals

1. **Smart Discovery** — Trending tracks/playlists, curated channels, "New Releases"
2. **Personalized Recommendations** — "Because You Liked X" (based on history + API)
3. **User Profile** — View/edit profile, saved playlists, recently played, follow artists
4. **Offline Support** — Download tracks/playlists for offline playback
5. **Notifications** — New releases from followed artists, playlist updates (future: push)
6. **UI Consistency** — Material 3, dark mode polish, animations matching Flutter reference

---

## API Surface (from Laravel Backend)

### New (Phase 4)

**Trending/Discovery**
- `GET /api/v1/trending?type=tracks|playlists|artists` → trending list
- `GET /api/v1/new-releases?genre={genreId}&limit=20` → recent additions

**Recommendations**
- `GET /api/v1/recommendations?based_on={trackId|artistId}&limit=10` → "Similar to X"

**User Profile**
- `GET /api/v1/me` → authenticated user profile
- `GET /api/v1/me/profile` → extended profile (bio, image, stats)
- `PUT /api/v1/me/profile` → update profile (name, bio, image)
- `GET /api/v1/me/recently-played` → play history (paginated)
- `GET /api/v1/me/saved-playlists` → user's playlists
- `GET /api/v1/me/followed-artists` → subscribed artists

**Follow Artists**
- `POST /api/v1/artists/{id}/follow` → add to followed artists
- `DELETE /api/v1/artists/{id}/follow` → remove from followed
- `GET /api/v1/artists/{id}/followers` → artist's follower count

**Downloads** (stub for Phase 4.1)
- `GET /api/v1/me/downloads` → list downloaded tracks
- `POST /api/v1/tracks/{id}/download` → initiate download
- `DELETE /api/v1/downloads/{trackId}` → delete cached file

### Existing (Phase 2/3)

All Phase 2/3 endpoints remain unchanged.

---

## Data Models

### New (core:model)

```kotlin
// User profile
@Serializable
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    @SerialName("image")
    val profileImage: String? = null,
    val bio: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("followed_count")
    val followedCount: Int = 0,
)

// Trending/discovery result
@Serializable
data class TrendingTrack(
    val track: Track,
    @SerialName("rank")
    val position: Int,
)

// Recommendation result
@Serializable
data class Recommendation(
    val track: Track,
    @SerialName("score")
    val relevanceScore: Float? = null, // 0.0–1.0
)

// Downloaded track metadata (local DB)
@Entity
data class DownloadedTrack(
    @PrimaryKey val trackId: Int,
    val fileName: String,
    val fileSizeBytes: Long,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis(),
)
```

---

## Architecture & State Management

### New ViewModels (feature:discovery, feature:profile, feature:downloads)

**DiscoveryViewModel** (home tab improvements)
- State: `trendingTracks: StateFlow<List<TrendingTrack>>`, `recommendations: StateFlow<List<Recommendation>>`, `isLoading`, `error`
- Actions: `loadTrending()`, `loadRecommendations(basedOn: Int)`

**ProfileViewModel** (profile tab / drawer)
- State: `userProfile: StateFlow<UserProfile>`, `recentlyPlayed: StateFlow<List<Track>>`, `savedPlaylists: StateFlow<List<Playlist>>`
- Actions: `loadProfile()`, `updateProfile(name, bio)`, `logout()`
- Note: Optional `SavedStateHandle` for cached offline profile

**DownloadViewModel** (downloads screen)
- State: `downloadedTracks: StateFlow<List<DownloadedTrack>>`, `downloadProgress: StateFlow<Map<Int, Float>>` (trackId → 0.0–1.0)
- Actions: `downloadTrack(trackId, filename)`, `deleteDownload(trackId)`, `loadDownloads()`
- Integration: Works with `PlaybackService` to serve local files when offline

### Navigation

```
app/NavHost
├── login (Phase 1, unchanged)
├── home (Phase 2/3 + Phase 4 enhancements)
│   ├── Home tab → DiscoveryScreen (trending + recommendations)
│   ├── Library tab → LibraryScreen (Phase 3, unchanged)
│   ├── Search tab → SearchScreen (Phase 3, unchanged)
│   ├── Profile tab → ProfileScreen (NEW)
│   │   └── Recently Played (sub-page)
│   │   └── My Playlists (sub-page)
│   │   └── Edit Profile (sub-page)
│   └── Downloads tab → DownloadsScreen (NEW, offline support)
├── artist/{id} (Phase 3, unchanged)
├── player (Phase 2, unchanged)
└── MiniPlayer (persistent below NavHost, Phase 2)
```

---

## UI/UX Flow

### Home Tab (Discovery)

1. User sees "Home" tab with:
   - **Trending Now** section (horizontal scroll of top 10 tracks)
   - **Recommended For You** section (based on recent play history)
   - **New Releases** section (latest added tracks/albums)
   - **Followed Artists Updates** (recently updated playlists from followed artists)

2. Tap a track → play + queue all from that section
3. Tap an artist name → navigate to `ArtistDetailScreen` (Phase 3)
4. Tap "See All Trending" → full page paginated list

### Profile Tab

1. Header: User image, name, follower/following counts
2. Actions: Edit profile, logout, settings (future)
3. Sections:
   - Recently Played (with timestamps, remove action)
   - Saved Playlists (tap → `PlaylistDetailScreen`)
   - Followed Artists (tap → `ArtistDetailScreen`, unfollow action)
4. Pull-to-refresh loads fresh profile data

### Downloads Tab

1. List of downloaded tracks (sorted by date descending)
2. Per-track actions: play, delete, info
3. Storage info: "1.2 GB of 2 GB used" (optional)
4. Status: "Downloads are available offline"
5. Download button appears on track detail / search results

---

## Offline Strategy

### Scope (Phase 4)

- Users can download tracks for offline playback
- Downloaded tracks appear in Downloads tab
- `PlaybackService` checks local DB before hitting API
- No background download scheduling (Phase 4.1+)

### Local Storage

- **Room DB** (core:database) for `DownloadedTrack` metadata
- **App-private storage** (`getExternalFilesDir()` or `getCacheDir()`) for audio files
- Naming: `${trackId}_${slugify(trackName)}.mp3` for readability

### Sync Offline → Online

- When online: App prefetches downloaded track list from API
- When offline: Shows only cached tracks
- On reconnect: Sync play history (buffered locally) with API

---

## UI/Visual Polish (Match Flutter App)

### Design Direction

Refer to the Flutter reference app at https://www.elsfm.com. Phase 4 Compose screens should:

1. **Material 3 consistency** — Use design tokens from `core:designsystem` (Phase 1)
2. **Color palette** — Dark mode as primary, light mode as alt
3. **Typography** — Headings bold/contrast, body readable at arm's length
4. **Spacing** — 16dp base unit, consistent rhythm
5. **Animations** — Smooth transitions (300ms standard), avoid jank

### Specific Polish Items

- **Profile avatar** — Circular, with optional image fallback (user initials)
- **Trending cards** — Large, image-forward, rank number overlay
- **Recommendation cards** — Similar to Trending, but with "Why You'll Like This" subtitle
- **Downloads progress** — Determinate progress bar per track, cancel button
- **Error states** — Illustrative, actionable (e.g., "No internet — downloads unavailable")
- **Empty states** — "No downloads yet" with call-to-action

---

## Testing Strategy

### Unit Tests

- `TrendingRepositoryTest` — mock API, test caching
- `RecommendationApiTest` — success + error paths
- `ProfileViewModelTest` — load profile, update flow
- `DownloadManagerTest` — local DB operations, file cleanup

### Integration Tests (if emulator available)

- Profile screen loads and displays user data
- Download button appears and initiates download (mock file)
- Recently Played list updates after playing a track
- Offline mode shows cached tracks only

### Manual Verification (when emulator/device available)

- Profile image loads (if available in API)
- Follow/unfollow reflects immediately
- Downloads tab shows files and storage used
- Logout clears cached profile

---

## Scope Boundaries (Phase 4 vs. Future)

### Included (Phase 4)

- Trending/recommendations (read-only)
- User profile view + edit (name, bio)
- Download management (download, delete, list)
- Recently played history display
- Follow/unfollow artists
- Offline track playback (cached files)
- UI polish to match Flutter reference

### Deferred (Phase 4.1+)

- Background download scheduling + resume
- Cloud sync / backup of playlists
- Advanced analytics (listening stats, top artists)
- Sharing playlists (social integration)
- Lyrics display
- Podcast / spoken-word support
- Social features (user follow, messages)
- Advanced search filters (year, language, mood)

---

## Success Criteria

1. **Discovery works** — Trending + recommendations load and display
2. **Profile complete** — User can view/edit profile, see recently played
3. **Downloads functional** — Can download tracks and play offline
4. **UI matches reference** — Screens visually consistent with Flutter app
5. **Build & tests pass** — 80%+ coverage, no Hilt errors, `assembleDebug` succeeds
6. **Offline mode works** — App serves cached tracks without API calls
7. **No regressions** — Phase 1–3 features still work

---

## Timeline & Estimated Effort

Phase 4 estimated **10–14 tasks** (building on Phase 3's foundation):

- Tasks 1–2: Data models & API integrations (TrendingApi, RecommendationApi, ProfileApi, DownloadManager)
- Tasks 3–5: ViewModels (DiscoveryViewModel, ProfileViewModel, DownloadViewModel)
- Tasks 6–9: UI screens (DiscoveryScreen, ProfileScreen, DownloadsScreen, + home tab polish)
- Tasks 10–11: Offline support (Room DB, download manager wiring, sync on reconnect)
- Tasks 12–14: Final polish, full build/test/device verification, release prep

Execution: **Subagent-driven development** (same as Phase 3), one subagent per task.

---

## Open Questions & Assumptions

1. **Recommendation algorithm** — Backend provides recommendations based on play history. Does it have a `/api/v1/recommendations` endpoint, or does the app build a local "similar to" list?
2. **Download format** — Audio file format (MP3, AAC, FLAC)? Bitrate? Assume same as streaming.
3. **Sync conflicts** — What happens if user edits profile offline, then syncs? Last-write-wins or merge?
4. **Follow limits** — Can user follow unlimited artists, or is there a cap?

---

## References

- **Phase 1 spec** (Foundation): `/docs/superpowers/specs/2026-07-04-phase1-foundation-design.md`
- **Phase 2 spec** (Player): `/docs/superpowers/specs/2026-07-05-phase2-player-design.md`
- **Phase 3 spec** (Library): `/docs/superpowers/specs/2026-07-05-phase3-library-design.md`
- **Laravel backend** (skills): `elsfm-laravel-backend/SKILL.md` + API endpoints skill
- **Flutter reference** (visual): https://www.elsfm.com (live app)
- **Material 3 guidelines**: https://m3.material.io

