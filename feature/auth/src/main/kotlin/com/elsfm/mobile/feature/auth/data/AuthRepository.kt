package com.elsfm.mobile.feature.auth.data

import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AuthApiLike
import java.util.UUID
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authApi: AuthApiLike,
) {
    suspend fun login(email: String, password: String): ApiResult<User> {
        val tokenName = "android-${UUID.randomUUID()}"
        return authApi.login(email, password, tokenName)
    }

    suspend fun logout(): ApiResult<Unit> {
        return ApiResult.Success(Unit)
    }
}
