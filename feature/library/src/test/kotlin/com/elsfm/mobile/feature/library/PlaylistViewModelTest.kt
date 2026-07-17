package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.core.network.api.TrackListApi
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

internal class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakePlaylistUserDao(private var cached: UserEntity?) : UserDao {
    override suspend fun upsert(user: UserEntity) {
        cached = user
    }

    override suspend fun get(): UserEntity? = cached

    override suspend fun clear() {
        cached = null
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaylistViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockTrackListApi(status: HttpStatusCode = HttpStatusCode.OK): TrackListApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "pagination": {
                        "data": [
                          {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []},
                          {"id": 2, "name": "Track 2", "image": null, "duration": 200000, "plays": "34", "artists": []}
                        ]
                      }
                    }
                """.trimIndent()
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return TrackListApi(httpClient)
    }

    /** Page 1 reports a `next_page`; page 2 is the last page. */
    private fun mockPaginatedTrackListApi(): TrackListApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                val page = request.url.parameters["page"]
                val body = if (page == "2") {
                    """
                        {
                          "pagination": {
                            "data": [
                              {"id": 3, "name": "Track 3", "image": null, "duration": 150000, "plays": "5", "artists": []}
                            ]
                          }
                        }
                    """.trimIndent()
                } else {
                    """
                        {
                          "pagination": {
                            "data": [
                              {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []},
                              {"id": 2, "name": "Track 2", "image": null, "duration": 200000, "plays": "34", "artists": []}
                            ],
                            "next_page": 2
                          }
                        }
                    """.trimIndent()
                }
                respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return TrackListApi(httpClient)
    }

    private fun mockUserApi(status: HttpStatusCode = HttpStatusCode.OK): UserApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return UserApi(httpClient)
    }

    private fun mockPlaylistApi(status: HttpStatusCode = HttpStatusCode.OK): PlaylistApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                val body = if (request.url.encodedPath.endsWith("/playlists/42")) {
                    """{"playlist": {"id": 42, "name": "Renamed Playlist"}}"""
                } else {
                    "{}"
                }
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return PlaylistApi(httpClient)
    }

    private fun mockRepostApi(status: HttpStatusCode = HttpStatusCode.OK): RepostApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond("""{"action": "added"}""", status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return RepostApi(httpClient)
    }

    private fun fakeUserDao() = FakePlaylistUserDao(
        UserEntity(id = 7, username = "user", name = "User", email = "u@e.com", avatarUrl = null),
    )

    private fun viewModel(
        status: HttpStatusCode = HttpStatusCode.OK,
        likeStatus: HttpStatusCode = HttpStatusCode.OK,
        playlistApiStatus: HttpStatusCode = HttpStatusCode.OK,
    ) = PlaylistViewModel(
        mockTrackListApi(status),
        mockPlaylistApi(playlistApiStatus),
        TrackLikeController(mockUserApi(likeStatus)),
        fakeDownloadRepository(),
        FakeDispatcherProvider(testDispatcher),
        fakeUserDao(),
        mockRepostApi(),
    )

    private fun paginatedViewModel() = PlaylistViewModel(
        mockPaginatedTrackListApi(),
        mockPlaylistApi(),
        TrackLikeController(mockUserApi()),
        fakeDownloadRepository(),
        FakeDispatcherProvider(testDispatcher),
        fakeUserDao(),
        mockRepostApi(),
    )

    @Test
    fun `loadPlaylist populates playlist and tracks on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(playlist, state.playlist)
        assertEquals(2, state.tracks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadPlaylist sets error when request fails`() = runTest(testDispatcher) {
        val viewModel = viewModel(status = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(playlist, state.playlist)
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `loadPlaylist automatically loads all remaining pages in the background`() = runTest(testDispatcher) {
        // Regression guard: tapping Play right after opening a playlist must queue every
        // track, not just the first page - otherwise background/lock-screen playback
        // stopped after 30 tracks instead of continuing through the whole playlist.
        val viewModel = paginatedViewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(1, 2, 3), state.tracks.map { it.id })
        assertFalse(state.hasMoreTracks)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadPlaylist stops auto-loading after a page fails instead of retrying forever`() = runTest(testDispatcher) {
        // Regression guard: confirmed live - a page that fails to parse (or a real network
        // error) previously re-requested the exact same page in an infinite tight loop,
        // since hasMoreTracks never changes on failure. This test would hang if that
        // regression came back.
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                val page = request.url.parameters["page"]
                if (page == "2") {
                    respond("{}", HttpStatusCode.InternalServerError, headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    val body = """
                        {
                          "pagination": {
                            "data": [
                              {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []}
                            ],
                            "next_page": 2
                          }
                        }
                    """.trimIndent()
                    respond(body, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                }
            }
        }
        val httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { json(elsfmJson()) } }
        val viewModel = PlaylistViewModel(
            TrackListApi(httpClient),
            mockPlaylistApi(),
            TrackLikeController(mockUserApi()),
            fakeDownloadRepository(),
            FakeDispatcherProvider(testDispatcher),
            fakeUserDao(),
            mockRepostApi(),
        )
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.tracks.size)
        assertTrue(state.hasMoreTracks)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadNextPage appends tracks and clears hasMoreTracks on the last page`() = runTest(testDispatcher) {
        val viewModel = paginatedViewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.loadNextPage()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(listOf(1, 2, 3), state.tracks.map { it.id })
        assertFalse(state.hasMoreTracks)
        assertFalse(state.isLoadingMore)
    }

    @Test
    fun `loadNextPage does nothing when hasMoreTracks is false`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hasMoreTracks)

        viewModel.loadNextPage()
        advanceUntilIdle()

        assertEquals(2, viewModel.state.value.tracks.size)
    }

    @Test
    fun `deleteTrack removes track from state and calls backend`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)

        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.deleteTrack(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.tracks.size)
        assertEquals(2, state.tracks.first().id)
        assertNull(state.error)
    }

    @Test
    fun `deleteTrack restores track and sets error when backend call fails`() = runTest(testDispatcher) {
        // Regression guard: deleteTrack used to only update local state without calling the
        // backend at all, so a "removed" track silently reappeared the next time the
        // playlist loaded. Now that it calls the real endpoint, a failure must restore the
        // track rather than leaving the UI showing a removal that never actually happened.
        val viewModel = viewModel(playlistApiStatus = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.deleteTrack(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.tracks.size)
        assertNotNull(state.error)
    }

    @Test
    fun `renamePlaylist updates playlist name on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.renamePlaylist("Renamed Playlist")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Renamed Playlist", state.playlist?.name)
        assertFalse(state.isRenamingPlaylist)
        assertNull(state.error)
    }

    @Test
    fun `renamePlaylist sets error on failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(playlistApiStatus = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.renamePlaylist("Renamed Playlist")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("Test Playlist", state.playlist?.name)
        assertFalse(state.isRenamingPlaylist)
        assertNotNull(state.error)
    }

    @Test
    fun `deletePlaylist sets playlistDeleted on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.deletePlaylist()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.playlistDeleted)
        assertFalse(state.isDeletingPlaylist)
    }

    @Test
    fun `deletePlaylist sets error on failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(playlistApiStatus = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.deletePlaylist()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.playlistDeleted)
        assertFalse(state.isDeletingPlaylist)
        assertNotNull(state.error)
    }

    @Test
    fun `toggleTrackLike adds track to likedTrackIds on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.likedTrackIds.contains(1))
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNull(state.error)
    }

    @Test
    fun `toggleTrackLike removes track from likedTrackIds when already liked`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()
        viewModel.toggleTrackLike(1)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.likedTrackIds.contains(1))

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.likedTrackIds.contains(1))
    }

    @Test
    fun `toggleTrackLike sets error and leaves state unchanged on failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(likeStatus = HttpStatusCode.InternalServerError)
        val playlist = Playlist(id = 42, name = "Test Playlist", image = null)
        viewModel.loadPlaylist(playlist)
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.likedTrackIds.contains(1))
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNotNull(state.error)
    }
}
