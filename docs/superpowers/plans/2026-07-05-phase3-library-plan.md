# Phase 3: Library, Search & Content Navigation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extend the music app from single-playlist playback to full library discovery, search, artist browsing, and context-aware queueing.

**Architecture:** Layer-by-layer approach—APIs first (data layer), then ViewModels (state + business logic), then UI screens, finally navigation wiring. Each task builds on the previous; all communicate via Hilt DI and StateFlow.

**Tech Stack:** Kotlin, Jetpack Compose, Media3/ExoPlayer (from Phase 2), Hilt, Ktor, Kotlin Serialization, coroutines, StateFlow.

## Global Constraints

- Target SDK: 34+, Min SDK: 26 (verbatim from Phase 2)
- Kotlin: 1.9.20+
- Hilt dependency injection mandatory (@Inject constructors, @Binds in modules)
- Hand-written Fakes only (no mocking frameworks)
- StateFlow/SharedFlow for reactive state
- 80%+ test coverage mandatory
- Media3: 1.5.0
- Room database with singleton cache-key semantics (PrimaryKey cacheKey=0)
- EncryptedSharedPreferences for token storage
- Ktor HTTP client with MockEngine for test isolation
- Immutable data patterns, explicit error handling

---

## Task Overview

| Task | Component | Deliverable |
|------|-----------|-------------|
| 1 | core:model | Channel, Playlist (refined), SearchResult union types |
| 2 | core:network | ChannelApi, SearchApi, ArtistApi (3 new API classes) |
| 3 | feature:library | LibraryViewModel (browse channels → playlists) |
| 4 | feature:search | SearchViewModel (query → mixed results) |
| 5 | feature:artist | ArtistDetailViewModel (artist → tracks) |
| 6 | feature:library | LibraryScreen + ChannelListComposable |
| 7 | feature:search | SearchScreen + SearchResultsComposable |
| 8 | feature:artist | ArtistDetailScreen |
| 9 | app | Navigation wiring (3 new routes) + bottom navigation |
| 10 | (verification) | Full build/test/push (Phase 2 pattern) |

---

### Task 1: core:model — Channel, Playlist, and SearchResult Data Classes

**Files:**
- Modify: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Track.kt` (already exists; Track model unchanged)
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Channel.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Playlist.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/SearchResult.kt`
- Create: `core/model/src/test/kotlin/com/elsfm/mobile/core/model/ChannelSerializationTest.kt`
- Create: `core/model/src/test/kotlin/com/elsfm/mobile/core/model/SearchResultSerializationTest.kt`

**Interfaces:**
- Consumes: `Artist.kt`, `Track.kt` (existing Phase 2 models)
- Produces: `Channel`, `Playlist`, `SearchResult` sealed class; used by Tasks 2–5

- [ ] **Step 1: Write failing test for Channel deserialization**

`core/model/src/test/kotlin/com/elsfm/mobile/core/model/ChannelSerializationTest.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `Channel deserializes from API response`() {
        val json = """
            {
              "id": 1,
              "name": "Sunday School"
            }
        """.trimIndent()

        val channel = this.json.decodeFromString<Channel>(json)

        assertEquals(1, channel.id)
        assertEquals("Sunday School", channel.name)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :core:model:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `Channel` not defined.

- [ ] **Step 3: Implement Channel, Playlist, SearchResult**

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/Channel.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: Int,
    val name: String
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/Playlist.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Int,
    val name: String,
    @SerialName("channel_id")
    val channelId: Int? = null
)
```

`core/model/src/main/kotlin/com/elsfm/mobile/core/model/SearchResult.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SearchResult {
    @Serializable
    @SerialName("track")
    data class TrackResult(val track: Track) : SearchResult()

    @Serializable
    @SerialName("playlist")
    data class PlaylistResult(val playlist: Playlist) : SearchResult()

    @Serializable
    @SerialName("artist")
    data class ArtistResult(val artist: Artist) : SearchResult()

    @Serializable
    @SerialName("channel")
    data class ChannelResult(val channel: Channel) : SearchResult()
}
```

