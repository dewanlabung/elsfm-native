package com.elsfm.mobile.feature.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.search.tabs.AlbumsTabContent
import com.elsfm.mobile.feature.search.tabs.ArtistsTabContent
import com.elsfm.mobile.feature.search.tabs.PlaylistsTabContent
import com.elsfm.mobile.feature.search.tabs.TracksTabContent

internal const val SEARCH_FIELD_TEST_TAG = "search_field"

internal enum class SearchTab {
    TRACKS,
    ALBUMS,
    ARTISTS,
    PLAYLISTS,
}

@Composable
fun SearchScreen(
    onTrackTap: (Track) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onArtistTap: (Artist) -> Unit,
    onPlaylistTap: (Playlist) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    val state by searchViewModel.state.collectAsState()

    SearchContent(
        state = state,
        onQueryChange = searchViewModel::search,
        onTrackTap = onTrackTap,
        onAlbumTap = onAlbumTap,
        onArtistTap = onArtistTap,
        onPlaylistTap = onPlaylistTap,
    )
}

@Composable
internal fun SearchContent(
    state: SearchUiState,
    onQueryChange: (String) -> Unit,
    onTrackTap: (Track) -> Unit,
    onAlbumTap: (Album) -> Unit,
    onArtistTap: (Artist) -> Unit,
    onPlaylistTap: (Playlist) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = SearchTab.entries

    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChange,
            label = { Text("Search") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag(SEARCH_FIELD_TEST_TAG),
        )

        TabRow(selectedTabIndex = selectedTabIndex) {
            tabs.forEachIndexed { index, tab ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(text = tabLabel(tab, state)) },
                )
            }
        }

        when (tabs[selectedTabIndex]) {
            SearchTab.TRACKS -> TracksTabContent(
                state = state,
                onTrackTap = onTrackTap,
                modifier = Modifier.fillMaxSize(),
            )
            SearchTab.ALBUMS -> AlbumsTabContent(
                state = state,
                onAlbumTap = onAlbumTap,
                modifier = Modifier.fillMaxSize(),
            )
            SearchTab.ARTISTS -> ArtistsTabContent(
                state = state,
                onArtistTap = onArtistTap,
                modifier = Modifier.fillMaxSize(),
            )
            SearchTab.PLAYLISTS -> PlaylistsTabContent(
                state = state,
                onPlaylistTap = onPlaylistTap,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun tabLabel(tab: SearchTab, state: SearchUiState): String = when (tab) {
    SearchTab.TRACKS -> "Tracks (${state.tracks.size})"
    SearchTab.ALBUMS -> "Albums (${state.albums.size})"
    SearchTab.ARTISTS -> "Artists (${state.artists.size})"
    SearchTab.PLAYLISTS -> "Playlists (${state.playlists.size})"
}

@Composable
internal fun SearchEmptyMessage(modifier: Modifier = Modifier) {
    Text(
        text = "No results found",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(16.dp),
    )
}
