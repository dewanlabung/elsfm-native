package com.elsfm.mobile.feature.library

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.elsfm.mobile.core.designsystem.LikeButton
import com.elsfm.mobile.core.designsystem.TrackContextMenu
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Comment
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.BlurredBackground
import com.elsfm.mobile.feature.library.composables.TrackListItem

internal const val ALBUM_TRACK_LIST_TEST_TAG = "albumTrackList"

@Composable
fun AlbumScreen(
    onTrackTap: (Track, List<Track>) -> Unit = { _, _ -> },
    onAddToQueue: (Track) -> Unit = {},
    onPlayAll: (List<Track>) -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    AlbumDetailContent(
        state = state,
        onPlayAll = { onPlayAll(state.tracks) },
        onTrackTap = onTrackTap,
        onAddToQueue = { trackId -> viewModel.addToQueue(trackId) },
        onToggleTrackLike = viewModel::toggleTrackLike,
        onToggleAlbumLike = viewModel::toggleAlbumLike,
        onToggleAlbumRepost = viewModel::toggleAlbumRepost,
        onShare = {
            viewModel.buildAlbumShareUrl()?.let { url -> shareAlbumUrl(context, state.album?.name.orEmpty(), url) }
        },
        onDownloadAlbum = viewModel::downloadAlbum,
        onDownloadTrack = viewModel::downloadTrack,
        onRepostTrack = viewModel::repostTrack,
        onShowPlaylistPicker = viewModel::showPlaylistPicker,
        onHidePlaylistPicker = viewModel::hidePlaylistPicker,
        onAddTrackToPlaylist = viewModel::addTrackToPlaylist,
        onCommentInputChanged = viewModel::onCommentInputChanged,
        onPostComment = viewModel::postComment,
        onViewComments = onViewComments,
        onGoToArtist = onGoToArtist,
        onGoToAlbum = onGoToAlbum,
        onGoToTrack = onGoToTrack,
        onViewLyrics = onViewLyrics,
        onShareTrack = { track ->
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
internal fun AlbumDetailContent(
    state: AlbumDetailState,
    onPlayAll: () -> Unit,
    onTrackTap: (Track, List<Track>) -> Unit,
    onAddToQueue: (Int) -> Unit = {},
    onToggleTrackLike: (Int) -> Unit = {},
    onToggleAlbumLike: () -> Unit = {},
    onToggleAlbumRepost: () -> Unit = {},
    onShare: () -> Unit = {},
    onDownloadAlbum: () -> Unit = {},
    onDownloadTrack: (Track) -> Unit = {},
    onRepostTrack: (Int) -> Unit = {},
    onShowPlaylistPicker: (Int) -> Unit = {},
    onHidePlaylistPicker: () -> Unit = {},
    onAddTrackToPlaylist: (Int) -> Unit = {},
    onCommentInputChanged: (String) -> Unit = {},
    onPostComment: () -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onShareTrack: (Track) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val album = state.album

    if (state.isPlaylistPickerVisible) {
        AlbumAddToPlaylistSheet(
            playlists = state.userPlaylists,
            isLoading = state.isLoadingPlaylists,
            isAdding = state.addToPlaylistLoading,
            onPlaylistTap = onAddTrackToPlaylist,
            onDismiss = onHidePlaylistPicker,
        )
    }

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
                            isLiked = state.isAlbumLiked,
                            isLikeLoading = state.isAlbumLikeLoading,
                            onToggleLike = onToggleAlbumLike,
                            isReposted = state.isAlbumReposted,
                            isRepostLoading = state.isAlbumRepostLoading,
                            onToggleRepost = onToggleAlbumRepost,
                            onShare = onShare,
                            isDownloading = state.isDownloadingAlbum,
                            onDownloadAlbum = onDownloadAlbum,
                        )
                    }
                    items(state.tracks, key = { it.id }) { track ->
                        AlbumTrackRow(
                            track = track,
                            onClick = { onTrackTap(track, state.tracks) },
                            onAddToQueue = { onAddToQueue(track.id) },
                            isLiked = state.likedTrackIds.contains(track.id),
                            isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                            onToggleLike = { onToggleTrackLike(track.id) },
                            isDownloading = state.downloadingTrackIds.contains(track.id),
                            isDownloaded = state.downloadedTrackIds.contains(track.id),
                            onDownload = { onDownloadTrack(track) },
                            onRepost = { onRepostTrack(track.id) },
                            onAddToPlaylist = { onShowPlaylistPicker(track.id) },
                            onViewComments = onViewComments,
                            onGoToArtist = onGoToArtist,
                            onGoToAlbum = onGoToAlbum,
                            onGoToTrack = onGoToTrack,
                            onViewLyrics = onViewLyrics,
                            onShare = { onShareTrack(track) },
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
                    item {
                        CommentsSection(
                            comments = state.comments,
                            isLoading = state.isLoadingComments,
                            commentInput = state.commentInput,
                            isPosting = state.isPostingComment,
                            onCommentInputChanged = onCommentInputChanged,
                            onPostComment = onPostComment,
                        )
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
    isLiked: Boolean,
    isLikeLoading: Boolean,
    onToggleLike: () -> Unit,
    isReposted: Boolean,
    isRepostLoading: Boolean,
    onToggleRepost: () -> Unit,
    onShare: () -> Unit,
    isDownloading: Boolean = false,
    onDownloadAlbum: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Thumbnail + title/metadata side-by-side
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                AsyncImage(
                    model = album.image,
                    contentDescription = album.name,
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                val releaseDate = album.releaseDate
                if (!releaseDate.isNullOrBlank()) {
                    Text(
                        text = releaseDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "$trackCount tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!album.plays.isNullOrBlank()) {
                    Text(
                        text = "${album.plays} plays",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Action buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(onClick = onPlayAll) {
                Icon(imageVector = Icons.Filled.PlayArrow, contentDescription = null)
                Text(text = "Play All", modifier = Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                LikeButton(isLiked = isLiked, isLoading = isLikeLoading, onClick = onToggleLike)
                Text(
                    text = ((album.likesCount ?: 0) + if (isLiked) 1 else 0).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onToggleRepost,
                enabled = !isRepostLoading,
                modifier = Modifier.testTag("album_repost_button"),
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = if (isReposted) "Remove repost" else "Repost",
                    tint = if (isReposted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onShare, modifier = Modifier.testTag("album_share_button")) {
                Icon(imageVector = Icons.Filled.Share, contentDescription = "Share album")
            }
            IconButton(
                onClick = onDownloadAlbum,
                enabled = !isDownloading,
                modifier = Modifier.testTag("album_download_button"),
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(imageVector = Icons.Filled.Download, contentDescription = "Make album available offline")
                }
            }
        }

        album.description?.takeIf { it.isNotBlank() }?.let { description ->
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (album.tags.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                items(album.tags, key = { it.id }) { tag ->
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "#${tag.displayName ?: tag.name}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentsSection(
    comments: List<Comment>,
    isLoading: Boolean,
    commentInput: String,
    isPosting: Boolean,
    onCommentInputChanged: (String) -> Unit,
    onPostComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = if (comments.isEmpty()) "Comments" else "Comments (${comments.size})",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = commentInput,
                onValueChange = onCommentInputChanged,
                modifier = Modifier.weight(1f).testTag("album_comment_input"),
                placeholder = { Text("Leave a comment") },
                singleLine = true,
                enabled = !isPosting,
            )
            IconButton(
                onClick = onPostComment,
                enabled = !isPosting && commentInput.isNotBlank(),
                modifier = Modifier.testTag("album_comment_send"),
            ) {
                Icon(imageVector = Icons.Filled.Send, contentDescription = "Post comment")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
            comments.isEmpty() -> {
                Text(
                    text = "Seems a little quiet over here — be the first to comment",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    comments.forEach { comment -> CommentRow(comment) }
                }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: Comment, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = comment.user?.name ?: "User",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = comment.content ?: "[deleted]",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    isDownloading: Boolean = false,
    isDownloaded: Boolean = false,
    onDownload: () -> Unit = {},
    onRepost: () -> Unit = {},
    onAddToPlaylist: () -> Unit = {},
    onViewComments: (Int) -> Unit = {},
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onShare: () -> Unit = {},
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
                onMakeAvailableOffline = { if (!isDownloading && !isDownloaded) onDownload() },
                onViewComments = onViewComments,
                onGoToArtist = onGoToArtist,
                onGoToAlbum = onGoToAlbum,
                onGoToTrack = onGoToTrack,
                onViewLyrics = onViewLyrics,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlbumAddToPlaylistSheet(
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

private fun shareAlbumUrl(context: android.content.Context, albumName: String, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, url)
        putExtra(Intent.EXTRA_SUBJECT, albumName)
    }
    context.startActivity(Intent.createChooser(intent, "Share album"))
}
