package com.elsfm.mobile.feature.discovery

import com.elsfm.mobile.core.model.Channel

data class DiscoveryState(
    val channel: Channel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
)
