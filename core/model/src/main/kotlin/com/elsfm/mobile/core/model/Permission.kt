package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class Permission(
    val id: Int,
    val name: String,
)
