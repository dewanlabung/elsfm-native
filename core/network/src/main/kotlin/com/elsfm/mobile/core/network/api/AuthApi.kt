package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.LaravelValidationError
import com.elsfm.mobile.core.model.LoginRequest
import com.elsfm.mobile.core.model.LoginResponse
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import java.io.IOException
import javax.inject.Inject

class AuthApi @Inject constructor(
    private val httpClient: HttpClient,
) : AuthApiLike {
    override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> {
        return try {
            val response = httpClient.post("api/v1/auth/login") {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password, tokenName = tokenName))
            }
            when (response.status) {
                HttpStatusCode.OK -> ApiResult.Success(response.body<LoginResponse>().user)
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        }
    }

    override suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String): ApiResult<User> {
        return try {
            val response = httpClient.get("api/v1/auth/social/google/callback") {
                parameter("tokenFromApi", googleAccessToken)
                parameter("tokenForDevice", tokenName)
            }
            when {
                response.status == HttpStatusCode.OK &&
                    response.contentType()?.match(ContentType.Application.Json) == true -> {
                    ApiResult.Success(response.body<LoginResponse>().user)
                }
                // The backend's web-only account-linking flow: this Google email already has a
                // password-based account not yet connected to Google, so it returns an HTML page
                // meant for a browser popup (BroadcastChannel + window.close()) asking the user to
                // confirm their password there - our API client has no way to complete that flow.
                response.status == HttpStatusCode.OK -> {
                    ApiResult.ValidationError(
                        mapOf(
                            "email" to listOf(
                                "This email already has a password-based account. Sign in with " +
                                    "your email and password instead.",
                            ),
                        ),
                    )
                }
                response.status == HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden -> {
                    ApiResult.Unauthorized
                }
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: IOException) {
            ApiResult.NetworkError(e)
        }
    }
}
