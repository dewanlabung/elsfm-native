package com.elsfm.mobile.core.network.api

import com.elsfm.mobile.core.network.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

/**
 * Real shape of `GET api/v1/billing/user` (`Common\Billing\BillingUserController::index`):
 * the signed-in user's own current subscription + card + invoices, no admin permission
 * required. This is the correct read endpoint for "what's my subscription status" -
 * `GET billing/subscriptions` is an admin list-all-subscriptions endpoint gated behind
 * `subscriptions.update` (`Common\Core\Policies\SubscriptionPolicy::index`) and 403s for a
 * normal user's token.
 *
 * Only the fields this screen actually renders are decoded here (status flags, renewal/end
 * date, product/price name) rather than the full raw model.
 */
@Serializable
private data class BillingUserResponse(
    val subscription: SubscriptionDto? = null,
)

@Serializable
private data class SubscriptionDto(
    val id: Int,
    @SerialName("product_id") val productId: Int? = null,
    @SerialName("price_id") val priceId: Int? = null,
    @SerialName("renews_at") val renewsAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
    @SerialName("trial_ends_at") val trialEndsAt: String? = null,
    @SerialName("on_grace_period") val onGracePeriod: Boolean = false,
    @SerialName("on_trial") val onTrial: Boolean = false,
    val valid: Boolean = false,
    val active: Boolean = false,
    val cancelled: Boolean = false,
    @SerialName("gateway_name") val gatewayName: String? = null,
    val product: BillingProductDto? = null,
    val price: BillingPriceDto? = null,
)

@Serializable
private data class BillingProductDto(val name: String? = null)

@Serializable
private data class BillingPriceDto(val name: String? = null)

/** Public, UI-facing shape - the subset of [SubscriptionDto] the screen needs. */
data class BillingSubscription(
    val id: Int,
    val productName: String?,
    val priceName: String?,
    val renewsAt: String?,
    val endsAt: String?,
    val trialEndsAt: String?,
    val onGracePeriod: Boolean,
    val onTrial: Boolean,
    val valid: Boolean,
    val active: Boolean,
    val cancelled: Boolean,
    val gatewayName: String?,
)

private fun SubscriptionDto.toBillingSubscription() = BillingSubscription(
    id = id,
    productName = product?.name,
    priceName = price?.name,
    renewsAt = renewsAt,
    endsAt = endsAt,
    trialEndsAt = trialEndsAt,
    onGracePeriod = onGracePeriod,
    onTrial = onTrial,
    valid = valid,
    active = active,
    cancelled = cancelled,
    gatewayName = gatewayName,
)

@Serializable
private data class CancelSubscriptionRequest(val delete: Boolean = false)

/**
 * Read/lifecycle actions for the signed-in user's own subscription. Native payment
 * collection (creating/changing a subscription via Stripe or PayPal SDKs) is
 * deliberately NOT part of this API surface - see the subscriptions screen plan for why
 * that's a separate follow-up.
 */
open class BillingApi @Inject constructor(
    private val httpClient: HttpClient,
) {
    /**
     * Returns `null` (not an error) both when the backend has no subscription for this
     * user (a "free tier" user is an expected state, not a failure) and when the
     * response simply omits the `subscription` key.
     */
    open suspend fun getCurrentSubscription(): ApiResult<BillingSubscription?> {
        return try {
            val response = httpClient.get("api/v1/billing/user")
            if (response.status.isSuccess()) {
                ApiResult.Success(response.body<BillingUserResponse>().subscription?.toBillingSubscription())
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /**
     * `POST billing/subscriptions/{id}/cancel`. `atPeriodEnd = true` (the default) keeps
     * access until the current billing period ends (`delete: false`); the underlying
     * policy's `show()` allows the subscription's own owner, so this is safe for a
     * normal user token.
     */
    open suspend fun cancelSubscription(id: Int, atPeriodEnd: Boolean = true): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/billing/subscriptions/$id/cancel") {
                contentType(ContentType.Application.Json)
                setBody(CancelSubscriptionRequest(delete = !atPeriodEnd))
            }
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }

    /** `POST billing/subscriptions/{id}/resume` - resumes a subscription on its grace period. */
    open suspend fun resumeSubscription(id: Int): ApiResult<Unit> {
        return try {
            val response = httpClient.post("api/v1/billing/subscriptions/$id/resume")
            if (response.status.isSuccess()) {
                ApiResult.Success(Unit)
            } else if (response.status == HttpStatusCode.Unauthorized || response.status == HttpStatusCode.Forbidden) {
                ApiResult.Unauthorized
            } else {
                ApiResult.NetworkError(RuntimeException("Unexpected status: ${response.status}"))
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
    }
}
