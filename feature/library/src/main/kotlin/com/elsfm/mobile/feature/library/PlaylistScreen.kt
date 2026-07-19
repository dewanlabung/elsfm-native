package com.elsfm.mobile.feature.library

import androidx.compose.foundation.background
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.designsystem.TrackContextMenu
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.api.PlaylistInfo
import com.elsfm.mobile.feature.library.composables.TrackListItem

internal const val PLAYLIST_TRACK_LIST_TEST_TAG = "playlistTrackList"
internal const val PLAYLIST_LAZY_COLUMN_TEST_TAG = "playlistLazyColumn"
internal const val PLAYLIST_LOAD_MORE_INDICATOR_TEST_TAG = "playlistLoadMoreIndicator"

/** Fire the next-page request this many items before the end of the list, so scrolling stays smooth. */
private const val LOAD_MORE_THRESHOLD = 5

@Composable
fun PlaylistScreen(
    playlist: Playlist,
    onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
    onAddToQueue: (Track) -> Unit = {},
    onPlayAll: (List<Track>) -> Unit = {},
    onBack: () -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(playlist.id) {
        viewModel.loadPlaylist(playlist)
    }

    LaunchedEffect(state.playlistDeleted) {
        if (state.playlistDeleted) onBack()
    }

    PlaylistDetailContent(
        state = state,
        onPlayAll = { onPlayAll(state.tracks) },
        onTrackTap = onTrackTap,
        onAddToQueue = onAddToQueue,
        onDeleteTrack = viewModel::deleteTrack,
        onToggleTrackLike = viewModel::toggleTrackLike,
        onDownloadPlaylist = viewModel::downloadPlaylist,
        onDownloadTrack = viewModel::downloadTrack,
        onLoadNextPage = viewModel::loadNextPage,
        onRenamePlaylist = viewModel::renamePlaylist,
        onDeletePlaylist = viewModel::deletePlaylist,
        onShowPlaylistPicker = viewModel::showPlaylistPicker,
        onHidePlaylistPicker = viewModel::hidePlaylistPicker,
        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
        onRepostTrack = viewModel::repostTrack,
        onGoToArtist = onGoToArtist,
        onGoToAlbum = onGoToAlbum,
        onGoToTrack = onGoToTrack,
        onViewLyrics = onViewLyrics,
        onViewComments = onViewComments,
    )
}

