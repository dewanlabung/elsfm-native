package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    @SerialName("password_confirmation") val passwordConfirmation: String,
    @SerialName("token_name") val tokenName: String,
)

/**
 * The real `MobileAuthController::register` response nests the user one level deeper
 * than login's response - under `bootstrapData.user` rather than a top-level `user` -
 * since `Common\Auth\Fortify\RegisterResponse::toResponse` reuses `MobileBootstrapData`
 * for the mobile (`token_name` present) branch.
 */
@Serializable
data class RegisterBootstrapData(val user: User)

@Serializable
data class RegisterResponse(
    val status: String,
    val bootstrapData: RegisterBootstrapData,
)

@Serializable
data class PasswordResetRequest(val email: String)
