package com.elsfm.mobile.feature.auth

import app.cash.turbine.test
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class LoginFakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class LoginFakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

private class LoginFakeAuthApi(private val result: ApiResult<User>) : AuthApiLike {
    override suspend fun login(email: String, password: String, tokenName: String) = result
}

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

class LoginViewModelTest {

    @Test
    fun `emits Loading then Success on successful login`() = runTest {
        val user = User(id = 207, email = "test.elsfm@gmail.com", accessToken = "1|abc")
        val repository = AuthRepository(LoginFakeAuthApi(ApiResult.Success(user)), SessionManager(LoginFakeTokenStore()), LoginFakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "secret")
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.Success(user), awaitItem())
        }
    }

    @Test
    fun `emits FieldErrors on validation failure`() = runTest {
        val errors = mapOf("email" to listOf("These credentials do not match our records."))
        val repository = AuthRepository(LoginFakeAuthApi(ApiResult.ValidationError(errors)), SessionManager(LoginFakeTokenStore()), LoginFakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "wrong")
            assertEquals(LoginUiState.Loading, awaitItem())
            val errorState = awaitItem()
            assertTrue(errorState is LoginUiState.FieldErrors)
            assertEquals(errors, (errorState as LoginUiState.FieldErrors).errors)
        }
    }

    @Test
    fun `emits NetworkError when the repository reports a network failure`() = runTest {
        val repository = AuthRepository(LoginFakeAuthApi(ApiResult.NetworkError(RuntimeException("offline"))), SessionManager(LoginFakeTokenStore()), LoginFakeUserDao())
        val viewModel = LoginViewModel(repository, TestDispatcherProvider(StandardTestDispatcher(testScheduler)))

        viewModel.state.test {
            assertEquals(LoginUiState.Idle, awaitItem())
            viewModel.onLoginClicked("test.elsfm@gmail.com", "secret")
            assertEquals(LoginUiState.Loading, awaitItem())
            assertEquals(LoginUiState.NetworkError, awaitItem())
        }
    }
}
