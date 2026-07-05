package com.elsfm.mobile.feature.discovery

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
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

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel = hiltViewModel(),
    onTrackClicked: (Track, List<Track>) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    when {
        state.isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        state.featuredChannels.isEmpty() && state.popularTracks.isEmpty() && state.error != null -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Failed to load home",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.error.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        else -> {
            LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (state.featuredChannels.isNotEmpty()) {
                    item {
                        Text(
                            "Featured Playlists",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        FeaturedPlaylistsSection(
                            channels = state.featuredChannels,
                            onChannelClicked = { },
                        )
                        Spacer(Modifier.height(24.dp))
                    }
                }
                if (state.popularTracks.isNotEmpty()) {
                    item {
                        Text(
                            "Popular Songs",
                            style = MaterialTheme.typography.headlineSmall,
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    item {
                        PopularSongsSection(
                            tracks = state.popularTracks,
                            onTrackClicked = { track ->
                                onTrackClicked(track, state.popularTracks)
                            },
                        )
                    }
                }
                if (state.error != null) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.error.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
