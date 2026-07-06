# Phase 4: Advanced Features & UI Polish — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task.

**Goal:** Add discovery, recommendations, user profile, downloads, and UI polish to create a feature-rich music streaming app matching the Flutter reference.

**Architecture:** Phase 4 builds on Phase 3's navigation foundation. Four new feature modules (feature:discovery, feature:profile, feature:downloads) integrate with expanded API layer (TrendingApi, RecommendationApi, ProfileApi, DownloadManager). Room DB added for offline track metadata. Compose screens follow Material 3 + Flutter reference visual language.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Kotlin Flow, Ktor HTTP client, Media3/ExoPlayer (Phase 2), Serializable with @SerialName.

## Global Constraints

- All models serializable with @Serializable / @SerialName for JSON mapping
- All APIs conservative fallback bodies (no live endpoint verification in CI)
- @HiltViewModel pattern for all new ViewModels with @Inject constructor
- StateFlow for reactive state, immutable updates via .copy()
- Hand-written test doubles (Fake* classes), no mocking frameworks
- Tests: 80%+ coverage, separate Unit and Instrumented suites
- Gradle module dependencies: app depends on all feature:* modules
- Git commits frequent (per task), descriptive messages
- No hardcoded secrets, strings, or magic numbers

---

### Task 1: core:model — New Data Types

**Files:**
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/UserProfile.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/TrendingTrack.kt`
- Create: `core/model/src/main/kotlin/com/elsfm/mobile/core/model/Recommendation.kt`
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/entity/DownloadedTrack.kt`
- Create: `core/model/src/test/kotlin/com/elsfm/mobile/core/model/UserProfileSerializationTest.kt`
- Modify: `core/database/build.gradle.kts` — add Room Compiler annotation processor (if not present)

**Interfaces:**
- Consumes: `Track`, `Playlist` (from Phase 2/3), Serializable infrastructure
- Produces: `UserProfile`, `TrendingTrack`, `Recommendation`, `DownloadedTrack` for later tasks

**Steps:**

- [ ] **Write test first:** `UserProfileSerializationTest.kt`

```kotlin
class UserProfileSerializationTest {
    @Test
    fun deserializeUserProfile() {
        val json = """
        {
            "id": 1,
            "name": "John Doe",
            "email": "john@example.com",
            "image": "https://example.com/avatar.jpg",
            "bio": "Music lover",
            "followers_count": 150,
            "followed_count": 50
        }
        """.trimIndent()
        
        val profile = Json.decodeFromString<UserProfile>(json)
        assert(profile.id == 1)
        assert(profile.name == "John Doe")
        assert(profile.profileImage == "https://example.com/avatar.jpg")
        assert(profile.followersCount == 150)
    }
}
```

- [ ] **Implement UserProfile:**

```kotlin
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
```

- [ ] **Implement TrendingTrack:**

```kotlin
@Serializable
data class TrendingTrack(
    val track: Track,
    @SerialName("rank")
    val position: Int,
)
```

- [ ] **Implement Recommendation:**

```kotlin
@Serializable
data class Recommendation(
    val track: Track,
    @SerialName("score")
    val relevanceScore: Float? = null,
)
```

- [ ] **Implement DownloadedTrack (Room entity):**

```kotlin
@Entity(tableName = "downloaded_tracks")
data class DownloadedTrack(
    @PrimaryKey val trackId: Int,
    val fileName: String,
    val fileSizeBytes: Long,
    @ColumnInfo(name = "downloaded_at")
    val downloadedAt: Long = System.currentTimeMillis(),
)
```

- [ ] **Run test:** `./gradlew :core:model:test -v` → expect PASS

- [ ] **Commit:**

```bash
git add core/model/ core/database/
git commit -m "feat(core:model): add UserProfile, TrendingTrack, Recommendation, DownloadedTrack"
```

---

### Task 2: core:database — Room DAO for Downloads

