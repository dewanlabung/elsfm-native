package com.elsfm.mobile.feature.auth.data

/**
 * Testable seam over [GoogleSignInService] - [AuthRepository] only needs [signOut],
 * and the concrete service wraps a real `GoogleSignInClient` that requires a live
 * `Context`, which a plain JUnit test cannot construct.
 */
interface GoogleSignInServiceLike {
    fun signOut()
}
