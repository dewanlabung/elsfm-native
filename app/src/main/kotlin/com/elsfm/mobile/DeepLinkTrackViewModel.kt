package com.elsfm.mobile

import androidx.lifecycle.ViewModel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.TrackApi
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Resolves a track deep link (`https://www.elsfm.com/track/{id}/{slug}`) to a real [Track]. */
@HiltViewModel
class DeepLinkTrackViewModel @Inject constructor(
    private val trackApi: TrackApi,
) : ViewModel() {
    suspend fun getTrack(id: Int): Track? {
        return (trackApi.getTrack(id) as? ApiResult.Success)?.data
    }
}
