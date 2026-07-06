# Phase 5: Navigation & UI Redesign — Complete Web App Parity

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the entire native app navigation and UI to match elsfm.com's web experience, with Material 3 Compose throughout and complete menu/screen coverage.

**Architecture:** Modular screen-by-screen redesign. Each feature module (discovery, library, search, artist, player, profile, downloads) gets a complete Material 3 makeover matching the web design language. Navigation tree expanded to cover all web app sections. State hoisting from ViewModels ensures consistent behavior.

**Tech Stack:** Jetpack Compose, Material 3, StateFlow, Hilt, Coil (image loading), Ktor (API client)

## Global Constraints

- All screens use Material 3 design system (tokens from core:designsystem)
- Navigation matches elsfm.com structure: Home > Library > Search > Profile > Downloads (+ Artist/Album/Playlist detail screens)
- Blurred artwork backgrounds with gradient overlays on all detail screens
- Smooth animations (300ms standard) on all state changes
- No hardcoded colors — use MaterialTheme tokens only
- All text uses Material 3 typography scale
- Rounded corners: 12dp (cards), 24dp (artwork), 50dp (buttons)
- All screens tested with Material 3 light AND dark themes
- 80%+ test coverage on new ViewModels
- Zero console errors/warnings on build

---

## File Structure

### New/Modified Screens (Compose UI)

```
feature/
├── discovery/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/discovery/
│   │   ├── DiscoveryScreen.kt          (REWRITE: trending, featured, popular sections)
│   │   └── DiscoveryViewModel.kt       (EXISTS: add state for sections)
│   └── src/test/kotlin/.../DiscoveryScreenTest.kt
│
├── library/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/library/
│   │   ├── LibraryScreen.kt            (REWRITE: sidebar + channel grid)
│   │   ├── PlaylistScreen.kt           (NEW: playlist detail/tracks)
│   │   ├── AlbumScreen.kt              (NEW: album detail/tracks)
│   │   ├── LibraryViewModel.kt         (EXISTS: update state)
│   │   ├── PlaylistViewModel.kt        (NEW: manage playlist)
│   │   └── AlbumViewModel.kt           (NEW: album details)
│   └── src/test/kotlin/.../
│
├── search/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/search/
│   │   ├── SearchScreen.kt             (REWRITE: tabs for tracks/albums/artists/playlists)
│   │   └── SearchViewModel.kt          (EXISTS: add tab state)
│   └── src/test/kotlin/.../SearchScreenTest.kt
│
├── artist/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/artist/
│   │   ├── ArtistDetailScreen.kt       (REWRITE: header, albums, tracks, followers, follow button)
│   │   └── ArtistDetailViewModel.kt    (EXISTS: add follow/unfollow)
│   └── src/test/kotlin/.../ArtistDetailScreenTest.kt
│
├── player/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/player/
│   │   ├── PlayerScreen.kt             (EXISTS: modern player from Phase 4)
│   │   ├── QueueScreen.kt              (NEW: queue management)
│   │   └── PlayerViewModel.kt          (EXISTS: add queue state)
│   └── src/test/kotlin/.../PlayerScreenTest.kt
│
├── profile/
│   ├── src/main/kotlin/com/elsfm/mobile/feature/profile/
│   │   ├── ProfileScreen.kt            (REWRITE: full Material 3 redesign)
│   │   ├── EditProfileScreen.kt        (NEW: edit name/bio/image)
│   │   ├── FollowedArtistsScreen.kt    (NEW: list of followed artists)
│   │   └── ProfileViewModel.kt         (EXISTS: add edit + follow state)
│   └── src/test/kotlin/.../ProfileScreenTest.kt
│
└── downloads/
    ├── src/main/kotlin/com/elsfm/mobile/feature/downloads/
    │   ├── DownloadsScreen.kt          (REWRITE: Material 3 list + storage info)
    │   └── DownloadViewModel.kt        (EXISTS: add storage state)
    └── src/test/kotlin/.../DownloadsScreenTest.kt

app/
└── src/main/kotlin/com/elsfm/mobile/
    ├── ElsfmNavHost.kt                 (UPDATE: route structure, tab order, naming)
    ├── ElsfmTheme.kt                   (UPDATE: ensure Material 3 token consistency)
    └── MainActivity.kt                 (EXISTS: no changes)

core/
├── designsystem/
│   ├── src/main/kotlin/.../Theme.kt    (UPDATE: add Material 3 tokens for app-specific colors)
│   └── src/main/kotlin/.../Typography.kt (EXISTS: verify alignment with web design)
└── model/
    └── src/main/kotlin/.../            (UPDATE: add missing model fields from web)
```

