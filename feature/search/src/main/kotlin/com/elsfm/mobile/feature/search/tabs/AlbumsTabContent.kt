package com.elsfm.mobile.feature.search.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.feature.library.composables.AlbumCard
import com.elsfm.mobile.feature.search.SearchEmptyMessage
import com.elsfm.mobile.feature.search.SearchUiState

internal const val ALBUMS_TAB_GRID_TEST_TAG = "searchAlbumsGrid"
private const val GRID_COLUMN_COUNT = 2
private const val SHIMMER_CARD_COUNT = 6

@Composable
internal fun AlbumsTabContent(
    state: SearchUiState,
    onAlbumTap: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.isLoading -> AlbumsShimmer(modifier = modifier)
        state.hasSearched && state.albums.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                SearchEmptyMessage()
            }
        }
        else -> {
            LazyVerticalGrid(
                columns = GridCells.Fixed(GRID_COLUMN_COUNT),
                modifier = modifier.testTag(ALBUMS_TAB_GRID_TEST_TAG),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.albums, key = { it.id }) { album ->
                    AlbumCard(album = album, onClick = { onAlbumTap(album) })
                }
            }
        }
    }
}

@Composable
private fun AlbumsShimmer(modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMN_COUNT),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(count = SHIMMER_CARD_COUNT) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(180.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
            ) {}
        }
    }
}
