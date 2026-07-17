package com.elsfm.mobile.feature.search

import app.cash.turbine.test
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.SearchApi
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun mockSearchApi(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = SUCCESS_BODY,
    ): SearchApi {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return SearchApi(httpClient)
    }

    private fun mockTrackLikeController(status: HttpStatusCode = HttpStatusCode.OK): TrackLikeController {
        val mockEngine = MockEngine { _ ->
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return TrackLikeController(UserApi(httpClient))
    }

    @Test
    fun `search populates tracks artists and playlists buckets`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(), mockTrackLikeController())
        viewModel.state.test {
            val initialState = awaitItem()
            assertEquals("", initialState.query)
            assertEquals(emptyList<Track>(), initialState.tracks)
            assertEquals(emptyList<Album>(), initialState.albums)
            assertEquals(emptyList<Artist>(), initialState.artists)
            assertEquals(emptyList<Playlist>(), initialState.playlists)
            assertFalse(initialState.isLoading)
            assertFalse(initialState.hasSearched)

            viewModel.search("test")
            val loadingState = awaitItem()
            assertEquals("test", loadingState.query)
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertEquals("test", successState.query)
            assertEquals(1, successState.tracks.size)
            assertEquals("Result Track", successState.tracks.first().name)
            assertEquals(1, successState.artists.size)
            assertEquals("Result Artist", successState.artists.first().name)
            assertEquals(1, successState.playlists.size)
            assertEquals("Result Playlist", successState.playlists.first().name)
            assertEquals(emptyList<Album>(), successState.albums)
            assertFalse(successState.isLoading)
            assertTrue(successState.hasSearched)
            assertNull(successState.error)
        }
    }

    @Test
    fun `search with empty results marks hasSearched true with empty buckets`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(body = EMPTY_BODY), mockTrackLikeController())
        viewModel.state.test {
            awaitItem() // initial
            viewModel.search("nomatches")
            awaitItem() // loading

            val successState = awaitItem()
            assertTrue(successState.hasSearched)
            assertTrue(successState.tracks.isEmpty())
            assertTrue(successState.artists.isEmpty())
            assertTrue(successState.playlists.isEmpty())
            assertTrue(successState.albums.isEmpty())
        }
    }

    @Test
    fun `search surfaces error on failure without silently swallowing it`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(status = HttpStatusCode.InternalServerError, body = "{}"), mockTrackLikeController())
        viewModel.state.test {
            awaitItem() // initial
            viewModel.search("test")
            awaitItem() // loading

            val errorState = awaitItem()
            assertFalse(errorState.isLoading)
            assertTrue(errorState.hasSearched)
            assertEquals("Search failed", errorState.error)
            assertTrue(errorState.tracks.isEmpty())
        }
    }

    @Test
    fun `search with blank query resets to initial state without calling api`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(), mockTrackLikeController())
        viewModel.state.test {
            awaitItem() // initial

            viewModel.search("test")
            awaitItem() // loading
            awaitItem() // success

            viewModel.search("")
            val blankState = awaitItem()
            assertEquals("", blankState.query)
            assertTrue(blankState.tracks.isEmpty())
            assertFalse(blankState.hasSearched)
            assertFalse(blankState.isLoading)
        }
    }

    @Test
    fun `clearResults resets state to defaults`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(), mockTrackLikeController())
        viewModel.state.test {
            awaitItem() // initial

            viewModel.search("test")
            awaitItem() // loading
            awaitItem() // success

            viewModel.clearResults()
            val clearedState = awaitItem()
            assertEquals(SearchUiState(), clearedState)
        }
    }

    @Test
    fun `toggleTrackLike adds track to likedTrackIds on success`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(), mockTrackLikeController())
        viewModel.search("test")
        advanceUntilIdle()

        viewModel.toggleTrackLike(100)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.likedTrackIds.contains(100))
        assertFalse(state.likeLoadingTrackIds.contains(100))
    }

    @Test
    fun `toggleTrackLike surfaces error without changing liked state on failure`() = runTest(testDispatcher) {
        val viewModel = SearchViewModel(mockSearchApi(), mockTrackLikeController(HttpStatusCode.InternalServerError))
        viewModel.search("test")
        advanceUntilIdle()

        viewModel.toggleTrackLike(100)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.likedTrackIds.contains(100))
        assertEquals("Failed to update library", state.error)
    }

    private companion object {
        const val SUCCESS_BODY = """
            {
              "query": "test",
              "results": {
                "tracks": {
                  "data": [
                    {
                      "id": 100,
                      "name": "Result Track",
                      "duration": 200000,
                      "plays": "5",
                      "image": "test.jpg",
                      "artists": [{"id": 1, "name": "Artist"}]
                    }
                  ]
                },
                "artists": {"data": [{"id": 5, "name": "Result Artist", "plays": "10"}]},
                "albums": {"data": []},
                "playlists": {"data": [{"id": 9, "name": "Result Playlist"}]},
                "users": {"data": []}
              }
            }
        """

        const val EMPTY_BODY = """
            {
              "query": "nomatches",
              "results": {
                "tracks": {"data": []},
                "artists": {"data": []},
                "albums": {"data": []},
                "playlists": {"data": []},
                "users": {"data": []}
              }
            }
        """
    }
}
