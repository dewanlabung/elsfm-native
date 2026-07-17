package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import com.elsfm.mobile.feature.auth.data.AuthRepository
import com.elsfm.mobile.feature.auth.data.GoogleSignInServiceLike
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class ResetFakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class ResetFakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

private class ResetFakeGoogleSignInService : GoogleSignInServiceLike {
    override fun signOut() = Unit
}

/**
 * Regression guard for the bug where the reset-password screen never called the
 * backend at all - it just set `isSubmitted = true` locally, so no email was ever sent.
 */
private class ResetFakeAuthApi(private val result: ApiResult<Unit>) : AuthApiLike {
    var requestPasswordResetCalled = false
        private set
    var requestedEmail: String? = null
        private set

    override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> =
        ApiResult.NetworkError(IllegalStateException("not used"))

    override suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String): ApiResult<User> =
        ApiResult.NetworkError(IllegalStateException("not used"))

    override suspend fun register(email: String, password: String, tokenName: String): ApiResult<User> =
        ApiResult.NetworkError(IllegalStateException("not used"))

    override suspend fun requestPasswordReset(email: String): ApiResult<Unit> {
        requestPasswordResetCalled = true
        requestedEmail = email
        return result
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PasswordResetViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(authApi: ResetFakeAuthApi): PasswordResetViewModel {
        val repository = AuthRepository(
            authApi = authApi,
            sessionManager = SessionManager(ResetFakeTokenStore()),
            userDao = ResetFakeUserDao(),
            googleSignInService = ResetFakeGoogleSignInService(),
        )
        return PasswordResetViewModel(repository)
    }

    @Test
    fun `reset calls the real password-reset endpoint with the entered email`() = runTest(testDispatcher) {
        val authApi = ResetFakeAuthApi(ApiResult.Success(Unit))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(PasswordResetEvent.EmailChanged("user@example.com"))
        viewModel.onEvent(PasswordResetEvent.ResetClicked)
        advanceUntilIdle()

        assertTrue(authApi.requestPasswordResetCalled)
        assertEquals("user@example.com", authApi.requestedEmail)
        assertTrue(viewModel.state.value.isSubmitted)
    }

    @Test
    fun `reset does not call the api when email is empty`() = runTest(testDispatcher) {
        val authApi = ResetFakeAuthApi(ApiResult.Success(Unit))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(PasswordResetEvent.ResetClicked)
        advanceUntilIdle()

        assertFalse(authApi.requestPasswordResetCalled)
        assertEquals("Please enter your email address", viewModel.state.value.error)
    }

    @Test
    fun `reset surfaces validation error from the backend`() = runTest(testDispatcher) {
        val errors = mapOf("email" to listOf("We can't find a user with that email address."))
        val authApi = ResetFakeAuthApi(ApiResult.ValidationError(errors))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(PasswordResetEvent.EmailChanged("unknown@example.com"))
        viewModel.onEvent(PasswordResetEvent.ResetClicked)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isSubmitted)
        assertEquals("We can't find a user with that email address.", viewModel.state.value.error)
    }
}
