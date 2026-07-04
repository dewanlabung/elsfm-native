package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: Int,
    val username: String? = null,
    val name: String? = null,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    val permissions: List<Permission> = emptyList(),
    @SerialName("access_token") val accessToken: String? = null,
)
