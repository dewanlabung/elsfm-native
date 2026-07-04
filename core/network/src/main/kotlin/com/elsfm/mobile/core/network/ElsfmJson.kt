package com.elsfm.mobile.core.network

import kotlinx.serialization.json.Json

fun elsfmJson(): Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    isLenient = true
}
