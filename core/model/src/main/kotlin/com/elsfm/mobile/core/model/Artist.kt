package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: Int,
    val name: String,
    val plays: String? = null,
)
