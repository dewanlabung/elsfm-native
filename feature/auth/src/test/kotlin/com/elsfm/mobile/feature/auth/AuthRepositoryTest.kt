package com.elsfm.mobile.feature.auth

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApi
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import com.elsfm.mobile.core.network.auth.TokenStore
import com.elsfm.mobile.feature.auth.data.AuthRepository
import com.elsfm.mobile.feature.auth.data.GoogleSignInServiceLike
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTokenStore : TokenStore {
    private var token: String? = null
    override suspend fun save(token: String) { this.token = token }
    override suspend fun read(): String? = token
    override suspend fun clear() { token = null }
}

private class FakeUserDao : UserDao {
    private var stored: UserEntity? = null
    override suspend fun upsert(user: UserEntity) { stored = user }
    override suspend fun get(): UserEntity? = stored
    override suspend fun clear() { stored = null }
}

private class FakeGoogleSignInService : GoogleSignInServiceLike {
    var signOutCalled = false
        private set

    override fun signOut() {
        signOutCalled = true
    }
}

class AuthRepositoryTest {
    private val user = User(id = 207, email = "test.elsfm@gmail.com", name = "ELSFM APP", accessToken = "1|abc")

    @Test
    fun `login saves token and caches user on success`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(
            authApi = FakeAuthApi(ApiResult.Success(user)),
            sessionManager = sessionManager,
            userDao = userDao,
            googleSignInService = FakeGoogleSignInService(),
        )

        val result = repository.login("test.elsfm@gmail.com", "secret")

        assertEquals(ApiResult.Success(user), result)
        assertEquals("1|abc", sessionManager.currentToken())
        assertEquals(207, userDao.get()?.id)
    }

    @Test
    fun `login does not store anything on validation error`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val validationError = ApiResult.ValidationError(mapOf("email" to listOf("These credentials do not match our records.")))
        val repository = AuthRepository(
            authApi = FakeAuthApi(validationError),
            sessionManager = sessionManager,
            userDao = userDao,
            googleSignInService = FakeGoogleSignInService(),
        )

        val result = repository.login("test.elsfm@gmail.com", "wrong")

        assertEquals(validationError, result)
        assertNull(sessionManager.currentToken())
        assertNull(userDao.get())
    }

    @Test
    fun `register saves token and caches user on success`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(
            authApi = FakeAuthApi(ApiResult.Success(user)),
            sessionManager = sessionManager,
            userDao = userDao,
            googleSignInService = FakeGoogleSignInService(),
        )

        val result = repository.register("test.elsfm@gmail.com", "secret")

        assertEquals(ApiResult.Success(user), result)
        assertEquals("1|abc", sessionManager.currentToken())
        assertEquals(207, userDao.get()?.id)
    }

    @Test
    fun `register does not store anything on validation error`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val validationError = ApiResult.ValidationError(mapOf("email" to listOf("The email has already been taken.")))
        val repository = AuthRepository(
            authApi = FakeAuthApi(validationError),
            sessionManager = sessionManager,
            userDao = userDao,
            googleSignInService = FakeGoogleSignInService(),
        )

        val result = repository.register("test.elsfm@gmail.com", "secret")

        assertEquals(validationError, result)
        assertNull(sessionManager.currentToken())
        assertNull(userDao.get())
    }

    @Test
    fun `requestPasswordReset delegates to the auth api`() = runTest {
        val repository = AuthRepository(
            authApi = FakeAuthApi(ApiResult.Success(user)),
            sessionManager = SessionManager(FakeTokenStore()),
            userDao = FakeUserDao(),
            googleSignInService = FakeGoogleSignInService(),
        )

        val result = repository.requestPasswordReset("test.elsfm@gmail.com")

        assertEquals(ApiResult.Success(Unit), result)
    }

    @Test
    fun `logout clears session and cached user`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), sessionManager, userDao, FakeGoogleSignInService())
        repository.login("test.elsfm@gmail.com", "secret")

        repository.logout()

        assertNull(sessionManager.currentToken())
        assertNull(userDao.get())
    }

    @Test
    fun `logout signs out of the cached Google account`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val googleSignInService = FakeGoogleSignInService()
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), sessionManager, userDao, googleSignInService)

        repository.logout()

        assertTrue(googleSignInService.signOutCalled)
    }

    @Test
    fun `restoredUser returns cached user only when a token is present`() = runTest {
        val sessionManager = SessionManager(FakeTokenStore())
        val userDao = FakeUserDao()
        val repository = AuthRepository(FakeAuthApi(ApiResult.Success(user)), sessionManager, userDao, FakeGoogleSignInService())

        assertNull(repository.restoredUser())

        repository.login("test.elsfm@gmail.com", "secret")
        assertEquals(207, repository.restoredUser()?.id)
    }

    private class FakeAuthApi(private val result: ApiResult<User>) : AuthApiLike {
        override suspend fun login(email: String, password: String, tokenName: String): ApiResult<User> = result
        override suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String): ApiResult<User> = result
        override suspend fun register(email: String, password: String, tokenName: String): ApiResult<User> = result
        override suspend fun requestPasswordReset(email: String): ApiResult<Unit> = ApiResult.Success(Unit)
    }
}
