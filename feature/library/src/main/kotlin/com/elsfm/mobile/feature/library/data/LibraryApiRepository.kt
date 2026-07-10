package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.ApiResult

/**
 * Repository for loading library data (playlists, albums, channels).
 *
 * Backed entirely by real API data. The backend has no dedicated
 * "list my playlists" / "list my albums" endpoint, so playlists and albums
 * are aggregated from Channel 5's real nested sub-channels via
 * [com.elsfm.mobile.core.network.api.ChannelApi.getChannelContent] — see
 * [LibraryApiRepositoryImpl] for details.
 */
interface LibraryApiRepository {
    /**
     * Load library data (playlists, albums, channels).
     */
    suspend fun loadLibrary(): ApiResult<LibraryData>
}
