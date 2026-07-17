package com.elsfm.mobile.feature.auth.data

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private const val USERINFO_EMAIL_SCOPE = "https://www.googleapis.com/auth/userinfo.email"
private const val USERINFO_PROFILE_SCOPE = "https://www.googleapis.com/auth/userinfo.profile"

/**
 * Wraps Google Sign-In to obtain a real Google OAuth *access token* - not a Firebase ID
 * token. The real elsfm.com backend's social login
 * (`SocialAuthController::loginCallback` -> `Socialite::driver('google')->userFromToken()`)
 * calls Google's userinfo REST endpoint with this token directly; it has no Firebase
 * Admin SDK integration, so a Firebase ID token would never work here.
 */
@Singleton
class GoogleSignInService @Inject constructor(
    @ApplicationContext private val context: Context,
) : GoogleSignInServiceLike {
    val signInClient: GoogleSignInClient by lazy {
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(USERINFO_PROFILE_SCOPE), Scope(USERINFO_EMAIL_SCOPE))
            .build()
        GoogleSignIn.getClient(context, options)
    }

    /**
     * [GoogleAuthUtil.getToken] is a blocking network call by contract - always invoked here
     * on [Dispatchers.IO].
     */
    suspend fun fetchAccessToken(accountEmail: String): String = withContext(Dispatchers.IO) {
        val scope = "oauth2:$USERINFO_PROFILE_SCOPE $USERINFO_EMAIL_SCOPE"
        GoogleAuthUtil.getToken(context, accountEmail, scope)
    }

    override fun signOut() {
        signInClient.signOut()
    }
}
