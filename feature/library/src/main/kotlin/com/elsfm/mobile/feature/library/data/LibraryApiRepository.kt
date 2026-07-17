package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult

/**
 * Repository for loading the signed-in user's library data (playlists, albums, channels).
 *
 * Backed entirely by real, per-user API data — see [LibraryApiRepositoryImpl] for details.
 */
interface LibraryApiRepository {
    /**
     * Load library data (playlists, albums, channels).
     */
    suspend fun loadLibrary(): ApiResult<LibraryData>

    /** Creates a new playlist for the signed-in user via the real `POST playlists` endpoint. */
    suspend fun createPlaylist(name: String): ApiResult<Playlist>
}
