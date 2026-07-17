package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.PlaylistInfo
import com.elsfm.mobile.core.network.api.UserApi
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

/**
 * Concrete implementation of [LibraryApiRepository].
 *
 * Loads the signed-in user's own library from the real per-user endpoints:
 * playlists via `GET users/{user}/playlists` ([PlaylistApi.getUserPlaylists]),
 * liked albums via `GET users/{user}/liked-albums` ([UserApi.getLikedAlbums]),
 * and liked artists via `GET users/{user}/liked-artists`
 * ([UserApi.getLikedArtists]) - all scoped to whichever user is cached in
 * [UserDao] after login. Channels stay the app's home/discovery channel list
 * ([ChannelApi.getChannels]) - there is no per-user "followed channels"
 * concept on the backend, but the app keeps this tab alongside the real
 * per-user Playlists/Albums/Artists tabs.
 */
@ViewModelScoped
class LibraryApiRepositoryImpl @Inject constructor(
    private val playlistApi: PlaylistApi,
    private val userApi: UserApi,
    private val channelApi: ChannelApi,
    private val userDao: UserDao,
) : LibraryApiRepository {

    override suspend fun loadLibrary(): ApiResult<LibraryData> {
        val userId = userDao.get()?.id ?: return ApiResult.Unauthorized

        return coroutineScope {
            val playlistsDeferred = async { playlistApi.getUserPlaylists(userId) }
            val albumsDeferred = async { userApi.getLikedAlbums(userId) }
            val artistsDeferred = async { userApi.getLikedArtists(userId) }
            val channelsDeferred = async { channelApi.getChannels() }

            val playlists = when (val result = playlistsDeferred.await()) {
                is ApiResult.Success -> result.data
                is ApiResult.NetworkError -> return@coroutineScope result
                is ApiResult.ValidationError -> return@coroutineScope result
                is ApiResult.Unauthorized -> return@coroutineScope result
            }

            val albums = when (val result = albumsDeferred.await()) {
                is ApiResult.Success -> result.data
                is ApiResult.NetworkError -> return@coroutineScope result
                is ApiResult.ValidationError -> return@coroutineScope result
                is ApiResult.Unauthorized -> return@coroutineScope result
            }

            val artists = when (val result = artistsDeferred.await()) {
                is ApiResult.Success -> result.data
                is ApiResult.NetworkError -> return@coroutineScope result
                is ApiResult.ValidationError -> return@coroutineScope result
                is ApiResult.Unauthorized -> return@coroutineScope result
            }

            val channels = when (val result = channelsDeferred.await()) {
                is ApiResult.Success -> result.data
                is ApiResult.NetworkError -> return@coroutineScope result
                is ApiResult.ValidationError -> return@coroutineScope result
                is ApiResult.Unauthorized -> return@coroutineScope result
            }

            ApiResult.Success(
                LibraryData(
                    playlists = playlists.map { it.toPlaylist() },
                    albums = albums,
                    artists = artists,
                    channels = channels,
                ),
            )
        }
    }

    override suspend fun createPlaylist(name: String): ApiResult<Playlist> {
        return when (val result = playlistApi.createPlaylist(name)) {
            is ApiResult.Success -> ApiResult.Success(result.data.toPlaylist())
            is ApiResult.NetworkError -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized -> result
        }
    }

    private fun PlaylistInfo.toPlaylist(): Playlist = Playlist(id = id, name = name, image = image)
}
