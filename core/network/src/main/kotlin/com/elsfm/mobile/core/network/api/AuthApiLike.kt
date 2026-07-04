package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult

interface AuthApiLike {
    suspend fun login(email: String, password: String, tokenName: String): ApiResult<User>
}
