package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
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

class ProfileApiTest {

    private val responseBody = """
        {
            "user": {
                "id": 1,
                "name": "Jane Doe",
                "username": "janedoe",
                "image": null,
                "profile": {
                    "description": "Music lover"
                },
                "followers_count": 5,
                "followed_users_count": 3
            }
        }
    """.trimIndent()

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `getProfile returns user profile`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.OK, responseBody))

        val result = api.getProfile(userId = 1)

        assertTrue(result is ApiResult.Success)
        val profile = (result as ApiResult.Success).data
        assertEquals("Jane Doe", profile.name)
        assertEquals("Music lover", profile.bio)
        assertEquals(5, profile.followersCount)
        assertEquals(3, profile.followedCount)
    }

    @Test
    fun `getProfile requests the real user-profile endpoint`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(responseBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = ProfileApi(HttpClient(mockEngine) { install(ContentNegotiation) { json() } })

        api.getProfile(userId = 1)

        assertEquals("/api/v1/user-profile/1", capturedPath)
    }

    @Test
    fun `getProfile returns NetworkError on failure`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getProfile(userId = 1)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `updateProfile puts to real endpoint then re-fetches the profile`() = runTest {
        var putPath: String? = null
        var getPath: String? = null
        val mockEngine = MockEngine { request ->
            if (request.method == io.ktor.http.HttpMethod.Put) {
                putPath = request.url.encodedPath
                respond("""{"status":"success"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            } else {
                getPath = request.url.encodedPath
                respond(responseBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
        val api = ProfileApi(HttpClient(mockEngine) { install(ContentNegotiation) { json() } })

        val result = api.updateProfile(userId = 1, name = "Jane Doe", bio = "Music lover")

        assertTrue(result is ApiResult.Success)
        assertEquals("/api/v1/users/profile/update", putPath)
        assertEquals("/api/v1/user-profile/1", getPath)
    }

    @Test
    fun `updateProfile returns NetworkError when the update request fails`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.updateProfile(userId = 1, name = "Jane", bio = null)

        assertTrue(result is ApiResult.NetworkError)
    }

    private val recentlyPlayedResponseBody = """
        {
          "pagination": {
            "data": [
              {"id": 1, "name": "Track 1", "image": null, "duration": 180000, "plays": "12", "artists": []}
            ]
          }
        }
    """.trimIndent()

    @Test
    fun `getRecentlyPlayed returns tracks from the real tracks-plays-me endpoint`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.OK, recentlyPlayedResponseBody))

        val result = api.getRecentlyPlayed()

        assertTrue(result is ApiResult.Success)
        val tracks = (result as ApiResult.Success).data
        assertEquals(1, tracks.size)
        assertEquals("Track 1", tracks[0].name)
    }

    @Test
    fun `getRecentlyPlayed returns NetworkError on failure`() = runTest {
        val api = ProfileApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getRecentlyPlayed()

        assertTrue(result is ApiResult.NetworkError)
    }
}
