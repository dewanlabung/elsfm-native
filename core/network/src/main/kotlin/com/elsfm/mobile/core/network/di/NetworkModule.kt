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
import com.elsfm.mobile.core.network.elsfmJson
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.url
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
            install(AuthPlugin) { this.sessionManager = sessionManager }
            install(Logging) { level = LogLevel.INFO }
        }
    }
}
