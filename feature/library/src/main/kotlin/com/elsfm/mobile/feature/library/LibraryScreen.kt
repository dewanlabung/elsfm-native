package com.elsfm.mobile.feature.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.feature.library.composables.AlbumCard
import com.elsfm.mobile.feature.library.composables.PlaylistCard

internal const val LIBRARY_GRID_TEST_TAG = "libraryGrid"
private const val GRID_COLUMN_COUNT = 2
private const val SHIMMER_CARD_COUNT = 6

@Composable
fun LibraryScreen(
    onPlaylistTap: (Playlist) -> Unit = {},
    onAlbumTap: (Album) -> Unit = {},
    onChannelTap: (Channel) -> Unit = {},
    libraryViewModel: LibraryViewModel = hiltViewModel(),
) {
    val state by libraryViewModel.state.collectAsState()

    LibraryContent(
        state = state,
        onFilterSelected = libraryViewModel::selectFilter,
        onPlaylistTap = onPlaylistTap,
        onAlbumTap = onAlbumTap,
        onChannelTap = { channel ->
            libraryViewModel.selectChannel(channel.id)
            onChannelTap(channel)
        },
        onRetry = libraryViewModel::loadLibrary,
    )
}

@Composable
internal fun LibraryContent(
    state: LibraryState,
    onFilterSelected: (LibraryFilter) -> Unit,
    onPlaylistTap: (Playlist) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onChannelTap: (Channel) -> Unit,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
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
                onChannelTap = onChannelTap,
            )
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
    LibraryFilter.CHANNELS -> "No channels yet"
}

@Composable
private fun LibraryGrid(
    state: LibraryState,
    onPlaylistTap: (Playlist) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onChannelTap: (Channel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val showPlaylists = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.PLAYLISTS
    val showAlbums = state.selectedFilter == LibraryFilter.ALL || state.selectedFilter == LibraryFilter.ALBUMS
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
