package com.elsfm.mobile.feature.artist

import app.cash.turbine.test
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity
import com.elsfm.mobile.core.database.repository.FollowStateRepository
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.ArtistFollower
import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ArtistApi
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.core.network.api.UserApiLike
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

// Fake implementations for unit testing
private class FakeArtistApi(
    private val artistResult: ApiResult<Artist> = ApiResult.Success(testArtist),
    private val tracksResult: ApiResult<List<Track>> = ApiResult.Success(testTracks),
    private val albumsResult: ApiResult<List<Album>> = ApiResult.Success(testAlbums),
    private val followersResult: ApiResult<List<ArtistFollower>> = ApiResult.Success(testFollowers),
) : ArtistApi(fakeHttpClient) {
    override suspend fun getArtist(id: Int) = artistResult
    override suspend fun getArtistTracks(id: Int) = tracksResult
    override suspend fun getArtistAlbums(id: Int) = albumsResult
    override suspend fun getArtistFollowers(id: Int) = followersResult
}

private fun buildFakeUserApi(): UserApi {
    val mockEngine = MockEngine { _ ->
        respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }
    return UserApi(httpClient)
}

private class FakeFollowStateDao : FollowStateDao {
    override fun observeIsFollowing(artistId: Int): Flow<Boolean> = flowOf(false)
    override suspend fun isFollowing(artistId: Int): Boolean = false
    override suspend fun insertFollowed(entity: FollowedArtistEntity) {}
    override suspend fun deleteFollowed(entity: FollowedArtistEntity) {}
    override suspend fun deleteFollowedByArtistId(artistId: Int) {}
}

private class FakeUserApi(
    private val followError: Exception? = null,
    private val unfollowError: Exception? = null,
) : UserApiLike {
    var followCalled = false
    var unfollowCalled = false

    override suspend fun followArtist(artistId: Int): ApiResult<FollowState> {
        followCalled = true
        if (followError != null) return ApiResult.NetworkError(followError)
        return ApiResult.Success(FollowState(following = true, timestamp = ""))
    }

    override suspend fun unfollowArtist(artistId: Int): ApiResult<FollowState> {
        unfollowCalled = true
        if (unfollowError != null) return ApiResult.NetworkError(unfollowError)
        return ApiResult.Success(FollowState(following = false, timestamp = ""))
    }

    override suspend fun isArtistFollowed(artistId: Int): ApiResult<FollowState> {
        return ApiResult.Success(FollowState(following = false, timestamp = ""))
    }
}

private class FakeFollowStateRepository(
    followError: Exception? = null,
    unfollowError: Exception? = null,
) : FollowStateRepository(
    followStateDao = FakeFollowStateDao(),
    userApi = FakeUserApi(followError, unfollowError)
) {
    val userApi get() = (this as FollowStateRepository).let {
        // Access parent's userApi through reflection or just track it
        when (val api = this::class.java.superclass?.getDeclaredField("userApi")) {
            null -> FakeUserApi()
            else -> {
                api.isAccessible = true
                api.get(this) as FakeUserApi
            }
        }
    }
}

// Fake HttpClient for testing (minimal mock engine)
private val fakeHttpClient = HttpClient(MockEngine { _ ->
    respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
})

private val testArtist = Artist(
    id = 1,
    name = "Test Artist",
    image = null,
    plays = null
)

private val testTracks = listOf(
    Track(id = 1, name = "Track 1", image = null, durationMs = 180000, artists = listOf(testArtist)),
    Track(id = 2, name = "Track 2", image = null, durationMs = 240000, artists = listOf(testArtist)),
)

private val testAlbums = listOf(
    Album(id = 1, name = "Album 1", image = null, releaseDate = "2023"),
    Album(id = 2, name = "Album 2", image = null, releaseDate = "2024"),
)

private val testFollowers = listOf(
    ArtistFollower(id = 10, name = "Follower One", username = "follower1"),
    ArtistFollower(id = 11, name = "Follower Two", username = "follower2"),
)

class ArtistDetailViewModelTest {

