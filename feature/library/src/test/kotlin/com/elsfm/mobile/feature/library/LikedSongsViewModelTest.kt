package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.core.network.elsfmJson
import com.elsfm.mobile.feature.library.data.TrackLikeController
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class FakeUserDao(private var cached: UserEntity?) : UserDao {
    override suspend fun upsert(user: UserEntity) {
        cached = user
    }

    override suspend fun get(): UserEntity? = cached

    override suspend fun clear() {
        cached = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class LikedSongsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockUserApi(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = likedTracksBody,
    ): UserApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return UserApi(httpClient)
    }

    private val likedTracksBody = """
        {
          "pagination": {
            "data": [
              {"id": 1, "name": "Liked Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []},
              {"id": 2, "name": "Liked Track 2", "image": null, "duration": 200000, "plays": "34", "artists": []}
            ]
          }
        }
    """.trimIndent()

    private fun viewModel(
        status: HttpStatusCode = HttpStatusCode.OK,
        likeStatus: HttpStatusCode = HttpStatusCode.OK,
        userDao: UserDao = FakeUserDao(UserEntity(id = 7, username = "user", name = "User", email = "u@e.com", avatarUrl = null)),
    ) = LikedSongsViewModel(
        mockUserApi(status),
        userDao,
        TrackLikeController(mockUserApi(likeStatus)),
        FakeDispatcherProvider(testDispatcher),
    )

    @Test
    fun `loadLikedSongs populates tracks on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.tracks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadLikedSongs sets error when request fails`() = runTest(testDispatcher) {
        val viewModel = viewModel(status = HttpStatusCode.InternalServerError)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `loadLikedSongs sets error when no user is signed in`() = runTest(testDispatcher) {
        val viewModel = viewModel(userDao = FakeUserDao(null))

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `onSearchQueryChanged filters tracks by name`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Track 1")

        val filtered = viewModel.state.value.filteredTracks
        assertEquals(1, filtered.size)
        assertEquals("Liked Track 1", filtered.first().name)
    }

    @Test
    fun `onSearchQueryChanged with blank query returns all tracks`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("")

        assertEquals(2, viewModel.state.value.filteredTracks.size)
    }

    @Test
    fun `toggleTrackLike removes track from list on successful unlike`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.tracks.size)
        assertEquals(2, state.tracks.first().id)
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNull(state.error)
    }

    @Test
    fun `toggleTrackLike keeps track and sets error on failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(likeStatus = HttpStatusCode.InternalServerError)
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.tracks.size)
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNotNull(state.error)
    }
}
