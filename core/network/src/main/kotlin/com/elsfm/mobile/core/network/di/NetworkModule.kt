package com.elsfm.mobile.core.network.di

import com.elsfm.mobile.core.common.DefaultDispatcherProvider
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ElsfmApiConfig
import com.elsfm.mobile.core.network.api.AuthApi
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.api.UserApi
import com.elsfm.mobile.core.network.api.UserApiLike
import com.elsfm.mobile.core.network.auth.AuthPlugin
import com.elsfm.mobile.core.network.auth.EncryptedTokenStore
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import com.elsfm.mobile.core.network.connectivity.DefaultNetworkMonitor
import com.elsfm.mobile.core.network.connectivity.NetworkMonitor
import com.elsfm.mobile.core.network.elsfmJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkBindsModule {
    @Binds
    @Singleton
    abstract fun bindTokenStore(impl: EncryptedTokenStore): TokenStore

    @Binds
    @Singleton
    abstract fun bindAuthApi(impl: AuthApi): AuthApiLike

    @Binds
    @Singleton
    abstract fun bindUserApi(impl: UserApi): UserApiLike

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(impl: DefaultDispatcherProvider): DispatcherProvider

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(impl: DefaultNetworkMonitor): NetworkMonitor
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideHttpClient(sessionManager: SessionManager): HttpClient {
        return HttpClient(OkHttp) {
            defaultRequest { url(ElsfmApiConfig.BASE_URL) }
            install(ContentNegotiation) { json(elsfmJson()) }
            // The backend has occasionally returned 401 on the very first authenticated
            // request right after a fresh login, using a token it had just issued seconds
            // earlier (confirmed via live device testing - the token becomes valid on
            // later requests, so this looks like backend-side replication lag rather than
            // an actually-invalid token). Retrying briefly here avoids AuthPlugin below
            // treating that transient 401 as a real session expiry and wiping a token that
            // was never actually bad, which was otherwise kicking freshly-logged-in users
            // straight back to the sign-in screen.
            install(HttpRequestRetry) {
                retryIf(maxRetries = 2) { request, response ->
                    response.status == HttpStatusCode.Unauthorized &&
                        !request.url.encodedPath.contains("/auth/login") &&
                        !request.url.encodedPath.contains("/auth/register") &&
                        !request.url.encodedPath.contains("/auth/social") &&
                        // Guest routes must never be retried on 401: their 401s are
                        // permanent (e.g. "email not found") and retrying wastes
                        // requests while also triggering notifyExpired() in AuthPlugin.
                        !request.url.encodedPath.contains("/auth/password") &&
                        !request.url.encodedPath.contains("/auth/email")
                }
                constantDelay(millis = 500)
            }
            install(AuthPlugin) { this.sessionManager = sessionManager }
            install(Logging) {
                level = LogLevel.BODY
                logger = object : Logger {
                    override fun log(message: String) {
                        android.util.Log.d("ElsfmHttp", message)
                    }
                }
            }
        }
    }
}
