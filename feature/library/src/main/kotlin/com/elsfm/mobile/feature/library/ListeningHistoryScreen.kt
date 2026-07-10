package com.elsfm.mobile.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.TrackListItem

internal const val LISTENING_HISTORY_TRACK_LIST_TEST_TAG = "listeningHistoryTrackList"

@Composable
fun ListeningHistoryScreen(
    onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
    viewModel: ListeningHistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ListeningHistoryContent(
        state = state,
        onPlayAll = viewModel::playAll,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onTrackTap = onTrackTap,
        onToggleTrackLike = viewModel::toggleTrackLike,
    )
}

@Composable
internal fun ListeningHistoryContent(
    state: ListeningHistoryState,
    onPlayAll: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTrackTap: (Track, List<Track>) -> Unit,
    onToggleTrackLike: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            state.error != null && state.tracks.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = state.error, color = MaterialTheme.colorScheme.error)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        ListeningHistoryHeader(
                            trackCount = state.tracks.size,
                            searchQuery = state.searchQuery,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onPlayAll = onPlayAll,
                        )
                    }
                    items(state.filteredTracks, key = { it.id }) { track ->
                        TrackListItem(
                            track = track,
                            onClick = { onTrackTap(track, state.tracks) },
                            isLiked = state.likedTrackIds.contains(track.id),
                            isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                            onLikeClick = { onToggleTrackLike(track.id) },
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                    if (state.error != null) {
                        item {
                            Text(
                                text = state.error,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ListeningHistoryHeader(
    trackCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "Listening history",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "$trackCount ${if (trackCount == 1) "track" else "tracks"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Button(
            onClick = onPlayAll,
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Text(text = "Play", modifier = Modifier.padding(start = 8.dp))
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            placeholder = { Text("Search within history") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
        )
    }
}
