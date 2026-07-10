package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryApiRepositoryImplTest {

    // Mirrors the real backend's Channel 5 nested sub-channels: Kids Zone
    // and Explore More Channel are playlist-backed, New Release ... songs is
    // album-backed, Mostly Played Songs is track-backed (contributes to
    // neither playlists nor albums).
    private val homeChannelsBody = """
        {
          "channel": {
            "id": 5,
            "name": "Nepali Christian Songs",
            "model_type": "channel",
            "content": {
              "data": [
                {"id": 24, "name": "Kids Zone", "model_type": "channel"},
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
                {"id": 8, "name": "All Sunday School Songs", "image": null},
                {"id": 9, "name": "Nepali Sunday School songs", "image": null}
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
                {"id": 1497, "name": "Khrist Timro Ikchya Lai", "image": null, "duration": 180000, "plays": "12", "artists": []}
              ]
            }
          }
        }
    """.trimIndent()

    private fun mockChannelApi(homeChannelsStatus: HttpStatusCode = HttpStatusCode.OK): ChannelApi {
        val mockEngine = MockEngine { request ->
            val path = request.url.encodedPath
            val body = when {
                path.endsWith("/channel/5") -> homeChannelsBody
                path.endsWith("/channel/24") -> kidsZoneChannelBody
                path.endsWith("/channel/1") -> newReleasesChannelBody
                path.endsWith("/channel/4") -> mostlyPlayedChannelBody
                else -> "{}"
            }
            val status = if (path.endsWith("/channel/5")) homeChannelsStatus else HttpStatusCode.OK
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun `loadLibrary returns playlists, albums and channels aggregated from real channel content`() = runTest {
        val channelApi = mockChannelApi()
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(3, data.channels.size)

        assertEquals(2, data.playlists.size)
        assertEquals("All Sunday School Songs", data.playlists[0].name)
        assertEquals("Nepali Sunday School songs", data.playlists[1].name)

        assertEquals(1, data.albums.size)
        assertEquals("2026 EL Shaddai Youth Camp Songs", data.albums[0].name)
    }

    @Test
    fun `loadLibrary returns NetworkError when channels API fails`() = runTest {
        val channelApi = mockChannelApi(homeChannelsStatus = HttpStatusCode.InternalServerError)
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
        assertNotNull((result as ApiResult.NetworkError).cause)
    }
}
