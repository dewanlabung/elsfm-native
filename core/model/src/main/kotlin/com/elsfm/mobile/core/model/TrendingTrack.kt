package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TrendingTrack(
    val track: Track,
    @SerialName("rank")
    val position: Int,
)
