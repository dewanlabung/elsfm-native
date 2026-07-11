package com.elsfm.mobile.feature.profile

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.UserDao
import com.elsfm.mobile.core.database.UserEntity
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private class FakeUserDao(private val userId: Int? = 1) : UserDao {
    override suspend fun upsert(user: UserEntity) {}
    override suspend fun get(): UserEntity? =
        userId?.let { UserEntity(id = it, username = null, name = null, email = "test@example.com", avatarUrl = null) }
    override suspend fun clear() {}
}

private class FakeProfileApi(
    private val profile: UserProfile?,
    private val recentlyPlayed: List<Track> = emptyList(),
) : ProfileApi(HttpClient(MockEngine { respond("{}") })) {
    override suspend fun getProfile(userId: Int): ApiResult<UserProfile> {
        return if (profile == null) {
            ApiResult.NetworkError(RuntimeException("Failed to load profile"))
        } else {
            ApiResult.Success(profile)
        }
    }

    override suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return ApiResult.Success(recentlyPlayed)
    }

    override suspend fun updateProfile(userId: Int, name: String, bio: String?): ApiResult<UserProfile> {
        return ApiResult.Success(
            (profile ?: UserProfile(id = 1, name = name))
                .copy(name = name, bio = bio)
        )
    }
}

private class FakeFailingProfileApi(
    private val profile: UserProfile?,
) : ProfileApi(HttpClient(MockEngine { respond("{}") })) {
    override suspend fun getProfile(userId: Int): ApiResult<UserProfile> {
        return if (profile == null) {
            ApiResult.NetworkError(RuntimeException("Failed to load profile"))
        } else {
            ApiResult.Success(profile)
        }
    }

    override suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return ApiResult.Success(emptyList())
    }

    override suspend fun updateProfile(userId: Int, name: String, bio: String?): ApiResult<UserProfile> {
        return ApiResult.NetworkError(RuntimeException("Update failed"))
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadProfileSuccess() = runTest(testDispatcher) {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com")
        val api = FakeProfileApi(profile = profile)
        val viewModel = ProfileViewModel(api, FakeUserDao(), FakeDispatcherProvider(testDispatcher))

        delay(100) // Allow coroutines to execute

        val state = viewModel.state.value
        assertEquals("John", state.userProfile?.name)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun `setEditMode true enables edit mode`() = runTest(testDispatcher) {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com")
        val api = FakeProfileApi(profile = profile)
        val viewModel = ProfileViewModel(api, FakeUserDao(), FakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.setEditMode(true)

        assertEquals(true, viewModel.state.value.isEditMode)
    }

    @Test
    fun `setEditMode false disables edit mode`() = runTest(testDispatcher) {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com")
        val api = FakeProfileApi(profile = profile)
        val viewModel = ProfileViewModel(api, FakeUserDao(), FakeDispatcherProvider(testDispatcher))
        delay(100)
        viewModel.setEditMode(true)

        viewModel.setEditMode(false)

        assertEquals(false, viewModel.state.value.isEditMode)
    }

    @Test
    fun `updateProfile success updates profile and exits edit mode`() = runTest(testDispatcher) {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com", bio = "old bio")
        val api = FakeProfileApi(profile = profile)
        val viewModel = ProfileViewModel(api, FakeUserDao(), FakeDispatcherProvider(testDispatcher))
        delay(100)
        viewModel.setEditMode(true)

        viewModel.updateProfile("Jane", "new bio")
        delay(100)

        val state = viewModel.state.value
        assertEquals("Jane", state.userProfile?.name)
        assertEquals("new bio", state.userProfile?.bio)
        assertEquals(false, state.isEditMode)
    }

    @Test
    fun `updateProfile network error surfaces error and stays in edit mode`() = runTest(testDispatcher) {
        val profile = UserProfile(id = 1, name = "John", email = "john@example.com")
        val api = FakeFailingProfileApi(profile = profile)
        val viewModel = ProfileViewModel(api, FakeUserDao(), FakeDispatcherProvider(testDispatcher))
        delay(100)
        viewModel.setEditMode(true)

        viewModel.updateProfile("Jane", null)
        delay(100)

        val state = viewModel.state.value
        assertEquals("John", state.userProfile?.name)
        assertEquals(true, state.isEditMode)
        assertEquals("Update failed", state.error)
    }
}
