package com.elsfm.mobile.feature.downloads

import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.database.repository.DownloadedTrack
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class DownloadsViewModelTest {
    private lateinit var viewModel: DownloadsViewModel
    private lateinit var repository: DownloadRepository

    @Before
    fun setup() {
        repository = mockk(relaxed = true)
        coEvery { repository.getCompletedDownloads() } returns flowOf(emptyList())
        viewModel = DownloadsViewModel(repository)
    }

    @Test
    fun testInitialStateEmpty() = runTest {
        val state = viewModel.state.value
        assertEquals(DownloadTab.SONGS, state.activeTab)
        assertEquals("", state.searchQuery)
        assertEquals(SortBy.RECENTLY_ADDED, state.sortBy)
        assertEquals(emptyList(), state.downloadedTracks)
    }

    @Test
    fun testLoadDownloadsSuccess() = runTest {
        val mockTracks = listOf(
            DownloadedTrack(
                trackId = 1,
                title = "Track 1",
                artist = "Artist 1",
                albumId = null,
                playlistId = null,
                artworkUrl = "http://example.com/art.jpg",
                filePath = "/path/to/file.mp3",
                downloadedAt = System.currentTimeMillis(),
                fileSize = 5_000_000,
                status = "COMPLETED"
            )
        )
        coEvery { repository.getCompletedDownloads() } returns flowOf(mockTracks)

        val newViewModel = DownloadsViewModel(repository)

        val state = newViewModel.state.value
        assertEquals(1, state.downloadedTracks.size)
        assertEquals("Track 1", state.downloadedTracks[0].title)
    }

    @Test
    fun testTabChanged() = runTest {
        viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.ALBUMS))
        val state = viewModel.state.value
        assertEquals(DownloadTab.ALBUMS, state.activeTab)
    }

    @Test
    fun testSearchQueryChanged() = runTest {
        viewModel.onEvent(DownloadsEvent.SearchQueryChanged("test"))
        val state = viewModel.state.value
        assertEquals("test", state.searchQuery)
    }

    @Test
    fun testSortChanged() = runTest {
        viewModel.onEvent(DownloadsEvent.SortChanged(SortBy.A_TO_Z))
        val state = viewModel.state.value
        assertEquals(SortBy.A_TO_Z, state.sortBy)
    }
}
