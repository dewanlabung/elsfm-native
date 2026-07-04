# ELSFM Native Phase 2 (Player) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add background audio playback (Media3/ExoPlayer), a full player screen + mini-player, and play-history sync to the elsfm-native app, plus a minimal track list on the Home screen so playback can actually be triggered before Phase 3's real library UI exists.

**Architecture:** Two new Gradle modules — `core:media` (playback service, MediaItem mapping, play-history API) and `feature:player` (player UI, ViewModel, and a `PlayerController` interface boundary so the ViewModel is unit-testable without a real Media3 session). `app` wires a minimal Home track list and the mini-player/full-player navigation.

**Tech Stack:** Media3/ExoPlayer (`MediaSessionService` + `MediaController`), Jetpack Compose, Hilt, Ktor, kotlinx.serialization, Turbine.

## Global Constraints

- Base package: `com.elsfm.mobile`.
- `core:*` modules must never depend on `feature:*` or `app` modules.
- `core:media` applies `com.android.library` + Hilt + KSP (like `core:network`), since it needs Media3's Android APIs and Hilt DI.
- Every module's Gradle dependency versions come from `gradle/libs.versions.toml` — no inline version strings. Media3 is not yet in the catalog; Task 1 adds it.
- No automated test may hit the real `elsfm.com` API or use real account credentials — Ktor `MockEngine` for network tests, hand-written Fakes (not a mocking framework) for interface boundaries, consistent with Phase 1.
- Never log the token, password, or full track URLs containing any auth-sensitive query params (none are expected here, but don't log request/response bodies containing user data).
- The audio file URL is `"https://www.elsfm.com/" + track.src` (`track.src` is a plain relative path like `storage/track_media/{hash}.mp3` — verified live against production, not a signed/expiring URL).
- The real request/response body of the play-history endpoint (`POST /api/v1/player/tracks`) and the exact track-listing endpoint were not fully confirmed live — Tasks 4 and 8 include an explicit live-verification step before finalizing their request/response shapes.

---

### Task 1: Add Media3 to the version catalog + `core:media` module scaffold

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `settings.gradle.kts`
- Create: `core/media/build.gradle.kts`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Artist.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Track.kt`
- Test: `core/model/src/test/kotlin/com/elsfm/mobile/core/model/TrackSerializationTest.kt`

**Interfaces:**
- Produces: `data class Artist(val id: Int, val name: String)`, `data class Track(val id: Int, val name: String, val image: String?, val durationMs: Long, val src: String, val artists: List<Artist>)`. Consumed by `core:media` (Task 2), `feature:player` (Tasks 5-7), `core:network`'s `TrackListApi` (Task 8).

- [ ] **Step 1: Add Media3 versions/libraries to the catalog**

Add to `gradle/libs.versions.toml`'s `[versions]` block:
```toml
media3 = "1.5.0"
```
Add to `[libraries]`:
```toml
androidx-media3-exoplayer = { group = "androidx.media3", name = "media3-exoplayer", version.ref = "media3" }
androidx-media3-session = { group = "androidx.media3", name = "media3-session", version.ref = "media3" }
androidx-media3-common = { group = "androidx.media3", name = "media3-common", version.ref = "media3" }
```

- [ ] **Step 2: Declare the new modules in settings.gradle.kts**

Add these two lines (keep alongside the existing `include(...)` lines):
```kotlin
include(":core:media")
include(":feature:player")
```

- [ ] **Step 3: Write the failing test for `Track` JSON deserialization**

`core/model/src/test/kotlin/com/elsfm/mobile/core/model/TrackSerializationTest.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackSerializationTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserializes a real track JSON shape`() {
        val body = """
            {
              "id": 1192,
              "name": "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
              "image": "storage/track_image_media/KvU3sDMqRQDPyT1oJ3LhXb6Dh4V0mmgAD5tVtthU.jpeg",
              "duration": 174000,
              "src": "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
              "artists": [
                {"id": 30, "name": "Sunday School Songs"}
              ]
            }
        """.trimIndent()

        val track = json.decodeFromString<Track>(body)

        assertEquals(1192, track.id)
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", track.name)
        assertEquals(174000L, track.durationMs)
        assertEquals("storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3", track.src)
        assertEquals(1, track.artists.size)
        assertEquals("Sunday School Songs", track.artists[0].name)
    }
}
```

- [ ] **Step 4: Run test to verify it fails**

Run: `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" && export ANDROID_HOME="$HOME/Library/Android/sdk" && ./gradlew :core:model:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `Track`/`Artist` classes do not exist yet.

- [ ] **Step 5: Create `Artist` and `Track`**

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/Artist.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: Int,
    val name: String,
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/Track.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Int,
    val name: String,
    val image: String?,
    @SerialName("duration") val durationMs: Long,
    val src: String,
    val artists: List<Artist>,
)
```

- [ ] **Step 6: Run test to verify it passes**

Run the same command as Step 4.
Expected: `BUILD SUCCESSFUL`, 1 new test passing.

- [ ] **Step 7: Scaffold `core:media`'s build.gradle.kts**

`core/media/build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elsfm.mobile.core.media"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:network"))

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.ktor.client.core)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
}
```

- [ ] **Step 8: Verify the module resolves (no source yet, just configuration)**

Run: `./gradlew :core:media:compileDebugKotlin --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL` (empty module compiles with no source).

- [ ] **Step 9: Commit**

```bash
git add gradle/libs.versions.toml settings.gradle.kts core/media/build.gradle.kts \
  core/model/src/main/kotlin/com/elsfm/mobile/core/model/Artist.kt \
  core/model/src/main/kotlin/com/elsfm/mobile/core/model/Track.kt \
  core/model/src/test/kotlin/com/elsfm/mobile/core/model/TrackSerializationTest.kt
git commit -m "feat(core-model,core-media): add Track/Artist models and scaffold core:media module"
```

---

### Task 2: `core:media` — MediaItem mapping

**Files:**
- Create: `core/media/src/main/kotlin/com/elsfm/mobile/core/media/MediaItemFactory.kt`
- Test: `core/media/src/test/kotlin/com/elsfm/mobile/core/media/MediaItemFactoryTest.kt`

**Interfaces:**
- Consumes: `Track` (Task 1).
- Produces: `fun Track.toMediaItem(baseUrl: String = "https://www.elsfm.com/"): MediaItem`. Consumed by `feature:player`'s `Media3PlayerController` (Task 5).

- [ ] **Step 1: Write the failing test**

`core/media/src/test/kotlin/com/elsfm/mobile/core/media/MediaItemFactoryTest.kt`:
```kotlin
package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaItemFactoryTest {

    @Test
    fun `builds a playable media item from a track`() {
        val track = Track(
            id = 1192,
            name = "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
            image = "storage/track_image_media/abc.jpeg",
            durationMs = 174000,
            src = "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
            artists = listOf(Artist(id = 30, name = "Sunday School Songs")),
        )

        val mediaItem = track.toMediaItem()

        assertEquals("1192", mediaItem.mediaId)
        assertEquals(
            "https://www.elsfm.com/storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
            mediaItem.localConfiguration?.uri.toString(),
        )
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", mediaItem.mediaMetadata.title.toString())
        assertEquals("Sunday School Songs", mediaItem.mediaMetadata.artist.toString())
        assertEquals(
            "https://www.elsfm.com/storage/track_image_media/abc.jpeg",
            mediaItem.mediaMetadata.artworkUri.toString(),
        )
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:media:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `toMediaItem` not defined.

- [ ] **Step 3: Implement `MediaItemFactory`**

`core/media/src/main/kotlin/com/elsfm/mobile/core/media/MediaItemFactory.kt`:
```kotlin
package com.elsfm.mobile.core.media

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.elsfm.mobile.core.model.Track

fun Track.toMediaItem(baseUrl: String = "https://www.elsfm.com/"): MediaItem {
    val metadata = MediaMetadata.Builder()
        .setTitle(name)
        .setArtist(artists.firstOrNull()?.name.orEmpty())
        .apply { image?.let { setArtworkUri(android.net.Uri.parse(baseUrl + it)) } }
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(baseUrl + src)
        .setMediaMetadata(metadata)
        .build()
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: `BUILD SUCCESSFUL`, 1 new test passing.

- [ ] **Step 5: Commit**

```bash
git add core/media/src/main/kotlin/com/elsfm/mobile/core/media/MediaItemFactory.kt \
  core/media/src/test/kotlin/com/elsfm/mobile/core/media/MediaItemFactoryTest.kt
git commit -m "feat(core-media): add Track to MediaItem mapping"
```

---

### Task 3: `core:media` — PlaybackService

**Files:**
- Create: `core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlaybackService.kt`
- Modify: `app/src/main/AndroidManifest.xml`

**Interfaces:**
- Consumes: nothing new (uses Media3's `ExoPlayer`/`MediaSession` directly).
- Produces: `PlaybackService` (a `MediaSessionService`), whose `MediaSession.Token` `feature:player`'s `Media3PlayerController` (Task 5) connects to via `MediaController.Builder`.

- [ ] **Step 1: Implement the service**

`core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlaybackService.kt`:
```kotlin
package com.elsfm.mobile.core.media

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
```

- [ ] **Step 2: Register the service in the app manifest**

Modify `app/src/main/AndroidManifest.xml` — add inside `<application>`, alongside the existing `<activity>` entry:
```xml
<service
    android:name="com.elsfm.mobile.core.media.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">
    <intent-filter>
        <action android:name="androidx.media3.session.MediaSessionService" />
    </intent-filter>
</service>
```

Also add these two permissions as siblings of the existing `<uses-permission>` entries (before `<application>`):
```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

- [ ] **Step 3: Verify the module and app compile**

Run: `./gradlew :core:media:compileDebugKotlin :app:assembleDebug --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`. (No unit test here — a `MediaSessionService` needs a real Android runtime to instantiate; it's covered by the connected-device smoke test in Task 10.)

- [ ] **Step 4: Commit**

```bash
git add core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlaybackService.kt app/src/main/AndroidManifest.xml
git commit -m "feat(core-media): add PlaybackService (MediaSessionService)"
```

---

### Task 4: `core:media` — PlayHistoryApi

**Files:**
- Create: `core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlayHistoryApi.kt`
- Test: `core/media/src/test/kotlin/com/elsfm/mobile/core/media/PlayHistoryApiTest.kt`

**Interfaces:**
- Consumes: `ApiResult<T>` (core:network, from Phase 1).
- Produces: `class PlayHistoryApi @Inject constructor(httpClient: HttpClient) { suspend fun recordPlay(trackId: Int): ApiResult<Unit> }`. Consumed by `feature:player`'s `PlayerViewModel` (Task 6).

**Before writing the implementation**, spend 10-15 minutes live-verifying the real request/response shape of `POST /api/v1/player/tracks` against elsfm.com (open the site, log in if needed, patch `window.fetch` or check the Network tab before clicking Play on a track, and read the actual request body / response). If you can determine the exact body shape, use it. If you genuinely cannot (e.g. no way to inspect a live session in this environment), send the conservative body below and treat any 2xx response as success — do not block this task on it.

- [ ] **Step 1: Write the failing test**

`core/media/src/test/kotlin/com/elsfm/mobile/core/media/PlayHistoryApiTest.kt`:
```kotlin
package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayHistoryApiTest {

    private fun clientReturning(status: HttpStatusCode): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `recordPlay returns Success on 200`() = runTest {
        val api = PlayHistoryApi(clientReturning(HttpStatusCode.OK))

        val result = api.recordPlay(trackId = 1192)

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `recordPlay returns NetworkError on 500`() = runTest {
        val api = PlayHistoryApi(clientReturning(HttpStatusCode.InternalServerError))

        val result = api.recordPlay(trackId = 1192)

        assertTrue(result is ApiResult.NetworkError)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:media:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `PlayHistoryApi` not defined.

- [ ] **Step 3: Implement `PlayHistoryApi`**

`core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlayHistoryApi.kt`:
```kotlin
package com.elsfm.mobile.core.media

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.contentType
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class RecordPlayRequest(val track_id: Int)

class PlayHistoryApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun recordPlay(trackId: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/player/tracks") {
                contentType(ContentType.Application.Json)
                setBody(RecordPlayRequest(track_id = trackId))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: `BUILD SUCCESSFUL`, 2 new tests passing.

- [ ] **Step 5: Commit**

```bash
git add core/media/src/main/kotlin/com/elsfm/mobile/core/media/PlayHistoryApi.kt \
  core/media/src/test/kotlin/com/elsfm/mobile/core/media/PlayHistoryApiTest.kt
git commit -m "feat(core-media): add PlayHistoryApi for play-count recording"
```

---

### Task 5: `feature:player` scaffold — PlayerController interface + Media3 implementation

**Files:**
- Create: `feature/player/build.gradle.kts`
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerState.kt`
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerController.kt`
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/Media3PlayerController.kt`

**Interfaces:**
- Consumes: `Track` (core:model), `PlaybackService` (core:media, for the session token via `SessionToken(context, ComponentName(context, PlaybackService::class.java))`).
- Produces: `interface PlayerController { fun play(track: Track, queue: List<Track>); fun togglePlayPause(); fun seekTo(positionMs: Long); fun skipNext(); fun skipPrevious(); val state: StateFlow<PlayerState> }`, `data class PlayerState(val currentTrack: Track? = null, val isPlaying: Boolean = false, val positionMs: Long = 0, val durationMs: Long = 0, val queue: List<Track> = emptyList(), val error: String? = null)`. Consumed by `PlayerViewModel` (Task 6).

- [ ] **Step 1: Scaffold `feature/player/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.elsfm.mobile.feature.player"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:media"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
```

- [ ] **Step 2: Define `PlayerState`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerState.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import com.elsfm.mobile.core.model.Track

data class PlayerState(
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val queue: List<Track> = emptyList(),
    val error: String? = null,
)
```

- [ ] **Step 3: Define the `PlayerController` interface**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerController.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import com.elsfm.mobile.core.model.Track
import kotlinx.coroutines.flow.StateFlow

interface PlayerController {
    val state: StateFlow<PlayerState>
    fun play(track: Track, queue: List<Track>)
    fun togglePlayPause()
    fun seekTo(positionMs: Long)
    fun skipNext()
    fun skipPrevious()
}
```

- [ ] **Step 4: Implement `Media3PlayerController`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/Media3PlayerController.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.elsfm.mobile.core.media.PlaybackService
import com.elsfm.mobile.core.media.toMediaItem
import com.elsfm.mobile.core.model.Track
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Media3PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerController {

    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state

    private var mediaController: MediaController? = null
    private var currentQueue: List<Track> = emptyList()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
                attachListener()
            },
            MoreExecutors.directExecutor(),
        )
    }

    private fun attachListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _state.value = _state.value.copy(isPlaying = isPlaying)
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val track = currentQueue.find { it.id.toString() == mediaItem?.mediaId }
                _state.value = _state.value.copy(
                    currentTrack = track,
                    durationMs = track?.durationMs ?: 0,
                    positionMs = 0,
                )
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                _state.value = _state.value.copy(error = error.message)
            }
        })
    }

    override fun play(track: Track, queue: List<Track>) {
        currentQueue = queue
        val startIndex = queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        val mediaItems = queue.map { it.toMediaItem() }
        _state.value = _state.value.copy(queue = queue, currentTrack = track, durationMs = track.durationMs)
        mediaController?.apply {
            setMediaItems(mediaItems, startIndex, 0)
            prepare()
            play()
        }
    }

    override fun togglePlayPause() {
        mediaController?.apply { if (isPlaying) pause() else play() }
    }

    override fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
    }

    override fun skipNext() {
        mediaController?.seekToNextMediaItem()
    }

    override fun skipPrevious() {
        mediaController?.seekToPreviousMediaItem()
    }
}
```

- [ ] **Step 5: Verify the module compiles**

Run: `./gradlew :feature:player:compileDebugKotlin --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`. (No unit test for `Media3PlayerController` itself — it needs a real service connection; `PlayerViewModel`'s tests in Task 6 use a hand-written fake instead.)

- [ ] **Step 6: Commit**

```bash
git add feature/player/build.gradle.kts \
  feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerState.kt \
  feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerController.kt \
  feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/Media3PlayerController.kt