### Supporting Utilities

```
feature/library/src/main/kotlin/.../
├── composables/
│   ├── PlaylistCard.kt                 (NEW: reusable playlist card component)
│   ├── AlbumCard.kt                    (NEW: reusable album card component)
│   ├── ArtistCard.kt                   (NEW: reusable artist card component)
│   ├── TrackListItem.kt                (NEW: reusable track list item)
│   ├── SectionHeader.kt                (NEW: reusable section header with "See All")
│   └── BlurredBackground.kt            (NEW: reusable blurred artwork + gradient)
└── util/
    └── FormatUtils.kt                  (EXISTS: verify duration/size formatting)
```

---

## Task Breakdown

### Task 1: Design System Tokens & Reusable Components

**Files:**
- Modify: `core/designsystem/src/main/kotlin/.../Theme.kt`
- Create: `feature/library/src/main/kotlin/.../composables/BlurredBackground.kt`
- Create: `feature/library/src/main/kotlin/.../composables/PlaylistCard.kt`
- Create: `feature/library/src/main/kotlin/.../composables/AlbumCard.kt`
- Create: `feature/library/src/main/kotlin/.../composables/ArtistCard.kt`
- Create: `feature/library/src/main/kotlin/.../composables/TrackListItem.kt`
- Create: `feature/library/src/main/kotlin/.../composables/SectionHeader.kt`
- Test: `feature/library/src/test/kotlin/.../composables/ComposablesTest.kt`

**Interfaces:**
- Produces: Reusable `@Composable` functions that all screens depend on
  - `BlurredBackground(imageUrl: String?, modifier: Modifier)`
  - `PlaylistCard(playlist: Playlist, onClick: () -> Unit, modifier: Modifier)`
  - `AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier)`
  - `ArtistCard(artist: Artist, onClick: () -> Unit, modifier: Modifier)`
  - `TrackListItem(track: Track, isPlaying: Boolean, onClick: () -> Unit, onMoreClick: () -> Unit, modifier: Modifier)`
  - `SectionHeader(title: String, onSeeAllClick: (() -> Unit)? = null, modifier: Modifier)`

- [ ] **Step 1: Create BlurredBackground composable**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/BlurredBackground.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun BlurredBackground(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Blurred background image
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(radius = 32.dp),
            contentScale = ContentScale.Crop,
        )
        
        // Dark gradient overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.5f),
                            Color.Black.copy(alpha = 0.7f),
                        )
                    )
                )
        )
        
        // Content layer
        content()
    }
}
```

- [ ] **Step 2: Create PlaylistCard**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/PlaylistCard.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.overflow.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Playlist

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            AsyncImage(
                model = playlist.image,
                contentDescription = playlist.name,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

- [ ] **Step 3: Create AlbumCard**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/AlbumCard.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.overflow.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Album

@Composable
fun AlbumCard(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth(),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            AsyncImage(
                model = album.image,
                contentDescription = album.name,
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop,
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        
        Text(
            text = album.releaseDate.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
```

- [ ] **Step 4: Create ArtistCard**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/ArtistCard.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.overflow.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Artist

