package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.elsfmJson
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationsApiTest {

    private val notificationsResponseBody = """
        {
          "pagination": {
            "data": [
              {
                "id": "b1f6f2f0-1111-4c3d-9c1a-000000000001",
                "read_at": null,
                "created_at": "2024-02-01T00:00:00Z",
                "type": "App\\Notifications\\NewRelease",
                "data": {
                  "image": "https://cdn.elsfm.com/a.jpg",
                  "lines": [
                    {"content": "ELShaddai Kalimpong uploaded a new release", "icon": null}
                  ]
                }
              }
            ]
          }
        }
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
    fun `getNotifications parses paginated list`() = runTest {
        var capturedPath: String? = null
        val api = NotificationsApi(
            clientReturning(HttpStatusCode.OK, notificationsResponseBody) { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.getNotifications(perPage = 10)

        assertEquals("/api/v1/notifications", capturedPath)
        assertTrue(result is ApiResult.Success)
        val notifications = (result as ApiResult.Success).data
        assertEquals(1, notifications.size)
        assertEquals(
            "ELShaddai Kalimpong uploaded a new release",
            notifications[0].data.lines.first().content,
        )
    }

    @Test
    fun `getNotifications returns Unauthorized on 401`() = runTest {
        val api = NotificationsApi(clientReturning(HttpStatusCode.Unauthorized, ""))

        val result = api.getNotifications()

        assertEquals(ApiResult.Unauthorized, result)
    }

    @Test
    fun `markAsRead posts ids to mark-as-read endpoint`() = runTest {
        var capturedPath: String? = null
        val api = NotificationsApi(
            clientReturning(HttpStatusCode.OK, """{"unreadCount": 0}""") { request ->
                capturedPath = request.url.encodedPath
            },
        )

        val result = api.markAsRead(listOf("abc"))

        assertEquals("/api/v1/notifications/mark-as-read", capturedPath)
        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `markAllAsRead returns NetworkError on server error`() = runTest {
        val api = NotificationsApi(clientReturning(HttpStatusCode.InternalServerError, ""))

        val result = api.markAllAsRead()

        assertTrue(result is ApiResult.NetworkError)
    }
}