- [ ] **Step 4: Write SearchResult deserialization test**

`core/model/src/test/kotlin/com/elsfm/mobile/core/model/SearchResultSerializationTest.kt`:
```kotlin
package com.elsfm.mobile.core.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchResultSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `SearchResult deserializes TrackResult`() {
        val json = """
            {
              "track": {
                "id": 100,
                "name": "Test Track",
                "duration": 180000,
                "src": "test.mp3",
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Artist"}]
              }
            }
        """.trimIndent()

        val result = this.json.decodeFromString<SearchResult>(json)

        assertTrue(result is SearchResult.TrackResult)
    }
}
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew :core:model:test --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`, all 4 tests (existing + 2 new) passing.

- [ ] **Step 6: Commit**

```bash
git add core/model/src/main/kotlin/com/elsfm/mobile/core/model/*.kt \
  core/model/src/test/kotlin/com/elsfm/mobile/core/model/*SerializationTest.kt
git commit -m "feat(core-model): add Channel, Playlist, and SearchResult types"
```

---

### Task 2: core:network — ChannelApi, SearchApi, ArtistApi

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/ChannelApi.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/SearchApi.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/ArtistApi.kt`
- Create: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/ChannelApiTest.kt`
- Create: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/SearchApiTest.kt`
- Create: `core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/ArtistApiTest.kt`

**Interfaces:**
- Consumes: `ApiResult<T>` (core:network, Phase 1), `Channel`, `Playlist`, `Artist`, `Track`, `SearchResult` (core:model, Task 1)
- Produces: `class ChannelApi { suspend fun getChannels(): ApiResult<List<Channel>> }`, `class SearchApi { suspend fun search(query: String): ApiResult<List<SearchResult>> }`, `class ArtistApi { suspend fun getArtist(id: Int): ApiResult<Artist>; suspend fun getArtistTracks(id: Int): ApiResult<List<Track>> }`

**Before writing**, note: live API verification against elsfm.com is not possible in this environment (same as Phase 2). Use conservative fallback endpoints:
- `GET /api/v1/channel` for listing channels
- `GET /api/v1/search?q={query}` for search
- `GET /api/v1/artist/{id}` and `GET /api/v1/artist/{id}/tracks` for artist data

- [ ] **Step 1: Write failing tests**

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/ChannelApiTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
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

class ChannelApiTest {

    private val responseBody = """
        {
          "data": [
            {"id": 1, "name": "Sunday School"},
            {"id": 2, "name": "Bhajans"}
          ]
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
    fun `getChannels returns list of channels`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getChannels()

        assertTrue(result is ApiResult.Success)
        val channels = (result as ApiResult.Success).data
        assertEquals(2, channels.size)
        assertEquals("Sunday School", channels[0].name)
    }

    @Test
    fun `getChannels returns NetworkError on failure`() = runTest {
        val api = ChannelApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getChannels()

        assertTrue(result is ApiResult.NetworkError)
    }
}
```

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/SearchApiTest.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.SearchResult
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

class SearchApiTest {