git commit -m "feat(feature-player): add PlayerController interface and Media3-backed implementation"
```

---

### Task 6: `feature:player` — PlayerViewModel

**Files:**
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerViewModel.kt`
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/di/PlayerModule.kt`
- Test: `feature/player/src/test/kotlin/com/elsfm/mobile/feature/player/PlayerViewModelTest.kt`

**Interfaces:**
- Consumes: `PlayerController` (Task 5), `PlayHistoryApi` (Task 4, `core:media`).
- Produces: `class PlayerViewModel @Inject constructor(playerController: PlayerController, playHistoryApi: PlayHistoryApi) : ViewModel() { val state: StateFlow<PlayerState>; fun play(track: Track, queue: List<Track>); fun togglePlayPause(); fun seekTo(positionMs: Long); fun skipNext(); fun skipPrevious() }`. Consumed by `PlayerScreen`/`MiniPlayer` (Task 7).

- [ ] **Step 1: Write the failing test**

`feature/player/src/test/kotlin/com/elsfm/mobile/feature/player/PlayerViewModelTest.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import app.cash.turbine.test
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private class FakePlayerController : PlayerController {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state
    var lastPlayedTrack: Track? = null

    override fun play(track: Track, queue: List<Track>) {
        lastPlayedTrack = track
        _state.value = _state.value.copy(currentTrack = track, isPlaying = true, queue = queue)
    }

    override fun togglePlayPause() {
        _state.value = _state.value.copy(isPlaying = !_state.value.isPlaying)
    }

    override fun seekTo(positionMs: Long) {
        _state.value = _state.value.copy(positionMs = positionMs)
    }

    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
}

