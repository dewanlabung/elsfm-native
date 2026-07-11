package com.elsfm.mobile.feature.search.tabs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.library.composables.TrackListItem
import com.elsfm.mobile.feature.search.SearchEmptyMessage
import com.elsfm.mobile.feature.search.SearchUiState

internal const val TRACKS_TAB_LIST_TEST_TAG = "searchTracksList"
private const val SHIMMER_ROW_COUNT = 6

@Composable
internal fun TracksTabContent(
    state: SearchUiState,
    onTrackTap: (Track, List<Track>) -> Unit,
    onToggleTrackLike: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> TracksShimmer(modifier = modifier)
        state.hasSearched && state.tracks.isEmpty() -> {
            Box(
                modifier = modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                SearchEmptyMessage()
            }
        }
        else -> {
            LazyColumn(
                modifier = modifier.testTag(TRACKS_TAB_LIST_TEST_TAG),
                contentPadding = PaddingValues(16.dp),
            ) {
                items(state.tracks, key = { it.id }) { track ->
                    TrackListItem(
                        track = track,
                        onClick = { onTrackTap(track, state.tracks) },
                        isLiked = state.likedTrackIds.contains(track.id),
                        isLikeLoading = state.likeLoadingTrackIds.contains(track.id),
                        onLikeClick = { onToggleTrackLike(track.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TracksShimmer(modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(count = SHIMMER_ROW_COUNT) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(66.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
            ) {}
        }
    }
}
