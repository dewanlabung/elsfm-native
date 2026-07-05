package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String,
    @SerialName("image")
    val profileImage: String? = null,
    val bio: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("followed_count")
    val followedCount: Int = 0,
)
