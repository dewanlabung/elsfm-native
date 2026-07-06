package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class FollowState(
    val following: Boolean,
    val timestamp: String = "",
)
