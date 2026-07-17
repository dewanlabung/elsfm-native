package com.elsfm.mobile.feature.subscriptions

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.BillingApi
import com.elsfm.mobile.core.network.api.BillingSubscription
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

private class SubscriptionFakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

private fun dummyHttpClient(): HttpClient = HttpClient(MockEngine { respond("{}") })

private fun sampleSubscription(
    id: Int = 42,
    onGracePeriod: Boolean = false,
) = BillingSubscription(
    id = id,
    productName = "Premium",
    priceName = "Monthly",
    renewsAt = "2026-08-17",
    endsAt = null,
    trialEndsAt = null,
    onGracePeriod = onGracePeriod,
    onTrial = false,
    valid = true,
    active = true,
    cancelled = false,
    gatewayName = "stripe",
)

private class FakeBillingApi(
    private val getResult: ApiResult<BillingSubscription?> = ApiResult.Success(sampleSubscription()),
    private val cancelResult: ApiResult<Unit> = ApiResult.Success(Unit),
    private val resumeResult: ApiResult<Unit> = ApiResult.Success(Unit),
) : BillingApi(dummyHttpClient()) {
    var getCallCount = 0
    var lastCancelId: Int? = null
    var lastCancelAtPeriodEnd: Boolean? = null
    var lastResumeId: Int? = null

    override suspend fun getCurrentSubscription(): ApiResult<BillingSubscription?> {
        getCallCount++
        return getResult
    }

    override suspend fun cancelSubscription(id: Int, atPeriodEnd: Boolean): ApiResult<Unit> {
        lastCancelId = id
        lastCancelAtPeriodEnd = atPeriodEnd
        return cancelResult
    }

    override suspend fun resumeSubscription(id: Int): ApiResult<Unit> {
        lastResumeId = id
        return resumeResult
    }
}

class SubscriptionViewModelTest {
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
    fun `loadSubscription populates state on success`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(sampleSubscription()))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))

        delay(100)

        assertEquals(42, viewModel.state.value.subscription?.id)
        assertEquals(false, viewModel.state.value.isLoading)
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `loadSubscription treats null subscription as free tier not an error`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(null))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))

        delay(100)

        assertNull(viewModel.state.value.subscription)
        assertNull(viewModel.state.value.error)
        assertEquals(false, viewModel.state.value.isLoading)
    }

    @Test
    fun `loadSubscription surfaces network error`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.NetworkError(RuntimeException("boom")))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))

        delay(100)

        assertEquals(false, viewModel.state.value.isLoading)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `loadSubscription surfaces unauthorized`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Unauthorized)
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))

        delay(100)

        assertEquals("Unauthorized", viewModel.state.value.error)
    }

    @Test
    fun `resume calls resumeSubscription with the loaded subscription id then reloads`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(sampleSubscription(id = 7, onGracePeriod = true)))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.resume()
        delay(100)

        assertEquals(7, billingApi.lastResumeId)
        assertEquals(false, viewModel.state.value.isResuming)
        assertEquals(2, billingApi.getCallCount)
    }

    @Test
    fun `resume does nothing when there is no loaded subscription`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(null))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.resume()
        delay(100)

        assertNull(billingApi.lastResumeId)
    }

    @Test
    fun `resume surfaces network error`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(
            getResult = ApiResult.Success(sampleSubscription(onGracePeriod = true)),
            resumeResult = ApiResult.NetworkError(RuntimeException("boom")),
        )
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.resume()
        delay(100)

        assertEquals(false, viewModel.state.value.isResuming)
        assertTrue(viewModel.state.value.error != null)
    }

    @Test
    fun `cancel calls cancelSubscription at period end by default then reloads`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(sampleSubscription(id = 9)))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.cancel()
        delay(100)

        assertEquals(9, billingApi.lastCancelId)
        assertEquals(true, billingApi.lastCancelAtPeriodEnd)
        assertEquals(false, viewModel.state.value.isCancelling)
        assertEquals(2, billingApi.getCallCount)
    }

    @Test
    fun `cancel does nothing when there is no loaded subscription`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(getResult = ApiResult.Success(null))
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.cancel()
        delay(100)

        assertNull(billingApi.lastCancelId)
    }

    @Test
    fun `cancel surfaces network error`() = runTest(testDispatcher) {
        val billingApi = FakeBillingApi(
            getResult = ApiResult.Success(sampleSubscription()),
            cancelResult = ApiResult.NetworkError(RuntimeException("boom")),
        )
        val viewModel = SubscriptionViewModel(billingApi, SubscriptionFakeDispatcherProvider(testDispatcher))
        delay(100)

        viewModel.cancel()
        delay(100)

        assertEquals(false, viewModel.state.value.isCancelling)
        assertTrue(viewModel.state.value.error != null)
    }
}
