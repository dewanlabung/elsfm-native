package com.elsfm.mobile.feature.discovery.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.TrackCard

private val CARD_WIDTH = 100.dp

@Composable
fun RecentlyPlayedSection(
    tracks: List<Track>,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
    ) {
        items(tracks, key = { it.id }) { track ->
            TrackCard(
                track = track,
                onClick = { onTrackClick(track) },
                modifier = Modifier.width(CARD_WIDTH),
            )
        }
    }
}
