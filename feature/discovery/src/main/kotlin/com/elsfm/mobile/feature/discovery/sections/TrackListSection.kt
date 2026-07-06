package com.elsfm.mobile.feature.discovery.sections

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.TrackListItem

/**
 * Vertical list of tracks, shared by the Popular Songs and Recently Played
 * sections on the Discovery screen.
 */
@Composable
fun TrackListSection(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    onTrackMoreClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
    playingTrackId: Int? = null,
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        tracks.forEach { track ->
            TrackListItem(
                track = track,
                isPlaying = track.id == playingTrackId,
                onClick = { onTrackClick(track) },
                onMoreClick = { onTrackMoreClick(track) },
            )
        }
    }
}
