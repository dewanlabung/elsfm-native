package com.elsfm.mobile.feature.artist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
fun ArtistDetailScreen(
    artistId: Int,
    onTrackClicked: (track: Track, queue: List<Track>) -> Unit,
    viewModel: ArtistDetailViewModel = hiltViewModel(key = artistId.toString()),
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        state.artist?.let { artist ->
            Text(
                text = artist.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(16.dp),
            )
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(state.tracks, key = { it.id }) { track ->
                Text(
                    text = track.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrackClicked(track, state.tracks) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                )
                HorizontalDivider()
            }
        }
    }
}
