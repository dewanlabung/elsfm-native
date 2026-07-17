package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BillingApiTest {

    private val activeSubscriptionBody = """
        {
          "subscription": {
            "id": 42,
            "product_id": 1,
            "price_id": 2,
            "renews_at": "2026-08-17T00:00:00Z",
            "ends_at": null,
            "trial_ends_at": null,
            "on_grace_period": false,
            "on_trial": false,
            "valid": true,
            "active": true,
            "cancelled": false,
            "gateway_name": "stripe",
            "product": {"name": "Premium"},
            "price": {"name": "Monthly"}
          }
        }
    """.trimIndent()

    private val noSubscriptionBody = """{ "subscription": null }"""

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json() }
        }
    }

    @Test
    fun `getCurrentSubscription returns subscription on success`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.OK, activeSubscriptionBody))

        val result = api.getCurrentSubscription()

        assertTrue(result is ApiResult.Success)
        val subscription = (result as ApiResult.Success).data
        assertEquals(42, subscription?.id)
        assertEquals("Premium", subscription?.productName)
        assertEquals("Monthly", subscription?.priceName)
        assertTrue(subscription?.active == true)
    }

    @Test
    fun `getCurrentSubscription returns success with null when no subscription exists`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.OK, noSubscriptionBody))

        val result = api.getCurrentSubscription()

        assertTrue(result is ApiResult.Success)
        assertNull((result as ApiResult.Success).data)
    }

    @Test
    fun `getCurrentSubscription requests the real billing user endpoint`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond(noSubscriptionBody, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = BillingApi(HttpClient(mockEngine) { install(ContentNegotiation) { json() } })

        api.getCurrentSubscription()

        assertEquals("/api/v1/billing/user", capturedPath)
    }

    @Test
    fun `getCurrentSubscription returns Unauthorized on 401`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.Unauthorized, "{}"))

        val result = api.getCurrentSubscription()

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun `getCurrentSubscription returns NetworkError on failure`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.getCurrentSubscription()

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `cancelSubscription posts to the cancel endpoint with delete false by default`() = runTest {
        var capturedPath: String? = null
        var capturedMethod: HttpMethod? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            capturedMethod = request.method
            respond("""{"status":"success"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = BillingApi(HttpClient(mockEngine) { install(ContentNegotiation) { json() } })

        val result = api.cancelSubscription(id = 42)

        assertTrue(result is ApiResult.Success)
        assertEquals("/api/v1/billing/subscriptions/42/cancel", capturedPath)
        assertEquals(HttpMethod.Post, capturedMethod)
    }

    @Test
    fun `cancelSubscription returns NetworkError on failure`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.cancelSubscription(id = 42)

        assertTrue(result is ApiResult.NetworkError)
    }

    @Test
    fun `resumeSubscription posts to the resume endpoint`() = runTest {
        var capturedPath: String? = null
        val mockEngine = MockEngine { request ->
            capturedPath = request.url.encodedPath
            respond("""{"status":"success"}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val api = BillingApi(HttpClient(mockEngine) { install(ContentNegotiation) { json() } })

        val result = api.resumeSubscription(id = 42)

        assertTrue(result is ApiResult.Success)
        assertEquals("/api/v1/billing/subscriptions/42/resume", capturedPath)
    }

    @Test
    fun `resumeSubscription returns Unauthorized on 403`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.Forbidden, "{}"))

        val result = api.resumeSubscription(id = 42)

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun `resumeSubscription returns NetworkError on failure`() = runTest {
        val api = BillingApi(clientReturning(HttpStatusCode.InternalServerError, "{}"))

        val result = api.resumeSubscription(id = 42)

        assertTrue(result is ApiResult.NetworkError)
    }
}
