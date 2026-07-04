package com.elsfm.mobile.core.network.auth

import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

class AuthPluginConfig {
    lateinit var sessionManager: SessionManager
}

val AuthPlugin = createClientPlugin("AuthPlugin", ::AuthPluginConfig) {
    val sessionManager = pluginConfig.sessionManager

    onRequest { request, _ ->
        val path = request.url.pathSegments.joinToString("/", "/")
        if (!path.contains("/auth/login") && !path.contains("/auth/register")) {
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
