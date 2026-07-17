package com.elsfm.mobile.feature.library

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.designsystem.TrackContextMenu
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.TrackListItem

internal const val LIKED_SONGS_TRACK_LIST_TEST_TAG = "likedSongsTrackList"

@Composable
fun LikedSongsScreen(
    onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    viewModel: LikedSongsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LikedSongsContent(
        state = state,
        onPlayAll = viewModel::playAll,
        onSearchQueryChanged = viewModel::onSearchQueryChanged,
        onTrackTap = onTrackTap,
        onToggleTrackLike = viewModel::toggleTrackLike,
        onAddToQueue = viewModel::addToQueue,
        onDownloadTrack = viewModel::downloadTrack,
        onRepostTrack = viewModel::repostTrack,
        onShowPlaylistPicker = viewModel::showPlaylistPicker,
        onHidePlaylistPicker = viewModel::hidePlaylistPicker,
        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
        onGoToArtist = onGoToArtist,
        onGoToAlbum = onGoToAlbum,
        onGoToTrack = onGoToTrack,
        onViewLyrics = onViewLyrics,
        onViewComments = onViewComments,
        onShare = { track ->
            val url = "https://www.elsfm.com/track/${track.id}/${slugifyTrackName(track.name)}"
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, url)
                putExtra(Intent.EXTRA_SUBJECT, track.name)
            }
            context.startActivity(Intent.createChooser(intent, "Share track"))
        },
    )
}

@Composable
internal fun LikedSongsContent(
    state: LikedSongsState,
    onPlayAll: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onTrackTap: (Track, List<Track>) -> Unit,
    onToggleTrackLike: (Int) -> Unit,
    onAddToQueue: (Int) -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {},
    onRepostTrack: (Int) -> Unit = {},
    onShowPlaylistPicker: (Int) -> Unit = {},
    onHidePlaylistPicker: () -> Unit = {},
    onAddTrackToPlaylist: (Int) -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    onShare: (Track) -> Unit = {},
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
                        LikedSongsHeader(
                            trackCount = state.tracks.size,
                            searchQuery = state.searchQuery,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onPlayAll = onPlayAll,
                        )
                    }
                    items(state.filteredTracks, key = { it.id }) { track ->
                        LikedSongTrackRow(
                            track = track,
                            isLiked = true,
                            isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                            isDownloading = state.downloadingTrackIds.contains(track.id),
                            isDownloaded = state.downloadedTrackIds.contains(track.id),
                            onClick = { onTrackTap(track, state.tracks) },
                            onToggleLike = { onToggleTrackLike(track.id) },
                            onAddToQueue = { onAddToQueue(track.id) },
                            onDownload = { onDownloadTrack(track) },
                            onRepost = { onRepostTrack(track.id) },
                            onAddToPlaylist = { onShowPlaylistPicker(track.id) },
                            onGoToArtist = onGoToArtist,
                            onGoToAlbum = onGoToAlbum,
                            onGoToTrack = onGoToTrack,
                            onViewLyrics = onViewLyrics,
                            onViewComments = onViewComments,
                            onShare = { onShare(track) },
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

    if (state.isPlaylistPickerVisible) {
        AddToPlaylistSheet(
            playlists = state.userPlaylists,
            isLoading = state.isLoadingPlaylists,
            isAdding = state.addToPlaylistLoading,
            onPlaylistTap = onAddTrackToPlaylist,
            onDismiss = onHidePlaylistPicker,
        )
    }
}

@Composable
private fun LikedSongTrackRow(
    track: Track,
    isLiked: Boolean,
    isLikeLoading: Boolean,
    isDownloading: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onAddToQueue: () -> Unit,
    onDownload: () -> Unit,
    onRepost: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToArtist: (Int) -> Unit,
    onGoToAlbum: (Int) -> Unit,
    onGoToTrack: (Int) -> Unit,
    onViewLyrics: (Int) -> Unit,
    onViewComments: (Int) -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
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
            TrackContextMenu(
                trackId = track.id,
                artistId = track.artists.firstOrNull()?.id,
                albumId = track.album?.id,
                isVisible = menuExpanded,
                onDismiss = { menuExpanded = false },
                onAddToQueue = { onAddToQueue(); },
                onAddToLibrary = { onToggleLike() },
                onAddToPlaylist = { onAddToPlaylist() },
                onShare = onShare,
                onRepost = { onRepost() },
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum,
                onGoToTrack = onGoToTrack,
                onViewLyrics = onViewLyrics,
                onViewComments = onViewComments,
                onMakeAvailableOffline = { if (!isDownloading && !isDownloaded) onDownload() },
            )
        }
    }
}

@Composable
private fun LikedSongsHeader(
    trackCount: Int,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onPlayAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "$trackCount liked ${if (trackCount == 1) "song" else "songs"}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
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
            placeholder = { Text("Search within tracks") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistSheet(
    playlists: List<com.elsfm.mobile.core.network.api.PlaylistInfo>,
    isLoading: Boolean,
    isAdding: Boolean,
    onPlaylistTap: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            text = "Add to playlist",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            playlists.isEmpty() -> {
                Text(
                    text = "You don't have any playlists yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
            else -> {
                LazyColumn {
                    items(playlists, key = { it.id }) { playlist ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !isAdding) { onPlaylistTap(playlist.id) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                            ) {
                                AsyncImage(
                                    model = playlist.image,
                                    contentDescription = playlist.name,
                                    modifier = Modifier.size(40.dp),
                                    contentScale = ContentScale.Crop,
                                )
                            }
                            Text(
                                text = playlist.name,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun slugifyTrackName(input: String): String =
    input.lowercase().replace(Regex("[^a-z0-9\\s-]"), "").trim().replace(Regex("\\s+"), "-")
