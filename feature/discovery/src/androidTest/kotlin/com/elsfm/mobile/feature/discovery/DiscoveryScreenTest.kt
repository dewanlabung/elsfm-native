package com.elsfm.mobile.feature.discovery

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.elsfm.mobile.core.model.Album
import com.elsfm.mobile.core.model.Playlist
import com.elsfm.mobile.core.model.Track
import org.junit.Rule
import org.junit.Test

class DiscoveryScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun testState() = DiscoveryUiState(
        kidsZone = listOf(
            Playlist(id = 8, name = "All Sunday School Songs", image = null),
        ),
        kidsZoneTitle = "Kids Zone",
        exploreMoreChannel = listOf(
            Playlist(id = 25, name = "Youth Camp Nepali Christian Songs", image = null),
        ),
        exploreMoreChannelTitle = "Explore More Channel",
        exploreMoreChannelId = 23,
        newReleases = listOf(
            Album(id = 460, name = "2026 EL Shaddai Youth Camp Songs", image = null, releaseDate = "2026-02-08"),
        ),
        newReleasesTitle = "New Release Nepali Christian songs",
        mostlyPlayedSongs = listOf(
            Track(id = 1, name = "Popular Track", image = null, durationMs = 180000, artists = emptyList()),
        ),
        mostlyPlayedSongsTitle = "Mostly Played Songs",
        recentlyPlayed = listOf(
            Track(id = 2, name = "Recent Track", image = null, durationMs = 200000, artists = emptyList()),
        ),
        isLoading = false,
        error = null,
    )

    private fun setContentWithData(
        onSeeAllKidsZone: () -> Unit = {},
        onSeeAllExploreMoreChannel: () -> Unit = {},
        onSeeAllNewReleases: () -> Unit = {},
        onSeeAllMostlyPlayedSongs: () -> Unit = {},
        onChannelClicked: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            DiscoveryContent(
                state = testState(),
                onTrackClicked = { _, _ -> },
                onSeeAllKidsZone = onSeeAllKidsZone,
                onSeeAllExploreMoreChannel = onSeeAllExploreMoreChannel,
                onSeeAllNewReleases = onSeeAllNewReleases,
                onSeeAllMostlyPlayedSongs = onSeeAllMostlyPlayedSongs,
                onPlaylistClicked = {},
                onAlbumClicked = {},
                onTrackMoreClicked = {},
                onChannelClicked = onChannelClicked,
            )
        }
    }

    /** Scrolls the Discovery LazyColumn so section [index] is composed and reachable. */
    private fun scrollToSection(index: Int) {
        composeRule.onNodeWithTag(DISCOVERY_CONTENT_LIST_TEST_TAG).performScrollToIndex(index)
    }

    @Test
    fun rendersAllFourSectionTitles() {
        setContentWithData()

        composeRule.onNodeWithText("Kids Zone").assertExists()
        scrollToSection(1)
        composeRule.onNodeWithText("Explore More Channel").assertExists()
        scrollToSection(2)
        composeRule.onNodeWithText("New Release Nepali Christian songs").assertExists()
        scrollToSection(3)
        composeRule.onNodeWithText("Mostly Played Songs").assertExists()
    }

    @Test
    fun seeAllKidsZoneInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllKidsZone = { invoked = true })

        composeRule.onNodeWithText("Kids Zone").performClick()

        assert(invoked) { "onSeeAllKidsZone was not invoked" }
    }

    @Test
    fun seeAllExploreMoreChannelInvokesChannelClickedWithRealChannelId() {
        var clickedChannelId: Int? = null
        setContentWithData(onChannelClicked = { clickedChannelId = it })

        scrollToSection(1)
        composeRule.onNodeWithText("Explore More Channel").performClick()

        assert(clickedChannelId == 23) { "onChannelClicked was not invoked with the Explore More channel id" }
    }

    @Test
    fun seeAllNewReleasesInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllNewReleases = { invoked = true })

        scrollToSection(2)
        composeRule.onNodeWithText("New Release Nepali Christian songs").performClick()

        assert(invoked) { "onSeeAllNewReleases was not invoked" }
    }

    @Test
    fun seeAllMostlyPlayedSongsInvokesCallback() {
        var invoked = false
        setContentWithData(onSeeAllMostlyPlayedSongs = { invoked = true })

        scrollToSection(3)
        composeRule.onNodeWithText("Mostly Played Songs").performClick()

        assert(invoked) { "onSeeAllMostlyPlayedSongs was not invoked" }
    }
}