**Files:**
- Create: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/dao/DownloadedTrackDao.kt`
- Modify: `core/database/src/main/kotlin/com/elsfm/mobile/core/database/ElsfmDatabase.kt` — add DownloadedTrack entity + version increment
- Create: `core/database/src/test/kotlin/com/elsfm/mobile/core/database/DownloadedTrackDaoTest.kt`

**Interfaces:**
- Consumes: `DownloadedTrack` (Task 1), Room framework
- Produces: `DownloadedTrackDao` for DownloadViewModel and download manager

**Steps:**

- [ ] **Write test first:** `DownloadedTrackDaoTest.kt`

```kotlin
@RunWith(RobolectricTestRunner::class)
class DownloadedTrackDaoTest {
    private lateinit var db: ElsfmDatabase
    private lateinit var dao: DownloadedTrackDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            ElsfmDatabase::class.java
        ).build()
        dao = db.downloadedTrackDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveDownload() {
        val track = DownloadedTrack(trackId = 1, fileName = "track.mp3", fileSizeBytes = 5_000_000)
        dao.insert(track)

        val retrieved = dao.getById(1)
        assert(retrieved?.fileName == "track.mp3")
    }

    @Test
    fun deleteDownload() {
        val track = DownloadedTrack(trackId = 1, fileName = "track.mp3", fileSizeBytes = 5_000_000)
        dao.insert(track)
        dao.delete(1)

        val retrieved = dao.getById(1)
        assert(retrieved == null)
    }
}
```

- [ ] **Implement DownloadedTrackDao:**

```kotlin
@Dao
interface DownloadedTrackDao {
    @Insert
    suspend fun insert(track: DownloadedTrack)

    @Query("SELECT * FROM downloaded_tracks ORDER BY downloaded_at DESC")
    fun getAll(): Flow<List<DownloadedTrack>>

    @Query("SELECT * FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun getById(trackId: Int): DownloadedTrack?

    @Query("DELETE FROM downloaded_tracks WHERE trackId = :trackId")
    suspend fun delete(trackId: Int)

    @Query("SELECT SUM(fileSizeBytes) FROM downloaded_tracks")
    fun getTotalSizeBytes(): Flow<Long>
}
```

- [ ] **Update ElsfmDatabase:**

```kotlin
@Database(
    entities = [
        User::class,
        Track::class,
        Artist::class,
        DownloadedTrack::class,  // ADD THIS
    ],
    version = 2,  // BUMP VERSION
    exportSchema = false
)
abstract class ElsfmDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun downloadedTrackDao(): DownloadedTrackDao  // ADD THIS
    
    companion object {
        // ... existing code ...
    }
}
```

- [ ] **Run test:** `./gradlew :core:database:testDebugUnitTest -v` → expect PASS

- [ ] **Commit:**

```bash
git add core/database/
git commit -m "feat(core:database): add DownloadedTrackDao for offline track management"
```

---

### Task 3: core:network — TrendingApi, RecommendationApi, ProfileApi

**Files:**
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/TrendingApi.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/RecommendationApi.kt`
- Create: `core/network/src/main/kotlin/com/elsfm/mobile/core/network/api/ProfileApi.kt`
- Create: test files: `TrendingApiTest.kt`, `RecommendationApiTest.kt`, `ProfileApiTest.kt` (6 tests total: success + 500 error)

**Interfaces:**
- Consumes: `HttpClient`, `TrendingTrack`, `Recommendation`, `UserProfile`
- Produces: Three API classes used by ViewModels in Tasks 4–6

**Steps:**

- [ ] **Write TrendingApiTest:**

```kotlin
class TrendingApiTest {
    @Test
    fun getTrendingSuccess() = runTest {
        val client = mockHttpClient("""
        {
            "data": [
                {"track": {"id": 1, "name": "Song 1"}, "rank": 1},
                {"track": {"id": 2, "name": "Song 2"}, "rank": 2}
            ]
        }
        """)
        val api = TrendingApi(client)
        
        val result = api.getTrending()
        assert(result is ApiResult.Success)
        assert((result as ApiResult.Success).data.size == 2)
    }

    @Test
    fun getTrendingError() = runTest {
        val client = mockHttpClient("", 500)
        val api = TrendingApi(client)
        
        val result = api.getTrending()
        assert(result is ApiResult.NetworkError)
    }
}
```

