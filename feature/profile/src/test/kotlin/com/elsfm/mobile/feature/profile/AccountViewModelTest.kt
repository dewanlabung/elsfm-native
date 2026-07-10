package com.elsfm.mobile.feature.profile

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.FileEntry
import com.elsfm.mobile.core.model.UserProfile
import com.elsfm.mobile.core.model.UserSessionInfo
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.AccountApi
import com.elsfm.mobile.core.network.api.SessionsApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class AccountFakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private fun dummyHttpClient(): HttpClient = HttpClient(MockEngine { respond("{}") })

private class FakeAccountApi(
    private val uploadResult: ApiResult<FileEntry> = ApiResult.Success(FileEntry(id = 1, url = "https://cdn/1.jpg")),
    private val updateResult: ApiResult<UserProfile>? = null,
) : AccountApi(dummyHttpClient()) {
    var lastUpdateUserId: Int? = null
    var lastUpdateName: String? = null
    var lastUpdateImage: String? = null
    var lastUpdateImageEntryId: Int? = null
    var removeAvatarCalled = false

    override suspend fun uploadAvatar(bytes: ByteArray, filename: String, mimeType: String): ApiResult<FileEntry> =
        uploadResult

    override suspend fun updateAccountDetails(
        userId: Int,
        name: String?,
        image: String?,
        imageEntryId: Int?,
    ): ApiResult<UserProfile> {
        lastUpdateUserId = userId
        lastUpdateName = name
        lastUpdateImage = image
        lastUpdateImageEntryId = imageEntryId
        return updateResult ?: ApiResult.Success(
            UserProfile(id = userId, name = name ?: "Existing", email = "user@example.com", profileImage = image),
        )
    }

    override suspend fun removeAvatar(userId: Int): ApiResult<UserProfile> {
        removeAvatarCalled = true
        return updateResult ?: ApiResult.Success(
            UserProfile(id = userId, name = "Existing", email = "user@example.com", profileImage = null),
        )
    }
}

private class FakeSessionsApi(
    private val result: ApiResult<List<UserSessionInfo>>,
) : SessionsApi(dummyHttpClient()) {
    override suspend fun getUserSessions(): ApiResult<List<UserSessionInfo>> = result
}

class AccountViewModelTest {
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
    fun `uploadAvatar uploads file then updates account details with returned entry`() = runTest(testDispatcher) {
        val accountApi = FakeAccountApi(uploadResult = ApiResult.Success(FileEntry(id = 42, url = "https://cdn/42.jpg")))
        val viewModel = AccountViewModel(accountApi, FakeSessionsApi(ApiResult.Success(emptyList())), AccountFakeDispatcherProvider(testDispatcher))

        viewModel.uploadAvatar(userId = 7, bytes = byteArrayOf(1, 2, 3), filename = "a.jpg", mimeType = "image/jpeg")
        delay(100)

        assertEquals(7, accountApi.lastUpdateUserId)
        assertEquals("https://cdn/42.jpg", accountApi.lastUpdateImage)
        assertEquals(42, accountApi.lastUpdateImageEntryId)
        assertEquals(false, viewModel.state.value.isUploadingAvatar)
        assertNull(viewModel.state.value.accountError)
    }

    @Test
    fun `uploadAvatar surfaces network error without calling update`() = runTest(testDispatcher) {
        val accountApi = FakeAccountApi(uploadResult = ApiResult.NetworkError(RuntimeException("boom")))
        val viewModel = AccountViewModel(accountApi, FakeSessionsApi(ApiResult.Success(emptyList())), AccountFakeDispatcherProvider(testDispatcher))

        viewModel.uploadAvatar(userId = 7, bytes = byteArrayOf(1), filename = "a.jpg", mimeType = "image/jpeg")
        delay(100)

        assertNull(accountApi.lastUpdateUserId)
        assertEquals(false, viewModel.state.value.isUploadingAvatar)
        assertTrue(viewModel.state.value.accountError != null)
    }

    @Test
    fun `updateName saves new name and updates account state`() = runTest(testDispatcher) {
        val accountApi = FakeAccountApi()
        val viewModel = AccountViewModel(accountApi, FakeSessionsApi(ApiResult.Success(emptyList())), AccountFakeDispatcherProvider(testDispatcher))

        viewModel.updateName(userId = 7, name = "Updated Name")
        delay(100)

        assertEquals("Updated Name", accountApi.lastUpdateName)
        assertEquals("Updated Name", viewModel.state.value.account?.name)
        assertEquals(false, viewModel.state.value.isSavingName)
    }

    @Test
    fun `removeAvatar clears avatar via account update`() = runTest(testDispatcher) {
        val accountApi = FakeAccountApi()
        val viewModel = AccountViewModel(accountApi, FakeSessionsApi(ApiResult.Success(emptyList())), AccountFakeDispatcherProvider(testDispatcher))

        viewModel.removeAvatar(userId = 7)
        delay(100)

        assertTrue(accountApi.removeAvatarCalled)
        assertEquals(false, viewModel.state.value.isUploadingAvatar)
        assertNull(viewModel.state.value.accountError)
    }

    @Test
    fun `loadSessions populates session list on success`() = runTest(testDispatcher) {
        val sessions = listOf(UserSessionInfo(id = 1, platform = "Android", isCurrentDevice = true))
        val viewModel = AccountViewModel(FakeAccountApi(), FakeSessionsApi(ApiResult.Success(sessions)), AccountFakeDispatcherProvider(testDispatcher))

        viewModel.loadSessions()
        delay(100)

        assertEquals(1, viewModel.state.value.sessions.size)
        assertEquals(false, viewModel.state.value.isLoadingSessions)
        assertNull(viewModel.state.value.sessionsError)
    }

    @Test
    fun `loadSessions surfaces error on failure`() = runTest(testDispatcher) {
        val viewModel = AccountViewModel(
            FakeAccountApi(),
            FakeSessionsApi(ApiResult.NetworkError(RuntimeException("boom"))),
            AccountFakeDispatcherProvider(testDispatcher),
        )

        viewModel.loadSessions()
        delay(100)

        assertEquals(false, viewModel.state.value.isLoadingSessions)
        assertTrue(viewModel.state.value.sessionsError != null)
    }
}
