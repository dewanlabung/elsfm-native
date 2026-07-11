package com.elsfm.mobile.feature.library

import com.elsfm.mobile.core.common.DispatcherProvider
import com.elsfm.mobile.core.database.dao.DownloadedTrackDao
import com.elsfm.mobile.core.database.entity.DownloadedTrack
import com.elsfm.mobile.core.database.repository.DownloadRepository
import com.elsfm.mobile.core.network.download.DownloadManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeDownloadedTrackDao(initial: List<DownloadedTrack> = emptyList()) : DownloadedTrackDao {
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

private class NoopDispatcherProvider(dispatcher: kotlinx.coroutines.CoroutineDispatcher) : DispatcherProvider {
    override val io = dispatcher
    override val main = dispatcher
    override val default = dispatcher
}

internal fun fakeDownloadRepository(): DownloadRepository {
    val httpClient = HttpClient(MockEngine { respond("{}") })
    val downloadManager = DownloadManager(
        context = null,
        httpClient = httpClient,
        dispatcherProvider = NoopDispatcherProvider(Dispatchers.Unconfined),
    )
    return DownloadRepository(FakeDownloadedTrackDao(), downloadManager)
}
