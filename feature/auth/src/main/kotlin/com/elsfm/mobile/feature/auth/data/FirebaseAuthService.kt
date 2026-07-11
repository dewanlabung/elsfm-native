package com.elsfm.mobile.feature.auth.data

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class GoogleSignInResult(
    val isSuccess: Boolean,
    val idToken: String? = null,
    val email: String? = null,
    val displayName: String? = null,
    val error: String? = null
)

@Singleton
class FirebaseAuthService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firebaseAuth: FirebaseAuth
) {
    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("237126652892-0t8drqrisvgta5873mlcaih48icf5v7l.apps.googleusercontent.com")
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            GoogleSignInResult(
                isSuccess = user != null,
                idToken = idToken,
                email = user?.email,
                displayName = user?.displayName
            )
        } catch (e: Exception) {
            GoogleSignInResult(
                isSuccess = false,
                error = e.message ?: "Unknown error occurred"
            )
        }
    }

    suspend fun signOutGoogle() {
        firebaseAuth.signOut()
        googleSignInClient.signOut().await()
    }

    fun getCurrentUser() = firebaseAuth.currentUser

    suspend fun getIdToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(false)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }
}
