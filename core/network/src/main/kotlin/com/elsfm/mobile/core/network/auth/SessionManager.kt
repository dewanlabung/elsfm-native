package com.elsfm.mobile.core.network.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val _events = MutableSharedFlow<SessionEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SessionEvent> = _events.asSharedFlow()

    suspend fun saveToken(token: String) {
        tokenStore.save(token)
    }

    suspend fun currentToken(): String? = tokenStore.read()

    suspend fun clear() {
        tokenStore.clear()
    }

    suspend fun notifyExpired() {
        tokenStore.clear()
        _events.emit(SessionEvent.Expired)
    }
}
