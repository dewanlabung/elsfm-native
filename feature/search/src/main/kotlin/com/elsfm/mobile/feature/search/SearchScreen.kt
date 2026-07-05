package com.elsfm.mobile.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.elsfm.mobile.core.model.SearchResult
import com.elsfm.mobile.core.model.Track

@Composable
fun SearchScreen(
    onTrackClicked: (track: Track, queue: List<Track>) -> Unit,
    onArtistClicked: (artistId: Int) -> Unit,
    searchViewModel: SearchViewModel = hiltViewModel(),
) {
    var query by remember { mutableStateOf("") }
    val state by searchViewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        TextField(
            value = query,
            onValueChange = {
                query = it
                searchViewModel.search(it)
            },
            label = { Text("Search") },
            modifier = Modifier.fillMaxWidth().testTag("search_field"),
        )
        LazyColumn(modifier = Modifier.testTag("search_results")) {
            items(state.results) { result ->
                when (result) {
                    is SearchResult.TrackResult -> {
                        Text(
                            text = result.track.name,
                            modifier = Modifier.clickable {
                                val queue = state.results
                                    .filterIsInstance<SearchResult.TrackResult>()
                                    .map { it.track }
                                onTrackClicked(result.track, queue)
                            },
                        )
                    }
                    is SearchResult.ArtistResult -> {
                        Text(
                            text = result.artist.name,
                            modifier = Modifier.clickable {
                                onArtistClicked(result.artist.id)
                            },
                        )
                    }
                    is SearchResult.PlaylistResult -> {
                        Text(text = result.playlist.name)
                    }
                    is SearchResult.ChannelResult -> {
                        Text(text = result.channel.name)
                    }
                }
            }
        }
    }
}
