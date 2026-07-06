package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.common.DispatcherProvider
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

    @Test
    fun loadHomeSuccessLoadsAllSections() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrackListApi(),
            mockProfileApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.featured.isNotEmpty())
        assertEquals(2, state.popular.size)
        assertTrue(state.newReleases.isNotEmpty())
        assertEquals(1, state.recentlyPlayed.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadHomeShowsSampleSectionsWhenPopularTracksFail() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrackListApi(status = HttpStatusCode.InternalServerError),
            mockProfileApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.popular.size)
        assertEquals(1, state.recentlyPlayed.size)
        assertTrue(state.featured.isNotEmpty())
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }
}
