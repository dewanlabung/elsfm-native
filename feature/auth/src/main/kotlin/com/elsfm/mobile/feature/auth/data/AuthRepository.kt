package com.elsfm.mobile.feature.auth.data

import com.elsfm.mobile.core.database.dao.TokenDao
import com.elsfm.mobile.core.database.entity.TokenEntity
import com.elsfm.mobile.core.model.ApiResult
import com.elsfm.mobile.core.network.AuthApi
import com.elsfm.mobile.core.network.Token
import javax.inject.Inject

data class StoredToken(
    val accessToken: String,
    val refreshToken: String?,
    val expiresAt: Long
)

class AuthRepository @Inject constructor(
    private val authApi: AuthApi,
    private val tokenDao: TokenDao
) {
    suspend fun login(email: String, password: String): ApiResult<Token> {
        return when (val result = authApi.login(email, password)) {
            is ApiResult.Success -> {
                tokenDao.insertToken(
                    TokenEntity(
                        userId = 0, // Mock for now, replace with actual user ID
                        accessToken = result.data.accessToken,
                        refreshToken = result.data.refreshToken,
                        expiresAt = result.data.expiresAt
                    )
                )
                result
            }
            else -> result
        }
    }

    suspend fun signup(email: String, password: String, confirmPassword: String): ApiResult<Token> {
        return when (val result = authApi.signup(email, password, confirmPassword)) {
            is ApiResult.Success -> {
                tokenDao.insertToken(
                    TokenEntity(
                        userId = 0,
                        accessToken = result.data.accessToken,
                        refreshToken = result.data.refreshToken,
                        expiresAt = result.data.expiresAt
                    )
                )
                result
            }
            else -> result
        }
    }

    suspend fun resetPassword(email: String): ApiResult<Unit> {
        return authApi.resetPassword(email)
    }

    suspend fun getStoredToken(): StoredToken? {
        val tokenEntity = tokenDao.getToken() ?: return null
        return StoredToken(
            accessToken = tokenEntity.accessToken,
            refreshToken = tokenEntity.refreshToken,
            expiresAt = tokenEntity.expiresAt
        )
    }

    suspend fun clearToken() {
        tokenDao.clearToken()
    }

    suspend fun isTokenExpired(): Boolean {
        val token = tokenDao.getToken() ?: return true
        return System.currentTimeMillis() > token.expiresAt
    }
}
