package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Int,
    val name: String,
    @Serializable(with = ImageUrlSerializer::class) val image: String?,
    @SerialName("duration") @Serializable(with = NullSafeLongSerializer::class) val durationMs: Long,
    val src: String? = null,
    val plays: String? = null,
    val artists: List<Artist>,
    /**
     * Present when the backend track resource has the `album` relation loaded
     * (`TrackLoader::toApiResource` sets `resource['album']`, nullable). Not every track
     * list endpoint eager-loads it, so callers must treat this as optional and hide/disable
     * "Go to album" navigation when it is null rather than guessing an album id.
     */
    val album: TrackAlbum? = null,
)

@Serializable
data class TrackAlbum(
    val id: Int,
    val name: String,
    @Serializable(with = ImageUrlSerializer::class) val image: String? = null,
)