@Composable
fun ArtistCard(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            AsyncImage(
                model = artist.imageSmall,
                contentDescription = artist.name,
                modifier = Modifier.size(120.dp),
                contentScale = ContentScale.Crop,
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = artist.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
```

- [ ] **Step 5: Create TrackListItem**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/TrackListItem.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.overflow.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.elsfm.mobile.core.model.Track

@Composable
fun TrackListItem(
    track: Track,
    isPlaying: Boolean = false,
    onClick: () -> Unit,
    onMoreClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clickable { onClick() }
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Album art thumbnail
        Surface(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
        ) {
            AsyncImage(
                model = track.image,
                contentDescription = track.name,
                modifier = Modifier.size(50.dp),
                contentScale = ContentScale.Crop,
            )
        }
        
        // Track info
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = track.name,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artists.firstOrNull()?.name ?: "Unknown",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        
        // More button
        IconButton(onClick = onMoreClick, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
```

- [ ] **Step 6: Create SectionHeader**

```kotlin
// feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/SectionHeader.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clickable(enabled = onSeeAllClick != null) { onSeeAllClick?.invoke() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        
        if (onSeeAllClick != null) {
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "See all",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
```

- [ ] **Step 7: Write test for composables**

```kotlin
// feature/library/src/test/kotlin/com/elsfm/mobile/feature/library/composables/ComposablesTest.kt
package com.elsfm.mobile.feature.library.composables

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ComposablesTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playlistCardRenders() {
        val playlist = Playlist(id = 1, name = "Test Playlist", image = null, description = null)
        composeRule.setContent {
            PlaylistCard(playlist = playlist, onClick = {})
        }
    }

    @Test
    fun albumCardRenders() {
        val album = Album(id = 1, name = "Test Album", image = null, releaseDate = "2024-01-01", description = null)
        composeRule.setContent {
            AlbumCard(album = album, onClick = {})
        }
    }

    @Test
    fun artistCardRenders() {
        val artist = Artist(id = 1, name = "Test Artist", imageSmall = null, verified = false)
        composeRule.setContent {
            ArtistCard(artist = artist, onClick = {})
        }
    }

    @Test
    fun trackListItemRenders() {
        val track = Track(
            id = 1,
            name = "Test Track",
            image = null,
            duration = 180000,
            description = null,
            releaseDate = "2024-01-01",
            artists = emptyList(),
            src = null,
            plays = null,
        )
        composeRule.setContent {
            TrackListItem(track = track, onClick = {})
        }
    }

    @Test
    fun sectionHeaderRenders() {
        composeRule.setContent {
            SectionHeader(title = "Featured", onSeeAllClick = {})
        }
    }
}
```

- [ ] **Step 8: Commit**

```bash
git add feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/composables/
git add feature/library/src/test/kotlin/com/elsfm/mobile/feature/library/composables/
git commit -m "feat(ui): add reusable Material 3 composables (cards, items, sections)"
```

---

### Task 2: Redesign DiscoveryScreen (Home)

**Files:**
- Modify: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryScreen.kt`
- Modify: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryViewModel.kt`
- Test: `feature/discovery/src/test/kotlin/.../DiscoveryScreenTest.kt`

**Interfaces:**
- Consumes: `BlurredBackground`, `PlaylistCard`, `SectionHeader`, `TrackListItem` from Task 1
- Produces: Complete home screen with sections (Featured Playlists, Popular Songs, New Releases, Your Recently Played)

[**Step details would follow same pattern as Task 1, showing complete code for all UI sections, tests, and commit**]

---

### Task 3: Redesign LibraryScreen + Create PlaylistScreen & AlbumScreen

**Files:**
- Modify: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/PlaylistScreen.kt`
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/AlbumScreen.kt`
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/PlaylistViewModel.kt`
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/AlbumViewModel.kt`
- Test: Full test suite for all new screens

**Interfaces:**
- Consumes: Composables from Task 1, ViewModel patterns from Phase 4
- Produces: Three complete screens with full CRUD state management

[**Full implementation would continue in same format**]

---

### Task 4: Redesign SearchScreen (Add Result Tabs)

### Task 5: Redesign ArtistDetailScreen (Add Follow, Albums, Verified Badge)

### Task 6: Redesign ProfileScreen + Create EditProfileScreen & FollowedArtistsScreen

### Task 7: Update PlayerScreen with Queue Management

### Task 8: Redesign DownloadsScreen with Storage Info

### Task 9: Update Navigation (ElsfmNavHost) with All Routes & Proper Naming

### Task 10: Full Build, Test, Integration

---

## Success Criteria

1. **Navigation complete** — All web app screens accessible from native app
2. **Material 3 throughout** — Every screen uses design tokens, no hardcoded colors
3. **Blurred backgrounds** — All detail screens have artwork-based backgrounds
4. **Smooth animations** — All state changes animated (300ms+ spring)
5. **State hoisting** — All state flows from ViewModels, UI is reactive
6. **Tests passing** — 80%+ coverage, all new screens tested
7. **Build succeeds** — `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL
8. **No regressions** — Phases 1–4 features still work

---

## Execution Options

**Plan saved to:** `docs/superpowers/plans/2026-07-06-phase5-navigation-ui-redesign.md`

**Two execution approaches:**

**1. Subagent-Driven (recommended)** — I dispatch a fresh subagent per task, review between tasks, fast iteration

**2. Inline Execution** — Execute tasks in this session using superpowers:executing-plans, batch execution with checkpoints

**Which approach?**

