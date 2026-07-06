package com.elsfm.mobile.core.database.repository

import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.database.entity.FollowedArtistEntity
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.UserApiLike
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

open class FollowStateRepository @Inject constructor(
    private val followStateDao: FollowStateDao,
    private val userApi: UserApiLike,
) {
    open fun observeFollowing(artistId: Int): Flow<Boolean> =
        followStateDao.observeIsFollowing(artistId)

    open suspend fun isFollowing(artistId: Int): Boolean =
        followStateDao.isFollowing(artistId)

    open suspend fun follow(artistId: Int) {
        try {
            val result = userApi.followArtist(artistId)
            when (result) {
                is ApiResult.Success -> {
                    followStateDao.insertFollowed(FollowedArtistEntity(artistId))
                }
                is ApiResult.NetworkError -> throw result.cause
                is ApiResult.ValidationError -> throw Exception("Validation error")
                is ApiResult.Unauthorized -> throw Exception("Unauthorized")
            }
        } catch (e: Exception) {
            followStateDao.deleteFollowedByArtistId(artistId)
            throw e
        }
    }

    open suspend fun unfollow(artistId: Int) {
        try {
            val result = userApi.unfollowArtist(artistId)
            when (result) {
                is ApiResult.Success -> {
                    followStateDao.deleteFollowedByArtistId(artistId)
                }
                is ApiResult.NetworkError -> throw result.cause
                is ApiResult.ValidationError -> throw Exception("Validation error")
                is ApiResult.Unauthorized -> throw Exception("Unauthorized")
            }
        } catch (e: Exception) {
            followStateDao.insertFollowed(FollowedArtistEntity(artistId))
            throw e
        }
    }
}
