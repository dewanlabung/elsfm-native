package com.elsfm.mobile.feature.auth.data

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import com.elsfm.mobile.core.network.auth.SessionManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val authApi: AuthApiLike,
    private val sessionManager: SessionManager,
    private val userDao: UserDao,
    private val googleSignInService: GoogleSignInServiceLike,
) {
    suspend fun login(email: String, password: String): ApiResult<User> {
        val tokenName = "android-${UUID.randomUUID()}"
        val result = authApi.login(email, password, tokenName)
        if (result is ApiResult.Success) {
            val token = result.data.accessToken
            if (token != null) {
                sessionManager.saveToken(token)
                userDao.upsert(result.data.toEntity())
            }
        }
        return result
    }

    /**
     * Creates a new account via the real `POST auth/register` endpoint, then stores the
     * session exactly like [login] does on success (the register endpoint issues a real
     * API token too when a `token_name` is sent).
     * The backend returns 201 Created; after success the server also sends an email
     * verification code — the caller must navigate to the email verification screen.
     */
    suspend fun register(email: String, password: String): ApiResult<User> {
        val tokenName = "android-${UUID.randomUUID()}"
        val result = authApi.register(email, password, tokenName)
        if (result is ApiResult.Success) {
            val token = result.data.accessToken
            if (token != null) {
                sessionManager.saveToken(token)
                userDao.upsert(result.data.toEntity())
            }
        }
        return result
    }

    /** Requests a password-reset email via the real `POST auth/password/email` endpoint. */
    suspend fun requestPasswordReset(email: String): ApiResult<Unit> =
        authApi.requestPasswordReset(email)

    /**
     * Verifies the email address using the 6-digit code the server emailed after registration.
     * Backed by `POST api/v1/auth/email/verify`. On success the account is considered verified
     * and the user can proceed into the app.
     */
    suspend fun verifyEmail(code: String, email: String): ApiResult<Unit> =
        authApi.verifyEmail(code = code, email = email)

    /** Same session-storage flow as [login], just backed by a Google OAuth access token. */
    suspend fun loginWithGoogle(googleAccessToken: String): ApiResult<User> {
        val tokenName = "android-${UUID.randomUUID()}"
        val result = authApi.loginWithGoogle(googleAccessToken, tokenName)
        if (result is ApiResult.Success) {
            val token = result.data.accessToken
            if (token != null) {
                sessionManager.saveToken(token)
                userDao.upsert(result.data.toEntity())
            }
        }
        return result
    }

    /**
     * Also signs out of the cached Google account ([GoogleSignInServiceLike.signOut]) -
     * without this, `GoogleSignInClient` keeps the last-used account cached, so the
     * next "Sign in with Google" tap silently resolves to that same account instead
     * of showing the account picker again.
     */
    suspend fun logout() {
        sessionManager.clear()
        userDao.clear()
        googleSignInService.signOut()
    }

    suspend fun restoredUser(): User? {
        val token = sessionManager.currentToken() ?: return null
        val cached = userDao.get() ?: return null
        return User(
            id = cached.id,
            username = cached.username,
            name = cached.name,
            email = cached.email,
            avatarUrl = cached.avatarUrl,
            accessToken = token,
        )
    }

    private fun User.toEntity() = UserEntity(
        id = id,
        username = username,
        name = name,
        email = email,
        avatarUrl = avatarUrl,
    )
}
