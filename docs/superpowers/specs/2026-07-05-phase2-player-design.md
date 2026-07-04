# ELSFM Native — Phase 2: Player Design

## Context

Phase 1 (Foundation) is complete: synced auth, session persistence, saved password via Credential
Manager, a Room profile cache, and a minimal Hilt/Compose Navigation shell. Phase 2 addresses the
other headline complaint about the old Flutter app — "music didn't play" — plus play-history sync.

## Verified ground truth (live-checked against elsfm.com, not assumed)

- Production elsfm.com has a fully intact, working music catalog (tracks, playlists, albums,
  artists) — a local, stale copy of the Laravel backend repo had its music tables dropped in an
  unrelated experiment; production is unaffected. Confirmed via `window.bootstrapData` and live
  network capture.
- Real track JSON shape (from `bootstrapData.loaders.playlistPage.tracks.data[]`):
  `id, name, image, number, duration (ms), plays, popularity, owner_id, created_at, artists[],
  position, album, genres, src, src_local, likes_count, reposts_count`.
- `src` is a plain relative path (e.g. `storage/track_media/{hash}.mp3`) — prepend
  `https://www.elsfm.com/` for a directly playable URL. No auth token needed on the file itself
  (confirmed against a real example URL the project owner provided independently).
- Clicking play on the web fires `POST /api/v1/player/tracks` — almost certainly the play-history/
  play-count recording call, given `plays` is a live-updating field on the track. The exact request/
  response body could not be captured live (likely a bundled fetch reference). **This is a specific,
  scoped verification step for the implementation task that builds play-history recording** — not a
  blocker for the rest of this design.
- No dedicated streaming/manifest endpoint exists or is needed — this is direct static file
  playback, not HLS/DASH. Simplifies the player considerably (no adaptive bitrate logic needed).

## Scope

**In scope for Phase 2:**
- `core:media` module: `PlaybackService` (`MediaSessionService`), builds `MediaItem`s from track
  data, exposes playback state/queue via `MediaController`.
- `feature:player` module: full player screen (art, controls, seek bar, queue list), a mini-player,
  and a `PlayerViewModel` wrapping the `MediaController`.
