package com.elsfm.mobile.feature.userprofile

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.ProfileApi
import com.elsfm.mobile.core.network.api.UserApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

private val fakeHttpClient = HttpClient(MockEngine { _ ->
    respond("{}", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
})

private val testProfile = UserProfile(
    id = 1,
    name = "Test User",
    bio = "Bio here",
    followersCount = 3,
    followedCount = 2,
)

private class FakeProfileApi(
    private val profileResult: ApiResult<UserProfile> = ApiResult.Success(testProfile),
) : ProfileApi(fakeHttpClient) {
    override suspend fun getProfile(userId: Int) = profileResult
}

/** Builds a real [UserApi] backed by a [MockEngine] that always answers with [body]. */
private fun buildUserApi(status: HttpStatusCode = HttpStatusCode.OK, body: String = "{}"): UserApi {
    val mockEngine = MockEngine { _ ->
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    val httpClient = HttpClient(mockEngine) {
        install(ContentNegotiation) { json() }
    }
    return UserApi(httpClient)
}

private fun buildViewModel(
    profileApi: ProfileApi = FakeProfileApi(),
    userApi: UserApi = buildUserApi(),
    userId: Int = 1,
    testScheduler: kotlinx.coroutines.test.TestCoroutineScheduler,
) = UserProfileViewModel(
    profileApi = profileApi,
    userApi = userApi,
    savedStateHandle = SavedStateHandle().apply { set(USER_ID_ARG, userId) },
    dispatcherProvider = TestDispatcherProvider(StandardTestDispatcher(testScheduler)),
)

class UserProfileViewModelTest {

    @Test
    fun `loads profile successfully`() = runTest {
        val viewModel = buildViewModel(testScheduler = testScheduler)

        viewModel.state.test {
            assertEquals(UserProfileState(), awaitItem())

            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)

            val successState = awaitItem()
            assertEquals(testProfile, successState.profile)
            assertFalse(successState.isLoading)
        }
    }

    @Test
    fun `shows error when profile fetch fails`() = runTest {
        val error = RuntimeException("Network error")
        val viewModel = buildViewModel(
            profileApi = FakeProfileApi(profileResult = ApiResult.NetworkError(error)),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading

            val errorState = awaitItem()
            assertEquals("Network error", errorState.error)
            assertFalse(errorState.isLoading)
        }
    }

    @Test
    fun `selectTab FOLLOWERS lazily loads followers once`() = runTest {
        val followersBody = """
            {"pagination":{"data":[
              {"id":10,"name":"Follower One","username":"follower1"},
              {"id":11,"name":"Follower Two","username":"follower2"}
            ]}}
        """.trimIndent()
        val viewModel = buildViewModel(
            userApi = buildUserApi(body = followersBody),
            testScheduler = testScheduler,
        )

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // profile success

            viewModel.selectTab(UserProfileTab.FOLLOWERS)

            // The tab-selection emission and the transient "loading" emission may or may
            // not both surface as distinct StateFlow emissions depending on how quickly the
            // mocked network call resolves - only the final "followers loaded" state is
            // asserted on strictly here.
            var latestState = awaitItem()
            assertEquals(UserProfileTab.FOLLOWERS, latestState.selectedTab)
            while (latestState.followers.isEmpty()) {
                latestState = awaitItem()
            }

            assertEquals(2, latestState.followers.size)
            assertFalse(latestState.isFollowersLoading)
        }
    }

    @Test
    fun `toggleFollow flips isFollowing on success`() = runTest {
        val viewModel = buildViewModel(testScheduler = testScheduler)

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            val successState = awaitItem()
            assertFalse(successState.isFollowing)

            viewModel.toggleFollow(successState.profile!!.id)

            // Same conflation caveat as the followers-tab test: only the final state is
            // asserted on strictly, the transient loading emission may or may not surface.
            var latestState = awaitItem()
            while (!latestState.isFollowing) {
                latestState = awaitItem()
            }
            assertFalse(latestState.isFollowLoading)
        }
    }

    @Test
    fun `toggleFollowUser tracks per-row follow state`() = runTest {
        val viewModel = buildViewModel(testScheduler = testScheduler)

        viewModel.state.test {
            awaitItem() // initial
            awaitItem() // loading
            awaitItem() // profile success

            viewModel.toggleFollowUser(10)

            val followedState = awaitItem()
            assertTrue(followedState.followedUserIds.contains(10))

            viewModel.toggleFollowUser(10)

            val unfollowedState = awaitItem()
            assertFalse(unfollowedState.followedUserIds.contains(10))
        }
    }
}
