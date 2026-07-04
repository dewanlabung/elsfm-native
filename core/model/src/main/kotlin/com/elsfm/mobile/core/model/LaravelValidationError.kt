package com.elsfm.mobile.core.model

import kotlinx.serialization.Serializable

@Serializable
data class LaravelValidationError(
    val message: String,
    val errors: Map<String, List<String>> = emptyMap(),
)
