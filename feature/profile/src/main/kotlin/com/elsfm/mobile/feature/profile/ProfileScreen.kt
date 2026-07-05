package com.elsfm.mobile.feature.profile

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.model.UserProfile

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onTrackClicked: (Track) -> Unit,
    onLogout: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                ErrorScreen(message = state.error ?: "Failed to load profile")
            }

            state.userProfile != null -> {
                ProfileHeader(
                    profile = state.userProfile!!,
                    onLogout = onLogout
                )
                HorizontalDivider()
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    item {
                        Text(
                            "Recently Played",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    items(state.recentlyPlayed) { track ->
                        RecentlyPlayedTrackItem(
                            track = track,
                            onTrackClicked = { onTrackClicked(track) }
                        )
                    }
                }
            }

            else -> {
                ErrorScreen(message = "No profile data available")
            }
        }
    }
}

@Composable
fun ErrorScreen(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Unable to Load Profile",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                message,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
