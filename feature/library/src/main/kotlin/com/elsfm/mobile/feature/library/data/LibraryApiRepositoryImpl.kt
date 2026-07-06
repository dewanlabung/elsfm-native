package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ChannelApi
import com.elsfm.mobile.feature.library.SampleLibraryData
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject

/**
 * Concrete implementation of [LibraryApiRepository].
 *
 * Fetches channels from the real API and provides sample data for playlists
 * and albums. This allows the library feature to work immediately while the
 * backend team adds list endpoints.
 *
 * TODO: Replace SampleLibraryData with real list endpoints when
 *       /api/v1/playlists and /api/v1/albums become available.
 */
@ViewModelScoped
class LibraryApiRepositoryImpl @Inject constructor(
    private val channelApi: ChannelApi,
) : LibraryApiRepository {

    override suspend fun loadLibrary(): ApiResult<LibraryData> {
        return when (val result = channelApi.getChannels()) {
            is ApiResult.Success -> {
                ApiResult.Success(
                    LibraryData(
                        playlists = SampleLibraryData.playlists,
                        albums = SampleLibraryData.albums,
                        channels = result.data,
                    )
                )
            }
            is ApiResult.NetworkError -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized -> result
        }
    }
}
