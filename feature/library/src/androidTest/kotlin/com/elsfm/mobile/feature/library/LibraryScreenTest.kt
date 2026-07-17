package com.elsfm.mobile.feature.library

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Artist
import com.elsfm.mobile.core.model.Channel
import com.elsfm.mobile.core.model.Playlist
import org.junit.Rule
import org.junit.Test

class LibraryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val samplePlaylists = listOf(Playlist(id = 1, name = "My Playlist", image = null))
    private val sampleAlbums = listOf(Album(id = 2, name = "My Album", image = null, releaseDate = "2026-01-01"))
    private val sampleArtists = listOf(Artist(id = 3, name = "My Artist"))
    private val sampleChannels = listOf(Channel(id = 4, name = "My Channel"))

    private fun setContent(
        state: LibraryState,
        onFilterSelected: (LibraryFilter) -> Unit = {},
        onPlaylistTap: (Playlist) -> Unit = {},
        onAlbumTap: (Album) -> Unit = {},
        onArtistTap: (Artist) -> Unit = {},
        onChannelTap: (Channel) -> Unit = {},
        onSongsClick: () -> Unit = {},
        onPlayHistoryClick: () -> Unit = {},
        onCreatePlaylistClick: () -> Unit = {},
    ) {
        composeRule.setContent {
            LibraryContent(
                state = state,
                onFilterSelected = onFilterSelected,
                onPlaylistTap = onPlaylistTap,
                onAlbumTap = onAlbumTap,
                onArtistTap = onArtistTap,
                onChannelTap = onChannelTap,
                onSongsClick = onSongsClick,
                onPlayHistoryClick = onPlayHistoryClick,
                onCreatePlaylistClick = onCreatePlaylistClick,
            )
        }
    }

    @Test
    fun rendersSongsAndPlayHistoryQuickLinks() {
        setContent(LibraryState())

        composeRule.onNodeWithText("Songs").assertExists()
        composeRule.onNodeWithText("Play history").assertExists()
    }

    @Test
    fun tappingSongsQuickLinkInvokesCallback() {
        var tapped = false
        setContent(LibraryState(), onSongsClick = { tapped = true })

        composeRule.onNodeWithText("Songs").performClick()

        assert(tapped) { "onSongsClick was not invoked" }
    }

    @Test
    fun tappingPlayHistoryQuickLinkInvokesCallback() {
        var tapped = false
        setContent(LibraryState(), onPlayHistoryClick = { tapped = true })

        composeRule.onNodeWithText("Play history").performClick()

        assert(tapped) { "onPlayHistoryClick was not invoked" }
    }

    @Test
    fun tappingCreatePlaylistIconInvokesCallback() {
        var tapped = false
        setContent(LibraryState(), onCreatePlaylistClick = { tapped = true })

        composeRule.onNodeWithContentDescription("Create playlist").performClick()

        assert(tapped) { "onCreatePlaylistClick was not invoked" }
    }

    @Test
    fun rendersAllFilterTabs() {
        setContent(LibraryState())

        composeRule.onNodeWithText("All").assertExists()
        composeRule.onNodeWithText("Playlists").assertExists()
        composeRule.onNodeWithText("Albums").assertExists()
        composeRule.onNodeWithText("Artists").assertExists()
        composeRule.onNodeWithText("Channels").assertExists()
    }

    @Test
    fun rendersEmptyStateWhenNoData() {
        setContent(LibraryState())

        composeRule.onNodeWithText("Your library is empty").assertExists()
    }

    @Test
    fun rendersPlaylistAndAlbumWhenPresent() {
        setContent(
            LibraryState(
                playlists = samplePlaylists,
                albums = sampleAlbums,
                artists = sampleArtists,
                channels = sampleChannels,
            ),
        )

        composeRule.onNodeWithText("My Playlist").assertExists()
        composeRule.onNodeWithText("My Album").assertExists()
        composeRule.onNodeWithText("My Artist").assertExists()
        composeRule.onNodeWithText("My Channel").assertExists()
    }

    @Test
    fun tappingFilterTabInvokesCallback() {
        var selected: LibraryFilter? = null
        setContent(LibraryState(), onFilterSelected = { selected = it })

        composeRule.onNodeWithText("Playlists").performClick()

        assert(selected == LibraryFilter.PLAYLISTS) { "onFilterSelected was not invoked with PLAYLISTS" }
    }

    @Test
    fun tappingPlaylistCardInvokesCallback() {
        var tapped: Playlist? = null
        setContent(
            LibraryState(playlists = samplePlaylists),
            onPlaylistTap = { tapped = it },
        )

        composeRule.onNodeWithText("My Playlist").performClick()

        assert(tapped == samplePlaylists.first()) { "onPlaylistTap was not invoked" }
    }

    @Test
    fun tappingAlbumCardInvokesCallback() {
        var tapped: Album? = null
        setContent(
            LibraryState(albums = sampleAlbums),
            onAlbumTap = { tapped = it },
        )

        composeRule.onNodeWithText("My Album").performClick()

        assert(tapped == sampleAlbums.first()) { "onAlbumTap was not invoked" }
    }

    @Test
    fun showsLoadingIndicatorWhenLoading() {
        setContent(LibraryState(isLoading = true))

        // Empty-state text should not render while loading.
        composeRule.onNodeWithText("Your library is empty").assertDoesNotExist()
    }
}