private class FakePlayHistoryApi(private val result: ApiResult<Unit> = ApiResult.Success(Unit)) {
    var recordedTrackId: Int? = null
    suspend fun recordPlay(trackId: Int): ApiResult<Unit> {
        recordedTrackId = trackId
        return result
    }
}

class PlayerViewModelTest {
    private val track = Track(
        id = 1192,
        name = "Test Track",
        image = null,
        durationMs = 10_000,
        src = "storage/track_media/x.mp3",
        artists = listOf(Artist(id = 1, name = "Test Artist")),
    )

    @Test
    fun `play delegates to controller and records play history`() = runTest {
        val controller = FakePlayerController()
        val playHistoryApi = FakePlayHistoryApi()
        val viewModel = PlayerViewModel(controller, playHistoryApi::recordPlay)

        viewModel.play(track, listOf(track))

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(track, state.currentTrack)
            assertEquals(true, state.isPlaying)
        }
        assertEquals(1192, playHistoryApi.recordedTrackId)
    }

    @Test
    fun `togglePlayPause delegates to controller`() = runTest {
        val controller = FakePlayerController()
        val viewModel = PlayerViewModel(controller) { ApiResult.Success(Unit) }
        viewModel.play(track, listOf(track))

        viewModel.togglePlayPause()

        viewModel.state.test {
            assertEquals(false, awaitItem().isPlaying)
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :feature:player:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `PlayerViewModel` not defined (note the constructor takes a plain suspend lambda for play-history recording in the test — see Step 3 for why).

- [ ] **Step 3: Implement `PlayerViewModel`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerViewModel.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.media.PlayHistoryApi
import com.elsfm.mobile.core.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playerController: PlayerController,
    private val recordPlay: suspend (Int) -> com.elsfm.mobile.core.network.ApiResult<Unit>,
) : ViewModel() {

    @Inject
    constructor(playerController: PlayerController, playHistoryApi: PlayHistoryApi) :
        this(playerController, playHistoryApi::recordPlay)

    val state: StateFlow<PlayerState> = playerController.state

    fun play(track: Track, queue: List<Track>) {
        playerController.play(track, queue)
        viewModelScope.launch { recordPlay(track.id) }
    }

    fun togglePlayPause() = playerController.togglePlayPause()
    fun seekTo(positionMs: Long) = playerController.seekTo(positionMs)
    fun skipNext() = playerController.skipNext()
    fun skipPrevious() = playerController.skipPrevious()
}
```

**Note on the two constructors:** Hilt needs a single `@Inject`-annotated constructor with concrete, injectable types (`PlayHistoryApi`, not a function type it can't resolve). The test needs a plain function so it can pass a fake without wiring a fake `PlayHistoryApi`'s own dependencies. If this dual-constructor shape causes a Hilt processing error when you compile, remove the primary `(PlayerController, suspend (Int) -> ApiResult<Unit>)` constructor and instead make `PlayerViewModel` take `PlayHistoryApi` directly as the single `@Inject` constructor param, and change the test to construct a minimal fake `PlayHistoryApi` subclass/instance instead (adjust `FakePlayHistoryApi` in the test to actually extend or replace `PlayHistoryApi` via the same "fake the boundary interface" pattern used elsewhere — check whether `PlayHistoryApi` needs to become an interface with a real Ktor-backed implementation, mirroring `AuthApiLike`/`AuthApi` from Phase 1, if a plain class can't be faked cleanly). Verify with a real build before deciding which shape to keep.

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: `BUILD SUCCESSFUL`, 2 new tests passing.

- [ ] **Step 5: Add the Hilt binding for `PlayerController`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/di/PlayerModule.kt`:
```kotlin
package com.elsfm.mobile.feature.player.di

import com.elsfm.mobile.feature.player.Media3PlayerController
import com.elsfm.mobile.feature.player.PlayerController
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayerModule {
    @Binds
    @Singleton
    abstract fun bindPlayerController(impl: Media3PlayerController): PlayerController
}
```

- [ ] **Step 6: Verify the module compiles with the DI binding in place**

Run: `./gradlew :feature:player:compileDebugKotlin --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 7: Commit**

```bash
git add feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerViewModel.kt \
  feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/di/PlayerModule.kt \
  feature/player/src/test/kotlin/com/elsfm/mobile/feature/player/PlayerViewModelTest.kt
git commit -m "feat(feature-player): add PlayerViewModel with play-history recording"
```

---

### Task 7: `feature:player` — PlayerScreen and MiniPlayer

**Files:**
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerScreen.kt`
- Create: `feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/MiniPlayer.kt`
- Test: `feature/player/src/androidTest/kotlin/com/elsfm/mobile/feature/player/MiniPlayerTest.kt`

**Interfaces:**
- Consumes: `PlayerViewModel` (Task 6).
- Produces: `@Composable fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel())`, `@Composable fun MiniPlayer(viewModel: PlayerViewModel = hiltViewModel(), onExpandClicked: () -> Unit)`. Consumed by `ElsfmNavHost` (Task 9, `app` module).

- [ ] **Step 1: Implement `PlayerScreen`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerScreen.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PlayerScreen(viewModel: PlayerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(state.currentTrack?.name ?: "Nothing playing", modifier = Modifier.testTag("player_track_title"))
        Text(state.currentTrack?.artists?.firstOrNull()?.name.orEmpty())
        Button(onClick = viewModel::skipPrevious, modifier = Modifier.testTag("player_previous")) { Text("Previous") }
        Button(onClick = viewModel::togglePlayPause, modifier = Modifier.testTag("player_play_pause")) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
        Button(onClick = viewModel::skipNext, modifier = Modifier.testTag("player_next")) { Text("Next") }
    }
}
```

- [ ] **Step 2: Implement `MiniPlayer`**

`feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/MiniPlayer.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun MiniPlayer(
    onExpandClicked: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val track = state.currentTrack ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onExpandClicked)
            .padding(12.dp)
            .testTag("mini_player"),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(track.name)
        Button(onClick = viewModel::togglePlayPause, modifier = Modifier.testTag("mini_player_play_pause")) {
            Text(if (state.isPlaying) "Pause" else "Play")
        }
    }
}
```

- [ ] **Step 3: Write an instrumented smoke test for MiniPlayer's conditional visibility**

`feature/player/src/androidTest/kotlin/com/elsfm/mobile/feature/player/MiniPlayerTest.kt`:
```kotlin
package com.elsfm.mobile.feature.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.assertDoesNotExist
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.elsfm.mobile.core.designsystem.ElsfmTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun miniPlayerIsAbsentWhenNothingIsPlaying() {
        composeTestRule.setContent {
            ElsfmTheme {
                MiniPlayer(onExpandClicked = {}, viewModel = FakeMiniPlayerViewModelHolder.viewModel)
            }
        }
        composeTestRule.onNodeWithTag("mini_player").assertDoesNotExist()
    }
}
```

**Note:** `FakeMiniPlayerViewModelHolder` above is a placeholder name for whatever minimal test-only Hilt or manual-construction shim you need to get a `PlayerViewModel` with an empty `PlayerState` into a Compose test without a real service connection — since `PlayerViewModel` requires a real `PlayerController`/`PlayHistoryApi` via Hilt in production. Check whether `@HiltAndroidTest` + a test-only module overriding `PlayerModule` with a fake `PlayerController` is warranted here, following the same `HiltTestRunner`/`@HiltAndroidTest` pattern used in `app`'s `LoginFlowInstrumentedTest` (Phase 1, Task 14) — adapt that exact pattern rather than inventing a new one.

- [ ] **Step 4: Verify the module and its androidTest APK build**

Run: `./gradlew :feature:player:assembleDebug :feature:player:assembleDebugAndroidTest --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`. (Execution on a device is covered by Task 10's final smoke test.)

- [ ] **Step 5: Commit**

```bash
git add feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/PlayerScreen.kt \
  feature/player/src/main/kotlin/com/elsfm/mobile/feature/player/MiniPlayer.kt \
  feature/player/src/androidTest/kotlin/com/elsfm/mobile/feature/player/MiniPlayerTest.kt
