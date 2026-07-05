package com.elsfm.mobile.feature.profile

import com.elsfm.mobile.core.common.DispatcherProvider
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

private class FakeProfileApi(
    private val profile: UserProfile?,
    private val recentlyPlayed: List<Track> = emptyList(),
) : ProfileApi(HttpClient(MockEngine { respond("{}") })) {
    override suspend fun getProfile(): ApiResult<UserProfile> {
        return if (profile == null) {
            ApiResult.NetworkError(RuntimeException("Failed to load profile"))
        } else {
            ApiResult.Success(profile)
        }
    }

    override suspend fun getRecentlyPlayed(): ApiResult<List<Track>> {
        return ApiResult.Success(recentlyPlayed)
    }

    override suspend fun updateProfile(name: String, bio: String?): ApiResult<UserProfile> {
        return ApiResult.Success(
            (profile ?: UserProfile(id = 1, name = name, email = "test@example.com"))
                .copy(name = name, bio = bio)
        )
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
        val viewModel = ProfileViewModel(api, FakeDispatcherProvider(testDispatcher))

        delay(100) // Allow coroutines to execute

        val state = viewModel.state.value
        assertEquals("John", state.userProfile?.name)
        assertEquals(false, state.isLoading)
    }
}
