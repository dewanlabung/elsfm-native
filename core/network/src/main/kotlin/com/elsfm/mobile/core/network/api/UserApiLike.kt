package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.network.ApiResult

interface UserApiLike {
    suspend fun isArtistFollowed(artistId: Int): ApiResult<FollowState>
    suspend fun followArtist(artistId: Int): ApiResult<FollowState>
    suspend fun unfollowArtist(artistId: Int): ApiResult<FollowState>
}
