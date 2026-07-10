package com.elsfm.mobile.feature.discovery

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.api.ChannelContentResult
import com.elsfm.mobile.feature.library.composables.AlbumCard
import com.elsfm.mobile.feature.library.composables.PlaylistCard
import com.elsfm.mobile.feature.library.composables.TrackListItem

/**
 * Detail screen for a single Channel 5 sub-channel (e.g. "Explore More
 * Channel"), reached by tapping into a channel from [DiscoveryScreen]. Its
 * content can be tracks, playlists, albums, or further nested channels
 * depending on the backend's `config.contentModel` for that channel; this
 * screen renders whichever variant [ChannelDetailViewModel] loads.
 */
@Composable
fun ChannelDetailScreen(
    onTrackClicked: (Track, List<Track>) -> Unit,
    onPlaylistClicked: (Playlist) -> Unit,
    onAlbumClicked: (Album) -> Unit,
    onChannelClicked: (channelId: Int) -> Unit,
    viewModel: ChannelDetailViewModel = hiltViewModel(),
    onTrackMoreClicked: (Track) -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = state.title ?: "Channel",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )

        when {
            state.isLoading -> ChannelDetailLoading()
            state.error != null -> ChannelDetailError(state.error)
            else -> ChannelDetailBody(
                content = state.content,
                onTrackClicked = onTrackClicked,
                onPlaylistClicked = onPlaylistClicked,
                onAlbumClicked = onAlbumClicked,
                onChannelClicked = onChannelClicked,
                onTrackMoreClicked = onTrackMoreClicked,
            )
        }
    }
}

@Composable
private fun ChannelDetailLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ChannelDetailError(error: String?, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = error ?: "Failed to load channel",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun ChannelDetailBody(
    content: ChannelContentResult?,
    onTrackClicked: (Track, List<Track>) -> Unit,
    onPlaylistClicked: (Playlist) -> Unit,
    onAlbumClicked: (Album) -> Unit,
    onChannelClicked: (channelId: Int) -> Unit,
    onTrackMoreClicked: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (content) {
        is ChannelContentResult.Tracks -> TrackListContent(
            tracks = content.items,
            onTrackClicked = onTrackClicked,
            onTrackMoreClicked = onTrackMoreClicked,
            modifier = modifier,
        )
        is ChannelContentResult.Playlists -> PlaylistGridContent(
            playlists = content.items,
            onPlaylistClicked = onPlaylistClicked,
            modifier = modifier,
        )
        is ChannelContentResult.Albums -> AlbumGridContent(
            albums = content.items,
            onAlbumClicked = onAlbumClicked,
            modifier = modifier,
        )
        is ChannelContentResult.Channels -> NestedChannelListContent(
            channels = content.items,
            onChannelClicked = onChannelClicked,
            modifier = modifier,
        )
        null -> Unit
    }
}

@Composable
private fun TrackListContent(
    tracks: List<Track>,
    onTrackClicked: (Track, List<Track>) -> Unit,
    onTrackMoreClicked: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(tracks, key = { it.id }) { track ->
            TrackListItem(
                track = track,
                onClick = { onTrackClicked(track, tracks) },
                onMoreClick = { onTrackMoreClicked(track) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

private val GRID_MIN_CELL_SIZE = 140.dp

@Composable
private fun PlaylistGridContent(
    playlists: List<Playlist>,
    onPlaylistClicked: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_MIN_CELL_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(playlists, key = { it.id }) { playlist ->
            PlaylistCard(playlist = playlist, onClick = { onPlaylistClicked(playlist) })
        }
    }
}

@Composable
private fun AlbumGridContent(
    albums: List<Album>,
    onAlbumClicked: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = GRID_MIN_CELL_SIZE),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            AlbumCard(album = album, onClick = { onAlbumClicked(album) })
        }
    }
}

@Composable
private fun NestedChannelListContent(
    channels: List<Channel>,
    onChannelClicked: (channelId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(channels, key = { it.id }) { channel ->
            Text(
                text = channel.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onChannelClicked(channel.id) }
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
        }
    }
}
