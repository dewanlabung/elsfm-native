package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.UserSessionInfo

/**
 * Read-only view of the real PWA "Sessions" account setting: lists active login
 * sessions/devices via `GET api/v1/user-sessions`.
 *
 * There is intentionally no per-row "Revoke" action here (unlike the web mockup this
 * screen was scoped from): the backend exposes no per-session revoke endpoint, only a
 * single bulk "logout other sessions" action, and that bulk action itself requires
 * Fortify's session-cookie-based password confirmation - which this app's stateless
 * Bearer-token client has no way to satisfy. See the account-notifications report.
 */
@Composable
fun SessionsPanel(
    sessions: List<UserSessionInfo>,
    isLoading: Boolean,
    error: String?,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Sessions", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Devices currently signed in to your account.",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> CircularProgressIndicator(modifier = Modifier.padding(8.dp))
            error != null -> Text(error, color = MaterialTheme.colorScheme.error)
            sessions.isEmpty() -> Text(
                "No active sessions found.",
                style = MaterialTheme.typography.bodySmall,
            )
            else -> sessions.forEach { session ->
                SessionRow(session)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}

@Composable
private fun SessionRow(session: UserSessionInfo) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row {
            Text(
                listOfNotNull(session.platform, session.browser).joinToString(" - ").ifBlank { "Unknown device" },
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (session.city != null || session.country != null) {
            Text(
                listOfNotNull(session.city, session.country?.uppercase()).joinToString(", "),
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Text(
            if (session.isCurrentDevice) "This device" else "Last used ${session.updatedAt.orEmpty()}",
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
