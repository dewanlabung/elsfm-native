package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.UserSessionInfo
import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class UserSessionsResponse(val sessions: List<UserSessionInfo>)

/**
 * Backs the read-only part of the real "Sessions" account setting
 * (`GET api/v1/user-sessions`, `Common\Auth\Controllers\UserSessionsController::index`).
 *
 * Deliberately does NOT expose a per-session revoke action: the backend has no such
 * endpoint (only a bulk `POST user-sessions/logout-other`), and that bulk action is
 * gated behind Fortify's session-cookie-based password confirmation, which this app's
 * stateless Bearer-token Ktor client cannot satisfy. See the account-notifications
 * report for details.
 */
open class SessionsApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    open suspend fun getUserSessions(): ApiResult<List<UserSessionInfo>> {
        return try {
            val response = httpClient.get("api/v1/user-sessions")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<UserSessionsResponse>().sessions)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
