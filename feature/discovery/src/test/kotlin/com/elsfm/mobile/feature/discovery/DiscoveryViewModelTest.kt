package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.api.ChannelApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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

    private fun mockChannelApi(status: HttpStatusCode = HttpStatusCode.OK): ChannelApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "channel": {
                        "id": 5,
                        "name": "Nepali Christian Songs",
                        "model_type": "channel",
                        "content": {
                          "data": [
                            {"id": 1, "name": "Sunday School", "model_type": "channel"},
                            {"id": 2, "name": "Bhajans", "model_type": "channel"}
                          ]
                        }
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
        return ChannelApi(httpClient)
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

    @Test
    fun loadHomeSuccessLoadsChannelsAndTracks() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockChannelApi(),
            mockTrackListApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.featuredChannels.size)
        assertEquals(2, state.popularTracks.size)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadHomeShowsPopularTracksWhenChannelsFail() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockChannelApi(status = HttpStatusCode.InternalServerError),
            mockTrackListApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.featuredChannels.size)
        assertEquals(2, state.popularTracks.size)
        assertEquals(false, state.isLoading)
        assertNotNull(state.error)
    }
}
