package com.elsfm.mobile.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsState().value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onEvent(DownloadsEvent.SearchQueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            placeholder = { Text("Search downloads") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true
        )

        // Tabs
        TabRow(
            selectedTabIndex = when (state.activeTab) {
                DownloadTab.SONGS -> 0
                DownloadTab.ALBUMS -> 1
                DownloadTab.PLAYLISTS -> 2
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = state.activeTab == DownloadTab.SONGS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.SONGS)) },
                text = { Text("Songs") }
            )
            Tab(
                selected = state.activeTab == DownloadTab.ALBUMS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.ALBUMS)) },
                text = { Text("Albums") }
            )
            Tab(
                selected = state.activeTab == DownloadTab.PLAYLISTS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.PLAYLISTS)) },
                text = { Text("Playlists") }
            )
        }

        // Tracks list
        if (state.downloadedTracks.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No downloads yet",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.downloadedTracks) { track ->
                    TrackDownloadItem(
                        track = track,
                        onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                        onShare = { viewModel.onEvent(DownloadsEvent.ShareDownload(it)) }
                    )
                }
            }
        }
    }
}

@Composable
fun TrackDownloadItem(
    track: DownloadedTrackUI,
    onDelete: (Int) -> Unit = {},
    onShare: (Int) -> Unit = {},
) {
    val showMenu = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .background(MaterialTheme.colorScheme.surface),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Artwork placeholder
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(4.dp)
                )
        )

        // Track info
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = track.fileSize,
                    style = MaterialTheme.typography.labelSmall
                )
                if (track.isOffline) {
                    Text(
                        text = "Offline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                shape = MaterialTheme.shapes.small
                            )
                            .padding(4.dp)
                    )
                }
            }
        }

        // Menu
        IconButton(onClick = { showMenu.value = !showMenu.value }) {
            Icon(Icons.Default.MoreVert, "Options")
        }

        DropdownMenu(
            expanded = showMenu.value,
            onDismissRequest = { showMenu.value = false }
        ) {
            DropdownMenuItem(
                text = { Text("Share") },
                onClick = {
                    onShare(track.trackId)
                    showMenu.value = false
                },
                leadingIcon = { Icon(Icons.Default.Share, "Share") }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete(track.trackId)
                    showMenu.value = false
                },
                leadingIcon = { Icon(Icons.Default.Delete, "Delete") }
            )
        }
    }
}
