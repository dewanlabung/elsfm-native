package com.elsfm.mobile.feature.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.feature.search.tabs.ALBUMS_TAB_GRID_TEST_TAG
import com.elsfm.mobile.feature.search.tabs.ARTISTS_TAB_GRID_TEST_TAG
import com.elsfm.mobile.feature.search.tabs.PLAYLISTS_TAB_GRID_TEST_TAG
import com.elsfm.mobile.feature.search.tabs.TRACKS_TAB_LIST_TEST_TAG
import org.junit.Rule
import org.junit.Test

class SearchScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val sampleTracks = listOf(
        Track(id = 1, name = "My Track", image = null, durationMs = 1000, artists = emptyList()),
    )
    private val sampleAlbums = listOf(Album(id = 2, name = "My Album", image = null, releaseDate = "2026-01-01"))
    private val sampleArtists = listOf(Artist(id = 3, name = "My Artist"))
    private val samplePlaylists = listOf(Playlist(id = 4, name = "My Playlist", image = null))

    private fun setContent(
        state: SearchUiState,
        onQueryChange: (String) -> Unit = {},
        onTrackTap: (Track) -> Unit = {},
        onAlbumTap: (Album) -> Unit = {},
        onArtistTap: (Artist) -> Unit = {},
        onPlaylistTap: (Playlist) -> Unit = {},
    ) {
        composeRule.setContent {
            SearchContent(
                state = state,
                onQueryChange = onQueryChange,
                onTrackTap = onTrackTap,
                onAlbumTap = onAlbumTap,
                onArtistTap = onArtistTap,
                onPlaylistTap = onPlaylistTap,
            )
        }
    }

    @Test
    fun rendersAllFourTabs() {
        setContent(SearchUiState())

        composeRule.onNodeWithText("Tracks (0)").assertExists()
        composeRule.onNodeWithText("Albums (0)").assertExists()
        composeRule.onNodeWithText("Artists (0)").assertExists()
        composeRule.onNodeWithText("Playlists (0)").assertExists()
    }

    @Test
    fun resultCountsAppearInTabLabels() {
        setContent(
            SearchUiState(
                tracks = sampleTracks,
                albums = sampleAlbums,
                artists = sampleArtists,
                playlists = samplePlaylists,
            ),
        )

        composeRule.onNodeWithText("Tracks (1)").assertExists()
        composeRule.onNodeWithText("Albums (1)").assertExists()
        composeRule.onNodeWithText("Artists (1)").assertExists()
        composeRule.onNodeWithText("Playlists (1)").assertExists()
    }

    @Test
    fun tracksTabShowsTrackListItemsByDefault() {
        setContent(SearchUiState(tracks = sampleTracks))

        composeRule.onNodeWithTag(TRACKS_TAB_LIST_TEST_TAG).assertExists()
        composeRule.onNodeWithText("My Track").assertExists()
    }

    @Test
    fun switchingToAlbumsTabShowsAlbumCards() {
        setContent(SearchUiState(albums = sampleAlbums))

        composeRule.onNodeWithText("Albums (1)").performClick()

        composeRule.onNodeWithTag(ALBUMS_TAB_GRID_TEST_TAG).assertExists()
        composeRule.onNodeWithText("My Album").assertExists()
    }

    @Test
    fun switchingToArtistsTabShowsArtistCards() {
        setContent(SearchUiState(artists = sampleArtists))

        composeRule.onNodeWithText("Artists (1)").performClick()

        composeRule.onNodeWithTag(ARTISTS_TAB_GRID_TEST_TAG).assertExists()
        composeRule.onNodeWithText("My Artist").assertExists()
    }

    @Test
    fun switchingToPlaylistsTabShowsPlaylistCards() {
        setContent(SearchUiState(playlists = samplePlaylists))

        composeRule.onNodeWithText("Playlists (1)").performClick()

        composeRule.onNodeWithTag(PLAYLISTS_TAB_GRID_TEST_TAG).assertExists()
        composeRule.onNodeWithText("My Playlist").assertExists()
    }

    @Test
    fun showsNoResultsFoundWhenSearchedAndTrackTabIsEmpty() {
        setContent(SearchUiState(hasSearched = true))

        composeRule.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun showsNoResultsFoundOnAlbumsTabWhenSearchedAndEmpty() {
        setContent(SearchUiState(hasSearched = true, tracks = sampleTracks))

        composeRule.onNodeWithText("Albums (0)").performClick()

        composeRule.onNodeWithText("No results found").assertExists()
    }

    @Test
    fun doesNotShowEmptyMessageBeforeAnySearchPerformed() {
        setContent(SearchUiState())

        composeRule.onNodeWithText("No results found").assertDoesNotExist()
    }

    @Test
    fun doesNotShowEmptyMessageWhileLoading() {
        setContent(SearchUiState(isLoading = true))

        composeRule.onNodeWithText("No results found").assertDoesNotExist()
        composeRule.onNodeWithTag(TRACKS_TAB_LIST_TEST_TAG).assertDoesNotExist()
    }

    @Test
    fun searchFieldExistsAndIsInteractable() {
        setContent(SearchUiState())

        composeRule.onNodeWithTag(SEARCH_FIELD_TEST_TAG).assertExists()
    }

    @Test
    fun tappingTrackInvokesOnTrackTap() {
        var tapped: Track? = null
        setContent(SearchUiState(tracks = sampleTracks), onTrackTap = { tapped = it })

        composeRule.onNodeWithText("My Track").performClick()

        assert(tapped == sampleTracks.first()) { "onTrackTap was not invoked" }
    }

    @Test
    fun tappingArtistInvokesOnArtistTap() {
        var tapped: Artist? = null
        setContent(SearchUiState(artists = sampleArtists), onArtistTap = { tapped = it })

        composeRule.onNodeWithText("Artists (1)").performClick()
        composeRule.onNodeWithText("My Artist").performClick()

        assert(tapped == sampleArtists.first()) { "onArtistTap was not invoked" }
    }
}
