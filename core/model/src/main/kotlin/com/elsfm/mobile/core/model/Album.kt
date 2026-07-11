package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Int,
    val name: String,
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    /**
     * Only populated by `GET /albums/{id}` (`AlbumApi.getAlbum`), which nests
     * the album's tracks in the same response - the backend has no separate
     * tracks endpoint. Empty for every other endpoint that returns an Album.
     */
    val tracks: List<Track> = emptyList(),
    /** Only populated by the `albumPage` loader (`AlbumApi.getAlbum`). */
    val description: String? = null,
    val tags: List<Tag> = emptyList(),
    val plays: String? = null,
    @SerialName("likes_count") val likesCount: Int? = null,
    @SerialName("reposts_count") val repostsCount: Int? = null,
    @SerialName("comments_count") val commentsCount: Int? = null,
)
