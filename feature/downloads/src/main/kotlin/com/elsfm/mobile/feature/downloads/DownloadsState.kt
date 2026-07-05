package com.elsfm.mobile.feature.downloads

import com.elsfm.mobile.core.database.entity.DownloadedTrack

data class DownloadsState(
    val downloadedTracks: List<DownloadedTrack> = emptyList(),
    val downloadProgress: Map<Int, Float> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