    private val responseBody = """
        {
          "data": [
            {
              "track": {
                "id": 100,
                "name": "Search Result Track",
                "duration": 200000,
                "src": "test.mp3",
                "image": "test.jpg",
                "artists": [{"id": 1, "name": "Artist"}]
              }
            }
          ]
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
    fun `search returns mixed results`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.search("query")

        assertTrue(result is ApiResult.Success)
        val results = (result as ApiResult.Success).data
        assertEquals(1, results.size)
    }

    @Test
    fun `search returns NetworkError on failure`() = runTest {
        val api = SearchApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.search("query")

        assertTrue(result is ApiResult.NetworkError)
    }
}
```

`core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/ArtistApiTest.kt`:
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

class ArtistApiTest {

    private val artistResponseBody = """
        {
          "id": 1,
          "name": "Test Artist"
        }
    """.trimIndent()

    private val tracksResponseBody = """
        {
          "data": [
            {
              "id": 100,
              "name": "Track 1",
              "duration": 200000,
              "src": "test1.mp3",
              "image": "test.jpg",
              "artists": [{"id": 1, "name": "Test Artist"}]
            }
          ]
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
    fun `getArtist returns artist detail`() = runTest {
        val api = ArtistApi(clientReturning(HttpStatusCode.OK, artistResponseBody))

        val result = api.getArtist(1)

        assertTrue(result is ApiResult.Success)
        val artist = (result as ApiResult.Success).data
        assertEquals("Test Artist", artist.name)
    }

    @Test
    fun `getArtistTracks returns list of tracks`() = runTest {
        val api = ArtistApi(clientReturning(HttpStatusCode.OK, tracksResponseBody))

        val result = api.getArtistTracks(1)

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew :core:network:test --console=plain --no-daemon --rerun-tasks`
Expected: FAIL — `ChannelApi`, `SearchApi`, `ArtistApi` not defined.

- [ ] **Step 3: Implement APIs**

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/ChannelApi.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class ChannelList(val data: List<Channel>)

class ChannelApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getChannels(): ApiResult<List<Channel>> {
        return try {
            val response = httpClient.get("api/v1/channel")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<ChannelList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/SearchApi.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class SearchResultList(val data: List<SearchResult>)

class SearchApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun search(query: String): ApiResult<List<SearchResult>> {
        return try {
            val response = httpClient.get("api/v1/search?q=$query")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<SearchResultList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
```

`core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/ArtistApi.kt`:
```kotlin
package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class TrackList(val data: List<Track>)

class ArtistApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    suspend fun getArtist(id: Int): ApiResult<Artist> {
        return try {
            val response = httpClient.get("api/v1/artist/$id")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<Artist>())
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    suspend fun getArtistTracks(id: Int): ApiResult<List<Track>> {
        return try {
            val response = httpClient.get("api/v1/artist/$id/tracks")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<TrackList>().data)
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew :core:network:test --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`, all 18 tests (existing 12 + 6 new) passing.

- [ ] **Step 5: Commit**

```bash
git add core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/*Api.kt \
  core/network/src/test/kotlin/com/elsfm/mobile/core/network/api/*ApiTest.kt
git commit -m "feat(core-network): add ChannelApi, SearchApi, and ArtistApi"
```

---

### Task 3: feature:library — LibraryViewModel

**Files:**
- Create: `feature/library/build.gradle.kts` (if not exists; include core:model, core:network, Hilt, coroutines)
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryViewModel.kt`
- Create: `feature/library/src/test/kotlin/com/elsfm/mobile/feature/library/LibraryViewModelTest.kt`

**Interfaces:**
- Consumes: `ChannelApi` (core:network, Task 2), `Channel`, `Playlist` (core:model, Task 1), `DispatcherProvider` (core:common, Phase 1)
- Produces: `class LibraryViewModel { state: StateFlow<LibraryState> }` where `LibraryState` contains `channels: List<Channel>`, `selectedChannelId: Int?`, `playlistsInChannel: List<Playlist>`

- [ ] **Step 1: Create feature:library module**

`feature/library/build.gradle.kts`:
```kotlin
plugins {
    id("com.android.library")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {
    namespace = "com.elsfm.mobile.feature.library"
    compileSdk = 34
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

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
```

- [ ] **Step 2: Write failing test**

`feature/library/src/test/kotlin/com/elsfm/mobile/feature/library/LibraryViewModelTest.kt`:
```kotlin
package com.elsfm.mobile.feature.library

import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
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
import org.junit.Test

class LibraryViewModelTest {

    private fun mockChannelApi(): ChannelApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "data": [
                    {"id": 1, "name": "Sunday School"}
                  ]
                }
            """.trimIndent()
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun `loadChannels updates state with channels`() = runTest {
        val viewModel = LibraryViewModel(mockChannelApi())

        // State should be populated from init
        val channels = viewModel.state.value.channels
        assertEquals(1, channels.size)
        assertEquals("Sunday School", channels[0].name)
    }
}
```

- [ ] **Step 3: Implement LibraryViewModel**

`feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryViewModel.kt`:
```kotlin
package com.elsfm.mobile.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryState(
    val channels: List<Channel> = emptyList(),
    val selectedChannelId: Int? = null,
    val playlistsInChannel: List<Playlist> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val channelApi: ChannelApi,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    init {
        loadChannels()
    }

    fun loadChannels() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val result = channelApi.getChannels()
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(channels = result.data, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load channels"
                )
            }
        }
    }

    fun selectChannel(channelId: Int) {
        _state.value = _state.value.copy(selectedChannelId = channelId)
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :feature:library:test --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`, 1 new test passing.

- [ ] **Step 5: Commit**

```bash
git add feature/library/build.gradle.kts \
  feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryViewModel.kt \
  feature/library/src/test/kotlin/com/elsfm/mobile/feature/library/LibraryViewModelTest.kt
git commit -m "feat(feature-library): add LibraryViewModel for channel browsing"
```

---

### Task 4: feature:search — SearchViewModel

**Files:**
- Create: `feature/search/build.gradle.kts`
- Create: `feature/search/src/main/kotlin/com/elsfm/mobile/feature/search/SearchViewModel.kt`
- Create: `feature/search/src/test/kotlin/com/elsfm/mobile/feature/search/SearchViewModelTest.kt`

**Interfaces:**
- Consumes: `SearchApi` (core:network, Task 2), `SearchResult`, `Track` (core:model)
- Produces: `class SearchViewModel { state: StateFlow<SearchState> }` where `SearchState` contains `query: String`, `results: List<SearchResult>`, `isLoading: Boolean`

- [ ] **Step 1: Create feature:search module**

`feature/search/build.gradle.kts` (identical structure to feature:library).

- [ ] **Step 2: Write failing test**

```kotlin
package com.elsfm.mobile.feature.search

import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
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
import org.junit.Test

class SearchViewModelTest {

    private fun mockSearchApi(): SearchApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "data": [
                    {
                      "track": {
                        "id": 100,
                        "name": "Result Track",
                        "duration": 200000,
                        "src": "test.mp3",
                        "image": "test.jpg",
                        "artists": [{"id": 1, "name": "Artist"}]
                      }
                    }
                  ]
                }
            """.trimIndent()
            respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return SearchApi(httpClient)
    }

    @Test
    fun `search updates results`() = runTest {
        val viewModel = SearchViewModel(mockSearchApi())
        viewModel.search("test")

        // Results should be populated
        val results = viewModel.state.value.results
        assertEquals(1, results.size)
    }
}
```

- [ ] **Step 3: Implement SearchViewModel**

```kotlin
package com.elsfm.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchState(
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchApi: SearchApi,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun search(query: String) {
        _state.value = _state.value.copy(query = query, isLoading = true)
        viewModelScope.launch {
            val result = searchApi.search(query)
            if (result is ApiResult.Success) {
                _state.value = _state.value.copy(results = result.data, isLoading = false)
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Search failed"
                )
            }
        }
    }

    fun clearSearch() {
        _state.value = SearchState()
    }
}
```

- [ ] **Step 4–5: Test + Commit** (per standard pattern)

Run: `./gradlew :feature:search:test --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`, 1 new test passing.

Commit: `git add feature/search/... && git commit -m "feat(feature-search): add SearchViewModel for content search"`

---

### Task 5: feature:artist — ArtistDetailViewModel

**Files:**
- Create: `feature/artist/build.gradle.kts`
- Create: `feature/artist/src/main/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailViewModel.kt`
- Create: `feature/artist/src/test/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailViewModelTest.kt`

**Interfaces:**
- Consumes: `ArtistApi` (core:network, Task 2), `Artist`, `Track` (core:model)
- Produces: `class ArtistDetailViewModel { state: StateFlow<ArtistDetailState> }`

- [ ] **Step 1–5: Follow pattern from Tasks 3–4**

Key differences:
- Constructor takes `artistId: Int` (saved state or nav param)
- `loadArtist(id)` + `loadTracks(id)` called in init
- State: `artist: Artist?`, `tracks: List<Track>`, `isLoading: Boolean`

Commit: `git add feature/artist/... && git commit -m "feat(feature-artist): add ArtistDetailViewModel for artist browsing"`

---

### Task 6: feature:library — LibraryScreen + ChannelListComposable

**Files:**
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryScreen.kt`
- Create: `feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/ChannelListComposable.kt`

**Interfaces:**
- Consumes: `LibraryViewModel`, `PlayerViewModel` (from Phase 2), `Channel`, `Playlist`
- Produces: `@Composable fun LibraryScreen(...)`

- [ ] **Step 1: Write LibraryScreen as top-level entry**

```kotlin
package com.elsfm.mobile.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun LibraryScreen(
    onChannelSelected: (channelId: Int) -> Unit,
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by libraryViewModel.state.collectAsState()

    LazyColumn {
        items(state.channels) { channel ->
            Text(
                text = channel.name,
                modifier = Modifier.clickable {
                    libraryViewModel.selectChannel(channel.id)
                    onChannelSelected(channel.id)
                },
            )
        }
    }
}
```

- [ ] **Step 2–5: Build + Test + Commit**

Commit: `git add feature/library/src/main/kotlin/com/elsfm/mobile/feature/library/LibraryScreen.kt && git commit -m "feat(feature-library): add LibraryScreen for channel browsing"`

---

### Task 7: feature:search — SearchScreen + SearchResultsComposable

**Files:**
- Create: `feature/search/src/main/kotlin/com/elsfm/mobile/feature/search/SearchScreen.kt`

**Interfaces:**
- Consumes: `SearchViewModel`, `PlayerViewModel`
- Produces: `@Composable fun SearchScreen(...)`

- [ ] **Step 1: Write SearchScreen**

```kotlin
@Composable
fun SearchScreen(
    onTrackClicked: (track: Track, queue: List<Track>) -> Unit,
    onArtistClicked: (artistId: Int) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val state by searchViewModel.state.collectAsState()

    Column {
        TextField(
            value = query,
            onValueChange = {
                query = it
                searchViewModel.search(it)
            },
            label = { Text("Search") },
        )
        LazyColumn {
            items(state.results) { result ->
                when (result) {
                    is SearchResult.TrackResult -> {
                        Text(
                            text = result.track.name,
                            modifier = Modifier.clickable {
                                onTrackClicked(result.track, state.results.filterIsInstance<SearchResult.TrackResult>().map { it.track })
                            },
                        )
                    }
                    is SearchResult.ArtistResult -> {
                        Text(
                            text = result.artist.name,
                            modifier = Modifier.clickable {
                                onArtistClicked(result.artist.id)
                            },
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}
```

- [ ] **Step 2–5: Build + Test + Commit**

Commit: `git add feature/search/src/main/kotlin/com/elsfm/mobile/feature/search/SearchScreen.kt && git commit -m "feat(feature-search): add SearchScreen for content search"`

---

### Task 8: feature:artist — ArtistDetailScreen

**Files:**
- Create: `feature/artist/src/main/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailScreen.kt`

**Interfaces:**
- Consumes: `ArtistDetailViewModel`, `PlayerViewModel`
- Produces: `@Composable fun ArtistDetailScreen(...)`

- [ ] **Step 1: Write ArtistDetailScreen**

```kotlin
@Composable
fun ArtistDetailScreen(
    artistId: Int,
    onTrackClicked: (track: Track, queue: List<Track>) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(key = artistId.toString()),
) {
    val state by viewModel.state.collectAsState()

    Column {
        state.artist?.let { artist ->
            Text(text = artist.name)
        }
        LazyColumn {
            items(state.tracks) { track ->
                Text(
                    text = track.name,
                    modifier = Modifier.clickable {
                        onTrackClicked(track, state.tracks)
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 2–5: Build + Test + Commit**

Commit: `git add feature/artist/src/main/kotlin/com/elsfm/mobile/feature/artist/ArtistDetailScreen.kt && git commit -m "feat(feature-artist): add ArtistDetailScreen for artist pages"`

---

### Task 9: app — Navigation Wiring (3 new routes + bottom tab nav)

**Files:**
- Modify: `app/build.gradle.kts` (add feature:library, feature:search, feature:artist deps)
- Modify: `app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt` (add 3 routes, bottom nav tabs)
- Modify: `app/src/main/kotlin/com/elsfm/mobile/HomePlaceholderScreen.kt` (integrate with new tabs)

**Interfaces:**
- Consumes: LibraryScreen, SearchScreen, ArtistDetailScreen, PlayerViewModel

- [ ] **Step 1: Add dependencies**

In `app/build.gradle.kts`, add to "Project modules":
```kotlin
implementation(project(":feature:library"))
implementation(project(":feature:search"))
implementation(project(":feature:artist"))
```

- [ ] **Step 2: Update ElsfmNavHost**

Add routes:
```kotlin
composable("library") {
    LibraryScreen(
        onChannelSelected = { /* TODO: show playlists */ }
    )
}
composable("search") {
    SearchScreen(
        onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
        onArtistClicked = { artistId -> navController.navigate("artist/$artistId") },
    )
}
composable("artist/{artistId}") { backStackEntry ->
    val artistId = backStackEntry.arguments?.getString("artistId")?.toIntOrNull() ?: return@composable
    ArtistDetailScreen(
        artistId = artistId,
        onTrackClicked = { track, queue -> playerViewModel.play(track, queue) },
    )
}
```

- [ ] **Step 3: Add bottom navigation tabs**

Wrap NavHost in a `Scaffold` with `bottomBar` showing 3 tabs (Home, Library, Search).

- [ ] **Step 4–5: Build + Commit**

Run: `./gradlew :app:assembleDebug --console=plain --no-daemon --rerun-tasks`
Expected: `BUILD SUCCESSFUL`, no Hilt MissingBinding errors.

Commit: `git add app/build.gradle.kts app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt && git commit -m "feat(app): add library, search, and artist navigation routes with bottom tabs"`

---

### Task 10: Full Build, Test, and Push

**Files:** none (verification only).

- [ ] **Step 1: Run full test suite**

```bash
./gradlew test :app:assembleDebug --console=plain --no-daemon --rerun-tasks
```

Expected: `BUILD SUCCESSFUL`, all tests passing (target: 80%+ coverage).

- [ ] **Step 2: Device check (if available)**

Same as Phase 2 Task 10 — document if no device.

- [ ] **Step 3: Push to remote**

```bash
git push origin main
```

---

## Global Test Coverage Target

**80%+ across all modules:**
- core:model: 100% (serialization tests for all new types)
- core:network: 100% (API tests for all new endpoints)
- feature:library: 80%+ (ViewModel + Screen tests)
- feature:search: 80%+ (ViewModel + Screen tests)
- feature:artist: 80%+ (ViewModel + Screen tests)
- app: N/A (UI/nav wiring, tested via compile + assemble)

---

## Known Gaps & Future Work

1. **Live API verification** — Same as Phase 2: endpoints are conservative fallbacks. Real device testing required.
2. **Playlist detail screen** — Task 9 references "show playlists in channel" but doesn't implement the detail screen. This is intentional; real playlist detail reuses the player logic from Phase 2. Can be added as Phase 3.5 if needed.
3. **Album support** — Phase 3 brief mentions "Album" but this plan doesn't include a separate Album model/screen. Confirm scope with stakeholder; if albums are distinct from playlists, add Task 11.

---

**END OF PLAN**

Ready for execution via superpowers:subagent-driven-development. Each task is 30–60 minutes for an experienced implementer.
