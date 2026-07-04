package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LoginResponse(
    val status: String = "success",
    val user: User,
)
