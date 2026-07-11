package com.elsfm.mobile.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Tag(
    val id: Int,
    val name: String,
    @SerialName("display_name") val displayName: String? = null,
)
