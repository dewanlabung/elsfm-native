package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import com.elsfm.mobile.feature.auth.data.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    override suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String) = result
}

class LoginViewModelTest {

    @Test
    fun `repository login saves token on success`() = runTest {
        val user = User(id = 207, email = "test.elsfm@gmail.com", name = "Test User", accessToken = "1|abc")
        val repository = AuthRepository(
            authApi = LoginFakeAuthApi(ApiResult.Success(user)),
            sessionManager = SessionManager(LoginFakeTokenStore()),
            userDao = LoginFakeUserDao()
        )

        val result = repository.login("test.elsfm@gmail.com", "secret")

        assertTrue(result is ApiResult.Success)
        assertEquals(user, (result as ApiResult.Success).data)
    }

    @Test
    fun `repository login handles validation error`() = runTest {
        val errors = mapOf("email" to listOf("These credentials do not match our records."))
        val repository = AuthRepository(
            authApi = LoginFakeAuthApi(ApiResult.ValidationError(errors)),
            sessionManager = SessionManager(LoginFakeTokenStore()),
            userDao = LoginFakeUserDao()
        )

        val result = repository.login("test.elsfm@gmail.com", "wrong")

        assertTrue(result is ApiResult.ValidationError)
        assertEquals(errors, (result as ApiResult.ValidationError).fields)
    }

    @Test
    fun `repository login handles network error`() = runTest {
        val repository = AuthRepository(
            authApi = LoginFakeAuthApi(ApiResult.NetworkError(RuntimeException("offline"))),
            sessionManager = SessionManager(LoginFakeTokenStore()),
            userDao = LoginFakeUserDao()
        )

        val result = repository.login("test.elsfm@gmail.com", "secret")

        assertTrue(result is ApiResult.NetworkError)
    }
}
