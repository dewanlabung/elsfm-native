package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Doubles as the decode target for two different real backend shapes:
 * - `PUT api/v1/users/{id}` ([AccountApi][com.elsfm.mobile.core.network.api.AccountApi])
 *   returns the raw Eloquent `User` model, which includes `email` only when viewing your
 *   own account - hence nullable.
 * - `GET api/v1/user-profile/{id}` (the public profile page) never includes `email` at all;
 *   [ProfileApi][com.elsfm.mobile.core.network.api.ProfileApi] decodes that response into a
 *   separate internal DTO and constructs this type manually, so `email` is simply absent there.
 */
@Serializable
data class UserProfile(
    val id: Int,
    val name: String,
    val email: String? = null,
    @SerialName("image")
    val profileImage: String? = null,
    val bio: String? = null,
    @SerialName("followers_count")
    val followersCount: Int = 0,
    @SerialName("followed_count")
    val followedCount: Int = 0,
)
