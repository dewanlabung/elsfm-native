package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: Int,
    val name: String,
    val slug: String? = null,
    @SerialName("model_type") val modelType: String? = null,
)
