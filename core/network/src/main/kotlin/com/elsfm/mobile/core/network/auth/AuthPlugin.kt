package com.elsfm.mobile.core.network.auth

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
}

/**
 * Guest-only routes that must NOT carry a Bearer token even when a session exists.
 * The backend (Laravel Fortify) rejects these with 401 if an authenticated token
 * is present, because they are protected by the `guest` middleware.
 */
private val GUEST_ONLY_PATHS = listOf(
    "/auth/login",
    "/auth/register",
    "/auth/password",   // covers /auth/password/email and /auth/password/reset
    "/auth/email/verify", // email verification with code - guest route on backend
)

val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val sessionManager = pluginConfig.sessionManager

    onRequest { request, _ ->
        val path = request.url.pathSegments.joinToString("/", "/")
        val isGuestRoute = GUEST_ONLY_PATHS.any { path.contains(it) }
        if (!isGuestRoute) {
            sessionManager.currentToken()?.let { token ->
                request.headers.append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }

    onResponse { response ->
        if (response.status == HttpStatusCode.Unauthorized) {
            sessionManager.notifyExpired()
        }
    }
}
