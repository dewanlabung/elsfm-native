package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ProfileApi
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

    private fun mockProfileApi(status: HttpStatusCode = HttpStatusCode.OK): ProfileApi {
        val mockEngine = MockEngine.create {
            dispatcher = testDispatcher
            addHandler { _ ->
                val body = """
                    {
                      "pagination": {
                        "data": [
                          {"id": 3, "name": "Track 3", "image": null, "duration": 210000, "plays": "5", "artists": []}
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
        return ProfileApi(httpClient)
    }

    // Home channel (5) response mirrors the real backend's four nested
    // sub-channels under "Nepali Christian Songs": Kids Zone (playlists),
    // Explore More Channel (playlists), New Release ... songs (albums) and
    // Mostly Played Songs (tracks). Ids match the live API as of this
    // session's verification via `curl .../api/v1/channel/5?loader=channelPage`.
    private val homeChannelsBody = """
        {
          "channel": {
            "id": 5,
            "name": "Nepali Christian Songs",
            "model_type": "channel",
            "content": {
              "data": [
                {"id": 24, "name": "Kids Zone", "model_type": "channel"},
                {"id": 23, "name": "Explore More Channel", "model_type": "channel"},
                {"id": 1, "name": "New Release Nepali Christian songs", "model_type": "channel"},
                {"id": 4, "name": "Mostly Played Songs", "model_type": "channel"}
              ]
            }
          }
        }
    """.trimIndent()

    private val kidsZoneChannelBody = """
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

    private val exploreMoreChannelBody = """
        {
          "channel": {
            "id": 23,
            "name": "Explore More Channel",
            "config": {"contentModel": "playlist"},
            "content": {
              "data": [
                {"id": 25, "name": "Youth Camp Nepali Christian Songs", "image": null},
                {"id": 26, "name": "Holy Convocation Songs", "image": null}
              ]
            }
          }
        }
    """.trimIndent()

    private val newReleasesChannelBody = """
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

    private val mostlyPlayedChannelBody = """
        {
          "channel": {
            "id": 4,
            "name": "Mostly Played Songs",
            "config": {"contentModel": "track"},
            "content": {
              "data": [
                {"id": 1497, "name": "Khrist Timro Ikchya Lai", "image": null, "duration": 180000, "plays": "12", "artists": []},
                {"id": 1506, "name": "Kasei Ma Timi Bich Maya", "image": null, "duration": 200000, "plays": "34", "artists": []}
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
                    path.endsWith("/channel/24") -> kidsZoneChannelBody
                    path.endsWith("/channel/23") -> exploreMoreChannelBody
                    path.endsWith("/channel/1") -> newReleasesChannelBody
                    path.endsWith("/channel/4") -> mostlyPlayedChannelBody
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
    fun loadHomeSuccessLoadsAllSectionsWithRealNames() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockProfileApi(),
            mockChannelApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1, state.kidsZone.size)
        assertEquals("All Sunday School Songs", state.kidsZone[0].name)
        assertEquals("Kids Zone", state.kidsZoneTitle)

        assertEquals(2, state.exploreMoreChannel.size)
        assertEquals("Youth Camp Nepali Christian Songs", state.exploreMoreChannel[0].name)
        assertEquals("Explore More Channel", state.exploreMoreChannelTitle)
        assertEquals(23, state.exploreMoreChannelId)

        assertEquals(1, state.newReleases.size)
        assertEquals("2026 EL Shaddai Youth Camp Songs", state.newReleases[0].name)
        assertEquals("New Release Nepali Christian songs", state.newReleasesTitle)

        assertEquals(2, state.mostlyPlayedSongs.size)
        assertEquals("Khrist Timro Ikchya Lai", state.mostlyPlayedSongs[0].name)
        assertEquals("Mostly Played Songs", state.mostlyPlayedSongsTitle)

        assertEquals(1, state.recentlyPlayed.size)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun loadHomeKeepsOtherSectionsWhenRecentlyPlayedFails() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockProfileApi(status = HttpStatusCode.InternalServerError),
            mockChannelApi(),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(0, state.recentlyPlayed.size)
        assertEquals(2, state.mostlyPlayedSongs.size)
        assertEquals(1, state.kidsZone.size)
        assertEquals(1, state.newReleases.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    @Test
    fun loadHomeKeepsOtherSectionsWhenHomeChannelsFail() = runTest(testDispatcher) {
        val viewModel = DiscoveryViewModel(
            mockProfileApi(),
            mockChannelApi(homeChannelsStatus = HttpStatusCode.InternalServerError),
            FakeDispatcherProvider(testDispatcher),
        )
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state.kidsZone.isEmpty())
        assertTrue(state.exploreMoreChannel.isEmpty())
        assertTrue(state.newReleases.isEmpty())
        assertTrue(state.mostlyPlayedSongs.isEmpty())
        assertEquals(1, state.recentlyPlayed.size)
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }
}
