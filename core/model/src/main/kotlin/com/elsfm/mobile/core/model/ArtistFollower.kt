package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A user following an artist, as returned by `GET api/v1/artists/{id}/followers`
 * (`ArtistFollowersController` -> `ArtistLoader::loadArtistFollowers`).
 */
@Serializable
data class ArtistFollower(
    val id: Int,
    val name: String,
    val username: String? = null,
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
    @SerialName("is_pro") val isPro: Boolean = false,
    @SerialName("followers_count") val followersCount: Int? = null,
)
