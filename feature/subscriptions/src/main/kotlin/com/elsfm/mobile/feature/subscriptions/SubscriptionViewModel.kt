package com.elsfm.mobile.feature.subscriptions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.network.ApiResult
import com.elsfm.mobile.core.network.api.BillingApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Backs the read-only subscription-status screen (`GET api/v1/billing/user` via
 * [BillingApi.getCurrentSubscription]) plus [resume], the one native lifecycle action
 * safe to expose without a payment SDK.
 *
 * [cancel] is implemented and tested here (the endpoint is safe to call for a normal
 * user - the subscription policy's `show()` allows the subscription's own owner) but is
 * deliberately NOT wired to a button in [SubscriptionScreen]: cancelling is better done
 * on the full web billing page where the user sees invoices/proration context, and
 * exposing it here would be a half-built confirmation flow. See the subscriptions
 * screen plan for the full reasoning.
 *
 * Full Stripe/PayPal SDK integration (creating or changing a subscription from this
 * app) is out of scope entirely - that's a separate follow-up task.
 */
@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val billingApi: BillingApi,
    private val dispatcherProvider: DispatcherProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionState())
    val state: StateFlow<SubscriptionState> = _state.asStateFlow()

    init {
        loadSubscription()
    }

    fun loadSubscription() {
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isLoading = true, error = null) }
            when (val result = billingApi.getCurrentSubscription()) {
                is ApiResult.Success -> {
                    _state.update { it.copy(subscription = result.data, isLoading = false) }
                }
                is ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(isLoading = false, error = result.cause.message ?: "Network error")
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isLoading = false, error = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isLoading = false, error = "Unauthorized") }
                }
            }
        }
    }

    fun cancel(atPeriodEnd: Boolean = true) {
        val subscriptionId = _state.value.subscription?.id ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isCancelling = true, error = null) }
            when (val result = billingApi.cancelSubscription(subscriptionId, atPeriodEnd)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isCancelling = false) }
                    loadSubscription()
                }
                is ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(isCancelling = false, error = result.cause.message ?: "Network error")
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isCancelling = false, error = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isCancelling = false, error = "Unauthorized") }
                }
            }
        }
    }

    fun resume() {
        val subscriptionId = _state.value.subscription?.id ?: return
        viewModelScope.launch(dispatcherProvider.io) {
            _state.update { it.copy(isResuming = true, error = null) }
            when (val result = billingApi.resumeSubscription(subscriptionId)) {
                is ApiResult.Success -> {
                    _state.update { it.copy(isResuming = false) }
                    loadSubscription()
                }
                is ApiResult.NetworkError -> {
                    _state.update {
                        it.copy(isResuming = false, error = result.cause.message ?: "Network error")
                    }
                }
                is ApiResult.ValidationError -> {
                    _state.update { it.copy(isResuming = false, error = "Validation error") }
                }
                is ApiResult.Unauthorized -> {
                    _state.update { it.copy(isResuming = false, error = "Unauthorized") }
                }
            }
        }
    }
}
