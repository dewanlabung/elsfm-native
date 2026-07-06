package com.elsfm.mobile.feature.downloads

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.model.Track
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeDispatcherProvider(dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher = dispatcher
    override val main: CoroutineDispatcher = dispatcher
    override val default: CoroutineDispatcher = dispatcher
}

internal class FakeDownloadedTrackDao(
    initialTracks: List<DownloadedTrack> = emptyList(),
) : DownloadedTrackDao {
    private val tracksFlow = MutableStateFlow(initialTracks)

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

private class FakeDownloadManager(dispatcher: CoroutineDispatcher) : DownloadManager(
    context = null,
    httpClient = HttpClient(MockEngine { respond("") }),
    dispatcherProvider = FakeDispatcherProvider(dispatcher),
) {
    override suspend fun downloadTrack(track: Track, onProgress: (Float) -> Unit): Result<File> =
        Result.success(File("${track.id}_track.mp3"))

    override suspend fun deleteDownload(trackId: Int): Boolean = true
}

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadViewModelTest {
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadDownloadsSuccess() = runTest(testDispatcher) {
        val dao = FakeDownloadedTrackDao(
            listOf(
                DownloadedTrack(
                    trackId = 1,
                    title = "Track 1",
                    artist = "Artist 1",
                    fileName = "track.mp3",
                    fileSizeBytes = 5_000_000,
                ),
            ),
        )
        val viewModel = DownloadViewModel(
            dao,
            FakeDownloadManager(testDispatcher),
            FakeDispatcherProvider(testDispatcher),
        )

        advanceUntilIdle()

        assertEquals(1, viewModel.state.value.downloadedTracks.size)
    }
}