git commit -m "feat(feature-player): add PlayerScreen and MiniPlayer"
```

---

### Task 8: `core:network` — TrackListApi for the Home screen's minimal track list

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/TrackListApi.kt`
- Test: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/TrackListApiTest.kt`

**Interfaces:**
- Consumes: `Track` (core:model, Task 1), `ApiResult<T>` (core:network, Phase 1).
- Produces: `class TrackListApi @Inject constructor(httpClient: HttpClient) { suspend fun getPlaylistTracks(playlistId: Int): ApiResult<List<Track>> }`. Consumed by `app`'s Home track list (Task 9).

**Before writing the implementation**, live-verify the real endpoint against elsfm.com: navigate to a playlist page (e.g. `https://www.elsfm.com/playlist/8/all-sunday-school-songs`), and check `window.bootstrapData.loaders.playlistPage` for the shape (already partially confirmed: `{playlist: {...}, tracks: {data: [...track...]}}`). By direct analogy with the confirmed `GET /api/v1/channel/{channel}` pattern (which returns `{..., content: {data: [...]}}`), the real REST endpoint is very likely `GET /api/v1/playlist/{playlist}`. Try fetching that path directly (e.g. via the browser's network tab while on the playlist page, or `fetch('/api/v1/playlist/8').then(r=>r.json())` from the browser console since you're on an authenticated session) to confirm before finalizing the path in code below. If it differs, use the real path you find instead of the one below.

