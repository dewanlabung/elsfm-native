package com.elsfm.mobile.feature.downloads

import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DownloadsViewModel
    private lateinit var repository: DownloadRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DownloadRepository(FakeDownloadedTrackDao())
        viewModel = DownloadsViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialStateEmpty() = runTest(testDispatcher) {
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(DownloadTab.SONGS, state.activeTab)
        assertEquals("", state.searchQuery)
        assertEquals(SortBy.RECENTLY_ADDED, state.sortBy)
        assertEquals(emptyList<DownloadedTrackUI>(), state.downloadedTracks)
    }

    @Test
    fun testLoadDownloadsSuccess() = runTest(testDispatcher) {
        val tracks = listOf(
            DownloadedTrack(
                trackId = 1,
                title = "Track 1",
                artist = "Artist 1",
                fileName = "track1.mp3",
                fileSizeBytes = 5_000_000,
                downloadedAt = System.currentTimeMillis(),
                artworkUrl = "http://example.com/art.jpg",
            )
        )
        val newViewModel = DownloadsViewModel(DownloadRepository(FakeDownloadedTrackDao(tracks)))

        advanceUntilIdle()

        val state = newViewModel.state.value
        assertEquals(1, state.downloadedTracks.size)
        assertEquals("Track 1", state.downloadedTracks[0].title)
    }

    @Test
    fun testTabChanged() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(DownloadsEvent.TabChanged(DownloadTab.ALBUMS))
        val state = viewModel.state.value
        assertEquals(DownloadTab.ALBUMS, state.activeTab)
    }

    @Test
    fun testSearchQueryChanged() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(DownloadsEvent.SearchQueryChanged("test"))
        val state = viewModel.state.value
        assertEquals("test", state.searchQuery)
    }

    @Test
    fun testSortChanged() = runTest(testDispatcher) {
        advanceUntilIdle()
        viewModel.onEvent(DownloadsEvent.SortChanged(SortBy.A_TO_Z))
        val state = viewModel.state.value
        assertEquals(SortBy.A_TO_Z, state.sortBy)
    }
}
