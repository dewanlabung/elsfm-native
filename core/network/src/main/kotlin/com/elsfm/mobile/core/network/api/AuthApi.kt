package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.LaravelValidationError
import com.elsfm.mobile.core.model.LoginRequest
import com.elsfm.mobile.core.model.LoginResponse
import com.elsfm.mobile.core.model.PasswordResetRequest
import com.elsfm.mobile.core.model.RegisterRequest
import com.elsfm.mobile.core.model.RegisterResponse
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import java.io.IOException
import javax.inject.Inject

@Serializable
private data class EmailVerifyRequest(val code: String)

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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    override suspend fun register(email: String, password: String, tokenName: String): ApiResult<User> {
        return try {
            val response = httpClient.post("api/v1/auth/register") {
                contentType(ContentType.Application.Json)
                setBody(
                    RegisterRequest(
                        email = email,
                        password = password,
                        passwordConfirmation = password,
                        tokenName = tokenName,
                    ),
                )
            }
            when (response.status) {
                // The real backend returns 201 Created (not 200 OK) for successful registration.
                // Use runCatching for body parsing: if the backend accepted the registration
                // but returned an unexpected JSON shape (ContentConvertException), we still
                // treat the outcome as a success so SignupViewModel navigates to the email
                // verification screen — the backend already sent the OTP at this point.
                HttpStatusCode.OK, HttpStatusCode.Created -> {
                    val user = runCatching { response.body<RegisterResponse>().bootstrapData.user }
                        .getOrElse { User(id = -1, email = email) }
                    ApiResult.Success(user)
                }
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    override suspend fun requestPasswordReset(email: String): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/auth/password/email") {
                contentType(ContentType.Application.Json)
                // Accept: application/json tells Laravel Fortify to return JSON errors
                // instead of HTML redirect responses, which can appear as 401/403.
                headers.append(HttpHeaders.Accept, "application/json")
                setBody(PasswordResetRequest(email = email))
            }
            when (response.status) {
                HttpStatusCode.OK -> ApiResult.Success(Unit)
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    override suspend fun verifyEmail(code: String): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/auth/email/verify") {
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Accept, "application/json")
                setBody(EmailVerifyRequest(code = code))
            }
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.Created -> ApiResult.Success(Unit)
                HttpStatusCode.UnprocessableEntity -> {
                    val error = response.body<LaravelValidationError>()
                    ApiResult.ValidationError(error.errors)
                }
                HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> ApiResult.Unauthorized
                else -> ApiResult.NetworkError(IllegalStateException("Unexpected status ${response.status}"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
