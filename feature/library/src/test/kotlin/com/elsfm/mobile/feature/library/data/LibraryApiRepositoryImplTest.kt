package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.model.Channel
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

    private fun mockChannelApi(status: HttpStatusCode = HttpStatusCode.OK): ChannelApi {
        val mockEngine = MockEngine { _ ->
            val body = """
                {
                  "channel": {
                    "id": 5,
                    "name": "Nepali Christian Songs",
                    "model_type": "channel",
                    "content": {
                      "data": [
                        {"id": 1, "name": "Mostly Played Songs", "model_type": "channel"}
                      ]
                    }
                  }
                }
            """.trimIndent()
            respond(
                body,
                status,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return ChannelApi(httpClient)
    }

    @Test
    fun `loadLibrary returns playlists, albums and channels on success`() = runTest {
        val channelApi = mockChannelApi()
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertTrue(data.playlists.isNotEmpty())
        assertTrue(data.albums.isNotEmpty())
        assertEquals(1, data.channels.size)
        assertEquals("Mostly Played Songs", data.channels[0].name)
    }

    @Test
    fun `loadLibrary returns NetworkError when channels API fails`() = runTest {
        val channelApi = mockChannelApi(status = HttpStatusCode.InternalServerError)
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.NetworkError)
        assertNotNull((result as ApiResult.NetworkError).cause)
    }

    @Test
    fun `loadLibrary includes all sample playlists`() = runTest {
        val channelApi = mockChannelApi()
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(4, data.playlists.size)
        assertEquals("Sunday Worship Favorites", data.playlists[0].name)
        assertEquals("Acoustic Praise", data.playlists[1].name)
    }

    @Test
    fun `loadLibrary includes all sample albums`() = runTest {
        val channelApi = mockChannelApi()
        val repository = LibraryApiRepositoryImpl(channelApi)

        val result = repository.loadLibrary()

        assertTrue(result is ApiResult.Success)
        val data = (result as ApiResult.Success).data
        assertEquals(3, data.albums.size)
        assertEquals("New Beginnings", data.albums[0].name)
        assertEquals("Rise Up", data.albums[1].name)
    }
}
