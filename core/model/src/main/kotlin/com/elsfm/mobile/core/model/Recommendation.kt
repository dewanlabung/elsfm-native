package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recommendation(
    val track: Track,
    @SerialName("score")
    val relevanceScore: Float? = null,
)
