package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Int,
    val name: String,
    val image: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
)
