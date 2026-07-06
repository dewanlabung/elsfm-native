package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
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
import org.junit.Assert.assertTrue
import org.junit.Test

class UserApiTest {

    private val followResponseBody = """
        {
          "following": true,
          "timestamp": "2024-01-15T10:30:00Z"
        }
    """.trimIndent()

    private val unfollowResponseBody = """
        {
          "following": false,
          "timestamp": "2024-01-15T10:30:00Z"
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `isArtistFollowed returns success with following true`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, followResponseBody))

        val result = api.isArtistFollowed(1)

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertEquals(true, response.following)
        assertEquals("2024-01-15T10:30:00Z", response.timestamp)
    }

    @Test
    fun `followArtist returns success with following true`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, followResponseBody))

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertEquals(true, response.following)
        assertEquals("2024-01-15T10:30:00Z", response.timestamp)
    }

    @Test
    fun `unfollowArtist returns success with following false`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, unfollowResponseBody))

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.Success)
        val response = (result as ApiResult.Success).data
        assertEquals(false, response.following)
        assertEquals("2024-01-15T10:30:00Z", response.timestamp)
    }

    @Test
    fun `isArtistFollowed returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.isArtistFollowed(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `followArtist returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `unfollowArtist returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `isArtistFollowed returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.isArtistFollowed(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `followArtist returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `unfollowArtist returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.NetworkError)
    }
}
