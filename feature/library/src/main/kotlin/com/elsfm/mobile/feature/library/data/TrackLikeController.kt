package com.elsfm.mobile.feature.library.data

import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.UserApi
import javax.inject.Inject

/**
 * Coordinates the "like" (add/remove from library) toggle for a track, backed
 * by the real `UserApi` add/remove-from-library endpoints.
 *
 * Extracted as a small standalone helper (rather than duplicated per-ViewModel
 * logic) so any feature that renders track rows (Library's Playlist/Album
 * screens, Search results) can reuse the exact same call + state-shape without
 * copy-pasting the mutation/error-handling logic.
 */
class TrackLikeController @Inject constructor(
    private val userApi: UserApi,
) {
    /**
     * Toggles the liked state for [trackId], given its current [currentlyLiked] value.
     * Returns the new liked state on success, or `null` on failure (caller should
     * leave the previous state unchanged and surface an error).
     */
    suspend fun toggleLike(trackId: Int, currentlyLiked: Boolean): Boolean? {
        val result = if (currentlyLiked) {
            userApi.removeTrackFromLibrary(trackId)
        } else {
            userApi.addTrackToLibrary(trackId)
        }
        return when (result) {
            is ApiResult.Success -> result.data
            is ApiResult.NetworkError,
            is ApiResult.ValidationError,
            is ApiResult.Unauthorized -> null
        }
    }
}
