package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Matches the Laravel `ArtistLoader::toApiResource()` shape returned by
 * `GET api/v1/artists/{id}` (default `loader=artistPage`). That single endpoint already
 * bundles profile bio/location, social links, like/follower counts, and Spotify-derived
 * "similar" artists (`GetSimilarArtists` service) — no separate endpoint is needed for
 * the Artist profile screen's bio or Similar Artists tab.
 */
@Serializable
data class Artist(
    val id: Int,
    val name: String,
    @SerialName("image_small")
    @Serializable(with = ImageUrlSerializer::class)
    val image: String? = null,
    val plays: String? = null,
    val verified: Boolean = false,
    val profile: ArtistProfile? = null,
    val links: List<ArtistLink> = emptyList(),
    @SerialName("likes_count") val likesCount: Int? = null,
    @SerialName("followers_count") val followersCount: Int? = null,
    val similar: List<Artist> = emptyList(),
)

@Serializable
data class ArtistProfile(
    val city: String? = null,
    val country: String? = null,
    val description: String? = null,
)

@Serializable
data class ArtistLink(
    val url: String,
    val title: String,
)
