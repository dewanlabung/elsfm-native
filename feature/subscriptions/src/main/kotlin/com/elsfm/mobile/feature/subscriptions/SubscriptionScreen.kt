package com.elsfm.mobile.feature.subscriptions

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.network.api.BillingSubscription

private const val BILLING_MANAGEMENT_URL = "https://www.elsfm.com/billing"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onBack: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.error != null -> {
                    SubscriptionErrorContent(message = state.error ?: "Failed to load subscription")
                }

                state.subscription != null -> {
                    SubscriptionContent(
                        subscription = state.subscription!!,
                        isResuming = state.isResuming,
                        onResumeClicked = { viewModel.resume() },
                        onManageClicked = { openBillingPage(context) },
                    )
                }

                else -> {
                    FreeTierContent(onManageClicked = { openBillingPage(context) })
                }
            }
        }
    }
}

private fun openBillingPage(context: Context) {
    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BILLING_MANAGEMENT_URL)))
}

@Composable
private fun SubscriptionContent(
    subscription: BillingSubscription,
    isResuming: Boolean,
    onResumeClicked: () -> Unit,
    onManageClicked: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = subscription.productName ?: "Subscription",
            style = MaterialTheme.typography.headlineSmall,
        )
        subscription.priceName?.let {
            Text(text = it, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer()

        Text(
            text = statusLabel(subscription),
            style = MaterialTheme.typography.bodyMedium,
        )
        dateLabel(subscription)?.let { label ->
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }

        Spacer()

        if (subscription.onGracePeriod) {
            Button(
                onClick = onResumeClicked,
                enabled = !isResuming,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isResuming) "Resuming..." else "Resume subscription")
            }
            Spacer()
        }

        OutlinedButton(
            onClick = onManageClicked,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Manage subscription")
        }
    }
}

@Composable
private fun FreeTierContent(onManageClicked: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "You're on the free plan",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer()
        Button(onClick = onManageClicked) {
            Text("View plans")
        }
    }
}

@Composable
private fun SubscriptionErrorContent(message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Unable to load subscription",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(text = message, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Spacer() {
    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
}

private fun statusLabel(subscription: BillingSubscription): String = when {
    subscription.onGracePeriod -> "Cancelled - active until period end"
    subscription.cancelled -> "Cancelled"
    subscription.onTrial -> "On trial"
    subscription.active -> "Active"
    subscription.valid -> "Valid"
    else -> "Inactive"
}

private fun dateLabel(subscription: BillingSubscription): String? = when {
    subscription.onGracePeriod && subscription.endsAt != null -> "Ends ${subscription.endsAt}"
    subscription.onTrial && subscription.trialEndsAt != null -> "Trial ends ${subscription.trialEndsAt}"
    subscription.renewsAt != null -> "Renews ${subscription.renewsAt}"
    else -> null
}