    @Test
    fun `loads artist, tracks, and albums successfully in parallel`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals(ArtistDetailState(), initialState)

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertEquals(testArtist, successState.artist)
            assertEquals(testTracks, successState.tracks)
            assertEquals(testAlbums, successState.albums)
            assertFalse(successState.isLoading)
            assertFalse(successState.isFollowLoading)
        }
    }

    @Test
    fun `shows error when artist fetch fails`() = runTest {
        val error = RuntimeException("Network error")
        val fakeApi = FakeArtistApi(artistResult = ApiResult.NetworkError(error))
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading

            val errorState = awaitItem()
            assertTrue(errorState.error?.contains("Network error") ?: false)
            assertFalse(errorState.isLoading)
            assertEquals(null, errorState.artist)
        }
    }

    @Test
    fun `loads artist with partial data when album fetch fails`() = runTest {
        val fakeApi = FakeArtistApi(
            albumsResult = ApiResult.NetworkError(RuntimeException("Album fetch failed"))
        )
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading

            val successState = awaitItem()
            assertEquals(testArtist, successState.artist)
            assertEquals(testTracks, successState.tracks)
            assertEquals(emptyList<Album>(), successState.albums)
            assertFalse(successState.isLoading)
        }
    }

    @Test
    fun `toggleFollow changes follow state`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // success

            viewModel.toggleFollow()
            // Wait for the follow operation to complete
            var updatedState = awaitItem()
            // Check that state changed (either isFollowLoading or followedByUser changed)
            assertTrue(updatedState.isFollowLoading || updatedState.followedByUser)

            if (updatedState.isFollowLoading) {
                // Wait for loading to finish
                updatedState = awaitItem()
            }

            assertTrue(updatedState.followedByUser)
            assertFalse(updatedState.isFollowLoading)
        }
    }

    @Test
    fun `shows loading state during follow operation`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // success

            viewModel.toggleFollow()
            val loadingState = awaitItem()
            assertTrue(loadingState.isFollowLoading)

            val completeState = awaitItem()
            assertFalse(completeState.isFollowLoading)
            assertTrue(completeState.followedByUser)
        }
    }


    @Test
    fun `does nothing when toggleFollow is called without artist`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            val state = awaitItem()
            assertEquals(null, state.artist)

            viewModel.toggleFollow()
            expectNoEvents()

            assertFalse(fakeUserApi.followCalled)
            assertFalse(fakeUserApi.unfollowCalled)
        }
    }

    @Test
    fun `selectTab updates selectedTab and lazily loads followers`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // success, followers not yet loaded

            viewModel.selectTab(ArtistTab.FOLLOWERS)
            val tabSelectedState = awaitItem()
            assertEquals(ArtistTab.FOLLOWERS, tabSelectedState.selectedTab)

            val followersLoadingState = awaitItem()
            assertTrue(followersLoadingState.isFollowersLoading)

            val followersLoadedState = awaitItem()
            assertFalse(followersLoadedState.isFollowersLoading)
            assertEquals(testFollowers, followersLoadedState.followers)
        }
    }

    @Test
    fun `toggleFollowUser marks a follower as followed`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // success

            viewModel.toggleFollowUser(testFollowers[0].id)
            val updatedState = awaitItem()

            assertTrue(updatedState.followedUserIds.contains(testFollowers[0].id))
        }
    }

    @Test
    fun `buildArtistShareUrl returns a slugified client-built link`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle().apply { set("artistId", 1) },
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // success

            assertEquals(
                "https://www.elsfm.com/artist/1/test-artist",
                viewModel.buildArtistShareUrl(),
            )
        }
    }

    @Test
    fun `buildArtistShareUrl returns null when artist has not loaded`() = runTest {
        val fakeApi = FakeArtistApi()
        val fakeUserApi = FakeUserApi()
        val fakeRepository = FollowStateRepository(FakeFollowStateDao(), fakeUserApi)
        val viewModel = ArtistDetailViewModel(
            artistApi = fakeApi,
            followStateRepository = fakeRepository,
            userApi = buildFakeUserApi(),
            savedStateHandle = androidx.lifecycle.SavedStateHandle(),
            dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        )

        assertEquals(null, viewModel.buildArtistShareUrl())
    }
}
