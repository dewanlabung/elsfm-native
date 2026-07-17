package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.feature.library.data.LibraryApiRepository
import com.elsfm.mobile.feature.library.data.LibraryData

/**
 * Fake implementation of [LibraryApiRepository] for testing.
 *
 * Allows injecting custom data for test scenarios.
 */
class FakeLibraryApiRepository(
    private val playlists: List<Playlist> = emptyList(),
    private val albums: List<Album> = emptyList(),
    private val artists: List<Artist> = emptyList(),
    private val channels: List<Channel> = emptyList(),
    private val error: Throwable? = null,
    private val createPlaylistResult: ApiResult<Playlist>? = null,
) : LibraryApiRepository {

    override suspend fun loadLibrary(): ApiResult<LibraryData> {
        error?.let {
            return ApiResult.NetworkError(it)
        }

        return ApiResult.Success(
            LibraryData(
                playlists = playlists,
                albums = albums,
                artists = artists,
                channels = channels,
            )
        )
    }

    override suspend fun createPlaylist(name: String): ApiResult<Playlist> =
        createPlaylistResult ?: ApiResult.Success(Playlist(id = 999, name = name, image = null))
}
