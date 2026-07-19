package com.elsfm.mobile.feature.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

        // In-flight downloads progress section
        val inFlightDownloads = state.downloadProgress.filter { (trackId, _) ->
            !state.downloadedTracks.any { it.trackId == trackId }
        }
        if (inFlightDownloads.isNotEmpty()) {
            var isExpanded by remember { mutableStateOf(true) }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isExpanded = !isExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Downloading (${inFlightDownloads.size})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isExpanded) {
                    // Capped height with internal scrolling - without this, downloading more
                    // than ~3 tracks at once pushes the tabs and downloaded list off screen.
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 168.dp)
                            .padding(top = 8.dp),
                    ) {
                        items(inFlightDownloads.entries.toList(), key = { it.key }) { (trackId, progress) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = state.downloadTrackNames[trackId] ?: "Track #$trackId",
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        }

        // Tabs
        TabRow(
            selectedTabIndex = when (state.activeTab) {
                DownloadTab.SONGS -> 0
                DownloadTab.ALBUMS -> 1
                DownloadTab.PLAYLISTS -> 2
                DownloadTab.FOLDER -> 3
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
            Tab(
                selected = state.activeTab == DownloadTab.FOLDER,
                onClick = { viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.FOLDER)) },
                text = { Text("Folder") }
            )
        }

        // Tab content — Albums uses a 2-column grid; all other tabs use a list.
        when (state.activeTab) {
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
            else -> {
                val isEmpty = when (state.activeTab) {
                    DownloadTab.SONGS -> state.downloadedTracks.isEmpty()
                    DownloadTab.PLAYLISTS -> state.downloadedPlaylists.isEmpty()
                    DownloadTab.FOLDER -> state.downloadedFiles.isEmpty()
                    else -> false
                }
                if (isEmpty) {
                    EmptyDownloads()
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        when (state.activeTab) {
                            DownloadTab.SONGS -> items(state.downloadedTracks) { track ->
                                TrackDownloadItem(
                                    track = track,
                                    onPlay = { viewModel.onEvent(DownloadsEvent.PlayTrack(it)) },
                                    onDelete = { viewModel.onEvent(DownloadsEvent.DeleteDownload(it)) },
                                    onShare = { viewModel.onEvent(DownloadsEvent.ShareDownload(it)) },
                                )
                            }
                            DownloadTab.PLAYLISTS -> items(state.downloadedPlaylists) { playlist ->
                                DownloadedGroupCard(
                                    name = playlist.name,
                                    artworkUrl = playlist.artworkUrl,
                                    subtitle = "${playlist.trackCount} tracks downloaded",
                                    onPlay = { viewModel.onEvent(DownloadsEvent.PlayPlaylist(playlist.playlistId)) },
                                )
                            }
                            DownloadTab.FOLDER -> items(state.downloadedFiles) { file ->
                                DownloadedFileRow(
                                    file = file,
                                    onPlay = { viewModel.onEvent(DownloadsEvent.PlayTrack(it)) },
                                )
                            }
                            else -> {}
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

/** A single downloaded album/playlist, shown as one clickable, playable card (not a scattered song list). */
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
            .padding(16.dp)
            .clickable(onClick = onPlay),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
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
                    .padding(12.dp)
                    .size(48.dp)
                    .background(Color.White, CircleShape),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black)
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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

/** A raw file on disk in the app's downloads folder - the Folder tab's file-browser view. */
@Composable
fun DownloadedFileRow(
    file: DownloadedFileUI,
    onPlay: (Int) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onPlay(file.trackId) }
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.PlayArrow, contentDescription = "Play")
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.fileName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
            Text(
                text = file.fileSize,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
