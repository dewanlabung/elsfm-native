package com.elsfm.mobile.feature.player.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.PlaylistApi
import com.elsfm.mobile.core.network.api.RepostApi
import com.elsfm.mobile.core.network.api.RepostToggleResponse
import com.elsfm.mobile.core.network.api.ShareTrackResponse
import com.elsfm.mobile.core.network.api.UserApi
import javax.inject.Inject

class PlayerMenuRepository @Inject constructor(
    private val playlistApi: PlaylistApi,
    private val userApi: UserApi,
    private val repostApi: RepostApi,
) {
    suspend fun addTrackToPlaylist(playlistId: Int, trackId: Int): ApiResult<Unit> {
        return playlistApi.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun addTrackToLibrary(trackId: Int): ApiResult<Boolean> {
        return userApi.addTrackToLibrary(trackId)
    }

    suspend fun shareTrack(trackId: Int): ApiResult<String> {
        return when (val result = userApi.shareTrack(trackId)) {
            is ApiResult.Success<ShareTrackResponse> -> ApiResult.Success(result.data.shareUrl)
            is ApiResult.NetworkError -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized -> result
        }
    }

    suspend fun repostTrack(trackId: Int): ApiResult<String> {
        return when (val result = repostApi.toggleTrackRepost(trackId)) {
            is ApiResult.Success<RepostToggleResponse> -> ApiResult.Success(result.data.action)
            is ApiResult.NetworkError -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized -> result
        }
    }
}
