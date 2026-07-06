package com.elsfm.mobile.feature.search.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.feature.library.composables.ArtistCard
import com.elsfm.mobile.feature.search.SearchEmptyMessage
import com.elsfm.mobile.feature.search.SearchUiState

internal const val ARTISTS_TAB_GRID_TEST_TAG = "searchArtistsGrid"
private const val GRID_COLUMN_COUNT = 3
private const val SHIMMER_CARD_COUNT = 6

@Composable
internal fun ArtistsTabContent(
    state: SearchUiState,
    onArtistTap: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> ArtistsShimmer(modifier = modifier)
        state.hasSearched && state.artists.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SearchEmptyMessage()
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMN_COUNT),
                modifier = modifier.testTag(ARTISTS_TAB_GRID_TEST_TAG),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.artists, key = { it.id }) { artist ->
                    ArtistCard(artist = artist, onClick = { onArtistTap(artist) })
                }
            }
        }
    }
}

@Composable
private fun ArtistsShimmer(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(count = SHIMMER_CARD_COUNT) {
            Surface(
                modifier = Modifier.size(120.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape,
            ) {}
        }
    }
}
