package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.TrackListApi
import com.elsfm.mobile.core.network.elsfmJson
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

private class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiscoveryViewModelTest {

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
                respond(
                    body,
                    status,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return TrackListApi(httpClient)
    }

    private fun mockProfileApi(status: HttpStatusCode = HttpStatusCode.OK): ProfileApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "data": [
                        {"id": 3, "name": "Track 3", "image": null, "duration": 210000, "plays": "5", "artists": []}
                      ]
                    }
                """.trimIndent()
                respond(
                    body,
                    status,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ProfileApi(httpClient)
    }

    // Home channel (5) response: two nested sub-channels, one playlist-backed
    // ("Kids Zone", id 24) and one album-backed ("New Releases", id 1).
    private val homeChannelsBody = """
        {
          "channel": {
            "id": 5,
            "name": "Nepali Christian Songs",
            "model_type": "channel",
            "content": {
              "data": [
                {"id": 24, "name": "Kids Zone", "model_type": "channel"},
                {"id": 1, "name": "New Release Nepali Christian songs", "model_type": "channel"}
              ]
            }
          }
        }
    """.trimIndent()

    private val playlistChannelBody = """
        {
          "channel": {
            "id": 24,
            "name": "Kids Zone",
            "config": {"contentModel": "playlist"},
            "content": {
              "data": [
                {"id": 8, "name": "All Sunday School Songs", "image": null}
              ]
            }
          }
        }
    """.trimIndent()

    private val albumChannelBody = """
        {
          "channel": {
            "id": 1,
            "name": "New Release Nepali Christian songs",
            "config": {"contentModel": "album"},
            "content": {
              "data": [
                {"id": 460, "name": "2026 EL Shaddai Youth Camp Songs", "image": null, "release_date": "2026-02-08T00:00:00.000000Z"}
              ]
            }
          }
        }
    """.trimIndent()

    private fun mockChannelApi(homeChannelsStatus: HttpStatusCode = HttpStatusCode.OK): ChannelApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { request ->
                val path = request.url.encodedPath
                val body = when {
                    path.endsWith("/channel/5") -> homeChannelsBody
                    path.endsWith("/channel/24") -> playlistChannelBody
                    path.endsWith("/channel/1") -> albumChannelBody
                    else -> "{}"
                }
                val status = if (path.endsWith("/channel/5")) homeChannelsStatus else HttpStatusCode.OK
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun loadHomeSuccessLoadsAllSections() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrackListApi(),
            mockProfileApi(),
            mockChannelApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.featured.size)
        assertEquals("All Sunday School Songs", state.featured[0].name)
        assertEquals(2, state.popular.size)
        assertEquals(1, state.newReleases.size)
        assertEquals("2026 EL Shaddai Youth Camp Songs", state.newReleases[0].name)
        assertEquals(1, state.recentlyPlayed.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadHomeKeepsOtherSectionsWhenPopularTracksFail() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrackListApi(status = HttpStatusCode.InternalServerError),
            mockProfileApi(),
            mockChannelApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.popular.size)
        assertEquals(1, state.recentlyPlayed.size)
        assertEquals(1, state.featured.size)
        assertEquals(1, state.newReleases.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun loadHomeKeepsOtherSectionsWhenHomeChannelsFail() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrackListApi(),
            mockProfileApi(),
            mockChannelApi(homeChannelsStatus = HttpStatusCode.InternalServerError),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.featured.isEmpty())
        assertTrue(state.newReleases.isEmpty())
        assertEquals(2, state.popular.size)
        assertEquals(1, state.recentlyPlayed.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }
}
