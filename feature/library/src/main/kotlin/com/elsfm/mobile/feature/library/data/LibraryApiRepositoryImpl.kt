package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.ChannelContentResult
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Concrete implementation of [LibraryApiRepository].
 *
 * There is no dedicated `/api/v1/playlists` or `/api/v1/albums` "list mine"
 * endpoint on the backend (only per-channel content and per-playlist/album
 * tracks exist — see `ChannelApi`, `PlaylistApi`, `AlbumApi`). Real playlists
 * and albums for the library screen are instead aggregated from Channel 5's
 * nested sub-channels: any sub-channel whose `config.contentModel` is
 * "playlist" contributes to [LibraryData.playlists], and any whose
 * `contentModel` is "album" contributes to [LibraryData.albums]. This is the
 * same real-data pattern `feature.discovery.DiscoveryViewModel` uses for the
 * home screen's Kids Zone / New Release sections.
 */
@ViewModelScoped
class LibraryApiRepositoryImpl @Inject constructor(
    private val channelApi: ChannelApi,
) : LibraryApiRepository {

    override suspend fun loadLibrary(): ApiResult<LibraryData> {
        return when (val result = channelApi.getChannels()) {
            is ApiResult.Success -> ApiResult.Success(aggregateLibraryData(result.data))
            is ApiResult.NetworkError -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized -> result
        }
    }

    private suspend fun aggregateLibraryData(channels: List<Channel>): LibraryData {
        val contentResults = coroutineScope {
            channels
                .map { channel -> async { channelApi.getChannelContent(channel.id) } }
                .awaitAll()
        }

        return LibraryData(
            playlists = contentResults.flatMap { it.asPlaylists() },
            albums = contentResults.flatMap { it.asAlbums() },
            channels = channels,
        )
    }

    private fun ApiResult<ChannelContentResult>.asPlaylists(): List<Playlist> {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Playlists)?.items.orEmpty()
    }

    private fun ApiResult<ChannelContentResult>.asAlbums(): List<Album> {
        return ((this as? ApiResult.Success)?.data as? ChannelContentResult.Albums)?.items.orEmpty()
    }
}
