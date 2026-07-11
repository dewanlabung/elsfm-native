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

    suspend fun logout() {
        sessionManager.clear()
        userDao.clear()
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
