package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user row returned by the user-to-user follow list endpoints
 * (`GET api/v1/users/{userId}/followers` and `GET api/v1/users/{userId}/followed-users`).
 *
 * Not the same as [ArtistFollower] ("who follows this artist") - this is "who follows/is
 * followed by this user". The shape is assumed to mirror [ArtistFollower] since both are
 * "list of user rows" endpoints on the same backend; this has not been confirmed against a
 * live response (no `FollowersController`/`FollowedUsersController` source available in this
 * repo). Verify and adjust if the real shape differs.
 */
@Serializable
data class FollowUser(
    val id: Int,
    val name: String,
    val username: String? = null,
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("followers_count") val followersCount: Int? = null,
)
