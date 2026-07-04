package com.elsfm.mobile.core.network.auth

import app.cash.turbine.test
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private class FakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

class SessionManagerTest {
    @Test
    fun `saves and reads back a token`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())

        sessionManager.saveToken("token-123")

        assertEquals("token-123", sessionManager.currentToken())
    }

    @Test
    fun `clear removes the stored token`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        sessionManager.saveToken("token-123")

        sessionManager.clear()

        assertNull(sessionManager.currentToken())
    }

    @Test
    fun `notifyExpired clears the token and emits an Expired event`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        sessionManager.saveToken("token-123")

        sessionManager.events.test {
            sessionManager.notifyExpired()
            assertEquals(SessionEvent.Expired, awaitItem())
        }
        assertNull(sessionManager.currentToken())
    }
}
