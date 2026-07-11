package com.elsfm.mobile.core.designsystem

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Track context menu matching the real elsfm.com PWA's item order and set:
 * Add to queue, Add to library, Add to playlist, Go to artist, Go to album, Go to track,
 * View lyrics, Make available offline, Share, Repost.
 *
 * [artistId] and [albumId] are nullable because the currently playing/listed [Track] may not
 * carry that information (a track's `album` relation is only populated when the backend
 * eager-loads it). The corresponding menu items are hidden rather than firing a callback with a
 * fabricated id.
 */
@Composable
fun TrackContextMenu(
    trackId: Int,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onAddToQueue: (Int) -> Unit,
    onAddToLibrary: (Int) -> Unit,
    onAddToPlaylist: (Int) -> Unit,
    onShare: () -> Unit,
    onRepost: (Int) -> Unit,
    artistId: Int? = null,
    albumId: Int? = null,
    onGoToArtist: (Int) -> Unit = {},
    onGoToAlbum: (Int) -> Unit = {},
    onGoToTrack: (Int) -> Unit = {},
    onViewLyrics: (Int) -> Unit = {},
    onMakeAvailableOffline: (Int) -> Unit = {},
    onRemoveFromPlaylist: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    if (!isVisible) return

    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismiss,
        modifier = modifier,
    ) {
        DropdownMenuItem(
            text = { Text("Add to queue") },
            onClick = { onAddToQueue(trackId); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Add to library") },
            onClick = { onAddToLibrary(trackId); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Add to playlist") },
            onClick = { onAddToPlaylist(trackId); onDismiss() },
        )
        if (artistId != null) {
            DropdownMenuItem(
                text = { Text("Go to artist") },
                onClick = { onGoToArtist(artistId); onDismiss() },
            )
        }
        if (albumId != null) {
            DropdownMenuItem(
                text = { Text("Go to album") },
                onClick = { onGoToAlbum(albumId); onDismiss() },
            )
        }
        DropdownMenuItem(
            text = { Text("Go to track") },
            onClick = { onGoToTrack(trackId); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("View lyrics") },
            onClick = { onViewLyrics(trackId); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Make available offline") },
            onClick = { onMakeAvailableOffline(trackId); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Share") },
            onClick = { onShare(); onDismiss() },
        )
        DropdownMenuItem(
            text = { Text("Repost") },
            onClick = { onRepost(trackId); onDismiss() },
        )
        if (onRemoveFromPlaylist != null) {
            DropdownMenuItem(
                text = { Text("Remove from playlist") },
                onClick = { onRemoveFromPlaylist(); onDismiss() },
            )
        }
    }
}
