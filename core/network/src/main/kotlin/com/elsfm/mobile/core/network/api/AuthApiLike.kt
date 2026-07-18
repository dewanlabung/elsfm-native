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

    /**
     * Backed by the real `POST auth/register` route (`MobileAuthController::register` ->
     * `FortifyRegisterUser::create`). The backend requires `password_confirmation` (Fortify's
     * default password rules append `confirmed`) - callers must pass a matching value.
     * Returns 201 Created (not 200 OK) on the real backend.
     */
    suspend fun register(email: String, password: String, tokenName: String): ApiResult<User>

    /**
     * Backed by the real `POST auth/password/email` route (Fortify's
     * `PasswordResetLinkController::store`), which emails the user a reset link. This route
     * is `guest`-only on the backend (rejects requests carrying a valid session/token).
     */
    suspend fun requestPasswordReset(email: String): ApiResult<Unit>

    /**
     * Verifies the user's email address using the 6-digit code sent by the server after
     * registration. Backed by `POST api/v1/auth/email/verify` on the BeMusic backend.
     * This route is `guest`-only (the account has no verified session yet at this point).
     */
    suspend fun verifyEmail(code: String): ApiResult<Unit>
}
