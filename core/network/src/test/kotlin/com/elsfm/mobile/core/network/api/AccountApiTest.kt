package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccountApiTest {

    private val fileEntryResponseBody = """
        {"fileEntry": {"id": 42, "url": "https://cdn.elsfm.com/avatars/42.jpg"}}
    """.trimIndent()

    private val userResponseBody = """
        {"user": {"id": 7, "name": "New Name", "email": "user@example.com", "image": "https://cdn.elsfm.com/avatars/42.jpg"}}
    """.trimIndent()

    private fun clientReturning(
        status: HttpStatusCode,
        body: String,
        onRequest: (HttpRequestData) -> Unit = {},
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
    fun `uploadAvatar posts multipart file and uploadType to the uploads endpoint`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val api = AccountApi(
            clientReturning(HttpStatusCode.Created, fileEntryResponseBody) { request ->
                capturedPath = request.url.encodedPath
                capturedMethod = request.method
            },
        )

        val result = api.uploadAvatar(byteArrayOf(1, 2, 3), "avatar.jpg", "image/jpeg")

        assertEquals("/api/v1/uploads", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
        assertTrue(result is ApiResult.Success)
        assertEquals(42, (result as ApiResult.Success).data.id)
        assertEquals("https://cdn.elsfm.com/avatars/42.jpg", result.data.url)
    }

    @Test
    fun `uploadAvatar returns Unauthorized on 401`() = runTest {
        val api = AccountApi(clientReturning(HttpStatusCode.Unauthorized, ""))

        val result = api.uploadAvatar(byteArrayOf(1), "a.jpg", "image/jpeg")

        assertEquals(ApiResult.Unauthorized, result)
    }

    @Test
    fun `updateAccountDetails puts to users id endpoint and returns updated profile`() = runTest {
        var capturedPath: String? = null
        val api = AccountApi(
            clientReturning(HttpStatusCode.OK, userResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.updateAccountDetails(userId = 7, name = "New Name", image = "https://cdn.elsfm.com/avatars/42.jpg", imageEntryId = 42)

        assertEquals("/api/v1/users/7", capturedPath)
        assertTrue(result is ApiResult.Success)
        assertEquals("New Name", (result as ApiResult.Success).data.name)
    }

    @Test
    fun `updateAccountDetails returns NetworkError on server error`() = runTest {
        val api = AccountApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.updateAccountDetails(userId = 7, name = "New Name")

        assertTrue(result is ApiResult.NetworkError)
    }
}
