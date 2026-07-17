package com.elsfm.mobile.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.feature.library.composables.AlbumCard
import com.elsfm.mobile.feature.library.composables.ArtistCard
import com.elsfm.mobile.feature.library.composables.PlaylistCard

internal const val LIBRARY_GRID_TEST_TAG = "libraryGrid"
private const val GRID_COLUMN_COUNT = 2
private const val SHIMMER_CARD_COUNT = 6

@Composable
fun LibraryScreen(
    onPlaylistTap: (Playlist) -> Unit = {},
    onAlbumTap: (Album) -> Unit = {},
    onArtistTap: (Artist) -> Unit = {},
    onChannelTap: (Channel) -> Unit = {},
    onSongsClicked: () -> Unit = {},
    onPlayHistoryClicked: () -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by libraryViewModel.state.collectAsState()
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.playlistCreated) {
        if (state.playlistCreated) {
            showCreatePlaylistDialog = false
            libraryViewModel.consumePlaylistCreatedEvent()
        }
    }

    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            isLoading = state.isCreatingPlaylist,
            error = state.createPlaylistError,
            onDismiss = { showCreatePlaylistDialog = false },
            onCreate = libraryViewModel::createPlaylist,
        )
    }

    LibraryContent(
        state = state,
        onFilterSelected = libraryViewModel::selectFilter,
        onPlaylistTap = onPlaylistTap,
        onAlbumTap = onAlbumTap,
        onArtistTap = onArtistTap,
        onChannelTap = onChannelTap,
        onSongsClick = onSongsClicked,
        onPlayHistoryClick = onPlayHistoryClicked,
        onCreatePlaylistClick = { showCreatePlaylistDialog = true },
        onRetry = libraryViewModel::loadLibrary,
    )
}

@Composable
private fun CreatePlaylistDialog(
    isLoading: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    isError = error != null,
                    enabled = !isLoading,
                )
                if (error != null) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(name) },
                enabled = !isLoading && name.isNotBlank(),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp))
                } else {
                    Text("Create")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isLoading) {
                Text("Cancel")
            }
        },
    )
}

@Composable
internal fun LibraryContent(
    state: LibraryState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onPlaylistTap: (Playlist) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onArtistTap: (Artist) -> Unit,
    onChannelTap: (Channel) -> Unit,
    onSongsClick: () -> Unit = {},
    onPlayHistoryClick: () -> Unit = {},
    onCreatePlaylistClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        LibraryHeader(onCreatePlaylistClick = onCreatePlaylistClick)

        LibraryQuickLinks(
            onSongsClick = onSongsClick,
            onPlayHistoryClick = onPlayHistoryClick,
        )

        LibraryFilterTabs(
            selectedFilter = state.selectedFilter,
            onFilterSelected = onFilterSelected,
        )

        if (state.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (state.error != null) {
            LibraryErrorBanner(
                error = state.error,
                onRetry = onRetry
            )
        }

        when {
            state.isLoading -> LibraryLoading()
            state.error != null -> {
                Box(modifier = Modifier.fillMaxSize())
            }
            state.isEmpty -> LibraryEmpty(filter = state.selectedFilter)
            else -> LibraryGrid(
                state = state,
                onPlaylistTap = onPlaylistTap,
                onAlbumTap = onAlbumTap,
                onArtistTap = onArtistTap,
                onChannelTap = onChannelTap,
            )
        }
    }
}

@Composable
private fun LibraryHeader(
    onCreatePlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = "Your library", style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = onCreatePlaylistClick) {
            Icon(Icons.Filled.Add, contentDescription = "Create playlist")
        }
    }
}

@Composable
private fun LibraryQuickLinks(
    onSongsClick: () -> Unit,
    onPlayHistoryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LibraryQuickLinkCard(
            icon = Icons.Filled.MusicNote,
            label = "Songs",
            onClick = onSongsClick,
            modifier = Modifier.weight(1f),
        )
        LibraryQuickLinkCard(
            icon = Icons.Filled.History,
            label = "Play history",
            onClick = onPlayHistoryClick,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun LibraryQuickLinkCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun LibraryFilterTabs(
    selectedFilter: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    val filters = LibraryFilter.entries
    ScrollableTabRow(
        selectedTabIndex = filters.indexOf(selectedFilter),
        modifier = modifier.fillMaxWidth(),
        edgePadding = 16.dp,
    ) {
        filters.forEach { filter ->
            Tab(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                text = { Text(text = filter.label()) },
            )
        }
    }
}

private fun LibraryFilter.label(): String = when (this) {
    LibraryFilter.ALL -> "All"
    LibraryFilter.PLAYLISTS -> "Playlists"
    LibraryFilter.ALBUMS -> "Albums"
    LibraryFilter.ARTISTS -> "Artists"
    LibraryFilter.CHANNELS -> "Channels"
}

@Composable
private fun LibraryLoading(modifier: Modifier = Modifier) {
    // Shimmer placeholder grid: static surfaces standing in for loading cards.
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(count = SHIMMER_CARD_COUNT) {
            LibraryShimmerCard()
        }
    }
}

@Composable
private fun LibraryShimmerCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
    ) {}
}

@Composable
private fun LibraryEmpty(filter: LibraryFilter, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = emptyMessage(filter),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun emptyMessage(filter: LibraryFilter): String = when (filter) {
    LibraryFilter.ALL -> "Your library is empty"
    LibraryFilter.PLAYLISTS -> "No playlists yet"
    LibraryFilter.ALBUMS -> "No albums yet"
    LibraryFilter.ARTISTS -> "No artists yet"
    LibraryFilter.CHANNELS -> "No channels yet"
}

@Composable
private fun LibraryGrid(
    state: LibraryState,
    onPlaylistTap: (Playlist) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onArtistTap: (Artist) -> Unit,
    onChannelTap: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showPlaylists = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.PLAYLISTS
    val showAlbums = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.ALBUMS
    val showArtists = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.ARTISTS
    val showChannels = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.CHANNELS

    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        modifier = modifier
            .fillMaxSize()
            .testTag(LIBRARY_GRID_TEST_TAG),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (showPlaylists) {
            items(state.playlists, key = { "playlist_${it.id}" }) { playlist ->
                PlaylistCard(playlist = playlist, onClick = { onPlaylistTap(playlist) })
            }
        }
        if (showAlbums) {
            items(state.albums, key = { "album_${it.id}" }) { album ->
                AlbumCard(album = album, onClick = { onAlbumTap(album) })
            }
        }
        if (showArtists) {
            items(state.artists, key = { "artist_${it.id}" }) { artist ->
                ArtistCard(artist = artist, onClick = { onArtistTap(artist) })
            }
        }
        if (showChannels) {
            items(state.channels, key = { "channel_${it.id}" }) { channel ->
                ChannelCard(channel = channel, onClick = { onChannelTap(channel) })
            }
        }
    }
}

@Composable
private fun ChannelCard(
    channel: Channel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.CenterStart) {
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun LibraryErrorBanner(
    error: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Button(
                onClick = onRetry,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Retry")
            }
        }
    }
}
