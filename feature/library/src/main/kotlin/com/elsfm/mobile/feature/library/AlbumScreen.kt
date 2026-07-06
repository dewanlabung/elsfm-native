package com.elsfm.mobile.feature.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.BlurredBackground
import com.elsfm.mobile.feature.library.composables.TrackListItem

internal const val ALBUM_TRACK_LIST_TEST_TAG = "albumTrackList"

@Composable
fun AlbumScreen(
    album: Album,
    onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
    onAddToQueue: (Track) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(album.id) {
        viewModel.loadAlbum(album)
    }

    AlbumDetailContent(
        state = state,
        onPlayAll = viewModel::playAll,
        onTrackTap = onTrackTap,
        onAddToQueue = onAddToQueue,
        onToggleTrackLike = viewModel::toggleTrackLike,
    )
}

@Composable
internal fun AlbumDetailContent(
    state: AlbumDetailState,
    onPlayAll: () -> Unit,
    onTrackTap: (Track, List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onToggleTrackLike: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val album = state.album

    BlurredBackground(imageUrl = album?.image, modifier = modifier) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            album == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.error ?: "Album not found",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        AlbumHeader(
                            album = album,
                            trackCount = state.tracks.size,
                            onPlayAll = onPlayAll,
                        )
                    }
                    items(state.tracks, key = { it.id }) { track ->
                        AlbumTrackRow(
                            track = track,
                            onClick = { onTrackTap(track, state.tracks) },
                            onAddToQueue = { onAddToQueue(track) },
                            isLiked = state.likedTrackIds.contains(track.id),
                            isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                            onToggleLike = { onToggleTrackLike(track.id) },
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
private fun AlbumHeader(
    album: Album,
    trackCount: Int,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = album.name,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = album.releaseDate.orEmpty(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = "$trackCount tracks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = onPlayAll) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
            Text(text = "Play All", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun AlbumTrackRow(
    track: Track,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    isLiked: Boolean = false,
    isLikeLoading: Boolean = false,
    onToggleLike: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(ALBUM_TRACK_LIST_TEST_TAG),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TrackListItem(
            track = track,
            onClick = onClick,
            onMoreClick = { menuExpanded = true },
            modifier = Modifier.weight(1f),
            isLiked = isLiked,
            isLikeLoading = isLikeLoading,
            onLikeClick = onToggleLike,
        )

        Box {
            DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                DropdownMenuItem(
                    text = { Text("Play") },
                    onClick = {
                        menuExpanded = false
                        onClick()
                    },
                )
                DropdownMenuItem(
                    text = { Text("Add to queue") },
                    onClick = {
                        menuExpanded = false
                        onAddToQueue()
                    },
                )
            }
        }
    }
}
