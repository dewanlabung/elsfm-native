package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
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

    private fun clientCapturingRequest(
        status: HttpStatusCode,
        body: String,
        onRequest: (HttpRequestData) -> Unit,
    ): HttpClient {
        val mockEngine = MockEngine { request ->
            onRequest(request)
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
        val followState = (result as ApiResult.Success).data
        assertEquals(true, followState.following)
        assertEquals("2024-01-15T10:30:00Z", followState.timestamp)
    }

    @Test
    fun `followArtist returns success with following true`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, followResponseBody))

        val result = api.followArtist(1)

        assertTrue(result is ApiResult.Success)
        val followState = (result as ApiResult.Success).data
        assertEquals(true, followState.following)
        assertEquals("2024-01-15T10:30:00Z", followState.timestamp)
    }

    @Test
    fun `unfollowArtist returns success with following false`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.OK, unfollowResponseBody))

        val result = api.unfollowArtist(1)

        assertTrue(result is ApiResult.Success)
        val followState = (result as ApiResult.Success).data
        assertEquals(false, followState.following)
        assertEquals("2024-01-15T10:30:00Z", followState.timestamp)
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

    private val successBody = """{"status": "success"}"""

    @Test
    fun `addTrackToLibrary posts likeables payload to add-to-library endpoint`() = runTest {
        var capturedPath: String? = null
        var capturedBody: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
                capturedBody = (request.body as? TextContent)?.text
            },
        )

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.Success)
        assertEquals(true, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/add-to-library", capturedPath)
        assertTrue(capturedBody?.contains("\"likeable_id\":101") == true)
        assertTrue(capturedBody?.contains("\"likeable_type\":\"track\"") == true)
    }

    @Test
    fun `addTrackToLibrary returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `addTrackToLibrary returns NetworkError on exception`() = runTest {
        val mockEngine = MockEngine { _ ->
            throw RuntimeException("Network error")
        }
        val client = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        val api = UserApi(client)

        val result = api.addTrackToLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `removeTrackFromLibrary posts likeables payload to remove-from-library endpoint`() = runTest {
        var capturedPath: String? = null
        val api = UserApi(
            clientCapturingRequest(HttpStatusCode.OK, successBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.removeTrackFromLibrary(101)

        assertTrue(result is ApiResult.Success)
        assertEquals(false, (result as ApiResult.Success).data)
        assertEquals("/api/v1/users/me/remove-from-library", capturedPath)
    }

    @Test
    fun `removeTrackFromLibrary returns NetworkError on server error`() = runTest {
        val api = UserApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.removeTrackFromLibrary(101)

        assertTrue(result is ApiResult.NetworkError)
    }
}
