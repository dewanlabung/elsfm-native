package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.User
import com.elsfm.mobile.core.network.ApiResult

interface AuthApiLike {
    suspend fun login(email: String, password: String, tokenName: String): ApiResult<User>

    /**
     * Backed by the real `GET auth/social/{provider}/callback` route
     * (`SocialAuthController::loginCallback`, via `Socialite::driver('google')->userFromToken()`).
     * [googleAccessToken] must be a genuine Google OAuth access token (obtained natively, e.g.
     * via `GoogleAuthUtil.getToken`) - Socialite calls Google's userinfo REST endpoint with it
     * directly, so a Firebase ID token will not work here.
     */
    suspend fun loginWithGoogle(googleAccessToken: String, tokenName: String): ApiResult<User>
}
