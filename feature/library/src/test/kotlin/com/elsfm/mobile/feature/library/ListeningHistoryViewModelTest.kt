package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.network.api.ProfileApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ListeningHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val historyBody = """
        {
          "data": [
            {"id": 1, "name": "History Track 1", "image": null, "duration": 180000, "plays": "12", "artists": [{"id": 9, "name": "Artist One"}]},
            {"id": 2, "name": "History Track 2", "image": null, "duration": 200000, "plays": "34", "artists": [{"id": 10, "name": "Artist Two"}]}
          ]
        }
    """.trimIndent()

    private fun mockProfileApi(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = historyBody,
    ): ProfileApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ProfileApi(httpClient)
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

    private fun viewModel(
        status: HttpStatusCode = HttpStatusCode.OK,
        likeStatus: HttpStatusCode = HttpStatusCode.OK,
    ) = ListeningHistoryViewModel(
        mockProfileApi(status),
        TrackLikeController(mockUserApi(likeStatus)),
        FakeDispatcherProvider(testDispatcher),
    )

    @Test
    fun `loadHistory populates tracks on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.tracks.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadHistory sets error when request fails`() = runTest(testDispatcher) {
        val viewModel = viewModel(status = HttpStatusCode.InternalServerError)

        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.tracks.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun `onSearchQueryChanged filters tracks by name or artist`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("Artist Two")

        val filtered = viewModel.state.value.filteredTracks
        assertEquals(1, filtered.size)
        assertEquals("History Track 2", filtered.first().name)
    }

    @Test
    fun `onSearchQueryChanged with blank query returns all tracks`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("")

        assertEquals(2, viewModel.state.value.filteredTracks.size)
    }

    @Test
    fun `toggleTrackLike adds track to likedTrackIds on success`() = runTest(testDispatcher) {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.likedTrackIds.contains(1))
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNull(state.error)
    }

    @Test
    fun `toggleTrackLike sets error and leaves state unchanged on failure`() = runTest(testDispatcher) {
        val viewModel = viewModel(likeStatus = HttpStatusCode.InternalServerError)
        advanceUntilIdle()

        viewModel.toggleTrackLike(1)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.likedTrackIds.contains(1))
        assertFalse(state.likeLoadingTrackIds.contains(1))
        assertNotNull(state.error)
    }
}
