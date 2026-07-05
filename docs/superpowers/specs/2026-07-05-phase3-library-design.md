# Phase 3: Library, Search & Content Navigation — Design Spec

**Date:** 2026-07-05  
**Phase:** 3 of 4  
**Scope:** Library browsing, search, playlists, artists, albums, and content discovery  
**Status:** Design phase

---

## Overview

Phase 2 delivered playback with a single hardcoded playlist (ID 8, "All Sunday School Songs"). Phase 3 extends the app with **library discovery and content navigation**: users can browse channels, playlists, artists, and albums; search for content; and navigate to artist/album pages. This phase bridges from "play one playlist" to "discover and play any content."

---

## Goals

1. **Library discovery** — Display channels (e.g., "Sunday School", "Bhajans", "New Releases") as the main entry point; users tap to see playlists within that channel
2. **Search** — Full-text search across tracks, artists, playlists, and channels
3. **Artist pages** — View all tracks by a given artist; play a track → queue starts with that artist's tracks
4. **Playlist pages** — View playlists with track listings; tap to play
5. **Smart queue** — Queue respects context (artist page → artist's tracks; search results → search results; playlist → playlist tracks)

---

## API Surface (Inferred from Bootstrap & Phase 2)

### Existing (Phase 2)
- `GET /api/v1/playlist/{id}` → `{playlist, tracks: {data: [...]}}`

### New (Phase 3)

**Channels** (top-level content grouping)
- `GET /api/v1/channel` → List all channels (or `GET /api/v1/channels`)
- `GET /api/v1/channel/{id}` → Channel detail + playlists within it
  - Expected shape: `{channel: {id, name, description}, playlists: {data: [...]}}` (by analogy with playlist endpoint)

**Search**
- `GET /api/v1/search?q={query}&type={tracks,playlists,artists,channels}` → Mixed results
  - Or separate endpoints: `/api/v1/search/tracks?q={q}`, etc.

**Artists**
- `GET /api/v1/artist/{id}` → Artist detail (name, image, bio if available)
- `GET /api/v1/artist/{id}/tracks` → All tracks by artist

**Playlists** (already partially covered; expand)
- `GET /api/v1/playlist/{id}` → Existing (Phase 2)

### Fallback (Bootstrap Data)
If live API endpoints are unavailable during Phase 3 implementation (same constraint as Phase 2):
- `window.bootstrapData.loaders` contains channel lists and playlists
- Use conservative request bodies and test with MockEngine, same as Phase 2 Task 4

---

## Data Models

### New (core:model)

```kotlin
// Channel — top-level content grouping
@Serializable
data class Channel(
    val id: Int,
    val name: String,
    @SerialName("playlists")
    val playlists: PlaylistList? = null  // lazy-loaded
)

@Serializable
data class PlaylistList(
    val data: List<Playlist>
)

// Playlist — refined from Phase 2's inferred shape
@Serializable
data class Playlist(
    val id: Int,
    val name: String,
    @SerialName("channel_id")
    val channelId: Int? = null
)

// Artist — already exists (Phase 2 Task 1); used in search
// (Track & Artist already defined in Phase 2)

// Search result (union type)
@Serializable
sealed class SearchResult {
    @SerialName("track")
    data class TrackResult(val track: Track) : SearchResult()
    
    @SerialName("playlist")
    data class PlaylistResult(val playlist: Playlist) : SearchResult()
    
    @SerialName("artist")
    data class ArtistResult(val artist: Artist) : SearchResult()
    
    @SerialName("channel")
    data class ChannelResult(val channel: Channel) : SearchResult()
}
```

---

## Architecture & State Management

### ViewModels (feature:library, feature:search)

**LibraryViewModel** (for channel + playlist browsing)
- State: `channelList: StateFlow<List<Channel>>`, `selectedChannel: StateFlow<Channel?>`, `playlistsInChannel: StateFlow<List<Playlist>>`
- Actions: `selectChannel(id)`, `loadChannels()`
- On playlist tap → navigate to `PlaylistDetailScreen` (re-use Phase 2's PlayerScreen or new detail screen)

**SearchViewModel** (for full-text search)
- State: `query: StateFlow<String>`, `results: StateFlow<List<SearchResult>>`, `isLoading: StateFlow<Boolean>`
- Actions: `search(query)`, `clearSearch()`

**ArtistDetailViewModel** (for artist pages)
- State: `artist: StateFlow<Artist>`, `tracks: StateFlow<List<Track>>`
- Actions: `loadArtist(id)`
- On track tap → queue starts with this artist's tracks

### Navigation Structure

```
app/NavHost
├── login (Phase 1)
├── home (Phase 2 + Phase 3 additions)
│   ├── HomePlaceholder (Phase 2, unchanged)
│   ├── Library tab → LibraryScreen
│   │   └── ChannelList
│   │       └── PlaylistsInChannel
│   │           └── PlaylistDetail (or PlayerScreen variant)
│   ├── Search tab → SearchScreen
│   │   └── SearchResults
│   │       └── TapTrack → play + queue with search results
│   │       └── TapArtist → ArtistDetailScreen
│   └── (existing Player route from Phase 2)
├── artist/{id} (ArtistDetailScreen)
└── player (Phase 2)

MiniPlayer persistent at bottom (Phase 2)
```

---

## UI/UX Flow

### Library Tab (Channel → Playlist → Play)
1. User taps "Library" tab
2. See list of channels (e.g., "Sunday School", "Bhajans", "Festivals")
3. Tap a channel → see playlists within it
4. Tap a playlist → see tracks (or jump to PlayerScreen and load tracks)
5. Tap a track → play; queue = all tracks in that playlist

### Search Tab (Query → Results → Play)
1. User taps "Search" tab, enters query (e.g., "Jesus Christ")
2. See mixed results: matching tracks, playlists, artists, channels
3. Tap a track → play; queue = all search results (or just this track)
4. Tap an artist → navigate to ArtistDetailScreen
5. Tap a channel → navigate to Library, pre-select that channel

### Artist Page (Artist → All Tracks → Play)
1. From search results or elsewhere, tap artist
2. See artist detail + all their tracks
3. Tap a track → play; queue = all tracks by this artist

---

## API Error Handling & Fallbacks

Per Phase 2 pattern:
- If live API unavailable → use conservative fallback (`GET /api/v1/channel` returns empty list, search returns no results)
- Test with `MockEngine` + realistic JSON shapes inferred from bootstrap data
- Explicit error reporting to user (e.g., "Search failed; try again")

---

## Scope Boundaries (Phase 3 vs. Future)

### Included (Phase 3)
- Channel browsing (lazy-load playlists on tap)
- Playlist selection & playback
- Artist pages with track lists
- Search (query + mixed results)
- Queue context (which tracks to queue when playing from different views)

### Deferred (Phase 4+)
- Downloads / offline playback
- Recommendations / "Similar Artists"
- Lyrics display
- Notifications / now-playing integration (advanced)
- User profile / saved playlists
- Advanced filters (genre, language, year)
- Streaming stats / recommendations based on history

---

## Success Criteria

1. **Channels load** — Library tab shows ≥1 channel; tap → playlists load
2. **Search works** — Query returns mixed results (tracks + artists + playlists)
3. **Artist pages functional** — Tap artist from search → see all their tracks
4. **Queue respects context** — Playing from a playlist queues all tracks in that playlist; playing from artist queues that artist's tracks
5. **Build & tests pass** — 80%+ test coverage, no Hilt errors, `assembleDebug` succeeds
6. **No device required** — Verify via compile + test suite (same as Phase 2)

---

## Timeline & Estimated Effort

Phase 3 estimated **8–12 tasks** (compared to Phase 2's 10):
- Tasks 1–2: Data models & API integrations (ChannelApi, SearchApi, ArtistApi)
- Tasks 3–5: ViewModels (LibraryViewModel, SearchViewModel, ArtistDetailViewModel)
- Tasks 6–8: UI screens (LibraryScreen, SearchScreen, ArtistDetailScreen)
- Tasks 9–10: Navigation wiring + full build/test/push (Phase 2 pattern)

Execution: **Subagent-driven development** (same as Phase 2), with one subagent per task.

---

## Open Questions & Assumptions

1. **Search scope** — Does the backend search API exist? If not, implement client-side filtering on locally-loaded data.
2. **Album model** — Inferred from "Library/Search/Playlists/Artist/**Album**" in the original scope. Confirm if albums are a separate entity or grouped within playlists.
3. **Artist images** — Are artist images available in the API? If yes, display; if no, use placeholder.
4. **Channel lazy-loading** — Load playlists on-demand (tap channel) or upfront (load all on app start)?

---

## References

- **Phase 1 spec** (Foundation): `/docs/superpowers/specs/2026-07-04-phase1-foundation-design.md`
- **Phase 2 spec** (Player): `/docs/superpowers/specs/2026-07-05-phase2-player-design.md`
- **elsfm.com** (live reference): https://www.elsfm.com
- **Bootstrap data** (from Phase 2 research): `window.bootstrapData.loaders` exposes channel/playlist structure
