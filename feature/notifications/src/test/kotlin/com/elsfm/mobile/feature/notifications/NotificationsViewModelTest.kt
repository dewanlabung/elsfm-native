package com.elsfm.mobile.feature.notifications

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.model.AppNotification
import com.elsfm.mobile.core.model.NotificationData
import com.elsfm.mobile.core.model.NotificationLine
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.NotificationsApi
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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class NotificationsFakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private fun dummyHttpClient(): HttpClient = HttpClient(MockEngine { respond("{}") })

private fun testNotification(id: String, readAt: String? = null) = AppNotification(
    id = id,
    readAt = readAt,
    createdAt = "2024-01-01T00:00:00Z",
    type = "App\\Notifications\\NewRelease",
    data = NotificationData(lines = listOf(NotificationLine(content = "Test notification $id"))),
)

private class FakeNotificationsApi(
    private val listResult: ApiResult<List<AppNotification>>,
    private val markAsReadResult: ApiResult<Unit> = ApiResult.Success(Unit),
) : NotificationsApi(dummyHttpClient()) {
    var lastMarkedIds: List<String>? = null

    override suspend fun getNotifications(perPage: Int): ApiResult<List<AppNotification>> = listResult

    override suspend fun markAsRead(ids: List<String>): ApiResult<Unit> {
        lastMarkedIds = ids
        return markAsReadResult
    }
}

class NotificationsViewModelTest {
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
    fun `loads notifications on init`() = runTest(testDispatcher) {
        val notifications = listOf(testNotification("1"), testNotification("2"))
        val viewModel = NotificationsViewModel(
            FakeNotificationsApi(ApiResult.Success(notifications)),
            NotificationsFakeDispatcherProvider(testDispatcher),
        )

        delay(100)

        assertEquals(2, viewModel.state.value.notifications.size)
        assertEquals(false, viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `surfaces error when loading fails`() = runTest(testDispatcher) {
        val viewModel = NotificationsViewModel(
            FakeNotificationsApi(ApiResult.NetworkError(RuntimeException("boom"))),
            NotificationsFakeDispatcherProvider(testDispatcher),
        )

        delay(100)

        assertTrue(viewModel.state.value.notifications.isEmpty())
        assertNotNull(viewModel.state.value.error)
    }

    @Test
    fun `markAsRead marks the notification read locally and calls the api`() = runTest(testDispatcher) {
        val api = FakeNotificationsApi(ApiResult.Success(listOf(testNotification("1"))))
        val viewModel = NotificationsViewModel(api, NotificationsFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.markAsRead("1")
        delay(100)

        assertEquals(listOf("1"), api.lastMarkedIds)
        assertNotNull(viewModel.state.value.notifications.first { it.id == "1" }.readAt)
    }

    @Test
    fun `markAsRead is a no-op for an already-read notification`() = runTest(testDispatcher) {
        val api = FakeNotificationsApi(ApiResult.Success(listOf(testNotification("1", readAt = "2024-01-02T00:00:00Z"))))
        val viewModel = NotificationsViewModel(api, NotificationsFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.markAsRead("1")
        delay(100)

        assertNull(api.lastMarkedIds)
    }
}