@Composable
internal fun PlaylistDetailContent(
    state: PlaylistDetailState,
    onPlayAll: () -> Unit,
    onTrackTap: (Track, List<Track>) -> Unit,
    onAddToQueue: (Track) -> Unit,
    onDeleteTrack: (Int) -> Unit,
    onToggleTrackLike: (Int) -> Unit = {},
    onDownloadPlaylist: () -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {},
    onLoadNextPage: () -> Unit = {},
    onRenamePlaylist: (String) -> Unit = {},
    onDeletePlaylist: () -> Unit = {},
    onShowPlaylistPicker: (Int) -> Unit = {},
    onHidePlaylistPicker: () -> Unit = {},
    onAddTrackToPlaylist: (Int) -> Unit = {},
    onRepostTrack: (Int) -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val playlist = state.playlist
    val context = LocalContext.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        when {
            state.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            playlist == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = state.error ?: "Playlist not found",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
            else -> {
                val listState = rememberLazyListState()
                // Read live values through rememberUpdatedState instead of keying the
                // LaunchedEffect on them - hasMoreTracks/isLoadingMore change as a direct
                // result of onLoadNextPage() firing below, so keying on them would restart
                // (cancel + relaunch) this snapshotFlow subscription on every page load.
                val currentState by rememberUpdatedState(state)

                LaunchedEffect(listState) {
                    snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
                        .distinctUntilChanged()
                        .collect { lastVisibleIndex ->
                            val totalItems = listState.layoutInfo.totalItemsCount
                            if (
                                lastVisibleIndex != null &&
                                lastVisibleIndex >= totalItems - LOAD_MORE_THRESHOLD &&
                                currentState.hasMoreTracks &&
                                !currentState.isLoadingMore
                            ) {
                                onLoadNextPage()
                            }
                        }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().testTag(PLAYLIST_LAZY_COLUMN_TEST_TAG),
                ) {
                    item {
                        val playlistUrl = "https://www.elsfm.com/playlist/${playlist.id}"
                        PlaylistHeader(
                            playlist = playlist,
                            firstTrackImage = state.tracks.firstOrNull()?.image,
                            trackCount = state.tracks.size,
                            onPlayAll = onPlayAll,
                            isDownloading = state.isDownloadingPlaylist,
                            onDownloadPlaylist = onDownloadPlaylist,
                            // Channel playlists are curated content owned by elsfm, not the
                            // signed-in user - only show rename/delete for personal playlists.
                            isOwnedByUser = playlist.channelId == null,
                            isRenamingPlaylist = state.isRenamingPlaylist,
                            isDeletingPlaylist = state.isDeletingPlaylist,
                            onRenamePlaylist = onRenamePlaylist,
                            onDeletePlaylist = onDeletePlaylist,
                            onShare = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, playlistUrl)
                                    putExtra(Intent.EXTRA_SUBJECT, playlist.name)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share playlist"))
                            },
                            onCopyLink = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Playlist link", playlistUrl))
                            },
                        )
                    }
                    item {
                        Divider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                    items(state.tracks, key = { it.id }) { track ->
                        PlaylistTrackRow(
                            track = track,
                            onClick = { onTrackTap(track, state.tracks) },
                            onAddToQueue = { onAddToQueue(track) },
                            onDelete = { onDeleteTrack(track.id) },
                            isLiked = state.likedTrackIds.contains(track.id),
                            isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                            onToggleLike = { onToggleTrackLike(track.id) },
                            isDownloading = state.downloadingTrackIds.contains(track.id),
                            isDownloaded = state.downloadedTrackIds.contains(track.id),
                            onDownload = { onDownloadTrack(track) },
                            onAddToPlaylist = { onShowPlaylistPicker(track.id) },
                            onShare = {
                                val url = "https://www.elsfm.com/track/${track.id}/${slugifyTrackName(track.name)}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, url)
                                    putExtra(Intent.EXTRA_SUBJECT, track.name)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share track"))
                            },
                            onRepost = { onRepostTrack(track.id) },
                            onGoToArtist = onGoToArtist,
                            onGoToAlbum = onGoToAlbum,
                            onGoToTrack = onGoToTrack,
                            onViewLyrics = onViewLyrics,
                            onViewComments = onViewComments,
                        )
                    }
                    if (state.isLoadingMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .testTag(PLAYLIST_LOAD_MORE_INDICATOR_TEST_TAG),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
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
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (state.isPlaylistPickerVisible) {
        AddToPlaylistBottomSheet(
            playlists = state.userPlaylists,
            isLoading = state.isLoadingPlaylists,
            isAdding = state.addToPlaylistLoading,
            onPlaylistTap = onAddTrackToPlaylist,
            onDismiss = onHidePlaylistPicker,
        )
    }
}

/** Matches the URL slug format used by `feature:player`'s track-share links. */
private fun slugifyTrackName(input: String): String {
    return input
        .lowercase()
        .replace(Regex("[^a-z0-9\\s-]"), "")
        .trim()
        .replace(Regex("\\s+"), "-")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToPlaylistBottomSheet(
    playlists: List<PlaylistInfo>,
    isLoading: Boolean,
    isAdding: Boolean,
    onPlaylistTap: (Int) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier.testTag("add_to_playlist_sheet"),
    ) {
        Text(
            text = "Add to playlist",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
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
                LazyColumn(modifier = Modifier.testTag("add_to_playlist_list")) {
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
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp)),
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

@Composable
private fun PlaylistHeader(
    playlist: Playlist,
    firstTrackImage: String? = null,
    trackCount: Int,
    onPlayAll: () -> Unit,
    isDownloading: Boolean = false,
    onDownloadPlaylist: () -> Unit = {},
    isOwnedByUser: Boolean = false,
    isRenamingPlaylist: Boolean = false,
    isDeletingPlaylist: Boolean = false,
    onRenamePlaylist: (String) -> Unit = {},
    onDeletePlaylist: () -> Unit = {},
    onShare: () -> Unit = {},
    onCopyLink: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Centered playlist art with shadow
        Box(
            modifier = Modifier
                .size(220.dp)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = playlist.image ?: firstTrackImage,
                contentDescription = playlist.name,
                modifier = Modifier.size(220.dp),
                contentScale = ContentScale.Crop,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Playlist name
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "$trackCount tracks",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Primary action: Play All
        Button(
            onClick = onPlayAll,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.size(8.dp))
            Text(text = "Play All", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Secondary actions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(
                onClick = onDownloadPlaylist,
                enabled = !isDownloading,
                modifier = Modifier.testTag("playlist_download_button"),
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = "Make playlist available offline")
                }
            }
            FilledTonalIconButton(onClick = onShare) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share")
            }
            FilledTonalIconButton(onClick = onCopyLink) {
                Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = "Copy link")
            }
            Box {
                FilledTonalIconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("playlist_menu_button"),
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Add to queue") },
                        leadingIcon = { Icon(Icons.Filled.QueueMusic, contentDescription = null) },
                        onClick = { menuExpanded = false; onPlayAll() },
                    )
                    if (isOwnedByUser) {
                        DropdownMenuItem(
                            text = { Text("Rename playlist") },
                            leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                            onClick = { menuExpanded = false; showRenameDialog = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Delete playlist", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuExpanded = false; showDeleteDialog = true },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showRenameDialog) {
        RenamePlaylistDialog(
            initialName = playlist.name,
            isSaving = isRenamingPlaylist,
            onDismiss = { showRenameDialog = false },
            onConfirm = { name ->
                onRenamePlaylist(name)
                showRenameDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete playlist?") },
            text = { Text("\"${playlist.name}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeletePlaylist()
                    },
                    enabled = !isDeletingPlaylist,
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

@Composable
private fun RenamePlaylistDialog(
    initialName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename playlist") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                modifier = Modifier.testTag("rename_playlist_input"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = !isSaving && name.isNotBlank() && name != initialName,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun PlaylistTrackRow(
    track: Track,
    onClick: () -> Unit,
    onAddToQueue: () -> Unit,
    onDelete: () -> Unit,
    isLiked: Boolean = false,
    isLikeLoading: Boolean = false,
    onToggleLike: () -> Unit = {},
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    onDownload: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onShare: () -> Unit = {},
    onRepost: () -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag(PLAYLIST_TRACK_LIST_TEST_TAG),
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
                onAddToQueue = { onAddToQueue() },
                onAddToLibrary = { onToggleLike() },
                onAddToPlaylist = { onAddToPlaylist() },
                onShare = onShare,
                onRepost = { onRepost() },
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum,
                onGoToTrack = onGoToTrack,
                onViewLyrics = onViewLyrics,
                onRemoveFromPlaylist = onDelete,
                onMakeAvailableOffline = { if (!isDownloading && !isDownloaded) onDownload() },
                onViewComments = onViewComments,
            )
        }
    }
}
