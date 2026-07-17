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

class AuthApiTest {

    private fun clientReturning(status: HttpStatusCode, body: String): HttpClient {
        val mockEngine = MockEngine { _ ->
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return HttpClient(mockEngine) {
            install(ContentNegotiation) { json(elsfmJson()) }
        }
    }

    @Test
    fun `login success returns user with access token`() = runTest {
        val body = """
            {"status":"success","user":{"id":207,"email":"test.elsfm@gmail.com","access_token":"1|abc"}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.OK, body))

        val result = authApi.login("test.elsfm@gmail.com", "secret", "android-uuid-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("1|abc", (result as ApiResult.Success).data.accessToken)
    }

    @Test
    fun `login with invalid credentials returns validation error`() = runTest {
        val body = """
            {"message":"The given data was invalid.","errors":{"email":["These credentials do not match our records."]}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.UnprocessableEntity, body))

        val result = authApi.login("test.elsfm@gmail.com", "wrong", "android-uuid-1")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(
            listOf("These credentials do not match our records."),
            (result as ApiResult.ValidationError).fields["email"],
        )
    }

    @Test
    fun `login with 401 response returns Unauthorized`() = runTest {
        val authApi = AuthApi(clientReturning(HttpStatusCode.Unauthorized, "{}"))

        val result = authApi.login("test.elsfm@gmail.com", "secret", "android-uuid-1")

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun `loginWithGoogle success returns user with access token`() = runTest {
        val body = """
            {"status":"success","user":{"id":207,"email":"test.elsfm@gmail.com","access_token":"1|abc"}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.OK, body))

        val result = authApi.loginWithGoogle("ya29.google-access-token", "android-uuid-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("1|abc", (result as ApiResult.Success).data.accessToken)
    }

    @Test
    fun `loginWithGoogle with 401 response returns Unauthorized`() = runTest {
        val authApi = AuthApi(clientReturning(HttpStatusCode.Unauthorized, "{}"))

        val result = authApi.loginWithGoogle("ya29.google-access-token", "android-uuid-1")

        assertTrue(result is ApiResult.Unauthorized)
    }

    @Test
    fun `loginWithGoogle with HTML account-linking popup returns validation error`() = runTest {
        val mockEngine = MockEngine { _ ->
            respond(
                "<h1>Logging in</h1><script>let status = \"REQUEST_PASSWORD\";</script>",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "text/html; charset=UTF-8"),
            )
        }
        val authApi = AuthApi(HttpClient(mockEngine) { install(ContentNegotiation) { json(elsfmJson()) } })

        val result = authApi.loginWithGoogle("ya29.google-access-token", "android-uuid-1")

        assertTrue(result is ApiResult.ValidationError)
        assertTrue((result as ApiResult.ValidationError).fields["email"]?.isNotEmpty() == true)
    }

    @Test
    fun `register success returns user nested under bootstrapData`() = runTest {
        val body = """
            {"status":"success","bootstrapData":{"user":{"id":210,"email":"new.user@example.com","access_token":"1|newtoken"}}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.OK, body))

        val result = authApi.register("new.user@example.com", "secret123", "android-uuid-1")

        assertTrue(result is ApiResult.Success)
        assertEquals("1|newtoken", (result as ApiResult.Success).data.accessToken)
    }

    @Test
    fun `register with an email already taken returns validation error`() = runTest {
        val body = """
            {"message":"The given data was invalid.","errors":{"email":["The email has already been taken."]}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.UnprocessableEntity, body))

        val result = authApi.register("existing@example.com", "secret123", "android-uuid-1")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(
            listOf("The email has already been taken."),
            (result as ApiResult.ValidationError).fields["email"],
        )
    }

    @Test
    fun `requestPasswordReset success returns Unit`() = runTest {
        val body = """{"status":"passwords.sent"}"""
        val authApi = AuthApi(clientReturning(HttpStatusCode.OK, body))

        val result = authApi.requestPasswordReset("user@example.com")

        assertTrue(result is ApiResult.Success)
    }

    @Test
    fun `requestPasswordReset for unknown email returns validation error`() = runTest {
        val body = """
            {"message":"The given data was invalid.","errors":{"email":["We can't find a user with that email address."]}}
        """.trimIndent()
        val authApi = AuthApi(clientReturning(HttpStatusCode.UnprocessableEntity, body))

        val result = authApi.requestPasswordReset("unknown@example.com")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(
            listOf("We can't find a user with that email address."),
            (result as ApiResult.ValidationError).fields["email"],
        )
    }
}
