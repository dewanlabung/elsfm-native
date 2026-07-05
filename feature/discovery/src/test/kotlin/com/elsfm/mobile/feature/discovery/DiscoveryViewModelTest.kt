package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.api.RecommendationApi
import com.elsfm.mobile.core.network.api.TrendingApi
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

    private fun mockTrendingApi(): TrendingApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "data": [
                        {"track": {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "src": "storage/t1.mp3", "artists": []}, "rank": 1},
                        {"track": {"id": 2, "name": "Track 2", "image": null, "duration": 200000, "src": "storage/t2.mp3", "artists": []}, "rank": 2}
                      ]
                    }
                """.trimIndent()
                respond(
                    body,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return TrendingApi(httpClient)
    }

    private fun mockRecommendationApi(): RecommendationApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """{ "data": [] }"""
                respond(
                    body,
                    HttpStatusCode.OK,
                    headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
        return RecommendationApi(httpClient)
    }

    @Test
    fun loadTrendingSuccess() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockTrendingApi(),
            mockRecommendationApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(2, state.trendingTracks.size)
        assertEquals(false, state.isLoading)
        assertNull(state.error)
    }
}