- A minimal track list on `HomePlaceholderScreen` (fetch one playlist's tracks) — just enough to
  pick a track and prove the whole pipeline works. This is explicitly a placeholder; real
  browse/search UI is Phase 3.
- Play-history recording: call the play-tracking endpoint once playback actually starts for a track
  (mirrors observed web behavior — not a listen-threshold heuristic, since inventing one without
  server confirmation would risk diverging from actual backend semantics).

**Out of scope for Phase 2** (deferred to later phases per the original roadmap):
- Lyrics display (Phase 4).
- Downloads/offline playback (Phase 4).
- Real library/search/artist/album browsing (Phase 3) — the Home track list here is temporary.
- Adaptive bitrate / quality selection (no evidence the backend supports it — single `src` per
  track).

## Architecture

### Module structure

```
elsfm-native/
├── core/
│   └── media/                  # NEW: PlaybackService, MediaItem builders, PlayHistoryApi
└── feature/
    └── player/                 # NEW: PlayerScreen, MiniPlayer, PlayerViewModel, QueueScreen
```

`core:media` depends on `core:model`, `core:network`, `core:common` (mirrors `core:network`'s own
pattern — it needs Android's Media3 APIs and Hilt, so it applies `com.android.library` + Hilt like
`core:network` does, not a plain-JVM module).

### Playback service (`core:media`)

- `PlaybackService : MediaSessionService`, Hilt-injected (`@AndroidEntryPoint` via Media3's Hilt
  extension), holds one `ExoPlayer` instance and one `MediaSession`.
- Notification: Media3's `DefaultMediaNotificationProvider` — no custom notification layout for
  Phase 2 (YAGNI; the default gives play/pause/skip, artwork, and lock-screen integration for free).
- Manifest: `android:foregroundServiceType="mediaPlayback"` on the service, `FOREGROUND_SERVICE` and
  `FOREGROUND_SERVICE_MEDIA_PLAYBACK` permissions (required on Android 14+).
- `MediaItem` construction: a small builder function maps a `Track` (see below) to a `MediaItem`
  with `mediaId = track.id`, `Uri = "https://www.elsfm.com/${track.src}"`, and
  `MediaMetadata` (title, artist, artwork URI from `track.image`).
- Queue: `ExoPlayer`'s own internal playlist (`setMediaItems`/`addMediaItem`) is the source of
  truth — no separate app-level queue state to keep in sync.

### UI ↔ service boundary (`feature:player`)

- `PlayerViewModel` obtains a `MediaController` via `MediaController.Builder(context,
  sessionToken).buildAsync()` (a `ListenableFuture`, bridged to a coroutine with
  `await()`/`Futures.await`).
- To keep `PlayerViewModel` unit-testable without a real `MediaController` (which needs a live
  service connection), it depends on a thin `PlayerController` interface
  (`play(track)`, `togglePlayPause()`, `seekTo(positionMs)`, `skipNext()`, `skipPrevious()`,
  `state: StateFlow<PlayerState>`) with a real Media3-backed implementation and a hand-written
  `FakePlayerController` for tests — the same "extract an interface at the boundary, fake it in
  tests" pattern already used for `AuthApiLike` and `TokenStore` in Phase 1.
- `PlayerState` (sealed-ish data class): `currentTrack: Track?`, `isPlaying: Boolean`,
  `positionMs: Long`, `durationMs: Long`, `queue: List<Track>`.
- `PlayerScreen` (full player) and `MiniPlayer` (persistent bottom bar) both collect
  `PlayerViewModel.state` — `MiniPlayer` is shown from `ElsfmNavHost` whenever `currentTrack != null`,
  regardless of which route is active.

### Data flow

1. User taps a track on `HomePlaceholderScreen`'s track list.
2. `PlayerViewModel.play(track, queue)` → `PlayerController.play(track, queue)` → builds
   `MediaItem`s, calls `mediaController.setMediaItems(items, startIndex, 0)` +
   `mediaController.play()`.
3. `PlaybackService`'s `ExoPlayer` starts playing; Media3 automatically updates the system
   notification and lock-screen controls via the `MediaSession`.
4. `PlayerController` observes `Player.Listener` callbacks (`onIsPlayingChanged`,
   `onMediaItemTransition`, position via a polling `Handler`/coroutine tick since Media3 doesn't
   push position updates) and republishes them as `PlayerState` through its own `StateFlow`.
5. On `onIsPlayingChanged(true)` for a new track, `PlayHistoryApi.recordPlay(trackId)` fires once
   (guarded so it doesn't refire on pause/resume of the same track).

### Error handling

- `ExoPlayer.Listener.onPlayerError` maps to a `PlayerState.error: String?` field the UI can show as
  a snackbar/toast — playback errors (network failure, 404 on the file) don't crash the service.
- `PlayHistoryApi.recordPlay()` failures are fire-and-forget (logged, not surfaced to the user) —
  losing a play-history ping shouldn't interrupt playback.

### Testing

- `core:media`: unit tests for the `Track → MediaItem` mapping function (pure, no service needed).
  The `PlaybackService` itself needs an instrumented test (real service binding) — deferred to a
  single connected-device smoke test in this phase's final task, consistent with how Phase 1
  handled Room/Compose instrumented tests.
- `feature:player`: `PlayerViewModel` unit-tested against `FakePlayerController` with Turbine,
  covering play/pause/track-transition/error state mapping — no real Media3 dependency needed.
- Play-history: `PlayHistoryApi` unit-tested with Ktor `MockEngine`, same pattern as `AuthApi`.

## Risks carried into implementation

1. **Confirm the exact play-history endpoint contract** (`POST /api/v1/player/tracks`'s real
   request/response body) during the task that implements `PlayHistoryApi` — verify live if
   possible, or treat conservatively (send `{track_id}` and treat any 2xx as success) if the exact
   schema can't be confirmed.
2. **Confirm the exact track-listing endpoint** used for the Home screen's minimal track list (the
   bootstrap loader's shape is known; the paginated XHR endpoint for "load more tracks" was not
   directly observed) — verify live during that task rather than guessing the URL pattern.
3. Android 14+ foreground service type restrictions are new-ish (API 34) — verify the manifest
   permissions against the project's actual `minSdk`/`targetSdk` (26/35 per Phase 1) during
   implementation; targetSdk 35 means these restrictions apply.