- [ ] **Implement TrendingApi:**

```kotlin
@Inject
class TrendingApi(private val httpClient: HttpClient) {
    suspend fun getTrending(): ApiResult<List<TrendingTrack>> = try {
        val response = httpClient.get("$BASE_URL/trending") {
            parameter("type", "tracks")
        }.body<ApiResponse<List<TrendingTrack>>>()
        ApiResult.Success(response.data)
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Unknown error")
    }
}
```

- [ ] **Implement RecommendationApi (similar pattern):**

```kotlin
@Inject
class RecommendationApi(private val httpClient: HttpClient) {
    suspend fun getRecommendations(basedOn: Int): ApiResult<List<Recommendation>> = try {
        val response = httpClient.get("$BASE_URL/recommendations") {
            parameter("based_on", basedOn)
            parameter("limit", 10)
        }.body<ApiResponse<List<Recommendation>>>()
        ApiResult.Success(response.data)
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Unknown error")
    }
}
```

- [ ] **Implement ProfileApi:**

```kotlin
@Inject
class ProfileApi(private val httpClient: HttpClient) {
    suspend fun getProfile(): ApiResult<UserProfile> = try {
        val response = httpClient.get("$BASE_URL/me/profile")
            .body<ApiResponse<UserProfile>>()
        ApiResult.Success(response.data)
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Unknown error")
    }

    suspend fun updateProfile(name: String, bio: String?): ApiResult<UserProfile> = try {
        val response = httpClient.put("$BASE_URL/me/profile") {
            setBody(mapOf("name" to name, "bio" to bio))
        }.body<ApiResponse<UserProfile>>()
        ApiResult.Success(response.data)
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Unknown error")
    }

    suspend fun getRecentlyPlayed(): ApiResult<List<Track>> = try {
        val response = httpClient.get("$BASE_URL/me/recently-played")
            .body<ApiResponse<List<Track>>>()
        ApiResult.Success(response.data)
    } catch (e: Exception) {
        ApiResult.NetworkError(e.message ?: "Unknown error")
    }
}
```

- [ ] **Run tests:** `./gradlew :core:network:testDebugUnitTest -v` → expect all 6 tests PASS

- [ ] **Commit:**

```bash
git add core/network/
git commit -m "feat(core:network): add TrendingApi, RecommendationApi, ProfileApi"
```

---

### Task 4: feature:discovery — DiscoveryViewModel

