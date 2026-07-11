package com.elsfm.mobile.feature.downloads

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.model.Track
import com.elsfm.mobile.core.network.download.DownloadManager
import com.elsfm.mobile.feature.player.PlayerController
import com.elsfm.mobile.feature.player.PlayerState
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

private class FakePlayerController : PlayerController {
    private val _state = MutableStateFlow(PlayerState())
    override val state: StateFlow<PlayerState> = _state
    var lastPlayedTrack: Track? = null
    var lastQueue: List<Track> = emptyList()

    override fun play(track: Track, queue: List<Track>) {
        lastPlayedTrack = track
        lastQueue = queue
    }

    override fun togglePlayPause() = Unit
    override fun seekTo(positionMs: Long) = Unit
    override fun skipNext() = Unit
    override fun skipPrevious() = Unit
    override fun jumpToQueueItem(track: Track) = Unit
    override fun addToQueue(track: Track) = Unit
    override fun toggleShuffle() = Unit
    override fun cycleRepeatMode() = Unit
}

private class FakeDownloadedTrackDao(initial: List<DownloadedTrack> = emptyList()) : DownloadedTrackDao {
    private val tracksFlow = MutableStateFlow(initial)

    override suspend fun insert(track: DownloadedTrack) {
        tracksFlow.value = tracksFlow.value + track
    }

    override fun getAll(): Flow<List<DownloadedTrack>> = tracksFlow.asStateFlow()

    override suspend fun getById(trackId: Int): DownloadedTrack? =
        tracksFlow.value.find { it.trackId == trackId }

    override suspend fun delete(trackId: Int) {
        tracksFlow.value = tracksFlow.value.filterNot { it.trackId == trackId }
    }

    override fun getTotalSizeBytes(): Flow<Long?> =
        MutableStateFlow(tracksFlow.value.sumOf { it.fileSizeBytes }).asStateFlow()
}

private class TestDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

private fun fakeDownloadManager(): DownloadManager {
    val httpClient = HttpClient(MockEngine { respond("{}") })
    return DownloadManager(
        context = null,
        httpClient = httpClient,
        dispatcherProvider = TestDispatcherProvider(Dispatchers.Unconfined),
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: DownloadsViewModel
    private lateinit var repository: DownloadRepository
    private lateinit var playerController: FakePlayerController

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = DownloadRepository(FakeDownloadedTrackDao(), fakeDownloadManager())
        playerController = FakePlayerController()
        viewModel = DownloadsViewModel(repository, playerController)
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
        val newViewModel = DownloadsViewModel(
            DownloadRepository(FakeDownloadedTrackDao(tracks), fakeDownloadManager()),
            FakePlayerController(),
        )

        advanceUntilIdle()

        val state = newViewModel.state.value
        assertEquals(1, state.downloadedTracks.size)
        assertEquals("Track 1", state.downloadedTracks[0].title)
    }

    @Test
    fun `groups downloaded tracks sharing an albumId into one album card`() = runTest(testDispatcher) {
        val tracks = listOf(
            DownloadedTrack(
                trackId = 1, title = "Track 1", artist = "Artist 1", fileName = "1.mp3",
                fileSizeBytes = 1_000, albumId = 42, albumName = "Youth Camp Songs",
            ),
            DownloadedTrack(
                trackId = 2, title = "Track 2", artist = "Artist 1", fileName = "2.mp3",
                fileSizeBytes = 1_000, albumId = 42, albumName = "Youth Camp Songs",
            ),
        )
        val newViewModel = DownloadsViewModel(
            DownloadRepository(FakeDownloadedTrackDao(tracks), fakeDownloadManager()),
            FakePlayerController(),
        )

        advanceUntilIdle()

        val albums = newViewModel.state.value.downloadedAlbums
        assertEquals(1, albums.size)
        assertEquals("Youth Camp Songs", albums[0].name)
        assertEquals(2, albums[0].trackCount)
    }

    @Test
    fun `groups downloaded tracks sharing a playlistId into one playlist card`() = runTest(testDispatcher) {
        val tracks = listOf(
            DownloadedTrack(
                trackId = 1, title = "Track 1", artist = "Artist 1", fileName = "1.mp3",
                fileSizeBytes = 1_000, playlistId = 7, playlistName = "My Playlist",
            ),
        )
        val newViewModel = DownloadsViewModel(
            DownloadRepository(FakeDownloadedTrackDao(tracks), fakeDownloadManager()),
            FakePlayerController(),
        )

        advanceUntilIdle()

        val playlists = newViewModel.state.value.downloadedPlaylists
        assertEquals(1, playlists.size)
        assertEquals("My Playlist", playlists[0].name)
        assertEquals(1, playlists[0].trackCount)
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
