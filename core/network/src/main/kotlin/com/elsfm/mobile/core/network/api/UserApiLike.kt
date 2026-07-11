package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult

interface UserApiLike {
    suspend fun followArtist(artistId: Int): ApiResult<Boolean>
    suspend fun unfollowArtist(artistId: Int): ApiResult<Boolean>
}