**Files:**
- Create: `feature/discovery/` module (if not present)
- Create: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryViewModel.kt`
- Create: `feature/discovery/src/test/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryViewModelTest.kt`
- Modify: `feature/discovery/build.gradle.kts` — add Hilt, Flow, test deps

**Interfaces:**
- Consumes: `TrendingApi`, `RecommendationApi`, `DispatcherProvider`
- Produces: `DiscoveryViewModel` + `DiscoveryState` for DiscoveryScreen (Task 8)

**Steps:**

- [ ] **Define DiscoveryState:**

```kotlin
@Serializable
data class DiscoveryState(
    val trendingTracks: List<TrendingTrack> = emptyList(),
    val recommendations: List<Recommendation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

- [ ] **Write DiscoveryViewModelTest:**

```kotlin
class DiscoveryViewModelTest {
    @Test
    fun loadTrendingSuccess() = runTest {
        val api = FakeTrendingApi(listOf(TrendingTrack(...), TrendingTrack(...)))
        val vm = DiscoveryViewModel(api, FakeRecommendationApi(), FakeDispatcherProvider())
        
        vm.loadTrending()
        advanceUntilIdle()
        
        val state = vm.state.value
        assert(state.trendingTracks.size == 2)
        assert(!state.isLoading)
        assert(state.error == null)
    }
}
```

- [ ] **Implement DiscoveryViewModel:**

```kotlin
@HiltViewModel
class DiscoveryViewModel @Inject constructor(
    private val trendingApi: TrendingApi,
    private val recommendationApi: RecommendationApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state.asStateFlow()

    init {
        loadTrending()
    }

    fun loadTrending() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = trendingApi.getTrending()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(trendingTracks = result.data, isLoading = false) }
                    loadRecommendations()
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadRecommendations() {
        viewModelScope.launch(dispatcherProvider.io) {
            // Load based on first trending track or default ID
            val trackId = _state.value.trendingTracks.firstOrNull()?.track?.id ?: 1
            when (val result = recommendationApi.getRecommendations(trackId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(recommendations = result.data) }
                }
                is ApiResult.NetworkError -> {
                    // Log but don't fail the entire page
                }
            }
        }
    }
}
```

- [ ] **Run test:** `./gradlew :feature:discovery:testDebugUnitTest -v` → expect PASS

- [ ] **Commit:**

```bash
git add feature/discovery/
git commit -m "feat(feature:discovery): add DiscoveryViewModel for trending + recommendations"
```

---

### Task 5: feature:profile — ProfileViewModel

**Files:**
- Create: `feature/profile/` module (if not present)
- Create: `feature/profile/src/main/kotlin/com/elsfm/mobile/feature/profile/ProfileViewModel.kt`
- Create: `feature/profile/src/test/kotlin/com/elsfm/mobile/feature/profile/ProfileViewModelTest.kt`
- Modify: `feature/profile/build.gradle.kts`

**Interfaces:**
- Consumes: `ProfileApi`, `DispatcherProvider`
- Produces: `ProfileViewModel` + `ProfileState` for ProfileScreen

**Steps:**

- [ ] **Define ProfileState:**

```kotlin
data class ProfileState(
    val userProfile: UserProfile? = null,
    val recentlyPlayed: List<Track> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEditMode: Boolean = false,
)
```

- [ ] **Write ProfileViewModelTest:**

```kotlin
class ProfileViewModelTest {
    @Test
    fun loadProfileSuccess() = runTest {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com")
        val api = FakeProfileApi(profile = profile)
        val vm = ProfileViewModel(api, FakeDispatcherProvider())
        
        advanceUntilIdle()
        
        assert(vm.state.value.userProfile?.name == "John")
    }
}
```

- [ ] **Implement ProfileViewModel:**

```kotlin
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoading = true) }
            when (val result = profileApi.getProfile()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(userProfile = result.data, isLoading = false) }
                    loadRecentlyPlayed()
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(isLoading = false, error = result.message) }
                }
            }
        }
    }

    private fun loadRecentlyPlayed() {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = profileApi.getRecentlyPlayed()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(recentlyPlayed = result.data) }
                }
                is ApiResult.NetworkError -> {}
            }
        }
    }

    fun updateProfile(name: String, bio: String?) {
        viewModelScope.launch(dispatcherProvider.io) {
            when (val result = profileApi.updateProfile(name, bio)) {
                is ApiResult.Success -> {
                    _state.update {
                        it.copy(userProfile = result.data, isEditMode = false)
                    }
                }
                is ApiResult.NetworkError -> {
                    _state.update { it.copy(error = result.message) }
                }
            }
        }
    }
}
```

- [ ] **Run test:** `./gradlew :feature:profile:testDebugUnitTest -v` → expect PASS

- [ ] **Commit:**

```bash
git add feature/profile/
git commit -m "feat(feature:profile): add ProfileViewModel for user profile + recently played"
```

---

### Task 6: feature:downloads — DownloadViewModel

**Files:**
- Create: `feature/downloads/` module (if not present)
- Create: `feature/downloads/src/main/kotlin/com/elsfm/mobile/feature/downloads/DownloadViewModel.kt`
- Create: `feature/downloads/src/main/kotlin/com/elsfm/mobile/core/downloads/DownloadManager.kt` (in core:network or separate feature:downloads)
- Create: test file `DownloadViewModelTest.kt`

**Interfaces:**
- Consumes: `DownloadedTrackDao`, `DownloadManager`, `DispatcherProvider`, File I/O
- Produces: `DownloadViewModel` + `DownloadsState` for DownloadsScreen

**Steps:**

- [ ] **Implement DownloadManager (simple version for Phase 4):**

```kotlin
class DownloadManager @Inject constructor(
    private val context: Context,
    private val httpClient: HttpClient,
    private val dispatcherProvider: DispatcherProvider,
) {
    private val downloadDir = context.getExternalFilesDir("downloads")

    suspend fun downloadTrack(track: Track, onProgress: (Float) -> Unit): Result<File> =
        withContext(dispatcherProvider.io) {
            try {
                val url = "$BASE_URL/tracks/${track.id}/download"
                val file = File(downloadDir, "${track.id}_${track.name.slugify()}.mp3")
                
                httpClient.get(url).bodyAsChannel().use { channel ->
                    file.outputStream().use { fileOut ->
                        val buffer = ByteArray(4096)
                        var bytesRead: Int
                        while (channel.readAvailable(buffer).also { bytesRead = it } >= 0) {
                            fileOut.write(buffer, 0, bytesRead)
                            onProgress((fileOut.channel().size() / channel.contentLength()).toFloat())
                        }
                    }
                }
                Result.success(file)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun deleteDownload(trackId: Int): Boolean =
        withContext(dispatcherProvider.io) {
            downloadDir?.listFiles()?.find { it.name.startsWith(trackId.toString()) }
                ?.delete() ?: false
        }
}
```

- [ ] **Define DownloadsState:**

```kotlin
data class DownloadsState(
    val downloadedTracks: List<DownloadedTrack> = emptyList(),
    val downloadProgress: Map<Int, Float> = emptyMap(), // trackId -> progress 0.0–1.0
    val isLoading: Boolean = false,
    val error: String? = null,
)
```

- [ ] **Write test:**

```kotlin
class DownloadViewModelTest {
    @Test
    fun loadDownloadsSuccess() = runTest {
        val dao = FakeDownloadedTrackDao(
            listOf(DownloadedTrack(trackId = 1, fileName = "track.mp3", fileSizeBytes = 5_000_000))
        )
        val vm = DownloadViewModel(dao, FakeDownloadManager(), FakeDispatcherProvider())
        
        advanceUntilIdle()
        
        assert(vm.state.value.downloadedTracks.size == 1)
    }
}
```

- [ ] **Implement DownloadViewModel:**

```kotlin
@HiltViewModel
class DownloadViewModel @Inject constructor(
    private val downloadedTrackDao: DownloadedTrackDao,
    private val downloadManager: DownloadManager,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        loadDownloads()
    }

    fun loadDownloads() {
        viewModelScope.launch {
            downloadedTrackDao.getAll().collect { tracks ->
                _state.update { it.copy(downloadedTracks = tracks) }
            }
        }
    }

    fun deleteDownload(trackId: Int) {
        viewModelScope.launch(dispatcherProvider.io) {
            downloadManager.deleteDownload(trackId)
            downloadedTrackDao.delete(trackId)
        }
    }

    fun downloadTrack(track: Track) {
        viewModelScope.launch(dispatcherProvider.io) {
            downloadManager.downloadTrack(track) { progress ->
                _state.update {
                    it.copy(downloadProgress = it.downloadProgress + (track.id to progress))
                }
            }.onSuccess { file ->
                val downloaded = DownloadedTrack(
                    trackId = track.id,
                    fileName = file.name,
                    fileSizeBytes = file.length()
                )
                downloadedTrackDao.insert(downloaded)
                _state.update { it.copy(downloadProgress = it.downloadProgress - track.id) }
            }.onFailure { error ->
                _state.update { it.copy(error = error.message) }
            }
        }
    }
}
```

- [ ] **Run test:** `./gradlew :feature:downloads:testDebugUnitTest -v` → expect PASS

- [ ] **Commit:**

```bash
git add feature/downloads/ && git commit -m "feat(feature:downloads): add DownloadViewModel + DownloadManager"
```

---

### Task 7: feature:discovery — DiscoveryScreen + Composables

**Files:**
- Create: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/DiscoveryScreen.kt`
- Create: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/TrendingSection.kt`
- Create: `feature/discovery/src/main/kotlin/com/elsfm/mobile/feature/discovery/RecommendationSection.kt`
- Modify: `feature/discovery/build.gradle.kts` — add Compose

**Interfaces:**
- Consumes: `DiscoveryViewModel`, `DiscoveryState`
- Produces: Composable screens for navigation in Task 11

**Steps:**

- [ ] **Implement DiscoveryScreen:**

```kotlin
@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel = hiltViewModel(),
    onTrackClicked: (Track) -> Unit,
    onArtistClicked: (Int) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.error != null -> {
            ErrorScreen(message = state.error ?: "Unknown error")
        }
        else -> {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text("Trending Now", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    TrendingSection(
                        tracks = state.trendingTracks,
                        onTrackClicked = onTrackClicked,
                        onArtistClicked = onArtistClicked
                    )
                }
                item {
                    Spacer(Modifier.height(24.dp))
                    Text("Recommended For You", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    RecommendationSection(
                        recommendations = state.recommendations,
                        onTrackClicked = onTrackClicked,
                        onArtistClicked = onArtistClicked
                    )
                }
            }
        }
    }
}
```

- [ ] **Implement TrendingSection:**

```kotlin
@Composable
fun TrendingSection(
    tracks: List<TrendingTrack>,
    onTrackClicked: (Track) -> Unit,
    onArtistClicked: (Int) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp)
    ) {
        items(tracks) { trending ->
            TrendingCard(
                trending = trending,
                onTrackClicked = { onTrackClicked(trending.track) },
                onArtistClicked = onArtistClicked
            )
        }
    }
}

@Composable
fun TrendingCard(
    trending: TrendingTrack,
    onTrackClicked: () -> Unit,
    onArtistClicked: (Int) -> Unit,
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable { onTrackClicked() },
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(Modifier.fillMaxWidth().height(160.dp)) {
            // Placeholder image
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                Box(Modifier.fillMaxSize())
            }
            // Rank overlay
            Surface(
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                shape = CircleShape
            ) {
                Text(
                    text = "#${trending.position}",
                    Modifier.padding(8.dp),
                    color = Color.White,
                    fontSize = 12.sp
                )
            }
        }
        Column(Modifier.padding(8.dp)) {
            Text(trending.track.name, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                trending.track.artists.firstOrNull()?.name ?: "Unknown",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
```

- [ ] **Implement RecommendationSection (similar to TrendingSection)**

- [ ] **Run build:** `./gradlew :feature:discovery:assembleDebug -v` → expect BUILD SUCCESSFUL

- [ ] **Commit:**

```bash
git add feature/discovery/
git commit -m "feat(feature:discovery): add DiscoveryScreen, TrendingSection, RecommendationSection"
```

---

### Task 8: feature:profile — ProfileScreen + Composables

**Files:**
- Create: `feature/profile/src/main/kotlin/com/elsfm/mobile/feature/profile/ProfileScreen.kt`
- Create: `feature/profile/src/main/kotlin/com/elsfm/mobile/feature/profile/ProfileHeader.kt`
- Create: `feature/profile/src/main/kotlin/com/elsfm/mobile/feature/profile/RecentlyPlayedSection.kt`
- Modify: `feature/profile/build.gradle.kts` — add Compose

**Interfaces:**
- Consumes: `ProfileViewModel`, `ProfileState`
- Produces: ProfileScreen composable for navigation

**Steps:**

- [ ] **Implement ProfileScreen:**

```kotlin
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onTrackClicked: (Track) -> Unit,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (state.userProfile != null) {
            ProfileHeader(
                profile = state.userProfile!!,
                onLogout = onLogout
            )
            Divider()
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                item {
                    Text("Recently Played", style = MaterialTheme.typography.headlineSmall)
                    RecentlyPlayedSection(
                        tracks = state.recentlyPlayed,
                        onTrackClicked = onTrackClicked
                    )
                }
            }
        } else {
            ErrorScreen(message = state.error ?: "Failed to load profile")
        }
    }
}
```

- [ ] **Implement ProfileHeader:**

```kotlin
@Composable
fun ProfileHeader(profile: UserProfile, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.size(80.dp)
        ) {
            if (!profile.profileImage.isNullOrEmpty()) {
                // Load image (placeholder for now)
                Box(Modifier.fillMaxSize())
            } else {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(profile.name, style = MaterialTheme.typography.headlineSmall)
        Text(profile.email, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(label = "Followers", value = profile.followersCount.toString())
            StatItem(label = "Following", value = profile.followedCount.toString())
        }
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogout, Modifier.fillMaxWidth()) {
            Text("Logout")
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
```

- [ ] **Run build:** `./gradlew :feature:profile:assembleDebug -v` → expect BUILD SUCCESSFUL

- [ ] **Commit:**

```bash
git add feature/profile/
git commit -m "feat(feature:profile): add ProfileScreen, ProfileHeader, RecentlyPlayedSection"
```

---

### Task 9: feature:downloads — DownloadsScreen

**Files:**
- Create: `feature/downloads/src/main/kotlin/com/elsfm/mobile/feature/downloads/DownloadsScreen.kt`
- Create: `feature/downloads/src/main/kotlin/com/elsfm/mobile/feature/downloads/DownloadItem.kt`
- Modify: `feature/downloads/build.gradle.kts` — add Compose

**Interfaces:**
- Consumes: `DownloadViewModel`, `DownloadsState`
- Produces: DownloadsScreen composable

**Steps:**

- [ ] **Implement DownloadsScreen:**

```kotlin
@Composable
fun DownloadsScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    onTrackClicked: (Track) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(Modifier.fillMaxSize()) {
        if (state.downloadedTracks.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No downloads yet", style = MaterialTheme.typography.headlineMedium)
                    Text("Download tracks for offline playback", style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
                items(state.downloadedTracks) { downloaded ->
                    DownloadItem(
                        downloaded = downloaded,
                        progress = state.downloadProgress[downloaded.trackId] ?: 0f,
                        onDelete = { viewModel.deleteDownload(downloaded.trackId) },
                        onPlay = { onTrackClicked(Track(id = downloaded.trackId)) }
                    )
                }
            }
        }
    }
}
```

- [ ] **Implement DownloadItem:**

```kotlin
@Composable
fun DownloadItem(
    downloaded: DownloadedTrack,
    progress: Float,
    onDelete: () -> Unit,
    onPlay: () -> Unit,
) {
    Card(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(Modifier.weight(1f)) {
                Text(downloaded.fileName, style = MaterialTheme.typography.bodyMedium)
                Text(
                    formatFileSize(downloaded.fileSizeBytes),
                    style = MaterialTheme.typography.bodySmall
                )
                if (progress in 0.01f..0.99f) {
                    Spacer(Modifier.height(4.dp))
                    LinearProgressIndicator(progress = progress, Modifier.fillMaxWidth())
                }
            }
            IconButton(onClick = onPlay) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
        }
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> "${bytes / (1024 * 1024)} MB"
    }
}
```

- [ ] **Run build:** `./gradlew :feature:downloads:assembleDebug -v` → expect BUILD SUCCESSFUL

- [ ] **Commit:**

```bash
git add feature/downloads/
git commit -m "feat(feature:downloads): add DownloadsScreen and DownloadItem"
```

---

### Task 10: app — Navigation Wiring (Phase 4 Routes + Bottom Tabs Update)

**Files:**
- Modify: `app/src/main/kotlin/com/elsfm/mobile/ElsfmNavHost.kt` — add 3 new routes (discovery, profile, downloads)
- Modify: `app/build.gradle.kts` — add deps on feature:discovery, feature:profile, feature:downloads
- Modify: bottom navigation to include Discovery, Profile, Downloads tabs (or replace Home with Discovery)

**Interfaces:**
- Consumes: All 3 new feature modules from Tasks 4–9
- Produces: Fully integrated 5-tab navigation (or 4-tab if Home merges with Discovery)

**Steps:**

- [ ] **Update app/build.gradle.kts:**

```gradle
dependencies {
    // ... existing ...
    implementation(project(":feature:discovery"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:downloads"))
}
```

- [ ] **Update ElsfmNavHost — add routes:**

```kotlin
composable("discovery") {
    DiscoveryScreen(
        onTrackClicked = { track ->
            playerViewModel.play(track, queueTracks = emptyList())
        },
        onArtistClicked = { artistId ->
            navController.navigate("artist/$artistId")
        }
    )
}

composable("profile") {
    ProfileScreen(
        onTrackClicked = { track ->
            playerViewModel.play(track, queueTracks = emptyList())
        },
        onLogout = {
            // Clear auth, navigate to login
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    )
}

composable("downloads") {
    DownloadsScreen(
        onTrackClicked = { track ->
            playerViewModel.play(track, queueTracks = emptyList())
        }
    )
}
```

- [ ] **Update bottom navigation bar — 5 tabs:**

Replace old 4-tab bar with 5 tabs: Discovery, Library, Search, Profile, Downloads. Update navigation logic accordingly.

- [ ] **Run build:** `./gradlew :app:assembleDebug --console=plain --no-daemon` → expect BUILD SUCCESSFUL

- [ ] **Commit:**

```bash
git add app/
git commit -m "feat(app): add discovery, profile, downloads routes + 5-tab bottom navigation"
```

---

### Task 11: Full Build, Test, Device Verification & Push

**Files:** (Read-only, verification task)

**Steps:**

- [ ] **Full test suite:**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"
./gradlew clean test :app:assembleDebug --console=plain --no-daemon --rerun-tasks
```

Expected: BUILD SUCCESSFUL, all tests pass, 80%+ coverage.

- [ ] **APK verification:**

```bash
ls -lh app/build/outputs/apk/debug/app-debug.apk
```

Expected: ~18–20 MB (increased from Phase 3 due to new features).

- [ ] **Device installation attempt:**

```bash
adb devices
# If available: adb install -r app-debug.apk
# If unavailable: document as Phase 1/2 constraint
```

- [ ] **Self-test checklist:**

- [ ] Discovery tab loads (Trending + Recommendations visible)
- [ ] Profile tab shows user profile + Recently Played
- [ ] Downloads tab accessible (empty or with cached tracks)
- [ ] All tabs switch smoothly, no ANR
- [ ] MiniPlayer persistent across all tabs
- [ ] Search / Library tabs unchanged from Phase 3

- [ ] **Push:**

```bash
git log --oneline -10  # verify commits
git push origin main
```

Expected: Fast-forward push, all 11 commits from Phase 4.

- [ ] **Final report:**

Write `/Users/siku/Documents/GitHub/elsfm-native/.superpowers/sdd/p4-final-report.md` with:
- Test count + pass rate
- APK size + filename
- Device verification status
- Any regressions or new issues
- Recommendations for Phase 4.1

---

## Success Criteria (Phase 4 Complete)

✅ All 11 tasks implemented, reviewed, and tested  
✅ Full test suite: 80%+ coverage, 0 failures  
✅ APK built (debug or release), ~18–20 MB  
✅ All new features (discovery, profile, downloads) functional  
✅ Navigation tabs working, no regressions from Phases 1–3  
✅ Code pushed to github.com/dewanlabung/elsfm-native main branch  

---

## Execution Handoff

**Ready for subagent-driven development.** Two execution options:

**Option 1: Subagent-Driven (Recommended)**  
Fresh implementer subagent per task, task review after each, continuous execution without pauses.

**Option 2: Inline Execution**  
Execute tasks in this session using the executing-plans skill, batch with checkpoints.

**Which approach?**

