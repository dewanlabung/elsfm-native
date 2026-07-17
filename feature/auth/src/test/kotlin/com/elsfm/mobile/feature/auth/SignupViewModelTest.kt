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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SignupFakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class SignupFakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

private class SignupFakeGoogleSignInService : GoogleSignInServiceLike {
    override fun signOut() = Unit
}

/**
 * Records which method was invoked - regression guard for the bug where the signup
 * screen used to call [login] instead of [register], which made an existing user's
 * credentials silently "sign up" (actually just log them in).
 */
private class SignupFakeAuthApi(private val registerResult: ApiResult<User>) : AuthApiLike {
    var registerCalled = false
        private set
    var loginCalled = false
        private set

    override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> {
        loginCalled = true
        return registerResult
    }

    override suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String) = registerResult

    override suspend fun register(email: String, password: String, tokenName: String): ApiResult<User> {
        registerCalled = true
        return registerResult
    }

    override suspend fun requestPasswordReset(email: String): ApiResult<Unit> = ApiResult.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class SignupViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private val user = User(id = 210, email = "new.user@example.com", name = "New User", accessToken = "1|newtoken")

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(authApi: SignupFakeAuthApi): SignupViewModel {
        val repository = AuthRepository(
            authApi = authApi,
            sessionManager = SessionManager(SignupFakeTokenStore()),
            userDao = SignupFakeUserDao(),
            googleSignInService = SignupFakeGoogleSignInService(),
        )
        return SignupViewModel(repository)
    }

    @Test
    fun `signup calls register, not login`() = runTest(testDispatcher) {
        val authApi = SignupFakeAuthApi(ApiResult.Success(user))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(SignupEvent.EmailChanged("new.user@example.com"))
        viewModel.onEvent(SignupEvent.PasswordChanged("secret123"))
        viewModel.onEvent(SignupEvent.ConfirmPasswordChanged("secret123"))
        viewModel.onEvent(SignupEvent.AcceptTermsChanged(true))
        viewModel.onEvent(SignupEvent.AcceptPrivacyChanged(true))
        viewModel.onEvent(SignupEvent.SignupClicked)
        advanceUntilIdle()

        assertTrue(authApi.registerCalled)
        assertEquals(false, authApi.loginCalled)
        assertTrue(viewModel.state.value.isSignedUp)
    }

    @Test
    fun `signup surfaces validation error from the register endpoint`() = runTest(testDispatcher) {
        val errors = mapOf("email" to listOf("The email has already been taken."))
        val authApi = SignupFakeAuthApi(ApiResult.ValidationError(errors))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(SignupEvent.EmailChanged("existing@example.com"))
        viewModel.onEvent(SignupEvent.PasswordChanged("secret123"))
        viewModel.onEvent(SignupEvent.ConfirmPasswordChanged("secret123"))
        viewModel.onEvent(SignupEvent.AcceptTermsChanged(true))
        viewModel.onEvent(SignupEvent.AcceptPrivacyChanged(true))
        viewModel.onEvent(SignupEvent.SignupClicked)
        advanceUntilIdle()

        assertEquals(false, viewModel.state.value.isSignedUp)
        assertEquals("The email has already been taken.", viewModel.state.value.error)
    }

    @Test
    fun `signup rejects mismatched passwords before calling the api`() = runTest(testDispatcher) {
        val authApi = SignupFakeAuthApi(ApiResult.Success(user))
        val viewModel = viewModel(authApi)

        viewModel.onEvent(SignupEvent.EmailChanged("new.user@example.com"))
        viewModel.onEvent(SignupEvent.PasswordChanged("secret123"))
        viewModel.onEvent(SignupEvent.ConfirmPasswordChanged("different"))
        viewModel.onEvent(SignupEvent.AcceptTermsChanged(true))
        viewModel.onEvent(SignupEvent.AcceptPrivacyChanged(true))
        viewModel.onEvent(SignupEvent.SignupClicked)
        advanceUntilIdle()

        assertEquals(false, authApi.registerCalled)
        assertEquals("Passwords do not match", viewModel.state.value.error)
    }
}
