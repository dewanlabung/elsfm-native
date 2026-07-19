package com.elsfm.mobile.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsState().value

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { viewModel.onEvent(DownloadsEvent.SearchQueryChanged(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text("Search downloads") },
            leadingIcon = { Icon(Icons.Default.Search, "Search") },
            singleLine = true,
        )

        TabRow(
            selectedTabIndex = when (state.activeTab) {
                DownloadTab.SONGS -> 0
                DownloadTab.ALBUMS -> 1
                DownloadTab.PLAYLISTS -> 2
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Tab(
                selected = state.activeTab == DownloadTab.SONGS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.SONGS)) },
                text = { Text("Songs") },
            )
            Tab(
                selected = state.activeTab == DownloadTab.ALBUMS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.ALBUMS)) },
                text = { Text("Albums") },
            )
            Tab(
                selected = state.activeTab == DownloadTab.PLAYLISTS,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.PLAYLISTS)) },
                text = { Text("Playlists") },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FilledTonalButton(
                onClick = { viewModel.onEvent(DownloadsEvent.PlayAll) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Text("Play All", modifier = Modifier.padding(start = 4.dp))
            }
            FilledTonalButton(
                onClick = { viewModel.onEvent(DownloadsEvent.ShuffleAll) },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Filled.Shuffle, contentDescription = null)
                Text("Shuffle", modifier = Modifier.padding(start = 4.dp))
            }
        }

        DownloadLibraryBanner(
            isDownloading = state.isDownloadingLibrary,
            statusText = state.libraryDownloadStatus,
            onDownload = { viewModel.onEvent(DownloadsEvent.DownloadLibrary) },
        )

        when (state.activeTab) {
            DownloadTab.SONGS -> {
                if (state.downloadedTracks.isEmpty()) {
                    EmptyDownloads()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.downloadedTracks) { track ->
                            TrackDownloadItem(
                                track = track,
                                onPlay = { viewModel.onEvent(DownloadsEvent.PlayTrack(it)) },
                                onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                                onShare = { viewModel.onEvent(DownloadsEvent.ShareDownload(it)) },
                            )
                        }
                    }
                }
            }
            DownloadTab.ALBUMS -> {
                if (state.downloadedAlbums.isEmpty()) {
                    EmptyDownloads()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.downloadedAlbums) { album ->
                            DownloadedGroupCard(
                                name = album.name,
                                artworkUrl = album.artworkUrl,
                                subtitle = album.artist,
                                onPlay = { viewModel.onEvent(DownloadsEvent.PlayAlbum(album.albumId)) },
                            )
                        }
                    }
                }
            }
            DownloadTab.PLAYLISTS -> {
                if (state.downloadedPlaylists.isEmpty()) {
                    EmptyDownloads()
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.downloadedPlaylists) { playlist ->
                            DownloadedGroupCard(
                                name = playlist.name,
                                artworkUrl = playlist.artworkUrl,
                                subtitle = "${playlist.trackCount} tracks downloaded",
                                onPlay = { viewModel.onEvent(DownloadsEvent.PlayPlaylist(playlist.playlistId)) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDownloads() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "No downloads yet", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DownloadedGroupCard(
    name: String,
    artworkUrl: String?,
    subtitle: String,
    onPlay: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable(onClick = onPlay),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp)
                    .size(40.dp)
                    .background(Color.White.copy(alpha = 0.9f), CircleShape),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black)
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
            maxLines = 1,
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(top = 2.dp, start = 4.dp, bottom = 4.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TrackDownloadItem(
    track: DownloadedTrackUI,
    onPlay: (Int) -> Unit = {},
    onDelete: (Int) -> Unit = {},
    onShare: (Int) -> Unit = {},
) {
    val showMenu = remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(track.trackId) }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = track.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(
                text = track.artist,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            Text(text = track.fileSize, style = MaterialTheme.typography.labelSmall)
        }

        Box {
            IconButton(onClick = { showMenu.value = true }) {
                Icon(Icons.Default.MoreVert, "Options")
            }
            DropdownMenu(
                expanded = showMenu.value,
                onDismissRequest = { showMenu.value = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Play") },
                    onClick = {
                        onPlay(track.trackId)
                        showMenu.value = false
                    },
                    leadingIcon = { Icon(Icons.Default.PlayArrow, "Play") },
                )
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = {
                        onShare(track.trackId)
                        showMenu.value = false
                    },
                    leadingIcon = { Icon(Icons.Default.Share, "Share") },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        onDelete(track.trackId)
                        showMenu.value = false
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, "Delete") },
                )
            }
        }
    }
}

@Composable
private fun DownloadLibraryBanner(
    isDownloading: Boolean,
    statusText: String?,
    onDownload: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        if (isDownloading) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Downloading library…", style = MaterialTheme.typography.labelMedium)
                }
                if (statusText != null) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Download all albums & playlists from your library",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                OutlinedButton(onClick = onDownload) {
                    Text("Sync")
                }
            }
        }
    }
}
