package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Channel(
    val id: Int,
    val name: String
)
