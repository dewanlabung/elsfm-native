package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Track(
    val id: Int,
    val name: String,
    val image: String?,
    @SerialName("duration") val durationMs: Long,
    val src: String? = null,
    val plays: String? = null,
    val artists: List<Artist>,
)
