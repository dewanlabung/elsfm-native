package com.elsfm.mobile.feature.downloads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
fun DownloadsScreen(
    viewModel: DownloadViewModel = hiltViewModel(),
    onTrackClicked: (Track) -> Unit,
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.downloadedTracks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "No downloads yet",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        "Download tracks for offline playback",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.downloadedTracks) { downloaded ->
                    DownloadItem(
                        downloaded = downloaded,
                        progress = state.downloadProgress[downloaded.trackId] ?: 0f,
                        onDelete = { viewModel.deleteDownload(downloaded.trackId) },
                        onPlay = { onTrackClicked(Track(id = downloaded.trackId, name = "", image = null, durationMs = 0, src = "", artists = emptyList())) }
                    )
                }
            }
        }
    }
}
