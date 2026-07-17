package com.elsfm.mobile.feature.subscriptions

import com.elsfm.mobile.core.network.api.BillingSubscription

/**
 * State for [SubscriptionViewModel]. `subscription == null` after a successful load is
 * the expected "free tier" state, not an error - see [SubscriptionViewModel] KDoc.
 */
data class SubscriptionState(
    val isLoading: Boolean = false,
    val subscription: BillingSubscription? = null,
    val error: String? = null,
    val isCancelling: Boolean = false,
    val isResuming: Boolean = false,
)
