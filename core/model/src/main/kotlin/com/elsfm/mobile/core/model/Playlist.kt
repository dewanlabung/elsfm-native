package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Playlist(
    val id: Int,
    val name: String,
    val image: String? = null,
    @SerialName("channel_id")
    val channelId: Int? = null
)