- [ ] **Step 1: Write the failing test**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/TrackListApiTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackListApiTest {

    private val responseBody = """
        {
          "playlist": {"id": 8, "name": "All Sunday School Songs"},
          "tracks": {
            "data": [
              {
                "id": 1192,
                "name": "Phul Phulyo Bana Pakhama Ashu Jhajhalkyo",
                "image": "storage/track_image_media/abc.jpeg",
                "duration": 174000,
                "src": "storage/track_media/9cw1dTF9NOayHdT4HdyCydn1jgaVJh7Cm9xu4waT.mp3",
                "artists": [{"id": 30, "name": "Sunday School Songs"}]
              }
            ]
          }
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `getPlaylistTracks parses tracks from the response`() = runTest {
        val api = TrackListApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getPlaylistTracks(playlistId = 8)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
        assertEquals("Phul Phulyo Bana Pakhama Ashu Jhajhalkyo", tracks[0].name)
    }

    @Test
    fun `getPlaylistTracks returns NetworkError on failure`() = runTest {
        val api = TrackListApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getPlaylistTracks(playlistId = 8)

        assertTrue(result is ApiResult.NetworkError)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:network:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `TrackListApi` not defined.

- [ ] **Step 3: Implement `TrackListApi`**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/TrackListApi.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class PlaylistTracksData(val data: List<Track>)

@Serializable
private data class PlaylistResponse(val tracks: PlaylistTracksData)

class TrackListApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getPlaylistTracks(playlistId: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/playlist/$playlistId")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<PlaylistResponse>().tracks.data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run the same command as Step 2.
Expected: `BUILD SUCCESSFUL`, 2 new tests passing.

- [ ] **Step 5: Commit**

```bash
git add core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/TrackListApi.kt \
  core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/TrackListApiTest.kt
git commit -m "feat(core-network): add TrackListApi for playlist track listing"
```

---

### Task 9: `app` — wire Home track list, MiniPlayer, and PlayerScreen into navigation

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt`
- Modify: `app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt`
- Create: `app/src/main/kotlin/com/elsfm/mobile/HomeViewModel.kt`

**Interfaces:**
- Consumes: `TrackListApi` (core:network, Task 8), `PlayerViewModel`/`PlayerScreen`/`MiniPlayer` (feature:player, Tasks 6-7).
- Produces: nothing new for later tasks — this is the final integration point for Phase 2.

**Use a hardcoded playlist ID (`8`, "All Sunday School Songs" — a real, live, public playlist verified during Phase 2's research) for this placeholder screen.** Real playlist selection is Phase 3's job.

- [ ] **Step 1: Add `feature:player` and `core:media` to `app/build.gradle.kts`**

Add to the "Project modules" block:
```kotlin
implementation(project(":core:media"))
implementation(project(":feature:player"))
```

- [ ] **Step 2: Add `HomeViewModel`**

`app/src/main/kotlin/com/elsfm/mobile/HomeViewModel.kt`:
```kotlin
package com.elsfm.mobile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.TrackListApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PLACEHOLDER_PLAYLIST_ID = 8

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val trackListApi: TrackListApi,
) : ViewModel() {
    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    init {
        viewModelScope.launch {
            val result = trackListApi.getPlaylistTracks(PLACEHOLDER_PLAYLIST_ID)
            if (result is ApiResult.Success) {
                _tracks.value = result.data
            }
        }
    }
}
```

- [ ] **Step 3: Add a track list to `HomePlaceholderScreen`**

Modify `app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt` to accept an `onTrackClicked` callback and render the tracks from `HomeViewModel`:
```kotlin
package com.elsfm.mobile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.User

@Composable
fun HomePlaceholderScreen(
    user: User,
    onLogoutClicked: () -> Unit,
    onTrackClicked: (Track, List<Track>) -> Unit,
    homeViewModel: HomeViewModel = hiltViewModel(),
) {
    val tracks by homeViewModel.tracks.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text("Logged in as ${user.email}")
        Button(onClick = onLogoutClicked) {
            Text("Log out")
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(tracks) { track ->
                Text(
                    text = track.name,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
```

Note: the `Text` item above needs a click handler wired to `onTrackClicked(track, tracks)` — use `Modifier.clickable { onTrackClicked(track, tracks) }` (add `import androidx.compose.foundation.clickable`) rather than `fillMaxSize()` on the row `Text`, which was left oversimplified above; fix this as you implement so each row is actually tappable.

- [ ] **Step 4: Wire `MiniPlayer` and the player route into `ElsfmNavHost`**

Modify `app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt`: add a `"player"` route rendering `PlayerScreen()`, add `MiniPlayer(onExpandClicked = { navController.navigate("player") })` rendered persistently (e.g. in a `Column` wrapping the `NavHost`, or via a `Scaffold`'s `bottomBar`), and change `HomePlaceholderScreen`'s call site to pass `onTrackClicked = { track, queue -> /* obtain a PlayerViewModel via hiltViewModel() at this composable's scope and call */ playerViewModel.play(track, queue) }`.

- [ ] **Step 5: Verify the full app builds**

Run: `./gradlew :app:assembleDebug --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`. If Hilt reports a `MissingBinding` error (the same class of issue found in Phase 1's Task 14), read the error carefully — it names the exact missing binding — and add it to the appropriate module's `@Binds`/`@Provides` set before proceeding, the same way Task 14 fixed `AuthApiLike`/`DispatcherProvider`.

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle.kts app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt \
  app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt app/src/main/kotlin/com/elsfm/mobile/HomeViewModel.kt
git commit -m "feat(app): wire Home track list, MiniPlayer, and PlayerScreen into navigation"
```

---

### Task 10: Full build, test, and device verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full test suite from a clean state**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
export GRADLE_USER_HOME="/tmp/gradle-home-phase2-final"
./gradlew test :app:assembleDebug --console=plain --no-daemon --rerun-tasks
```
Expected: `BUILD SUCCESSFUL`, 0 failures across every test class in every module (check the actual JUnit XML under each module's `build/test-results/`, not just the console summary).

- [ ] **Step 2: If a device is connected, install and manually verify playback**

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
Launch the app, log in (or confirm session restore), tap a track on the Home screen, confirm audio actually plays, confirm the notification/lock-screen shows playback controls, background the app and confirm playback continues. If no device is available, document this as a carried-forward gap in the plan's final report — do not skip silently.

- [ ] **Step 3: Push to the remote**

```bash
git push origin main
```

- [ ] **Step 4: Commit any final fixes discovered during this task**

If Step 1 or Step 2 surfaced any bugs, fix them with focused commits before pushing, following the same pattern as every prior task.

## Self-Review Notes

- **Spec coverage:** every section of the Phase 2 design doc has a corresponding task — `PlaybackService`/notification (Task 3), `MediaItem` mapping (Task 2), play-history (Task 4, 6), `PlayerController`/`PlayerViewModel` boundary (Tasks 5-6), player UI (Task 7), minimal Home track list (Tasks 8-9), final device verification (Task 10).
- **Placeholder scan:** Task 6 and Task 9 both contain deliberately-flagged "verify and adjust" notes (the dual-constructor Hilt shape, and the click-handler wiring) rather than vague TODOs — these are real, bounded decisions for the implementer to resolve with a concrete fallback given, not open-ended gaps.
- **Type consistency:** `Track`/`Artist` (Task 1) are used identically in Tasks 2, 4, 6, 8, 9. `PlayerState`/`PlayerController` (Task 5) match exactly what `PlayerViewModel` (Task 6) and `PlayerScreen`/`MiniPlayer` (Task 7) consume. `ApiResult<T>` usage matches Phase 1's established shape throughout.
