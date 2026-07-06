package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.ApiResult

/**
 * Repository for loading library data (playlists, albums, channels).
 *
 * Coordinates fetching from real API endpoints (channels) and sample data
 * (playlists, albums) when list endpoints are unavailable.
 */
interface LibraryApiRepository {
    /**
     * Load library data (playlists, albums, channels).
     *
     * Playlists and albums are currently from [SampleLibraryData]; replace with
     * real API calls once `/api/v1/playlists` and `/api/v1/albums` list endpoints
     * become available.
     */
    suspend fun loadLibrary(): ApiResult<LibraryData>
}
