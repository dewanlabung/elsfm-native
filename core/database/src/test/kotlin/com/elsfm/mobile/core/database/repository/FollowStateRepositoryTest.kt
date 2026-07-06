package com.elsfm.mobile.core.database.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.elsfm.mobile.core.database.AppDatabase
import com.elsfm.mobile.core.database.dao.FollowStateDao
import com.elsfm.mobile.core.model.FollowState
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.UserApiLike
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FollowStateRepositoryTest {
    private lateinit var database: AppDatabase
    private lateinit var followStateDao: FollowStateDao
    private lateinit var userApi: FakeUserApi
    private lateinit var followStateRepository: FollowStateRepository

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
        followStateDao = database.followStateDao()
        userApi = FakeUserApi()
        followStateRepository = FollowStateRepository(followStateDao, userApi)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun follow_insertEntityAndCallApi() = runTest {
        userApi.setFollowResult(
            ApiResult.Success(FollowState(following = true, timestamp = "2024-01-15T10:00:00Z"))
        )

        followStateRepository.follow(1)

        assertTrue(followStateRepository.isFollowing(1))
    }

    @Test
    fun unfollow_deleteEntityAndCallApi() = runTest {
        userApi.setFollowResult(
            ApiResult.Success(FollowState(following = true, timestamp = "2024-01-15T10:00:00Z"))
        )
        followStateRepository.follow(1)

        userApi.setUnfollowResult(
            ApiResult.Success(FollowState(following = false, timestamp = "2024-01-15T11:00:00Z"))
        )
        followStateRepository.unfollow(1)

        assertFalse(followStateRepository.isFollowing(1))
    }

    @Test
    fun follow_rollsBackOnApiError() = runTest {
        userApi.setFollowResult(
            ApiResult.NetworkError(Exception("API error"))
        )

        try {
            followStateRepository.follow(1)
        } catch (e: Exception) {
            // Expected
        }
        assertFalse(followStateRepository.isFollowing(1))
    }

    @Test
    fun unfollow_rollsBackOnApiError() = runTest {
        userApi.setFollowResult(
            ApiResult.Success(FollowState(following = true, timestamp = "2024-01-15T10:00:00Z"))
        )
        followStateRepository.follow(1)

        userApi.setUnfollowResult(
            ApiResult.NetworkError(Exception("API error"))
        )

        try {
            followStateRepository.unfollow(1)
        } catch (e: Exception) {
            // Expected
        }
        assertTrue(followStateRepository.isFollowing(1))
    }

    @Test
    fun isFollowing_returnsFalseWhenNotFollowing() = runTest {
        assertFalse(followStateRepository.isFollowing(1))
    }

    @Test
    fun isFollowing_returnsTrueWhenFollowing() = runTest {
        userApi.setFollowResult(
            ApiResult.Success(FollowState(following = true, timestamp = "2024-01-15T10:00:00Z"))
        )
        followStateRepository.follow(1)

        assertTrue(followStateRepository.isFollowing(1))
    }
}

/**
 * Fake implementation of UserApiLike for testing
 */
internal class FakeUserApi : UserApiLike {
    private var followResult: ApiResult<FollowState> = ApiResult.NetworkError(Exception("Not set"))
    private var unfollowResult: ApiResult<FollowState> = ApiResult.NetworkError(Exception("Not set"))

    fun setFollowResult(result: ApiResult<FollowState>) {
        this.followResult = result
    }

    fun setUnfollowResult(result: ApiResult<FollowState>) {
        this.unfollowResult = result
    }

    override suspend fun isArtistFollowed(artistId: Int): ApiResult<FollowState> =
        ApiResult.Success(FollowState(following = false))

    override suspend fun followArtist(artistId: Int): ApiResult<FollowState> = followResult

    override suspend fun unfollowArtist(artistId: Int): ApiResult<FollowState> = unfollowResult
}
