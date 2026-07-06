package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.api.UserApi
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
import org.junit.Assert.assertNull
import org.junit.Test

class TrackLikeControllerTest {

    private fun controllerReturning(status: HttpStatusCode): TrackLikeController {
        val mockEngine = MockEngine { _ ->
            respond("{}", status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val httpClient = HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
        return TrackLikeController(UserApi(httpClient))
    }

    @Test
    fun `toggleLike calls addTrackToLibrary and returns true when not currently liked`() = runTest {
        val controller = controllerReturning(HttpStatusCode.OK)

        val result = controller.toggleLike(trackId = 1, currentlyLiked = false)

        assertEquals(true, result)
    }

    @Test
    fun `toggleLike calls removeTrackFromLibrary and returns false when currently liked`() = runTest {
        val controller = controllerReturning(HttpStatusCode.OK)

        val result = controller.toggleLike(trackId = 1, currentlyLiked = true)

        assertEquals(false, result)
    }

    @Test
    fun `toggleLike returns null on server error`() = runTest {
        val controller = controllerReturning(HttpStatusCode.InternalServerError)

        val result = controller.toggleLike(trackId = 1, currentlyLiked = false)

        assertNull(result)
    }
}
